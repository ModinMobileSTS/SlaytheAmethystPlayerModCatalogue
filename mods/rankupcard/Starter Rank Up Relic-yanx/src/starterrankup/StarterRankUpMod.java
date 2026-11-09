package starterrankup;

import basemod.BaseMod;
import basemod.interfaces.PostCreateStartingRelicsSubscriber;
import basemod.interfaces.PostInitializeSubscriber;
import basemod.interfaces.StartActSubscriber;
import com.evacipated.cardcrawl.modthespire.lib.SpireConfig;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;

import java.util.ArrayList;
import java.util.Properties;

/**
 * ============================================================================
 *  Starter Rank Up Relic  ——  开局自带指定遗物（通用小工具）
 * ============================================================================
 *
 * 这个补丁模组本身不添加任何遗物、图片或文案。它只做两件事：
 *
 *   1) 在「创建初始遗物」之后，把一个遗物的字符串 ID 追加到初始遗物列表里
 *      —— 于是所有角色（原版四职业 + 其他模组的自定义角色）开局都会带上它。
 *
 *   2) 如果玩家在第 1 章就拥有这个遗物，就把它从第 1 章的遗物掉落池里剔除，
 *      避免同一个遗物在一局里被拿到两次；第 2 章起游戏会重建池子，它恢复为正常掉落。
 *
 * 默认目标遗物：Rank Up Cards（modid: rankupcard）的 "Card Rank Up!" 遗物
 *              其字符串 ID 为 rankupcard:RankUp
 *
 *
 * ----------------------------------------------------------------------------
 *  ★ 可以自定义的地方（按需要改下面这段「配置区」即可，其余代码无需改动）★
 * ----------------------------------------------------------------------------
 *
 *  CUSTOM-1  目标遗物 ID  ------- TARGET_RELIC_ID
 *            想换成别的模组 / 原版的遗物，只改这一行。
 *            格式必须是 "<modid>:<遗物ID>"，冒号两边不能有空格。
 *            例：
 *              "rankupcard:RankUp"                  Rank Up Cards 的合并遗物（默认）
 *              "mymod:MyRelic"                      其它模组的遗物
 *              "Burning Blood"                      原版遗物一般没有 modid 前缀
 *            注意：如果目标遗物在别的模组里，请同时修改 ModTheSpire.json 的 dependencies，
 *            把那个模组的 modid 加进去（大小写必须与其 ModTheSpire.json 完全一致）。
 *
 *            ⚠ 不要去掉 "rankupcard:" 前缀。BaseMod 的自定义遗物 ID 一律是
 *            "<modid>:<ID>"，且父模组的 RelicStrings.json 用的就是带前缀的键；
 *            写成 "RankUp" 会让父模组 BaseRelic.testStrings() 直接抛异常。
 *
 *  CUSTOM-2  是否开局赠送 --- GIVE_AT_START_DEFAULT
 *            false 则完全不注入，只保留"池子剔除"逻辑。
 *
 *  CUSTOM-3  是否剔除掉落池 - REMOVE_FROM_POOLS_DEFAULT
 *            第 1 章拥有该遗物时，是否把它从掉落池移除。
 *
 *  CUSTOM-4  只对部分角色  --- ACTIVE_PLAYER_CLASSES
 *            默认 null = 全部角色（含自定义角色）都生效。
 *            想限定职业，就填入 AbstractPlayer.PlayerClass 常量，例如：
 *              new AbstractPlayer.PlayerClass[]{
 *                  AbstractPlayer.PlayerClass.IRONCLAD,
 *                  AbstractPlayer.PlayerClass.THE_SILENT
 *              }
 *            自定义角色的职业枚举需要用 @SpireEnum 定义后才能在这里引用。
 *
 *  CUSTOM-5  剔除哪些池子  --- 见 removeFromActOnePools() 方法内的注释，
 *            可以单独注释掉 shopRelicPool / bossRelicPool 等。
 *
 *  运行时配置（不需要重新编译）：
 *            ModTheSpire 配置目录下会生成 starterrankup/config，
 *            里面有 GiveAtStart 和 RemoveFromPoolsWhileOwned 两个布尔值，
 *            改成 false 即可在不动代码的情况下关闭对应行为。
 * ============================================================================
 */
@SpireInitializer
public class StarterRankUpMod implements PostCreateStartingRelicsSubscriber,
        StartActSubscriber,
        PostInitializeSubscriber {

    // ========================================================================
    // 配置区：日常想改的东西都在这里
    // ========================================================================

    /** 日志前缀 / 配置文件里的模组标识，一般不用改。 */
    public static final String MOD_ID = "starterrankup";

    /** ★ CUSTOM-1：目标遗物的字符串 ID（"<modid>:<遗物ID>"，必须带冒号前缀）。 */
    public static final String TARGET_RELIC_ID = "rankupcard:RankUp";

    /** ★ CUSTOM-2：默认是否开局赠送（可被配置文件覆盖）。 */
    private static final boolean GIVE_AT_START_DEFAULT = true;

    /** ★ CUSTOM-3：默认是否在拥有时剔除掉落池（可被配置文件覆盖）。 */
    private static final boolean REMOVE_FROM_POOLS_DEFAULT = true;

    /**
     * ★ CUSTOM-4：生效的职业白名单。
     * null 表示所有角色都生效（推荐，包含自定义角色）。
     * 想限定职业就换成数组，见文件头注释。
     */
    private static final AbstractPlayer.PlayerClass[] ACTIVE_PLAYER_CLASSES = null;

    // ========================================================================
    // 以下为实现，通常不需要改动
    // ========================================================================

    private static final String CONFIG_FILE = "starterrankup";
    private static final String CONFIG_KEY_GIVE = "GiveAtStart";
    private static final String CONFIG_KEY_REMOVE = "RemoveFromPoolsWhileOwned";

    private static SpireConfig config;
    private static boolean giveAtStart = GIVE_AT_START_DEFAULT;
    private static boolean removeFromPool = REMOVE_FROM_POOLS_DEFAULT;

    /** ModTheSpire 的 @SpireInitializer 入口，必须叫这个名字。 */
    public static void initialize() {
        loadConfig();
        new StarterRankUpMod();
    }

    public StarterRankUpMod() {
        // 只在这里订阅一次；务必不要在其他回调里再 new 本类，否则会出现重复订阅。
        BaseMod.subscribe(this);
    }

    // ------------------------------------------------------------------ 配置

    private static void loadConfig() {
        try {
            Properties defaults = new Properties();
            defaults.setProperty(CONFIG_KEY_GIVE, Boolean.toString(GIVE_AT_START_DEFAULT));
            defaults.setProperty(CONFIG_KEY_REMOVE, Boolean.toString(REMOVE_FROM_POOLS_DEFAULT));
            config = new SpireConfig(CONFIG_FILE, "config", defaults);
            config.load();
            giveAtStart = config.getBool(CONFIG_KEY_GIVE);
            removeFromPool = config.getBool(CONFIG_KEY_REMOVE);
        } catch (Exception e) {
            // 配置读不出来也不能让游戏起不来，退回默认值。
            config = null;
            giveAtStart = GIVE_AT_START_DEFAULT;
            removeFromPool = REMOVE_FROM_POOLS_DEFAULT;
        }
    }

    private static void saveConfig() {
        if (config == null) {
            return;
        }
        config.setBool(CONFIG_KEY_GIVE, giveAtStart);
        config.setBool(CONFIG_KEY_REMOVE, removeFromPool);
        try {
            config.save();
        } catch (Exception ignored) {
            // 写盘失败不影响游戏进程。
        }
    }

    public static boolean isGiveAtStart() {
        return giveAtStart;
    }

    /** 运行期开关：开局赠送。 */
    public static void setGiveAtStart(boolean value) {
        giveAtStart = value;
        saveConfig();
    }

    public static boolean isRemoveFromPool() {
        return removeFromPool;
    }

    /** 运行期开关：剔除掉落池。 */
    public static void setRemoveFromPool(boolean value) {
        removeFromPool = value;
        saveConfig();
    }

    // -------------------------------------------------------------- BaseMod 钩子

    /**
     * 所有内容注册完成之后调用一次。
     * 这里只做一次"目标遗物是否存在"的体检，缺了就打印提示而不是崩溃。
     */
    @Override
    public void receivePostInitialize() {
        if (!targetRelicExists()) {
            System.out.println("[" + MOD_ID + "] 遗物 " + TARGET_RELIC_ID
                    + " 未被注册 —— 目标模组是否已安装并启用？本补丁将不做任何改动。");
        }
    }

    /**
     * 「初始遗物列表已生成」之后调用，参数 relicPool 就是玩家即将获得的初始遗物 ID 列表。
     *
     * 注意：这个时机 AbstractDungeon.player 尚未拥有任何遗物（此刻只是字符串列表），
     * 所以这里不能也不需要用 player.hasRelic() 去判断，只比对列表内容即可。
     *
     * 这里不 new 遗物实例、不改贴图，只往列表尾部追加一个字符串 ID，
     * 所以原有的初始遗物（燃烧之血等）与其顺序都不会被动到。
     */
    @Override
    public void receivePostCreateStartingRelics(AbstractPlayer.PlayerClass chosenClass,
                                                ArrayList<String> relicPool) {
        if (!giveAtStart || relicPool == null) {
            return;
        }
        if (!isPlayerClassAllowed(chosenClass)) {
            return;
        }
        // 该角色本来就自带（写在 getStartingRelics() 里）：不重复添加，也不替换。
        if (relicPool.contains(TARGET_RELIC_ID)) {
            return;
        }
        // 遗物根本没被注册（目标模组缺失）：跳过，绝不抛异常。
        if (!targetRelicExists()) {
            return;
        }

        relicPool.add(TARGET_RELIC_ID);
    }

    /**
     * 每次进入新章节时调用（第 1 章在开局后也会触发）。
     * 只处理第 1 章：此时普通/精英/宝箱/商店/第 1 章 Boss 的掉落都来自开局前建好的池子。
     *
     * 这里不依赖任何"本局是否由我注入"的内存标记，所以存档退出再读档进第 1 章时，
     * 依然能正确地把池子清理干净（读档后静态变量会被重置，用标记反而不可靠）。
     *
     * ⚠ 不要改成 actNum == 1：第 1 章（Exordium）的 actNum 是 0，== 1 会跳过整个第 1 章。
     */
    @Override
    public void receiveStartAct() {
        if (!removeFromPool) {
            return;
        }
        AbstractPlayer player = AbstractDungeon.player;
        // 第 1 章（actNum 0/1）且玩家已持有目标遗物时清理池子。
        if (player != null
                && AbstractDungeon.actNum <= 1
                && player.hasRelic(TARGET_RELIC_ID)) {
            removeFromActOnePools();
        }
    }

    // ------------------------------------------------------------------ 工具

    private static boolean isPlayerClassAllowed(AbstractPlayer.PlayerClass chosenClass) {
        if (ACTIVE_PLAYER_CLASSES == null) {
            return true; // 默认对所有角色生效，包括自定义角色
        }
        for (AbstractPlayer.PlayerClass allowed : ACTIVE_PLAYER_CLASSES) {
            if (allowed == chosenClass) {
                return true;
            }
        }
        return false;
    }

    /**
     * 目标遗物是否真的被注册了。
     *
     * 这里刻意用 languagePack.getRelicStrings() 而不是 RelicLibrary.getRelic()：
     * RelicLibrary.getRelic(id) 内部是 sharedRelics.get(id).relicId.equals(...)，
     * 目标模组缺失时会直接抛 NullPointerException，用它做存在性检查等于没有保护。
     * getRelicStrings 是纯字典查表，查不到只返回 null，绝对安全。
     */
    private static boolean targetRelicExists() {
        return CardCrawlGame.languagePack != null
                && CardCrawlGame.languagePack.getRelicStrings(TARGET_RELIC_ID) != null;
    }

    /**
     * ★ CUSTOM-5：剔除哪些掉落池。
     * 注释掉某一行即表示"该池子仍可正常刷出这个遗物"。
     */
    private static void removeFromActOnePools() {
        removeFrom(AbstractDungeon.commonRelicPool);    // 普通战斗 / 宝箱
        removeFrom(AbstractDungeon.uncommonRelicPool);  // 普通战斗 / 宝箱
        removeFrom(AbstractDungeon.rareRelicPool);      // 普通战斗 / 宝箱
        removeFrom(AbstractDungeon.shopRelicPool);      // 商店
        removeFrom(AbstractDungeon.bossRelicPool);      // 第 1 章末尾 Boss 遗物三选一
    }

    /** 移除池中所有同名 ID；ID 不存在也不报错。 */
    private static void removeFrom(ArrayList<String> pool) {
        if (pool == null) {
            return;
        }
        while (pool.remove(TARGET_RELIC_ID)) {
            // 池子里可能有重复条目，全部清掉
        }
    }
}
