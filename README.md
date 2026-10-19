# Slay the Amethyst Player Mod Catalogue

玩家分享的 AI 补丁模组目录，供 Slay the Amethyst 后续上传/下载功能使用。

> 当前阶段：仓库结构、元数据协议、可加载的示例 JAR、校验与自动索引已建立。
> **尚未实现** Android 上传/下载 UI 和现有云函数的上传接口。

## 存储结构

```text
mods/
  <modid>/
    <mod_name>-<mod-author>/
      metadata.json          # 上传元数据
      patch.jar              # 上传的完整 AI 补丁 JAR
      README.md              # 可选：功能/修复说明
      src/                   # 可选：补丁源代码
catalogue/
  index.json                 # 全局 modid 列表
  <modid>.json               # 该 modid 的所有补丁及下载信息
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
- **同路径直接覆盖** `metadata.json` 和 `patch.jar`，不自动增加数字后缀，也不保留多个版本目录。
  不同作者名对应不同目录；邮箱不参与目录标识。同路径包括组合名称含连字符产生的碰撞。
  Git 历史保留此前版本。改名或更换作者名会形成新路径，旧路径不会自动删除。

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
  "jar": "patch.jar"
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
- `jar` 固定为 `patch.jar`；`example: true` 是可选的示例标识，客户端应默认隐藏或明确标注。
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

1. 运行回归测试，扫描并校验 `mods/` 中全部元数据和 JAR。
2. 按 modid 分组生成 `catalogue/<modid>.json`，生成 `catalogue/index.json` 总目录。
3. 自动提交有变更的 JSON 至触发分支，提交信息为 `chores: regenerate mod catalogue`。
4. 无内容变化不产生空提交；输出排序固定且没有当前时间字段，相同输入生成相同结果。
5. 删除条目后重建列表，删除最后一个条目后移除对应 modid 的旧索引。

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
https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/catalogue/index.json
```

BaseMod 补丁列表：

```text
https://raw.githubusercontent.com/ModinMobileSTS/SlaytheAmethystPlayerModCatalogue/main/catalogue/basemod.json
```

每个 modid 文件包含 `schemaVersion`、`modId`、`workshopId`、`count` 和 `patches` 数组。
每个条目复制完整上传元数据，并附加：

- `id`：`mods/<modid>/<补丁名>-<作者名>` 稳定仓库路径；
- `patchModId`：从 JAR 读取的补丁 modid；
- `metadataUrl`：元数据 Raw URL；
- `download.path` / `download.url`：JAR 路径和 URL（中文、空格等已进行 URL 编码）；
- `download.size` / `download.sha256`：JAR 字节数和 SHA-256。

这些字段不能由上传者伪造，均由生成器计算。通过 `target.versions` 选择适用条目。
Raw URL 默认跟随 `main`，覆盖上传后 GitHub CDN 可能暂时缓存旧内容；出现哈希不匹配应刷新索引重试，
不能忽略校验。需要不可变快照时可指定 `--ref <commit-sha>` 或把所有请求固定至同一 Git 提交。

## 现有云函数的后续接入约定

此节定义下一阶段协议，**本仓库不包含云函数接口实现**：

1. 客户端通过现有云函数上传 JAR + 上述 JSON；不要把 GitHub 凭证发到客户端。
2. 云函数进行身份/权限、限流、大小限制和完整校验，取得邮箱公开及再分发授权。
3. 计算固定目录，**一次 Git commit 原子写入** `metadata.json` 和 `patch.jar`，同名直接替换。
   推荐 Git Data API：以最新分支树为 base tree 创建两个 blob/tree，再创建 commit 和非强制更新 ref。
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

示例位于 [`mods/basemod/Catalogue Example-ModinMobileSTS`](mods/basemod/Catalogue%20Example-ModinMobileSTS)。
它只在加载时输出日志，不修复游戏，也不更改游戏行为；包含 Java 源码、真实编译的 Java 8 JAR、
两种清单和示例元数据。邮箱是 `example.com` 的占位邮箱，不是项目联系地址。

Python 3.11+ 即可运行，无需安装第三方依赖：

```bash
python3 -m unittest discover -s tests -v
python3 scripts/catalogue.py
python3 scripts/catalogue.py --check
```

可通过 `--root` 指定仓库副本、`--repository owner/repo` 指定下载源、`--ref branch-or-sha` 指定快照。
临时文件和构建输出置于已忽略的 `agent-tmp/`。示例重建方法见示例目录的 README。

## 许可与来源

本仓库维护的工具、文档和示例代码采用 [MIT](LICENSE) 许可。根目录许可**不自动重新许可**
玩家上传的第三方 JAR；每个作者必须确保有权分发自己的补丁，并附适用许可及必要的来源声明。
不得上传原版游戏 JAR、第三方模组本体、ModTheSpire 或 BaseMod 的再打包副本。
