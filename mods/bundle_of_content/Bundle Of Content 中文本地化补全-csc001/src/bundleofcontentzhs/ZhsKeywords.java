package bundleofcontentzhs;

import basemod.BaseMod;
import bundle_of_content.BundleOfContent;

/**
 * Re-registers Bundle Of Content's keywords with Chinese text.
 *
 * <p>{@code BaseMod.addKeyword} keys its maps by "modid:name", so registering the same English
 * NAMES again simply replaces the proper name and description that the parent mod loaded from its
 * own (partially untranslated) zhs keyword file.</p>
 */
public final class ZhsKeywords {

    private ZhsKeywords() {
    }

    private static final Object[][] KEYWORDS = {
            {"Fascinating Fleet",
                    new String[]{"Fascinating Fleet", "Fascinating_Fleet", "fascinating fleet", "fascinating_fleet"},
                    "致敬 Call of Fleet 的遗物，他创作了《废墟图书馆》与 Elena 等诸多热门模组。"},
            {"Pitted Paparatto",
                    new String[]{"Pitted Paparatto", "Pitted_Paparatto", "pitted paparatto", "pitted_paparatto"},
                    "致敬模组作者兼 Twitch 主播 Paparatto 的遗物，他创造了我最爱的搞笑角色“牛油果”。"},
            {"Auty Albino",
                    new String[]{"Auty Albino", "Auty_Albino", "auty albino", "auty_albino"},
                    "致敬 albino 的遗物，他是一位杂谈主播，也是魔改肉鸽游戏的高手。"},
            {"Furry Frost",
                    new String[]{"Furry Frost", "Furry_Frost", "furry frost", "furry_frost"},
                    "致敬 FrostPrime_ 的遗物，他是一位包容友善的《杀戮尖塔》Twitch 主播。"},
            {"Webbed Wavewhyse",
                    new String[]{"Webbed Wavewhyse", "Webbed_Wavewhyse", "webbed wavewhyse", "webbed_wavewhyse"},
                    "致敬 Wavewhyse 的遗物，他热情地创作了角色模组“寡妇”与“连线者”。"},
            {"Tidy Tldyl",
                    new String[]{"Tidy Tldyl", "Tidy_Tldyl", "tidy tldyl", "tidy_tldyl"},
                    "致敬 Tldyl 的遗物，他是资深模组作者，创作了 Zehhyo、替罪羊与 Shion 等诸多优秀角色模组。"},
            {"Dope Diamsword",
                    new String[]{"Dope Diamsword", "Dope_Diamsword", "dope diamsword", "dope_diamsword"},
                    "致敬 Diamsword 的遗物，他创作了 Slaytabase 最爱的角色“坚果哑铃”。"},
            {"焦油捕网",
                    new String[]{"Web Tarball", "Web_Tarball", "web tarball", "web_tarball"},
                    "如果你拥有遗物 #y黏糊焦油团 ，你每回合只会获得 #r1 点能量。"},
            {"异蛇之视",
                    new String[]{"Snecko Eye", "Snecko_Eye", "snecko eye", "snecko_eye"},
                    "如果你拥有遗物 #y异蛇之眼 ，使你的手牌上限翻倍，并使“第二只眼”的抽牌效果翻倍。来自“第二只眼”的 #y中毒 提高 1 点。"},
            {"Evaluating Evab",
                    new String[]{"Evaluating Evab", "Evaluating_Evab", "evaluating evab", "evaluating_evab"},
                    "致敬 Evab 的遗物，他是 YouTube 系列节目《守卫对局分析》的作者。"},
            {"无瑕者之致意",
                    new String[]{"Champion Of Brace", "Champion_Of_Brace", "champion of brace", "champion_of_brace"},
                    "只有在进阶 20 完成“一封小信”的任务后，该遗物才会自然出现。"},
            {"已讨伐：皇家守卫",
                    new String[]{"Slayer Of Brace", "Slayer_Of_Brace", "slayer of brace", "slayer_of_brace"},
                    "只有在完成“一封小信”的任务后，该遗物才会自然出现。"},
            {"Cakey Chocolatecake",
                    new String[]{"Cakey Chocolatecake", "Cakey_Chocolatecake", "cakey chocolatecake", "cakey_chocolatecake"},
                    "致敬 Chocolatecake 的遗物，他是角色模组“缠绕者”的创作者。"},
            {"海洋",
                    new String[]{"Ocean", "ocean"},
                    "把海洋重新装回罐子里 >:) 。"},
            {"Dev Demi",
                    new String[]{"Dev Demi", "Dev_Demi", "dev demi", "dev_demi"},
                    "本作开发商 Megacrit 的公关经理！"},
            {"Cool Casey",
                    new String[]{"Cool Casey", "Cool_Casey", "cool casey", "cool_casey"},
                    "本作的初始开发者之一！"},
            {"Mad Maddii",
                    new String[]{"Mad Maddii", "Mad_Maddii", "mad Maddii", "mad_Maddii"},
                    "致敬 Maddii 的遗物，她是一位“体验优化”模组作者，也创作了许多邪门模组。"},
            {"Meticulous Monty",
                    new String[]{"Meticulous Monty", "Meticulous_Monty", "meticulous monty", "meticulous_monty"},
                    "致敬 Monty 的遗物，他是角色模组“棱彩”的创作者。"},
            {"Dapper Draco",
                    new String[]{"Dapper Draco", "Dapper_Draco", "dapper draco", "dapper_draco"},
                    "致敬 Draco 的遗物，他是联机模组 Together in Spire 的主要开发者。"},
            {"Big Blue",
                    new String[]{"Big_Blue", "Big Blue", "big_blue", "big blue"},
                    "致敬 Blue Guy 的遗物，他是 Downfall 的维护者，创作了觉醒者，并重做了 Boss 池、异蛇与六火亡魂等内容。"},
            {"Allstar Alex",
                    new String[]{"Allstar_Alex", "Allstar Alex", "allstar_Alex", "allstar Alex"},
                    "致敬 Alex Mdle 的遗物，他是角色模组“隐士”的创作者，也参与了《Providence 之星》与《三角符文》等独立游戏的开发。"},
            {"Mayhem Micheal",
                    new String[]{"Mayhem_Micheal", "Mayhem Micheal", "mayhem_micheal", "mayhem micheal"},
                    "致敬 Micheal 的遗物，他是 Downfall 的创作者、Vex 的开发伙伴，两人后来一同参与制作了独立游戏《Tales and Tactics》。"},
            {"Oceanic OccultPyromancer",
                    new String[]{"Oceanic OccultPyromancer", "Oceanic_OccultPyromancer", "oceanic occultPyromancer", "oceanic_occultPyromancer"},
                    "致敬 OccultPyromancer 的遗物，他是 Downfall 的贡献者，参与了觉醒者等多个项目。"},
            {"Opulent Opal",
                    new String[]{"Opulent_Opal", "Opulent Opal", "opulent_opal", "opulent opal"},
                    "致敬 Opal 的遗物，她是 Downfall 的主要美术贡献者，绘制了该模组大量角色卡图。"},
            {"Sunny Soylar",
                    new String[]{"Sunny_Soylar", "Sunny Soylar", "sunny_soylar", "sunny soylar"},
                    "致敬 Soylar 的遗物，他是角色模组“研究者”的作者，并且沉迷卡牌模组。"},
            {"Artistic Aurora",
                    new String[]{"Artistic_Aurora", "Artistic Aurora", "artistic_aurora", "artistic aurora"},
                    "致敬 Aurora 的遗物，她是角色模组“研究者”的画师。"},
            {"Novel Neuvottelut",
                    new String[]{"Novel_Neuvottelut", "Novel Neuvottelut", "novel_neuvottelut", "novel neuvottelut"},
                    "致敬 Neuvottelut 的遗物，他是金牌角色模组“猪”的作者。"},
            {"Keen Kobe",
                    new String[]{"Keen_Kobe", "Keen Kobe", "keen_kobe", "keen kobe"},
                    "致敬 Kobe 的遗物，他创作了一系列补给包模组，每个角色都有一个！"},
            {"Courteous Carbon",
                    new String[]{"Courteous_Carbon", "Courteous Carbon", "courteous_carbon", "courteous carbon"},
                    "致敬 Carbon 的遗物，他是 Downfall Discord 服务器的管理员，并为多次测试版美术募集做出了贡献。"}
    };

    /** Called after the parent mod registered its own keywords. */
    public static void registerKeywords() {
        String modID = BundleOfContent.modID.toLowerCase();
        for (int i = 0; i < KEYWORDS.length; i++) {
            Object[] entry = KEYWORDS[i];
            String proper = (String) entry[0];
            String[] names = (String[]) entry[1];
            String description = (String) entry[2];
            BaseMod.addKeyword(modID, proper, names.clone(), description);
        }
    }
}
