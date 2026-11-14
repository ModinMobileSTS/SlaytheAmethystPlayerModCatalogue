# Slay the Amethyst Player Mod Catalogue

玩家分享的 AI 补丁模组目录，供 Slay the Amethyst 浏览、下载和分享适用的模组补丁。

<!-- MOD_LIST_START -->
## BlueLaboratory

### 蓝色实验室中文汉化

模组自带的中文文本从未被加载（receiveEditStrings 只读取 eng 文件），因此中文环境下药剂与能力都显示英文。本补丁在模组加载完英文文本后，于中文环境载入完整中文文本：55 种药剂与 17 种能力的名称与描述全部译为中文，并统一使用游戏官方术语（多层护甲、残影、印记、真言等），修正了原自带文本中残留的英文与错译。

- **补丁版本**：`0.2.0`
- **作者**：dhdkfkfkf
- **适用版本**：`1.0.45`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/BlueLaboratory/%E8%93%9D%E8%89%B2%E5%AE%9E%E9%AA%8C%E5%AE%A4%E4%B8%AD%E6%96%87%E6%B1%89%E5%8C%96-dhdkfkfkf/amethyst.ai.patch.bluelaboratory.patch-1794633145363-82cc36a4.jar)

---

## DNKmod

### DNKmod 中文汉化

将 DNKmod 界面上的英文文本替换为中文：模组配置面板的开关标签（初始生命值/初始金币/初始能量/药水槽上限/初始随机药水/初始随机遗物/手牌上限/启用随从/启用坦克随从）、主菜单模组徽章的悬停简介，以及随从名称（随从/坦克随从）与其技能提示（攻击、防御、嘲讽及对应说明）。只替换显示文本，不改变任何数值和逻辑。

- **补丁版本**：`1.0.0`
- **作者**：枫陵
- **适用版本**：`2.3.0`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/DNKmod/DNKmod%20%E4%B8%AD%E6%96%87%E6%B1%89%E5%8C%96-%E6%9E%AB%E9%99%B5/amethyst.ai.patch.dnkmod.patch-1793374002735-342aafcd.jar)

### DNKmod 汉化与金币倍率

保留 DNKmod 界面汉化（配置面板开关标签、模组徽章悬停简介、随从名称与技能提示），并在 DNKmod 配置面板新增“金币倍率”滑块（1\~100 倍，默认 1 倍，数值保存在本补丁独立配置文件中）。倍率作用于战斗（普通/精英/Boss，以及战斗事件）掉落的金币奖励，奖励卡片显示的数字与实际获得的金币一致。

- **补丁版本**：`1.1.0`
- **作者**：枫陵
- **适用版本**：`2.3.0`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/DNKmod/DNKmod%20%E6%B1%89%E5%8C%96%E4%B8%8E%E9%87%91%E5%B8%81%E5%80%8D%E7%8E%87-%E6%9E%AB%E9%99%B5/amethyst.ai.patch.dnkmod.patch-1793374002735-342aafcd.jar)

---

## FrierenMod

### 芙莉莲特殊BGM闪退修复

修复 FrierenMod 特殊 BGM 导致的闪退：放弃本局/死亡结算时 FrierenMod 会请求自定义音乐键 Frieren\_DeathStinger，原版 TempMusic 兜底成无扩展名的 audio/music/Frieren\_DeathStinger 并抛出 Unknown file extension for music。补丁为 TempMusic\.getSong 增加前置处理，把 Frieren 开头的自定义键指向 Mod 内的 mp3；并在 MainMusic\.newMusic 对无扩展名或不可用的音乐路径补全扩展名或替换为原版合法 ogg，使坏音乐路径不再造成崩溃。

- **补丁版本**：`1.0.0`
- **作者**：dhdkfkfkf
- **适用版本**：`2.3.1`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/FrierenMod/%E8%8A%99%E8%8E%89%E8%8E%B2%E7%89%B9%E6%AE%8ABGM%E9%97%AA%E9%80%80%E4%BF%AE%E5%A4%8D-dhdkfkfkf/amethyst.ai.patch.frierenmod.patch-1794513610169-f6f84220.jar)

---

## PickAllBossRelics

### Pick All Boss Relics 修复

修复 Pick All Boss Relics 在 Boss 选遗物界面（Boss宝箱）的问题：1\) 触摸模式下点确认只关闭界面、不给予遗物；2\) 拿完遗物后列表变短，机器手柄左右/上下键触发数组越界崩溃；3\) Black Blood / Ring of the Serpent / Frozen Core / Holy Water 被吞掉、拿不到；4\) Empty Cage / Astrolabe / Pandora&\#x27;s Box / Tiny House / Orrery 等会打开子界面的遗物与 Boss 选遗物界面互相抢屏幕（该类遗物会让本次多选正常结束，避免嵌套界面错乱）；5\) 点“跳过/取消”会把已经获得的遗物收回；6\) 离开 Boss 宝箱房后旧的选遗物界面被复活。

- **补丁版本**：`1.0.0`
- **作者**：lution
- **适用版本**：`1.1.3`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/PickAllBossRelics/Pick%20All%20Boss%20Relics%20%E4%BF%AE%E5%A4%8D-lution/amethyst.ai.patch.pickallbossrelics.patch-1793968537129-ed7cef8e.jar)

---

## PotionMod

### 药水扩展修复

修复幽灵药水缺少第 3 条 DESCRIPTIONS 导致构造函数越界、药剂图鉴与随机药水空指针；修复三个无效效果：转化药水会清空全部能量且只塞无色牌（改为随机转化手牌 2 张牌、不再扣能量）、混乱药水无实际作用（喝下时与每回合结束让敌人重新决策意图）、打折能力只闪不降费（回合开始手牌费用 \-1，最低 0）；并按实际强度改写描述：折扣药水改为“下回合抽 3 张”、偷窃药水与偷窃能力说明整场战斗按伤害 7 倍获得金币、虚化药水说明授予腐化。

- **补丁版本**：`1.2.0`
- **作者**：yanx
- **适用版本**：`3.0.1`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/PotionMod/%E8%8D%AF%E6%B0%B4%E6%89%A9%E5%B1%95%E4%BF%AE%E5%A4%8D-yanx/amethyst.ai.patch.potionmod.patch-1794584157184-8b6d64c0.jar)

---

## ReplayTheSpireMod

### Replay 漏译补全

Replay The Spire 自带中文已基本完整，本补丁补齐仍显示英文的内容：失明、潜藏威胁两个能力，蓝衣法师怪物及其招式，4 个地精/流浪者联动遗物与地精敲击牌，以及 3 条遗物解锁成就说明。

- **补丁版本**：`1.0.0`
- **作者**：csc001
- **适用版本**：`1.6.7`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/ReplayTheSpireMod/Replay%20%E6%BC%8F%E8%AF%91%E8%A1%A5%E5%85%A8-csc001/amethyst.ai.patch.replaythespiremod.patch-1794225505019-e080e50e.jar)

---

## RiskOfRelics

### Risk Of Relics 中文补全

补齐 Risk Of Relics 在简体中文下仍未翻译或缺失的文本：能力名称与描述（14 项全部汉化）、遗物文本（念珠、AtG 导弹、飞镖、雏菊、护盾发生器、酒瓮、众筹者、打火机、神像、聚焦装置、黄玉胸针、雨衣、便士）、卡牌“念珠”、机运神龛/净化池/死亡元素/神器密室事件、相位圣龛的行动条、UI 文本（遗物摧毁提示、3D 打印机废料串、模组设置说明、指令与复仇神格）以及 6 个关键字的名称与说明。仅在游戏语言为简体中文时加载，其余语言不受影响。

- **补丁版本**：`1.0.3`
- **作者**：csc001
- **适用版本**：`2.3.12`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/RiskOfRelics/Risk%20Of%20Relics%20%E4%B8%AD%E6%96%87%E8%A1%A5%E5%85%A8-csc001/amethyst.ai.patch.riskofrelics.patch-1794228127166-1b6c2f11.jar)

---

## RyoikiTenkai

### 猎手能力牌特效补齐

按截图重修「精准」的供能连线与标靶。连线是上一版最大问题：它被画笔的 7px 线宽下限撑粗、几乎不弯、且暗金色在加色混合后呈土褐色，看起来像横穿屏幕的电缆。现改为不走下限的 1\.6\~2\.6px 细线，两端亮中间极淡，控制点明显外抬做出弧形垂挂，颜色改为暖白偏金，并把同时连线数封顶到 3 条。标靶方面回调尺寸与亮度以免糊成一个红点：半径改回 0\.70×半宽、下限 44px，外环提到 0\.72、内环 0\.50、靶心 0\.55，并把暗红改回饱和红、内环改暖白以保证层次。标靶仍不含任何向外辐射的横条。

- **补丁版本**：`2.5.0`
- **作者**：apricityx
- **适用版本**：`1.1.5`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/RyoikiTenkai/%E7%8C%8E%E6%89%8B%E8%83%BD%E5%8A%9B%E7%89%8C%E7%89%B9%E6%95%88%E8%A1%A5%E9%BD%90-apricityx/amethyst.ai.patch.ryoikitenkai.patch-1793074330811-30586f74.jar)

### 观者能力牌特效补齐

观者能力牌特效：洞悉现实改为专用的「校准刻印」——右侧身外的＋刻印，下方六道刻痕＝本场已升级的牌数；施放时演刻印成形与滑到栖位，触发时按新牌的真实去向落到手牌区／抽牌堆／弃牌堆。同时移除以手拒之（攻击牌）的全部特效与其设置开关。

- **补丁版本**：`6.6.0`
- **作者**：apricityx
- **适用版本**：`1.1.5`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/RyoikiTenkai/%E8%A7%82%E8%80%85%E8%83%BD%E5%8A%9B%E7%89%8C%E7%89%B9%E6%95%88%E8%A1%A5%E9%BD%90-apricityx/amethyst.ai.patch.ryoikitenkai.patch-1793076375821-ac0e26c7.jar)

### 铁甲战士能力牌特效补齐

为铁甲战士两张能力牌补齐/重做战斗特效：进化改为背后向上生长的三叉光芽（股数 = 1 \+ 额外抽牌数），抽到状态牌时标记环跟随那张牌移动、吸收弧汇入芽尖，并按额外抽牌数抛出光苞；金属化改为腰肋处环绕身体的分片护甲带（甲片数 = 每回合格挡点数），按深度排序绘制、甲体走普通混合、高光沿环扫过，回合末获得格挡时甲片合拢并亮起接缝，施放时甲片逐片飞入锁上；清除星芒与每回合的火星粒子，删除被取代的旧特效类。

- **补丁版本**：`2.6.0`
- **作者**：apricityx
- **适用版本**：`1.1.5`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/RyoikiTenkai/%E9%93%81%E7%94%B2%E6%88%98%E5%A3%AB%E8%83%BD%E5%8A%9B%E7%89%8C%E7%89%B9%E6%95%88%E8%A1%A5%E9%BD%90-apricityx/amethyst.ai.patch.ryoikitenkai.patch-1792905380963-42a27a3a.jar)

---

## Spirepass

### Spirepass 41级彩蛋一键解锁

进入游戏时自动解锁 Spirepass（尖塔通行证）隐藏的 41 级彩蛋并写盘保存，无需再连点 marcus 开关 5 次、满足 2000 XP 且玩家名为 &quot;pee&quot;。

- **补丁版本**：`0.2.0`
- **作者**：ZAQOU
- **适用版本**：`1.4.3`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/Spirepass/Spirepass%2041%E7%BA%A7%E5%BD%A9%E8%9B%8B%E4%B8%80%E9%94%AE%E8%A7%A3%E9%94%81-ZAQOU/amethyst.ai.patch.spirepass.patch-1792849960971-900cc5c1.jar)

---

## TheNecrobinder

### 亡灵契约师汉化修复

为「亡灵契约师」模组提供简体中文本地化及缺陷修复。涵盖全部内容（卡牌、遗物、能力、关键词、角色、事件等）的中文翻译，以及以下修复：四处选牌界面一帧即结束导致游戏死锁的卡死问题；奥斯蒂死亡淡出期间误清除新召唤实例；右手法掌牌补丁硬编码行号在版本更新后失效；领域与友谊能力描述中能量图标显示为红色的问题；牺牲牌在奥斯蒂未存活时打出会崩溃的空指针；解缚命匣因遗物标识缺少前缀导致永远无法在商店刷出；女妖之嚎在刚抽入手牌时追溯计算本战斗全部有飘牌造成费用骤降的问题；中文环境下赋予有飘/保留/重放描述前缀显示为英文的问题。

- **补丁版本**：`0.0.8`
- **作者**：24
- **适用版本**：`1.1.0`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/TheNecrobinder/%E4%BA%A1%E7%81%B5%E5%A5%91%E7%BA%A6%E5%B8%88%E6%B1%89%E5%8C%96%E4%BF%AE%E5%A4%8D-24/amethyst.ai.patch.thenecrobinder.patch-1793620024231-b157a118.jar)

---

## TimeKeeperMod

### TimeKeeper皮肤立绘修复

修复 TimeKeeper 附带的原版角色皮肤（铁甲战士「苦痛恶魔」、静默猎手「花烛君主」）：选人界面立绘改到背景立绘之后绘制，正常显示；皮肤序号改用独立配置文件，避免母模组三个界面共用存档键 TimeCutter:skin 在读档时把序号刷回默认；玩家角色进入游戏第一帧无条件重设一次骨架，之后定期复查，被还原就补回；换骨架按「皮肤→备用皮肤→默认」逐级尝试，用骨架是否真的被替换判定成功。

- **补丁版本**：`0.6.1`
- **作者**：浮尘
- **适用版本**：`1.4.5`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/TimeKeeperMod/TimeKeeper%E7%9A%AE%E8%82%A4%E7%AB%8B%E7%BB%98%E4%BF%AE%E5%A4%8D-%E6%B5%AE%E5%B0%98/amethyst.ai.patch.timekeepermod.patch-1794519907664-4dd7ac7b.jar)

---

## aristocrat

### 加速卡牌失效修复

StSLib 弹窗加速修复

修复加速模组（Superfastnnode 等倍增 deltaTime）下 StS Lib 弹窗选择类卡牌（如大金库）选择无效果的问题。

- **补丁版本**：`1.0.5`
- **作者**：24
- **适用版本**：`1.0.5`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/aristocrat/%E5%8A%A0%E9%80%9F%E5%8D%A1%E7%89%8C%E5%A4%B1%E6%95%88%E4%BF%AE%E5%A4%8D-24/amethyst.ai.patch.aristocrat.patch-1793315821082-9a3ddbe7.jar)

---

## ascensionmod

### Ascension Reborn 汉化补丁

Ascension Reborn（进阶重制）简体中文汉化：补齐模组中仍为英文的自定义进阶界面按钮、短进阶/负进阶说明、遗物文本、重命名弹窗标题，并修复自定义模式&quot;纯净&quot;词条的翻译缺失。

- **补丁版本**：`1.0.0`
- **作者**：apricityx
- **适用版本**：`2.9.0`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/ascensionmod/Ascension%20Reborn%20%E6%B1%89%E5%8C%96%E8%A1%A5%E4%B8%81-apricityx/catalogue-7a08ab17eee21122b71abe9b.jar)

---

## bloodstains

### Bloodstains 血迹卡汉化

Bloodstains 新增的「血迹」卡牌（致敬《黑暗之魂》系列）此前只有英文文本，现补充简体/繁体中文：卡名「血迹」与描述（虚无、获得金币、消耗），以及地图节点提示「此处保留着你上一次冒险中失去的 X 金币」和模组设置项「金币取回比例」。文本在父模组加载英文字符串之后写入游戏的本地化表，因此优先显示；并按游戏语言只加载简体或繁体一份，避免两者互相覆盖（旧版补丁同时加载，繁体总是盖掉简体）。

- **补丁版本**：`1.0.0`
- **作者**：apricityx
- **适用版本**：`1.1.0`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/bloodstains/Bloodstains%20%E8%A1%80%E8%BF%B9%E5%8D%A1%E6%B1%89%E5%8C%96-apricityx/amethyst.ai.patch.bloodstains.patch-1793414372835-120a9a7d.jar)

---

## bundle\_of\_content

### Bundle Of Content 中文本地化补全

补全 Bundle Of Content 的简体中文文本：全部 60 件遗物的名称、风味文字与效果描述重译为中文并按当前英文版本校正，关键词的标题与说明一并译为中文（创作者致敬类关键词保留原名）。在原模组加载完自带文本后再覆盖，由此修正原中文文件中仍为英文或已过时的条目。

- **补丁版本**：`1.0.0`
- **作者**：csc001
- **适用版本**：`4.0.15`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/bundle_of_content/Bundle%20Of%20Content%20%E4%B8%AD%E6%96%87%E6%9C%AC%E5%9C%B0%E5%8C%96%E8%A1%A5%E5%85%A8-csc001/amethyst.ai.patch.bundle_of_content.patch-1794224955296-4e0ec5b5.jar)

---

## card\-forge

### Card Forge 汉化

将 Card Forge 的三个遗物（灼热之火、雷神之锤、众神之砧）的遗物名、遗物描述、风味文本以及选卡界面的提示文字汉化为中文，仅在游戏语言为中文时生效；顺带修正「雷神之锤」英文名中多余的重复空格。

- **补丁版本**：`1.0.1`
- **作者**：dhdkfkfkf
- **适用版本**：`1.4`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/card-forge/Card%20Forge%20%E6%B1%89%E5%8C%96-dhdkfkfkf/amethyst.ai.patch.card-forge.patch-1794619003678-3033c233.jar)

### Card Forge 汉化与铸卡界面

为 Card Forge 提供中文汉化（遗物名、风味文字、描述、选卡提示），并重写雷神之锤的铸卡界面：卡牌固定格位不再抖动，可按角色卡包筛选（默认当前角色），右侧竖条可长按拖动、两端 ▲▼ 翻页；已移除搜索输入功能。

- **补丁版本**：`1.6.0`
- **作者**：dhdkfkfkf
- **适用版本**：`1.4`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/card-forge/Card%20Forge%20%E6%B1%89%E5%8C%96%E4%B8%8E%E9%93%B8%E5%8D%A1%E7%95%8C%E9%9D%A2-dhdkfkfkf/amethyst.ai.patch.card-forge.patch-1794619003678-3033c233.jar)

### Card Forge 汉化与铸卡筛选

Card Forge 中文汉化，并重写雷神之锤的选卡界面：固定格位消除抖动、按角色卡包筛选（默认当前角色）、卡名搜索。右上角显示“输入中… / 搜索：xx”；右侧竖条可点轨道跳转、按住滑块拖动、两端 ▲/▼ 翻一屏。键盘预览横条由加载器绘制，游戏内只负责接收输入法与显示文本。

- **补丁版本**：`1.5.1`
- **作者**：dhdkfkfkf
- **适用版本**：`1.4`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/card-forge/Card%20Forge%20%E6%B1%89%E5%8C%96%E4%B8%8E%E9%93%B8%E5%8D%A1%E7%AD%9B%E9%80%89-dhdkfkfkf/amethyst.ai.patch.card-forge.patch-1794619003678-3033c233.jar)

---

## cardrefresh

### 卡牌刷新单卡模式

•修复了sl之后，刷新次数不会还原的问题

•增加单卡刷新模式

- **补丁版本**：`1.4.0`
- **作者**：yanx
- **适用版本**：`0.3.0`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/cardrefresh/%E5%8D%A1%E7%89%8C%E5%88%B7%E6%96%B0%E5%8D%95%E5%8D%A1%E6%A8%A1%E5%BC%8F-yanx/amethyst.ai.patch.cardrefresh.patch-1794577030315-27d056f8.jar)

---

## chronomuncher

### 信徒中文补丁

为 The Disciple 提供全量中文本地化。覆盖卡牌、遗物、复刻品、事件、关键词、力量、药水与界面文本，并修复能量图标渲染与悬浮提示翻译。当时序已至，信徒终临此刻。

- **补丁版本**：`3.7.0`
- **作者**：24
- **适用版本**：`2.0`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/chronomuncher/%E4%BF%A1%E5%BE%92%E4%B8%AD%E6%96%87%E8%A1%A5%E4%B8%81-24/amethyst.ai.patch.chronomuncher.patch-1793399884157-536381f1.jar)

### 时序·信徒中文补丁

为 The Disciple 提供全量中文本地化。覆盖卡牌、遗物、复刻品、事件、关键词、力量、药水与界面文本，并修复能量图标渲染与悬浮提示翻译。当时序已至，信徒终临此刻。

- **补丁版本**：`3.7.1`
- **作者**：24
- **适用版本**：`2.0`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/chronomuncher/%E6%97%B6%E5%BA%8F%C2%B7%E4%BF%A1%E5%BE%92%E4%B8%AD%E6%96%87%E8%A1%A5%E4%B8%81-24/amethyst.ai.patch.chronomuncher.patch-1793399884157-536381f1.jar)

---

## fnh

### 恐惧与饥饿音乐按角色限定

Fear And Hunger 模组原本无条件替换全部游戏音乐（主菜单、三层楼、商店、神龛、精英、Boss、心之绽、终幕与片尾），游玩其他角色时也会播放其音乐。本补丁在原模组的两个音乐替换补丁前抢先判定：只有在游玩恐惧与饥饿自建角色（或该角色为当前选中角色）时才使用其音乐，其他情况直接采用游戏原版音乐。模组自带的音效只会由它自己的卡牌、怪物、遗物等触发，不存在全局替换，因此无需改动。

- **补丁版本**：`1.0.0`
- **作者**：lution
- **适用版本**：`0.0.1`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/fnh/%E6%81%90%E6%83%A7%E4%B8%8E%E9%A5%A5%E9%A5%BF%E9%9F%B3%E4%B9%90%E6%8C%89%E8%A7%92%E8%89%B2%E9%99%90%E5%AE%9A-lution/amethyst.ai.patch.fnh.patch-1794047405169-716e9cb7.jar)

---

## fuZhouMod

### 圣主卡死现场抓取

卡死只能强杀时游戏写不出日志，本补丁内置看门狗自动记录现场。输出位置：主文件为游戏 logs 目录下的 fuzhou\-watchdog\.log，可随启动器「导出日志」一起打包；同时在游戏根目录留一份 fuzhou\-watchdog\.txt 备份，文件开头写明两条绝对路径与用法。主线程停滞 3 秒即记录内存、房间、玩家生命与能力、手牌、动作队列、怪物与线程栈；每 10 秒一行【存活】便于判断主线程是否还在跑；文件超 8MB 自动轮换。

- **补丁版本**：`0.3.0`
- **作者**：dhdkfkfkf
- **适用版本**：`1.2.9`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/fuZhouMod/%E5%9C%A3%E4%B8%BB%E5%8D%A1%E6%AD%BB%E7%8E%B0%E5%9C%BA%E6%8A%93%E5%8F%96-dhdkfkfkf/amethyst.ai.patch.fuzhoumod.patch-1794510181124-ae2439b0.jar)

### 圣主受击卡死修复

修复圣主受击时的卡死。根因是 GuFu（古夫）mod 的 luShi\.luShi\.onAttackAssit\(\) 用一个不推进下标的 while 循环找挡伤随从，遇到既不是随从也不是空槽的充能球（圣主的面具占槽）会永久循环，导致主线程彻底卡死、只能强杀。补丁不改 GuFu 也不改圣主：在 BaseMod 派发玩家受伤钩子前后，按原循环的判定顺序做一次无害推演，只在这次派发必然死循环或下标越界时，把毒住循环的球临时替换为空槽，派发结束立即原样还原；正常情况不做任何改动。不修改任何数值与其它逻辑。

- **补丁版本**：`0.1.0`
- **作者**：dhdkfkfkf
- **适用版本**：`1.2.9`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/fuZhouMod/%E5%9C%A3%E4%B8%BB%E5%8F%97%E5%87%BB%E5%8D%A1%E6%AD%BB%E4%BF%AE%E5%A4%8D-dhdkfkfkf/amethyst.ai.patch.fuzhoumod.patch-1794511969797-4188cd95.jar)

### 圣主贴图泄漏修复

修复圣主待机动画的贴图泄漏：disableIdleAnimation\(\) 只把 idleFrames 置空，20 张 517x572 的待机帧纹理（约 22MB）从不释放，鼠符咒被老爹封印后返还时会重新加载一整套，反复循环持续占用显存。现改为置空前先释放旧帧纹理；同时 loadIdleFrames\(\) 发现帧数组含无效项时，先释放残留帧再重新加载，避免一直复用坏数组。不涉及任何玩法数值。

- **补丁版本**：`0.1.2`
- **作者**：dhdkfkfkf
- **适用版本**：`1.2.9`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/fuZhouMod/%E5%9C%A3%E4%B8%BB%E8%B4%B4%E5%9B%BE%E6%B3%84%E6%BC%8F%E4%BF%AE%E5%A4%8D-dhdkfkfkf/amethyst.ai.patch.fuzhoumod.patch-1794507323585-b2a6a9e3.jar)

---

## invincible\-one

### Bingo UI 溢出修复

调整了 Bingo 的 UI，防止溢出，并调整 Bingo 遗物为左键触发。

- **补丁版本**：`1.1.2`
- **作者**：apricityx
- **适用版本**：`1.2.1`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/invincible-one/Bingo%20UI%20%E6%BA%A2%E5%87%BA%E4%BF%AE%E5%A4%8D-apricityx/amethyst.ai.patch.invincible-one.patch-1792491621264-07095165.jar)

---

## morerelics

### More Relics 简体中文汉化

修复中文说明显示不完整的问题：中文渲染按空格切词，词首为 \[ 的片段会被整段丢弃，因此锡手镯、鸟类标本、纯洁印记、辅助轮中的 \[E\] 能量符号会让“获得”之后的文字全部消失，现改为“\#b1 点能量”；同时修正位移斗篷 \#y 颜色标记位置（此前会显示成字面 \#y 格挡）、折线图百分号重复、营火改为休息处。仍仅在选择简体中文时生效。

- **补丁版本**：`1.0.2`
- **作者**：lution
- **适用版本**：`1.0.1`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/morerelics/More%20Relics%20%E7%AE%80%E4%BD%93%E4%B8%AD%E6%96%87%E6%B1%89%E5%8C%96-lution/amethyst.ai.patch.morerelics.patch-1794148218902-9e5804b6.jar)

---

## ninja

### 欧内死手特效

给蕾忍mod 的「我夺取心脏」加专属斩杀演出：前冲、破空音效与震屏、暗红速度线、红色拖影，敌人死亡瞬间炸开血滴并在地面留下按模型体积等比放大的静态血迹；同时补齐补丁元数据（mts\_version、credits），修正社区一键上传时报 Missing metadata fields 的问题。

- **补丁版本**：`1.0.2`
- **作者**：145141
- **适用版本**：`1.7.1`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/ninja/%E6%AC%A7%E5%86%85%E6%AD%BB%E6%89%8B%E7%89%B9%E6%95%88-145141/amethyst.ai.patch.ninja.patch-1793708455455-c63a5506.jar)

---

## omnipotence

### 全能模组中文翻译补丁

将 Omnipotence 模组的卡牌名称与描述汉化为简体中文（卡牌&quot;全能&quot;：本场战斗升级所有牌、战斗中创建卡牌时升级该牌）。修复先前汉化不生效：改为在父模组加载字符串后强制注入中文，无论游戏语言设置如何都显示中文。

- **补丁版本**：`1.3.0`
- **作者**：枫陵
- **适用版本**：`1.0.0`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/omnipotence/%E5%85%A8%E8%83%BD%E6%A8%A1%E7%BB%84%E4%B8%AD%E6%96%87%E7%BF%BB%E8%AF%91%E8%A1%A5%E4%B8%81-%E6%9E%AB%E9%99%B5/amethyst.ai.patch.omnipotence.patch-1793525606054-68d0882b.jar)

---

## payToFire

### 付费火堆营火选项修复

修复付费火堆在触摸确认流程下营火选项失效、以及递增计费与实际扣费不一致的问题。1\) 选项失效：启动器触摸二次确认兼容层把营火选项的 useOption 推迟到确认按钮，而 BaseMod 的营火后处理回调仍在首次点击时触发，模组随即禁用全部选项并熄灭火焰，使待确认状态被取消、休息/锻造等效果从未执行。2\) 计费：模组的次数在点击点火时就自增（此时尚未扣钱），使用休息/锻造时又自增一次，导致首次点火显示 15 却扣 50、每轮涨价两步、界面价格与金币检查都落后于实际扣费。现改为次数只在一次点火真正付款完成后 \+1，并立即刷新点火选项的显示价格与可用状态，使显示价格、金币检查与实际扣费一致。

- **补丁版本**：`0.2.0`
- **作者**：lution
- **适用版本**：`0.1.7`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/payToFire/%E4%BB%98%E8%B4%B9%E7%81%AB%E5%A0%86%E8%90%A5%E7%81%AB%E9%80%89%E9%A1%B9%E4%BF%AE%E5%A4%8D-lution/amethyst.ai.patch.paytofire.patch-1793977101105-dd2db156.jar)

---

## rankupcard

### Starter Rank Up Relic

修复了仅为原版角色，自动添加三合遗物模组的bug

现在所有其他mod角色也会自动装备该遗物

- **补丁版本**：`1.3.0`
- **作者**：yanx
- **适用版本**：`1.0.0`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/rankupcard/Starter%20Rank%20Up%20Relic-yanx/amethyst.ai.patch.rankupcard.patch-1793234089739-d7b6579d.jar)

---

## simplemap

### Simple Map 崩坠地图适配

修复 Simple Map 与崩坠（Downfall）反派模式翻图地图不兼容。开启地图收缩且崩坠处于反派模式时，在 generateMap / generateSpecialMap 完成后按收缩网格重排节点并把进度轴镜像（起始行在左、Boss 侧在右），直接写入绝对偏移以抹掉崩坠给节点叠加的 400\*scale 平移，并重建全部连线的几何；同时固定 Boss 图标与点击框到收缩布局的 Boss 位置，绕过崩坠与父模组在 renderBossIcon、Hitbox\.move 上的改写顺序冲突，避免图标画到屏幕外或点不到 Boss。

- **补丁版本**：`0.1.0`
- **作者**：apricityx
- **适用版本**：`0.0.6`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/simplemap/Simple%20Map%20%E5%B4%A9%E5%9D%A0%E5%9C%B0%E5%9B%BE%E9%80%82%E9%85%8D-apricityx/amethyst.ai.patch.simplemap.patch-1794147939680-fdaa0bbe.jar)

---

## slaytheperktree

### 技能树移动端适配

修复 Slay the Perk Tree 技能树界面在移动端无法平移、也无法退出的问题：单指拖动平移视图，点击天赋购买，新增「点击：移除」模式用于移除一级，右上角新增「关闭」按钮并支持返回键关闭，底部操作提示改为触屏说明。界面左上角另加「加成：开启/暂停」开关（状态会保存），暂停后该模组的初始生命/金币/药水、战斗开始能力与额外卡牌奖励、金币加成、击杀回血、额外遗物选项均暂不生效，经验与技能点仍照常获得。

- **补丁版本**：`1.0.0`
- **作者**：lution
- **适用版本**：`1.2.2`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/slaytheperktree/%E6%8A%80%E8%83%BD%E6%A0%91%E7%A7%BB%E5%8A%A8%E7%AB%AF%E9%80%82%E9%85%8D-lution/amethyst.ai.patch.slaytheperktree.patch-1794360197161-0042d6ca.jar)

### 添加主菜单技能树入口和左键拖动

主菜单添加技能树入口并新增左键按住拖动平移技能树视图

- **补丁版本**：`2.1.0`
- **作者**：apricityx
- **适用版本**：`1.2.2`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/slaytheperktree/%E6%B7%BB%E5%8A%A0%E4%B8%BB%E8%8F%9C%E5%8D%95%E6%8A%80%E8%83%BD%E6%A0%91%E5%85%A5%E5%8F%A3%E5%92%8C%E5%B7%A6%E9%94%AE%E6%8B%96%E5%8A%A8-apricityx/amethyst.ai.patch.slaytheperktree.patch-1792835635529-438a3205.jar)

---

## spireTogether

### Skindex全皮肤解锁

解锁 Skindex 皮肤系统中全部角色皮肤：模组加载时打开 Skindex 自带的 SkindexDev\.unlockAll 开关，并在 OwnableItem\.canUse 处对所有角色皮肤直接返回已解锁。效果为角色与皮肤自定义界面不再显示 LOCKED、可直接设为默认皮肤，Neow 衣柜左右箭头可轮换出全部皮肤。仅运行时放行，不写入玩家解锁存档。

- **补丁版本**：`1.0.0`
- **作者**：apricityx
- **适用版本**：`6.4.20`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/spireTogether/Skindex%E5%85%A8%E7%9A%AE%E8%82%A4%E8%A7%A3%E9%94%81-apricityx/amethyst.ai.patch.spiretogether.patch-1794016797685-8cfa4e44.jar)

---

## sts\-metrics

### 紫音模组兼容

修复了紫音模组不显示抓取率的问题。

- **补丁版本**：`1.2.0`
- **作者**：apricityx
- **适用版本**：`0.1.3`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/sts-metrics/%E7%B4%AB%E9%9F%B3%E6%A8%A1%E7%BB%84%E5%85%BC%E5%AE%B9-apricityx/amethyst.ai.patch.sts-metrics.patch-1792774005194-72779d35.jar)

---

## stscustomrelics

### 自定义遗物中文本地化

为 Custom Relics Mod 的 5 个遗物补充简体中文文本：游戏语言为简体中文时，在父模组注册英文文本后追加中文名称、风味文本与描述，并覆盖 5 个遗物硬编码返回英文的 getUpdatedDescription，使遗物名与描述均为中文；其它语言保持原英文。

- **补丁版本**：`1.0.0`
- **作者**：lution
- **适用版本**：`1.0.0`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/stscustomrelics/%E8%87%AA%E5%AE%9A%E4%B9%89%E9%81%97%E7%89%A9%E4%B8%AD%E6%96%87%E6%9C%AC%E5%9C%B0%E5%8C%96-lution/amethyst.ai.patch.stscustomrelics.patch-1794148677130-921d7dd0.jar)

---

## umapyoi

### The Uma 中文本地化

为《The Uma》提供完整简体中文文本，并修复中文卡面显示问题：卡牌/遗物/能力/药水/事件全量译文；保留日式心情术语（绝好调/好调/普通/不调/绝不调），心情关键字概率为 1/4；订正卡名 G00 1st\.F∞、赤色王牌、蓝蔷薇追猎者；在构建卡面描述前清除紧贴中文文字或标点的 umapyoi:/stslib: 前缀，并在描述行上再做一次兜底清洗，使监督、腿部骨折、三条七、传承永存等卡不再显示原始 ID；把裸中文关键字补登记进关键字表以保留金色高亮与悬浮说明；修复动态变量紧贴中文标点时显示成 umapyoi:m2 等裸文本的问题；遗物文本里的 \#yumapyoi:心情 改为 \#y心情。

- **补丁版本**：`1.1.1`
- **作者**：八幡海鈴
- **适用版本**：`1.1.0`
- **下载**：[下载](https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/umapyoi/The%20Uma%20%E4%B8%AD%E6%96%87%E6%9C%AC%E5%9C%B0%E5%8C%96-%E5%85%AB%E5%B9%A1%E6%B5%B7%E9%88%B4/amethyst.ai.patch.umapyoi.patch-1793277282963-bc68e946.jar)
<!-- MOD_LIST_END -->
