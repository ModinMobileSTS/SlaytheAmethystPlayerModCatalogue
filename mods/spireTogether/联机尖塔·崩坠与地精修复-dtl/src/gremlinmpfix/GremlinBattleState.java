package gremlinmpfix;

import basemod.interfaces.PostBattleSubscriber;
import com.megacrit.cardcrawl.actions.AbstractGameAction;
import com.megacrit.cardcrawl.actions.defect.ChannelAction;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.orbs.AbstractOrb;
import com.megacrit.cardcrawl.orbs.EmptyOrbSlot;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spireTogether.SpireTogetherMod;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class GremlinBattleState {
    private static final String CHARACTER = "gremlin.characters.GremlinCharacter";
    private static final String SUBSCRIBER = "gremlin.GremlinMod";
    private static final Logger LOG = LogManager.getLogger(GremlinBattleState.class);
    private static OpeningLineup opening;
    private static boolean warned;

    private GremlinBattleState() {}

    // 只记录开场还在排队的后排地精，不从旧存档猜测战斗中失去的成员。
    static void capture(AbstractRoom room) {
        opening = null;
        AbstractPlayer player = AbstractDungeon.player;
        if (!isLocalSquad(player) || AbstractDungeon.actionManager == null) return;
        Bindings b = OptionalDownfall.API;
        if (b == null) return;
        try {
            Object state = b.mobState.get(player);
            if (state == null || !b.inCombat.getBoolean(state)) return;
            OpeningLineup lineup = new OpeningLineup(room, player, state);
            for (AbstractGameAction action : AbstractDungeon.actionManager.actions) {
                if (action == null || action.getClass() != ChannelAction.class) continue;
                Object orb = b.channelOrb.get(action);
                if (b.standby.isInstance(orb) && !player.orbs.contains(orb)) {
                    lineup.pending.add((AbstractOrb) orb);
                }
            }
            if (!lineup.pending.isEmpty()) opening = lineup;
        } catch (ReflectiveOperationException | RuntimeException e) {
            warn(e);
        }
    }

    static void onChannelUpdated(ChannelAction action) {
        OpeningLineup lineup = opening;
        if (lineup == null) return;
        AbstractPlayer player = AbstractDungeon.player;
        if (!SpireTogetherMod.isConnected || player != lineup.owner.get()) {
            opening = null;
            return;
        }
        // 已经登场的成员立即退出保护：之后真的阵亡也不补回。
        lineup.pending.removeIf(orb -> player.orbs.contains(orb));
        if (lineup.pending.isEmpty()) opening = null;
    }

    public static void settle(PostBattleSubscriber subscriber, AbstractRoom room) {
        AbstractPlayer player = AbstractDungeon.player;
        if (!SUBSCRIBER.equals(subscriber.getClass().getName()) || !isLocalSquad(player)) {
            subscriber.receivePostBattle(room);
            return;
        }
        Bindings b = OptionalDownfall.API;
        if (b == null) {
            subscriber.receivePostBattle(room);
            return;
        }
        ArrayList<AbstractOrb> orbs = player.orbs;
        List<Slot> borrowed = new ArrayList<>();
        OpeningLineup lineup = opening;
        opening = null;
        try {
            Object state = b.mobState.get(player);
            if (state != null && !b.inCombat.getBoolean(state)) {
                // 未开始本地战斗（或已经结算过）：后排的血量表仍是有效记录。
                // 补记正常的前排血量变动，不从尚未生成的后排栏位重建小队。
                List<?> names = (List<?>) b.names.get(state);
                if (names != null && !names.isEmpty()
                        && names.get(0).equals(b.front.get(player))) {
                    @SuppressWarnings("unchecked")
                    List<Integer> hp = (List<Integer>) b.health.get(state);
                    if (hp != null && !hp.isEmpty()) hp.set(0, Math.max(0, player.currentHealth));
                }
                return;
            }
            if (lineup != null && room == lineup.room.get() && player == lineup.owner.get()
                    && state == lineup.state.get()) {
                fillForSettlement(b, state, player, lineup.pending, borrowed);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            warn(e);
        }
        try {
            subscriber.receivePostBattle(room);
        } finally {
            // 只补充结算读到的数据，不触发充能球事件，也不在战后留下额外地精。
            for (Slot slot : borrowed) {
                if (slot.index < orbs.size() && orbs.get(slot.index) == slot.pending) {
                    orbs.set(slot.index, slot.original);
                }
            }
        }
    }

    private static void fillForSettlement(Bindings b, Object state, AbstractPlayer player,
                                          List<AbstractOrb> pending, List<Slot> borrowed)
            throws ReflectiveOperationException {
        int limit = Math.min(player.maxOrbs, player.orbs.size());
        Set<String> present = new HashSet<>();
        present.add((String) b.front.get(player));
        for (int i = 0; i < limit; i++) {
            AbstractOrb orb = player.orbs.get(i);
            if (b.standby.isInstance(orb)) present.add((String) b.assetFolder.get(orb));
        }
        List<?> names = (List<?>) b.names.get(state);
        int next = 0;
        for (AbstractOrb orb : pending) {
            String name = (String) b.assetFolder.get(orb);
            if (name == null || present.contains(name) || !names.contains(name)
                    || b.orbHealth.getInt(orb) <= 0 || (Boolean) b.enslaved.invoke(state, name)) continue;
            while (next < limit && !(player.orbs.get(next) instanceof EmptyOrbSlot)) next++;
            if (next >= limit) break;
            borrowed.add(new Slot(next, player.orbs.get(next), orb));
            player.orbs.set(next++, orb);
            present.add(name);
        }
    }

    private static boolean isLocalSquad(AbstractPlayer player) {
        return SpireTogetherMod.isConnected && player != null && CHARACTER.equals(player.getClass().getName());
    }

    private static void warn(Exception e) {
        if (!warned) {
            warned = true;
            LOG.warn("地精小队联机血量修复：无法读取当前版本的地精状态。", e);
        }
    }

    private static final class OpeningLineup {
        final WeakReference<AbstractRoom> room;
        final WeakReference<AbstractPlayer> owner;
        final WeakReference<Object> state;
        final List<AbstractOrb> pending = new ArrayList<>(4);

        OpeningLineup(AbstractRoom room, AbstractPlayer owner, Object state) {
            this.room = new WeakReference<>(room);
            this.owner = new WeakReference<>(owner);
            this.state = new WeakReference<>(state);
        }
    }

    private static final class Slot {
        final int index;
        final AbstractOrb original;
        final AbstractOrb pending;

        Slot(int index, AbstractOrb original, AbstractOrb pending) {
            this.index = index;
            this.original = original;
            this.pending = pending;
        }
    }

    private static final class OptionalDownfall {
        static final Bindings API = loadBindings();

        private static Bindings loadBindings() {
            try {
                return new Bindings();
            } catch (ReflectiveOperationException | RuntimeException e) {
                warn(e);
                return null;
            }
        }
    }

    private static final class Bindings {
        final Field mobState;
        final Field front;
        final Field inCombat;
        final Field names;
        final Field health;
        final Class<?> standby;
        final Field assetFolder;
        final Field orbHealth;
        final Method enslaved;
        final Field channelOrb;

        Bindings() throws ReflectiveOperationException {
            ClassLoader loader = AbstractDungeon.player.getClass().getClassLoader();
            Class<?> character = Class.forName(CHARACTER, false, loader);
            mobState = character.getField("mobState");
            front = character.getField("currentGremlin");
            Class<?> state = mobState.getType();
            inCombat = state.getField("inCombat");
            names = state.getField("gremlins");
            health = state.getField("gremlinHP");
            enslaved = state.getMethod("isEnslaved", String.class);
            standby = Class.forName("gremlin.orbs.GremlinStandby", false, loader);
            assetFolder = standby.getField("assetFolder");
            orbHealth = standby.getField("hp");
            channelOrb = ChannelAction.class.getDeclaredField("orbType");
            channelOrb.setAccessible(true);
        }
    }
}
