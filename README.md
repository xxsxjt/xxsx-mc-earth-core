# The Earth Core (`earth_core`)

地球系列统一 **瘦核心基座（Thin Core）**（NeoForge `26.1.2` / JDK `25` / `AGPL-3.0-only`）。

## v0.3.0 瘦身与统一命名重构
- **统一标识**：`modId = earth_core`、Java 包名 `com.xxsx.earth.core`、目录 `neoforge-26.1.2/`、产物 `earth-core-neoforge-26.1.2-0.3.0.jar`、`AGPL-3.0-only`。
- **彻底剥离旧单体包袱**：移除了待重构的 60+ 旧工业/化工机器、`living/` 聚落系统与 1,300+ 旧高分辨率中间产物贴图，JAR 体积从 **22.63 MB** 精简至 **~200 KB**。
- **核心仅保留全系列共享基座**：
  - `EarthCoreApi` v1（`GeologyQueryApi`、`ThermalUnitsApi`、`MaterialPropertyApi`、`RealitySupportApi`）
  - 共享数据组件（`Grade`、`MaterialBatch`）
  - 32 类基础真实矿床与围岩蚀变晕方块（含有限储量 `DepositBlockEntity` 与原版风格 16×16 像素贴图）
  - 3 件基础野外工具（地质锤、矿床探测仪、地球核心野外手册）
  - 8 项 `earth_lore` 基础学科根学识节点（供地理、生命、化学、工程、物理、社会、超凡等附属挂接）。
