# Mantle Unofficial 26.1 Port

Mantle 的 Minecraft 26.1 / NeoForge 非官方移植版，为
Tinkers' Construct Unofficial 26.1 Port 及其他依赖 Mantle 的模组提供公共 API。

当前正式版本：`26.1.2-1.12.0`

## 运行环境

- Minecraft `26.1.2`
- NeoForge `26.1.2.95` 或更高的 26.1.2 兼容版本
- Java `25`
- JEI `29.21.0` 或更高版本（可选）

本分支只承诺上述 26.1.2 目标，不承诺跨 Minecraft 大版本兼容。

## 构建与测试

Windows：

```powershell
.\gradlew.bat clean test build
.\gradlew.bat runGameTestServer
```

Linux/macOS：

```bash
./gradlew clean test build
./gradlew runGameTestServer
```

正式发布前还应执行 `runReleaseServer`，确认专用服务器能够启动、加载 Mantle，
并通过控制台 `stop` 正常退出。

## 26.1 兼容边界

NeoForge 26.1 已提供事务式资源处理 API，但旧的流体与物品处理器适配器在
26.1.2 中仍然存在。为了保持当前 TConstruct 26.1 代码和第三方依赖的二进制/API
兼容，本分支暂时保留这些入口，并在源码中将其明确标记为兼容边界。未来升级到
移除旧 API 的 NeoForge 版本时，Mantle 与 TConstruct 必须一起迁移，不能只替换
Mantle 一侧。

## 上游与问题反馈

本项目基于 [SlimeKnights/Mantle](https://github.com/SlimeKnights/Mantle) 移植，
不是 SlimeKnights 官方发布。请不要将本移植版的问题提交到上游仓库。

## 许可证

Mantle 及本移植版修改继续使用 [MIT License](LICENSE)。原项目版权归
SlimeKnights 及其贡献者所有。
