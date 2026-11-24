package neowpreview;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.events.RoomEventDialog;
import com.megacrit.cardcrawl.helpers.FontHelper;
import com.megacrit.cardcrawl.helpers.PowerTip;
import com.megacrit.cardcrawl.helpers.TipHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.neow.NeowEvent;
import com.megacrit.cardcrawl.neow.NeowReward;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import com.megacrit.cardcrawl.ui.buttons.LargeDialogOptionButton;
import com.megacrit.cardcrawl.vfx.AbstractGameEffect;
import com.megacrit.cardcrawl.vfx.FastCardObtainEffect;
import com.megacrit.cardcrawl.vfx.cardManip.ShowCardAndObtainEffect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * 涅奥祝福界面的状态机：预告 -> 选择 -> 可撤回。
 */
public final class NeowState {

    public enum Phase {
        /** 不在涅奥祝福界面。 */
        NONE,
        /** 正在展示祝福选项。 */
        CHOOSING,
        /** 祝福已经生效，可以撤回。 */
        APPLIED,
        /** 读档回到“已经领过祝福”的界面，磁盘上有撤回点，可以撤回。 */
        RESTORE_READY
    }

    public static Phase phase = Phase.NONE;
    public static final ArrayList<Preview> previews = new ArrayList<>();
    public static NeowEvent eventRef = null;
    public static Snapshot snapshot = null;
    public static boolean tookSnapshot = false;

    /** 当前这组祝福是“两选项的小型祝福”（撤回后要还原成同一组，不能变成四个）。 */
    public static boolean miniOffered = false;

    /** 撤回后需要作废的“获得卡牌”特效（否则它们稍后仍会把卡塞进卡组）。 */
    private static final Set<AbstractGameEffect> droppedObtains =
            Collections.newSetFromMap(new IdentityHashMap<AbstractGameEffect, Boolean>());

    private NeowState() {
    }

    public static void reset() {
        phase = Phase.NONE;
        eventRef = null;
        snapshot = null;
        tookSnapshot = false;
        miniOffered = false;
        previews.clear();
        droppedObtains.clear();
    }

    /** 当前点击是否发生在本局的涅奥事件上。 */
    public static boolean isActive(NeowEvent event) {
        if (event == null || eventRef != event) {
            return false;
        }
        AbstractRoom room = AbstractDungeon.getCurrRoom();
        return room != null && room.event == event;
    }

    /** blessing() / miniBlessing() 结束时调用：生成每个选项的具体结果预告。 */
    public static void onOffered(NeowEvent event) {
        phase = Phase.CHOOSING;
        eventRef = event;
        snapshot = null;
        tookSnapshot = false;
        previews.clear();
        if (event == null) {
            return;
        }
        Object rewards = Reflect.get(event, "rewards");
        miniOffered = true;
        if (rewards instanceof List) {
            for (Object o : (List<?>) rewards) {
                if (o instanceof NeowReward) {
                    previews.add(NeowPredictor.predict((NeowReward) o));
                }
            }
            // 四个选项就是完整祝福，两个选项是“小型祝福”
            miniOffered = ((List<?>) rewards).size() < 4;
        }
        attachPreviews();
    }

    /** 把预告挂到原版按钮上，鼠标悬停时显示整张牌 / 遗物说明。 */
    private static void attachPreviews() {
        List<LargeDialogOptionButton> buttons = RoomEventDialog.optionList;
        for (int i = 0; i < previews.size() && i < buttons.size(); ++i) {
            Preview preview = previews.get(i);
            LargeDialogOptionButton button = buttons.get(i);
            if (preview.relic != null) {
                Reflect.set(button, "relicToPreview", preview.relic);
            }
            // 卡牌由本模组一次性整组绘制，关掉原版的“只弹一张”预览
            Reflect.set(button, "cardToPreview", null);
        }
    }

    /** 玩家点了某个祝福，即将生效：先存快照（同时写一份到磁盘，SL 之后还能撤回）。 */
    public static void beforeChoice(NeowEvent event) {
        eventRef = event;
        snapshot = new Snapshot();
        snapshot.mini = miniOffered;
        tookSnapshot = true;
        RestorePoint.save(snapshot);
    }

    /**
     * 进入涅奥事件时调用：如果这次是读档回到「涅奥已经领过祝福」的界面，而磁盘上还有本局的撤回点，
     * 就把「撤回祝福并重新选择」那一行加回去。
     */
    public static void onCreated(NeowEvent event) {
        if (event == null || !RestorePoint.exists()) {
            return;
        }
        Object screen = Reflect.get(event, "screenNum");
        if (!(screen instanceof Integer) || ((Integer) screen).intValue() != 99) {
            return;
        }
        phase = Phase.RESTORE_READY;
        eventRef = event;
        snapshot = null;
        tookSnapshot = false;
        previews.clear();
        try {
            if (event.roomEventText != null && RoomEventDialog.optionList.size() == 1) {
                event.roomEventText.addDialogOption(Lang.undoOption());
            }
        } catch (Throwable ignored) {
        }
    }

    /** 读档后点“更改祝福”：从磁盘恢复选祝福前的状态，再按原版流程重新给出祝福列表。 */
    private static boolean undoFromDisk(NeowEvent event) {
        if (event == null) {
            return false;
        }
        Snapshot restored = RestorePoint.restore();
        if (restored == null) {
            return false;
        }
        Boolean mini = vanillaMini(event);
        return regenerate(event, mini == null ? restored.mini : mini.booleanValue());
    }

    /**
     * 该给两个还是四个选项：先看设置，设置没开就交给原版判断。
     *
     * <p>原版 buttonEffect 的规则是：bossCount == 0 且不是测试涅奥时只给两个
     * （bossCount 是构造时按原版逻辑读出来的，整局不会变），否则给四个。</p>
     *
     * @return null 表示拿不到原版的值
     */
    private static Boolean vanillaMini(NeowEvent event) {
        if (Config.alwaysFour()) {
            return Boolean.FALSE;
        }
        Object bossCount = Reflect.get(event, "bossCount");
        if (!(bossCount instanceof Integer)) {
            return null;
        }
        return Boolean.valueOf(!Settings.isTestingNeow && ((Integer) bossCount).intValue() == 0);
    }

    /**
     * 清掉现有的祝福列表，按原版方法重新生成一组（原版方法自己会补齐对话框选项与相关状态）。
     *
     * @return true 表示已经重新生成
     */
    @SuppressWarnings("unchecked")
    private static boolean regenerate(NeowEvent event, boolean mini) {
        Object rewardsObj = Reflect.get(event, "rewards");
        if (!(rewardsObj instanceof List)) {
            return false;
        }
        List<Object> rewards = (List<Object>) rewardsObj;
        List<Object> backup = new ArrayList<>(rewards);
        rewards.clear();
        if (!Reflect.call(event, mini ? "miniBlessing" : "blessing")) {
            rewards.addAll(backup);
            return false;
        }
        NeowEvent.waitingToSave = true;
        clearMetrics();
        RoomEventDialog.waitForInput = true;
        if (phase != Phase.CHOOSING) {
            onOffered(event);
        }
        return true;
    }

    /** 祝福已经生效：增加“撤回”选项。 */
    public static void afterChoice(NeowEvent event) {
        if (!tookSnapshot) {
            return;
        }
        tookSnapshot = false;
        phase = Phase.APPLIED;
        if (event != null && event.roomEventText != null) {
            try {
                event.roomEventText.addDialogOption(Lang.undoOption());
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * 处理“撤回”那一行的点击。
     *
     * @return true 表示已经处理完毕，跳过原版 buttonEffect
     */
    public static boolean handleUndoClick(NeowEvent event, int buttonPressed) {
        if (phase == Phase.RESTORE_READY) {
            if (buttonPressed == 1 && RoomEventDialog.optionList.size() >= 2) {
                return undoFromDisk(event);
            }
            return false;
        }
        if (phase != Phase.APPLIED) {
            return false;
        }
        if (buttonPressed == 1 && RoomEventDialog.optionList.size() >= 2) {
            undo(event);
            return true;
        }
        // 玩家直接离开，放弃撤回机会
        phase = Phase.NONE;
        previews.clear();
        snapshot = null;
        return false;
    }

    private static void clearMetrics() {
        try {
            Object metrics = CardCrawlGame.metricData;
            Reflect.set(metrics, "neowBonus", null);
            Reflect.set(metrics, "neowCost", null);
        } catch (Throwable ignored) {
        }
    }

    private static void undo(NeowEvent event) {
        if (event == null || snapshot == null) {
            phase = Phase.NONE;
            return;
        }
        markDroppedObtains();
        snapshot.restore();
        snapshot = null;

        Object rewards = Reflect.get(event, "rewards");
        int rewardCount = rewards instanceof List ? ((List<?>) rewards).size() : 0;
        // 选项数量不按快照，按原版现在的判断（设置里开了“始终四个选项”就是四个）
        Boolean mini = vanillaMini(event);
        if (mini != null) {
            int want = mini.booleanValue() ? 2 : 4;
            if (rewardCount != want && regenerate(event, mini.booleanValue())) {
                return;
            }
        }
        if (rewards instanceof List) {
            for (Object o : (List<?>) rewards) {
                if (o instanceof NeowReward) {
                    Reflect.set(o, "activated", Boolean.FALSE);
                }
            }
        }

        Reflect.set(event, "screenNum", Integer.valueOf(3));
        NeowEvent.waitingToSave = true;
        try {
            Object metrics = CardCrawlGame.metricData;
            Reflect.set(metrics, "neowBonus", null);
            Reflect.set(metrics, "neowCost", null);
        } catch (Throwable ignored) {
        }

        RoomEventDialog dialog = event.roomEventText;
        if (dialog != null && rewards instanceof List) {
            List<?> list = (List<?>) rewards;
            dialog.clearRemainingOptions();
            if (!list.isEmpty() && list.get(0) instanceof NeowReward) {
                dialog.updateDialogOption(0, ((NeowReward) list.get(0)).optionLabel);
            }
            for (int i = 1; i < list.size(); ++i) {
                if (list.get(i) instanceof NeowReward) {
                    dialog.addDialogOption(((NeowReward) list.get(i)).optionLabel);
                }
            }
            RoomEventDialog.waitForInput = true;
            attachPreviews();
        }
        phase = Phase.CHOOSING;
    }

    private static void markDroppedObtains() {
        scanDropped(AbstractDungeon.topLevelEffects);
        scanDropped(AbstractDungeon.topLevelEffectsQueue);
        scanDropped(AbstractDungeon.effectList);
        scanDropped(AbstractDungeon.effectsQueue);
    }

    private static void scanDropped(ArrayList<AbstractGameEffect> effects) {
        if (effects == null) {
            return;
        }
        for (AbstractGameEffect e : effects) {
            if (e instanceof ShowCardAndObtainEffect || e instanceof FastCardObtainEffect) {
                droppedObtains.add(e);
            }
        }
    }

    public static boolean isDropped(AbstractGameEffect effect) {
        return effect != null && droppedObtains.contains(effect);
    }

    /** 在事件对话框渲染完成后画出每个选项的具体结果。 */
    public static void renderPreviews(SpriteBatch sb) {
        if (phase != Phase.CHOOSING || previews.isEmpty() || eventRef == null) {
            return;
        }
        AbstractRoom room = AbstractDungeon.getCurrRoom();
        if (room == null || room.event != eventRef || AbstractDungeon.isScreenUp) {
            return;
        }
        List<LargeDialogOptionButton> buttons = RoomEventDialog.optionList;
        if (buttons.size() != previews.size()) {
            return;
        }
        for (int i = 0; i < previews.size(); ++i) {
            LargeDialogOptionButton button = buttons.get(i);
            if (button == null || button.hb == null || button.hb.y < 0.0f || !button.hb.hovered) {
                continue;
            }
            renderPreview(sb, previews.get(i), button);
        }
    }

    private static void renderPreview(SpriteBatch sb, Preview preview, LargeDialogOptionButton button) {
        if (preview == null) {
            return;
        }
        float imageScale = Config.imageScale();
        float right = button.hb.x + 445.0f * Settings.xScale + 24.0f * Settings.scale;
        float centerY = button.hb.y;
        float gap = 12.0f * Settings.scale;
        float cardW = AbstractCard.IMG_WIDTH * imageScale;
        float cardH = AbstractCard.IMG_HEIGHT * imageScale;

        // 药水用原版的提示框（首行名字、次行效果），和原版事件里的遗物预览是同一个机制
        if (preview.cards.isEmpty() && !preview.potions.isEmpty()) {
            queuePotionTips(preview.potions);
        }

        // 先算出卡牌占多宽，再把“代价诅咒”一起挤进屏幕
        float mainWidth = 0.0f;
        if (!preview.cards.isEmpty()) {
            int count = preview.cards.size();
            mainWidth = count * cardW + (count - 1) * gap;
        }
        float curseWidth = preview.curse != null ? gap + cardW : 0.0f;
        float total = mainWidth + curseWidth;
        float left = right;
        if (left + total > Settings.WIDTH - 16.0f * Settings.scale) {
            left = Settings.WIDTH - 16.0f * Settings.scale - total;
        }
        if (left < 16.0f * Settings.scale) {
            left = 16.0f * Settings.scale;
        }
        float cy = clampY(centerY, cardH);
        sb.setColor(Color.WHITE);

        if (!preview.cards.isEmpty()) {
            // 卡牌一次画出实际会出现的那几张
            int count = preview.cards.size();
            for (int i = 0; i < count; ++i) {
                drawCard(sb, preview.cards.get(i), left + cardW * 0.5f + i * (cardW + gap), cy, imageScale);
            }
        }

        // 代价是随机诅咒时，把具体是哪张诅咒也画出来
        if (preview.curse != null) {
            drawCard(sb, preview.curse, left + mainWidth + gap + cardW * 0.5f, cy, imageScale);
        }
    }

    private static void drawCard(SpriteBatch sb, AbstractCard card, float x, float y, float scale) {
        if (card == null) {
            return;
        }
        card.current_x = x;
        card.current_y = y;
        card.drawScale = scale;
        card.targetDrawScale = scale;
        card.render(sb);
    }

    /**
     * 药水用原版的提示框：首行药水名字、次行效果说明。
     *
     * <p>和原版事件里 LargeDialogOptionButton.renderRelicPreview 显示遗物详情用的是同一个机制
     * （TipHelper.queuePowerTips，由 CardCrawlGame.render 统一画出来）。三瓶就排三个提示框。</p>
     */
    private static void queuePotionTips(List<AbstractPotion> potions) {
        ArrayList<PowerTip> tips = new ArrayList<>();
        for (AbstractPotion potion : potions) {
            if (potion == null) {
                continue;
            }
            tips.add(new PowerTip(potion.name, potion.description));
        }
        if (tips.isEmpty()) {
            return;
        }
        float y = (float) InputHelper.mY + TipHelper.calculateToAvoidOffscreen(tips, InputHelper.mY);
        TipHelper.queuePowerTips(470.0f * Settings.scale, y, tips);
    }

    private static float clampY(float y, float height) {
        float half = height * 0.5f + 12.0f * Settings.scale;
        float max = Settings.HEIGHT - half;
        if (max < half) {
            return Settings.HEIGHT * 0.5f;
        }
        return Math.max(half, Math.min(max, y));
    }
}
