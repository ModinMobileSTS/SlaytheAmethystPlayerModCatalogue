# 目录维护与上传协议

玩家分享的 AI 补丁模组目录，供 Slay the Amethyst 后续上传/下载功能使用。

> 当前阶段：仓库结构、元数据协议、可加载的示例 JAR、校验与自动索引已建立。
> **尚未实现** Android 上传/下载 UI 和现有云函数的上传接口。

## 存储结构

```text
mods/
  <modid>/
    index.json               # Action 生成的该 modid 补丁列表，可单独下载
    <mod_name>-<mod-author>/
      src/                   # 可选：补丁源代码
      metadata.json          # 上传元数据，与 src/、JAR 同级
      xxxx.jar               # 上传的完整 AI 补丁 JAR，保留文件名
      README.md              # 可选：补丁简介
index.json                   # 轻量徽标索引：按 modid / 版本查补丁数量和详情校验值
README.md                    # 仓库简介 + Action 自动生成的模组列表
schemas/metadata.schema.json # v1 元数据 JSON Schema
scripts/catalogue.py         # 标准库实现的校验器与索引生成器
.github/workflows/catalogue.yml
```

- `modid` 必须与原模组 `ModTheSpire.json` 的 `modid` **完全一致，区分大小写**。
- 目录名直接使用 `<补丁名>-<作者名>`，支持中文和空格，不自动小写或转换 slug。
- 名称需去除首尾空白并采用 Unicode NFC；禁止 `/ \\ < > : " | ? *`、控制/格式字符、
  `.`/`..`、前导或末尾点、Windows 保留名。`modid` 与组合目录各不超过 240 UTF-8 字节。
- 不接受大小写折叠后重名的 modid/补丁路径；`index` 是全局目录文件保留的 modid。
- `mods/` 下的前两层必须为目录；所有补丁目录必须含元数据和 JAR，禁止符号链接。
- 一个条目对应一个目标 modid；同一 JAR 适用多个 modid 时分别提交条目。
- **同路径直接覆盖** `metadata.json` 和元数据 `jar` 指定的文件，不自动增加数字后缀，也不保留多个版本目录。
  不同作者名对应不同目录；邮箱不参与目录标识。同路径包括组合名称含连字符产生的碰撞。
  Git 历史保留此前版本。改名或更换作者名会形成新路径，旧路径不会自动删除。
- 如果同一条目更新了 JAR 文件名，云函数应在同一提交中删除旧 JAR，避免遗留旧文件。
- 原始元数据仅保存在各补丁目录的 `metadata.json` 中，没有集中元数据文件夹。

## 上传元数据 v1

```json
{
  "schemaVersion": 1,
  "name": "Example Patch",
  "description": "补丁简介：说明解决的问题、行为变化和使用限制。",
  "version": "1.0.0",
  "author": {
    "name": "Example Author",
    "email": "author@example.com"
  },
  "target": {
    "modId": "basemod",
    "workshopId": "1605833019",
    "versions": ["5.56.0"]
  },
  "jar": "ExamplePatch.jar"
}
```

- `name` / `description`：补丁名和简介，分别不超过 120 / 4000 个字符。
- `author.name` / `author.email`：作者名称和邮箱，供公开展示与 Git 提交署名。
- `target.modId`：适用的原模组 ID，不是补丁 JAR 自己的 modid。
- `target.workshopId`：原模组的 Steam Workshop ID，必须用**十进制字符串**，避免大整数精度丢失。
  同一 modid 的条目必须映射到同一个 Workshop ID。
- `target.versions`：适用的原模组版本列表，按原模组版本字符串精确匹配，不解析 semver 区间。
  `["*"]` 表示作者声明适用全部版本，不能与其他版本混用。
- `version`：补丁自己的版本，与原模组版本分开。
- `jar` 是同目录下的 JAR 文件名（支持中文和空格，禁止绝对路径、子目录和 `..` 路径穿越）；
  文件名需符合安全路径规则、不超过 120 字符及 240 UTF-8 字节，扩展名不区分大小写。
  `patch.jar` 仍可使用，但不再要求固定名称。
- `example: true` 是可选的示例标识，客户端应默认隐藏或明确标注。
- 不接受未知元数据字段。JSON Schema 描述基本结构；目录规则、NFC、JAR 一致性等以校验器为准。

### JAR 校验

JAR 上限为 **25 MiB**（不代表云函数 multipart 请求总大小限制），最多 4096 个 ZIP 项，
声明解压总大小不超过 512 MiB；JSON 文件不超过 64 KiB。

JAR 必须包含至少一个编译后的 `.class` 和根目录 `ModTheSpire.json`，其中：

- `modid` 是补丁自己的 ID，不能与目标模组 ID 相同；
- `name`、`description`、`version` 必须与上传元数据一致；
- `dependencies` 必须包含大小写一致的 `target.modId`；
- 如果包含启动器生成的 `agent-patch.json`，其父 modid、补丁 modid、名称、简介、版本必须一致。

校验器仅检查归档、元数据和路径，**不运行上传的 Java 代码**，不保证代码安全、无版权问题或实际兼容。
禁止重复 ZIP 条目、路径穿越、加密条目和符号链接。客户端下载后必须先校验 `size` / `sha256`，
再让玩家明确确认安装和启用；不能自动执行所有新上传的 JAR。

## 自动生成 JSON 目录

每次 push（无路径过滤）运行 `Generate mod catalogue`：

1. 运行回归测试，每次完整扫描 `mods/*/*/metadata.json` 并校验其元数据和 `jar` 字段引用的同目录 JAR。
   不依赖集中注册表；`src/` 内的同名 JSON 只是源文件，不作为额外条目收录。
2. 在仓库根目录生成轻量 `index.json`，以 modid 为键，预计算各版本的可用补丁数；
   每个模组的分组列表放在 `mods/<modid>/index.json`，不在根目录堆放 `<modid>.json`。
3. 从同一份扫描结果生成 README 的模组列表，保留列表标记之外的简介。
4. 自动提交有变更的索引 JSON 和 README 至触发分支，提交信息为 `chores: regenerate mod catalogue`。
5. 无内容变化不产生空提交；输出排序固定且没有当前时间字段，相同输入生成相同结果。
6. 删除条目后重建 JSON 和 README 列表，删除最后一个条目后移除对应 modid 的旧索引。
   旧分组索引从上一次 `index.json` 的 modid 键推导，只清理生成文件，不删除无关的 JSON。
   生成器也能读取旧版平铺总目录，校验后迁移旧的根目录 `<modid>.json`。

根 README 只保留简介和自动生成的模组列表。列表位于 `<!-- MOD_LIST_START -->` 与
`<!-- MOD_LIST_END -->` 之间，Action 只替换该区域；保留这对标记，技术说明统一放 `docs/`。
简介等玩家输入作为纯文本转义，避免注入 Markdown 表格、链接或 HTML。

Pull request 只校验和试生成，不回写；`workflow_dispatch` 可以手动重建。
默认仅授予只读权限，发布 job 单独使用 `contents: write`。不需要额外 Secret。
机器人提交使用 `GITHUB_TOKEN`，按 GitHub 机制不会再次触发 push workflow，避免无限循环。
同一分支的生成任务串行；推送遇到并发上传时，重新拉取最新树、重跑测试和生成，最多重试五次，
**不 force push**。受保护分支若禁止此令牌直接写入，需要管理员调整规则或另行接入发布分支。

无效上传会让 Action 失败并保留原目录索引，但已推送的原始文件不会被回滚。
因此云函数必须在提交前校验，不能把 Action 当作上传事务的唯一校验步骤。

### 下载入口

全局目录：

```text
https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/index.json
```

BaseMod 补丁列表：

```text
https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/mods/basemod/index.json
```

根 `index.json` 使用 **`schemaVersion: 2`**，包含补丁总数 `count` 和 **`mods` 对象**。
`mods` 的键为大小写精确匹配的目标 modid，每组包含：

- `workshopId`：目标模组的 Workshop ID，字符串。
- `count`：该模组的补丁总数，包含示例，不能直接作为兼容补丁徽标的数量。
- `versions`：以版本字符串为键、**不含示例的可用补丁数**为值的对象；`*` 表示通配版本。
  一条补丁声明多个版本时，分别计入对应版本；通配补丁仅计入 `*`，不复制到精确版本。
- `sha256`：`mods/<modid>/index.json` 文件原始字节的 SHA-256。详情更新但数量不变时也会改变，
  用来判断已缓存的补丁列表是否仍然有效。

下面是有两个精确版本补丁、一个通配补丁的结构示例；真实校验值由生成器填写：

```json
{
  "schemaVersion": 2,
  "count": 3,
  "mods": {
    "basemod": {
      "workshopId": "1605833019",
      "count": 3,
      "versions": {"*": 1, "5.56.0": 2},
      "sha256": "<详情文件的 SHA-256>"
    }
  }
}
```

根索引不重复携带补丁简介、作者和下载地址，避免每次刷新徽标下载全部详情。
每个 `mods/<modid>/index.json` 包含 `schemaVersion: 1`、`modId`、`workshopId`、`count`
和 `patches` 数组，其中保留完整补丁记录。上传元数据的 `schemaVersion` 仍为 1。
详情列表中每个补丁条目复制完整上传元数据（含 `description`），并附加：

- `patchModId`：从 JAR 读取的补丁 modid；
- `download_url`：直接指向该条目 JAR 的下载地址（中文、空格等已进行 URL 编码）；
- `size` / `sha256`：JAR 字节数和 SHA-256。

不输出 `path`、`url`、`metadataUrl`、嵌套 `download` 或路径式 `id` 字段。

这些附加字段均由生成器计算。通过 `target.versions` 选择适用条目。

### 客户端卡片与徽标流程

1. 每次刷新模组页面只请求一次根 `index.json`，用 ETag / `If-None-Match` 做条件请求。
   收到 304 时复用缓存的解析结果；收到新内容才解析一次，将 `mods` 和 `versions` 存为哈希映射。
   以一次不可变快照更新页面状态，网络与解析放在后台，避免每张卡片分别请求或重复解析。
   请求失败时保留上次成功的数据，界面标明缓存状态，不把失败误当作“没有补丁”。
2. 对每张卡片，按原模组的精确 `modid` 取 `mods[modId]`。
   该组存在且 `versions` 非空时展示“有补丁”徽标，满足有正式补丁就标记的规则；
   示例不会点亮徽标。需要展示兼容补丁数时，另算
   `compatibleCount = versions[currentVersion] + versions["*"]`，不存在的键按 0 处理。
   未知版本只计入 `*`，不把 `*` 当作实际版本相加两次。版本是原始字符串，
   不自动小写、不推断 SemVer 范围；多个模组共用的版本字符串不会互相影响。
3. 点击徽标才请求 `mods/<URL 编码的 modid>/index.json`，地址从固定布局构造。
   如果已有该 modid 的详情缓存且 SHA-256 与当前根索引一致，直接复用；否则获取详情并校验原始字节。
   同一 modid 的并发点击共用一个请求。后台解析后，只展示 `target.modId` 一致、
   且 `example != true` 的记录；当前版本在 `target.versions` 中或该字段为 `["*"]`
   的条目排在前面，其他条目仍展示并标明适用版本。详情文件包含示例以便其他视图使用，
   默认列表不能直接展示未筛选的数组。
4. 弹层从详情读取名称、`description`、作者和 `download_url`；玩家选择下载后再校验 JAR 的
   `size` / `sha256`。根索引更新时，移除已删除模组的详情缓存，并只使 SHA-256 发生变化的详情缓存失效，
   没有变化的模组可以继续用缓存。当前示例被标记为 `example: true`，因此默认不会点亮徽标。

若根索引大小为 B、当前页面有 M 张模组卡片，首次解析为 **O(B)**，全部徽标匹配平均为
**O(M)**，每张卡片仅做常数次哈希查找。点击详情后，只有该模组的 K 条补丁和每条至多 V 个
支持版本参与筛选，为 **O(K × V)**。根索引体积随 modid 数量及不同支持版本数增长，
不会重复下载每条补丁的长简介、邮箱和 JAR URL。首次解析整个 JSON 仍需要读取全部字节；
对象结构提升的是解析后的查找效率。

当前阶段仅实现仓库的生成协议；客户端按上述流程接入，不在本仓库增加 Android 代码。

Raw URL 默认跟随 `main`，覆盖上传后 GitHub CDN 可能暂时缓存旧内容。
详情文件与根索引的哈希不匹配时刷新根索引并重新获取详情，重试次数要有上限，
仍不一致则显示“目录更新中”并稍后重试；不能混用不同快照的数据或忽略校验。
JAR 哈希不匹配时同样刷新重试。需要不可变快照时可指定 `--ref <commit-sha>`
或把所有请求固定至同一 Git 提交。

## 现有云函数的后续接入约定

此节定义下一阶段协议，**本仓库不包含云函数接口实现**：

1. 客户端通过现有云函数上传 JAR + 上述 JSON；不要把 GitHub 凭证发到客户端。
2. 云函数进行身份/权限、限流、大小限制和完整校验，取得邮箱公开及再分发授权。
3. 计算固定目录，**一次 Git commit 原子写入** `metadata.json`、元数据 `jar` 指定的 JAR，
   以及有上传时的 `src/` 源码，同名直接替换。更新 JAR 名称时在同一提交移除原文件。
   推荐 Git Data API：以最新分支树为 base tree 创建对应 blob/tree，再创建 commit 和非强制更新 ref。
   不要使用两次 Contents API 写入制造两个不完整提交；无历史分支由初始化流程预先建立。
4. 每次成功上传均创建一个 commit，包括字节完全相同的重复上传。commit.message **准确为**：

   ```text
   <作者名称> <作者邮箱>
   ```

   例如 `Example Author <author@example.com>`（尖括号是实际格式的一部分）。
   同时设置 Git `author.name` / `author.email` 为这两个值，`committer` 为云函数服务身份；
   上传作者为玩家自报身份，不等于 GitHub 验证身份。
5. 非强制更新分支遇到竞争时，基于最新 HEAD 重建 tree/commit 后重试，避免覆盖其他人的上传。
   上传凭证应使用现有云函数的 **GitHub App installation token**，安装范围包含本仓库、
   授予 `Contents: Read and write`；它的 push 可以正常触发 Action。
   **不要用 workflow 的 `GITHUB_TOKEN` 做上传提交**，否则 GitHub 不会触发后续 Action。
6. 返回上传 commit SHA、路径及索引 URL。上传 commit 成功不代表异步索引已生成，
   客户端需区分“上传成功”和“目录可见”，处理 Action 失败或 GitHub Raw 缓存。

覆盖是产品约定，不是授权机制；公开上传前应单独决定作者身份验证和覆盖权限。
不能仅凭知道作者名/邮箱就视为本人，避免恶意覆盖别人的条目。

> **隐私：本仓库是公开仓库。作者邮箱会出现在元数据、生成索引和 Git 提交历史中，
> 后续覆盖文件不会删除历史邮箱。客户端必须事先明确告知并征得同意；建议玩家使用专用公开邮箱。**

## 示例与本地检查

示例位于 [`mods/basemod/Catalogue Example-ModinMobileSTS`](../mods/basemod/Catalogue%20Example-ModinMobileSTS)。
它只在加载时输出日志，不修复游戏，也不更改游戏行为；包含 Java 源码、真实编译的 Java 8 JAR、
两种清单和示例元数据。邮箱是 `example.com` 的占位邮箱，不是项目联系地址。

Python 3.11+ 即可运行，无需安装第三方依赖：

```bash
python3 -m unittest discover -s tests -v
python3 scripts/catalogue.py
python3 scripts/catalogue.py --check
```

可通过 `--root` 指定仓库副本、`--repository owner/repo` 指定下载源、`--ref branch-or-sha` 指定快照。
临时文件和构建输出置于已忽略的 `agent-tmp/`。示例重建方法见 [example.md](example.md)。

## 许可与来源

本仓库维护的工具、文档和示例代码采用 [MIT](../LICENSE) 许可。根目录许可**不自动重新许可**
玩家上传的第三方 JAR；每个作者必须确保有权分发自己的补丁，并附适用许可及必要的来源声明。
不得上传原版游戏 JAR、第三方模组本体、ModTheSpire 或 BaseMod 的再打包副本。
