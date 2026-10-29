package sts.ryoikitenkai.compat.purple;

/**
 * 通用图案骨架能画的母题。
 * <p>
 * 这些母题由 {@code MotifAuraEffect} / {@code MotifBurstEffect} 共用一套
 * 「常驻 + 爆发」的通用代码，所以适合形状简单、不需要状态联动的牌。
 * <ul>
 *   <li>OMEGA —— 欧米伽：金色 Ω 徽记 + 上升的红烟余烬</li>
 * </ul>
 * <p>
 * <b>现在只剩欧米伽还走这张表</b>，其余能力牌各自有专用实现。它们退出通用骨架的
 * 原因各不相同，但共同点是「通用骨架表达不了它们的核心」：
 * <ul>
 *   <li>如水 —— {@code WaterAuraEffect} / {@code WaterBurstEffect}：
 *       自写 GLSL 逐像素画水面</li>
 *   <li>心之堡垒 —— {@code FortressAuraEffect} / {@code FortressBurstEffect}：
 *       立体立方体的深度排序</li>
 *   <li>确立基础 —— {@code FoundationAuraEffect}：<b>一块碑 = 一层能力</b>，
 *       核心是「可数的数量」</li>
 *   <li>虔信 —— {@code DevotionAuraEffect}：<b>10 根刻度 = 真言进度</b>，
 *       核心是「随状态变化的读数」</li>
 *   <li>先见之明 —— {@code ForesightAuraEffect}：<b>一个框 = 一张要占卜的牌</b>；
 *       旧实现 {@link #FLAME_EYE} 本身也不成立（见下）</li>
 *   <li>研习 —— {@code StudyAuraEffect}：<b>一页纸 = 一张洞见</b>；
 *       而且它是唯一一个挂在<b>屏幕 UI 区</b>的常驻。旧实现 {@link #RUNE} 也不成立</li>
 *   <li>涅槃 —— {@code NirvanaAuraEffect}：<b>一层护壁 = 一次占卜的格挡</b>，
 *       且<b>层数跟着格挡实时增减、受击变薄</b> —— 通用骨架无法与战斗状态联动。
 *       旧实现 {@link #MANDALA} 同样不成立（见下）</li>
 *   <li>洞悉现实 —— {@code MasterRealityAuraEffect}：右侧身外的「＋」刻印 +
 *       <b>六道刻痕 = 本场已升级的牌数</b>；触发演出还要落到具体的牌堆上。
 *       旧实现 {@link #MIRROR} 已停用（见下）</li>
 *   <li>猛虎下山 —— <b>没有常驻表现，也没有任何画在玩家身上的元素</b>。
 *       全部视觉只有一个：{@link RushdownClawEffect} 把抓痕刻在<b>被打的敌人身上</b></li>
 *   <li>战歌 —— {@code HymnAuraEffect}：一牌一声部（见补充规范文档）</li>
 *   <li>斋戒 —— {@code FastingFx}：两条方向相反的单向因果链，没有常驻</li>
 * </ul>
 * <p>
 * <h2>已停用的母题（枚举值与旧绘制方法保留，避免为了删一个形状去改两张 switch 表）</h2>
 * <ul>
 *   <li>{@link #BLAST}：12 道径向星芒向外 + 人形残影，与铁甲「径向火弹环」、
 *       猎手「残像」同构</li>
 *   <li>{@link #FLAME_EYE}：原本给「先见之明」。火是铁甲语汇，
 *       且 {@code Draw.ring()} + 一条竖线拼出的是<b>眼睛</b>（禁止的具象物）。
 *       现改用 {@code ForesightFx} 的「空心光框斜列」</li>
 *   <li>{@link #RUNE}：原本给「研习」。橙刺尖端调了 {@code Draw.star}（硬禁），
 *       材质借错火焰语汇，几何仍是「环绕 + 头顶」。
 *       现改用 {@code StudyFx} 的「抽牌堆上方的扇形纸页」</li>
 *   <li>{@link #MANDALA}：原本给「涅槃」，四条都不成立 ——
 *       <b>(a)</b> 每个花瓣尖端调了 {@code Draw.star}（本补丁最后三处星芒之一）；
 *       <b>(b)</b> 用 {@code 3.4f / 2.4f / 1.4f / 1.8f} 四档厚度，
 *       底层兜底把它们全拉到 4.5px，层次全丢；
 *       <b>(c)</b> 八瓣莲花是具象物，小尺寸下读成放射状碎线；
 *       <b>(d)</b> 曼陀罗是「圆满」的符号，但这张牌产出的是<b>格挡</b>，符号对不上产物。
 *       现改用 {@code NirvanaFx} 的「贴身层叠六边护壁」</li>
 *   <li>{@link #MIRROR}：原本给「洞悉现实」。三条都不成立 ——
 *       <b>(a)</b> {@code Draw.silhouette()} 画的是<b>人</b>（头/肩/躯干/双腿），
 *       属检查清单明令禁止的具象物；<b>(b)</b> 竖直光柱 + 环绕错位与
 *       心之堡垒（环绕）、如水（竖面）同构；<b>(c)</b> 这张牌作用在
 *       <b>新造出来的牌</b>上，整组挂在角色身上是位置错位。
 *       现改用 {@code MasterRealityFx} 的「右侧身外的 ＋ 刻印 + 六道刻痕」</li>
 *   <li>{@link #PALM}：原本给「以手拒之」。那是<b>攻击牌</b>不是能力牌，
 *       它贴在敌人身上的 {@code BlockReturnPower} 特效<b>已整体移出本补丁</b>，
 *       这一格不再被任何卡牌使用。形状本身也违规：画的是<b>手</b>（具象物），
 *       且「中心环 + 放射锯齿」踩在规范点名最拥挤的形状上。<b>不得复用。</b></li>
 *   <li>{@link #SCORE} / {@link #VORTEX} / {@link #HALO} / {@link #RIBBON} / {@link #CUBE}：
 *       已在更早的重做中被专用实现取代（战歌 / 树立 / 斋戒 / 如水 / 心之堡垒）</li>
 * </ul>
 * <h2>星芒已全部清除</h2>
 * 本补丁此前有三处在战斗特效里调用 {@code Draw.star}：战歌、研习、涅槃。
 * 战歌改用了 {@code NoteGlyph} 的实心符头，研习改画纸页，涅槃改画六边护壁。
 * 已停用母题的旧绘制方法里的 {@code Draw.star} 也一并换成了粗弧光斑，
 * <b>现在整个补丁的 {@code Draw.star} 调用点为 0</b>。
 * {@link Draw#star} 方法本身保留（其它补丁可能引用），但不得用于新的观者特效。
 */
enum Motif {
    SCORE,
    VORTEX,
    HALO,
    RIBBON,
    MIRROR,
    CUBE,
    MANDALA,
    BLAST,
    RUNE,
    PALM,
    FLAME_EYE,
    OMEGA
}
