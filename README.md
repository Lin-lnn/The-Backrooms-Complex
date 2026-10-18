# BackroomsComplex（后室综合体）

[![License](https://img.shields.io/badge/License-All%20Rights%20Reserved-red.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green.svg)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.x-orange.svg)](https://neoforged.net/)
[![Build](https://github.com/Lin-lnn/The-Backrooms-Complex/actions/workflows/build.yml/badge.svg)](https://github.com/Lin-lnn/The-Backrooms-Complex/actions/workflows/build.yml)

基于 Minecraft 1.21.1 + NeoForge 的自定义维度模组。

## 环境要求

| 项目 | 版本 |
|---|---|
| Minecraft | **1.21.1** |
| NeoForge | **21.1.x**（开发时使用 21.1.257） |
| Java | **21**（运行时与编译时均需 21；建议 21.0.3 及以上） |

## 构建

仓库已随附 Gradle Wrapper，无需本机安装 Gradle。

```bash
# Windows
gradlew.bat build

# Linux / macOS
./gradlew build
```

产物：`build/libs/backrooms_complex-1.0.0.jar`，放入游戏目录的 `mods/` 即可。

## 许可

All Rights Reserved。详见 [LICENSE](LICENSE)。