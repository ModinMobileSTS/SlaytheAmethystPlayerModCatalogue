# Catalogue Example

这是一个可加载的 AI 补丁格式示例，不是实际的游戏修复，也不是 BaseMod 官方发布。

- 目标：`basemod`，Workshop ID `1605833019`，声明适用版本 `5.56.0`。
- 行为：ModTheSpire 调用 `io.stamethyst.catalogue.example.CatalogueExampleMod.initialize()`
  时输出一条 `[amethyst-catalogue-example]` 日志。
- 症状/修复：无；不包含 `@SpirePatch`，不更改游戏状态、存档或 BaseMod。
- `example: true` 供未来客户端默认隐藏或标记这个测试条目，避免误认为真实修复。
- `catalogue-example@example.com` 是保留示例域名邮箱，不是项目联系邮箱。
- 已通过本地 JVM 的初始化方法调用及运行时注解检查；未做设备端完整 ModTheSpire 加载测试。
  仓库测试验证 JAR 结构、Java 8 字节码和元数据一致性。

`patch.jar` 中含真实编译的 Java 8 类、`ModTheSpire.json`、`agent-patch.json`；
后者与启动器现有 `AgentPatchModManager` 的打包描述格式一致。JAR 不捆绑游戏、
BaseMod 或 ModTheSpire 类。

## 重建

安装 JDK 11+，准备合法获得的 ModTheSpire JAR，然后在仓库根目录执行：

```bash
python3 scripts/build_example.py --modthespire /path/to/ModTheSpire.jar
python3 scripts/catalogue.py
python3 -m unittest discover -s tests -v
```

编译器使用 `--release 8 -g:none`，ZIP 文件顺序、时间戳与压缩方式固定；
构建临时目录位于已忽略的 `agent-tmp/` 下。源代码与工具适用仓库根目录 MIT 许可。
