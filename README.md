# Mantle Unofficial 26.1 Port

Mantle 的 Minecraft 26.1 / NeoForge 非官方移植版，为
Tinkers' Construct Unofficial 26.1 Port 及其他依赖 Mantle 的模组提供公共 API。

## 运行环境

- Minecraft `26.1.2`
- NeoForge `26.1.2.95` 或更高的 26.1.2 兼容版本
- Java `25`
- JEI `29.21.0` 或更高版本（可选）

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

## 上游与问题反馈

本项目基于 [SlimeKnights/Mantle](https://github.com/SlimeKnights/Mantle) 移植，
不是 SlimeKnights 官方发布。请不要将本移植版的问题提交到上游仓库。

## 许可证

Mantle 及本移植版修改继续使用 [MIT License](LICENSE)。原项目版权归
SlimeKnights 及其贡献者所有。
