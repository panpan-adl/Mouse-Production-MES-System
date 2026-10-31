# Mouse Production MES System

面向鼠标生产场景的制造执行系统，包含 Web 管理端、微信小程序、后端服务、数据库脚本与项目文档。

支持基础资料、BOM、订单、生产计划、工单、领料、报工、质量检验、库存与出入库、发货及追溯；同时提供业务看板、自然语言业务查询和报工后质检待办自动化。

## 目录

| 目录 | 内容 |
| --- | --- |
| `01-backend` | Spring Boot 后端 |
| `02-frontend-web` | Vue 3 Web 管理端 |
| `03-miniprogram` | 微信小程序 |
| `04-database` | 初始化及分阶段迁移 SQL |
| `05-docs` | 需求、设计、测试、操作与阶段交付文档 |
| `06-api-test` | Postman 接口集合 |
| `07-tools-barcodes` | 条码工具与业务验收脚本 |
| `08-environment` | Windows 环境配置与启动脚本 |

## 本地运行

使用 Java 17、Maven、Node.js、MySQL 和 Redis。前端依赖版本以 `02-frontend-web/package-lock.json` 为准。

1. 将 `08-environment/config.example.json` 复制为 `config.local.json`，根据本机安装位置修改各工具路径。
2. 在 MySQL 中准备 `mouse_mes` 数据库，按项目数据库文档选择初始化和迁移脚本。已有业务数据时先备份，勿直接执行名称含 `recreate` 的重建脚本。各 SQL 是不同阶段的脚本，不应无差别全部执行。
3. 检查 `01-backend/src/main/resources/application.yml` 中的数据库连接。在不上传的 `config.local.json` 中填写 `databasePassword`，或设置 `SPRING_DATASOURCE_PASSWORD` 环境变量。仓库不包含数据库密码。
4. 双击根目录 `启动MouseMES.cmd`，或执行 `08-environment/Start-MouseMES.ps1`。首次启动会构建后端并安装前端依赖。
5. 打开 <http://127.0.0.1:5173/login>。已初始化对应演示用户时，管理员账号为 `109001`，密码为 `123456`。

停止应用可使用根目录 `停止MouseMES.cmd`。停止脚本保留 MySQL 和 Redis 运行。

## 智能查询与自动化

智能查询默认使用本地规则模式。远程模型需另行配置 `MES_AI_MODE`、`MES_AI_BASE_URL`、`MES_AI_MODEL`、`MES_AI_API_KEY`；密钥仅在服务端配置，不应提交到仓库。本地协议测试不能替代真实模型联调。

`Start-Stage34-Demo.ps1` 使用已完成验收的隔离数据库和 `.runtime` 状态文件，供原开发机答辩演示使用；新克隆的项目不包含这些运行数据，不能直接执行该脚本。隔离库准备与验收方法参见 `05-docs/第三四阶段交付` 和 `07-tools-barcodes`。

## 验证与交付

后端可在 `01-backend` 执行 `mvn test`；前端可在 `02-frontend-web` 执行 `npm ci` 与 `npm run build`。分阶段业务验收脚本会创建或修改业务数据，必须阅读脚本说明并使用隔离测试库。

本机工具路径配置、运行日志、数据库备份、依赖缓存和编译产物均不随仓库上传。详细功能范围、验收记录及已知限制见 `05-docs/第三四阶段交付`。

## MES清单补齐版本（2026-10-24）

新增MES运营中心（`/mes`），包括工序排产与甘特图、合格/不良报工审批、统一线边仓、分批收发退货、半成品子工单、数值质检及返工报废、轮班日历、到期点检、装箱赋码、真实公告与运行监控。

已有阶段数据库在备份后应用`04-database/mes_completion.sql`；原业务库不可执行重建脚本。完整隔离复验使用`08-environment/Verify-MES-Completion.ps1`，在8085运行并创建测试业务数据。系统原有业务库与演示验收库分别使用5173和5174。

新增入口、操作顺序、83项业务验收与已知边界见[功能补齐与验收说明](05-docs/第三四阶段交付/MouseMES功能补齐与验收说明.md)。微信小程序尚未进行真机验收，前端构建和接口验收不能替代真实设备验证。


## 四档功能验收环境

四档功能整改与实际测试结果见 [最终验收记录](05-docs/四档功能整改与最终验收-20261030.md)。本机已准备独立演示入口 <http://127.0.0.1:5175/login>，重启使用 `08-environment/Start-Four-Tier-Demo.ps1`，停止使用 `08-environment/Stop-Four-Tier-Demo.ps1`。它使用新建隔离库，不替换原站点数据；完整重建步骤见验收记录。升级已有库前运行 `python 07-tools-barcodes/four_tier_database.py --database 实际数据库名`，该工具先备份，再执行幂等增量迁移。
