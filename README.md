# BackroomsComplex（后室综合体）

[![License](https://img.shields.io/badge/License-All%20Rights%20Reserved-red.svg)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green.svg)](https://www.minecraft.net/)
[![NeoForge](https://img.shields.io/badge/NeoForge-21.1.x-orange.svg)](https://neoforged.net/)
[![Build](https://github.com/Lin-lnn/The-Backrooms-Complex/actions/workflows/build.yml/badge.svg)](https://github.com/Lin-lnn/The-Backrooms-Complex/actions/workflows/build.yml)

基于 Minecraft 1.21.1 + NeoForge 的自定义维度模组。

## 内容概览

- **Level 0**：无限黄色房间迷宫，可变宽度走廊、多层堆叠、刮纸刀与墙纸玩法。
- **Level 1「宜居地带」**：12 格层高的两层停车场，天鹰 / 走廊 / 跃金 / 哥特 / 衔尾 / 花园 / 传说七个区段，金属门、板条箱与整层闪烁的照明。
- 近十种群系分别控制雾色与氛围；Level 1 的雾色随楼层和区段切换。

## 环境要求

| 项目 | 版本 |
|---|---|
| Minecraft | **1.21.1** |
| NeoForge | **21.1.x**（开发时使用 21.1.190） |
| Java | **21**（运行时与编译时均需 21；建议 21.0.3 及以上） |

## 构建

仓库已随附 Gradle Wrapper，无需本机安装 Gradle。把 `JAVA_HOME` 指向 JDK 21 后：

```bash
# Windows
gradlew.bat build

# Linux / macOS
./gradlew build
```

产物：`build/libs/backrooms_complex-1.0.3.jar`，放入游戏目录的 `mods/` 即可。

## 开发检查

```bash
python tools/audit.py      # 死代码 / 常量接线 / 层级隔离 / 孤儿注释
python tools/selfcheck.py  # JSON / 层高不变式 / 模型与贴图引用 / 语言键
```

两个脚本都不需要启动游戏；提交前应保持全部通过。

## 许可

All Rights Reserved。详见 [LICENSE](LICENSE)。