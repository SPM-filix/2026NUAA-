# docs — 文档索引

所有文档按开发阶段归档。阶段①结束前 [api.md](api.md) 必须冻结 v0.1，之后前后端才能开始并行开发。

| 阶段 | 文档 | 位置 | 负责人 | 状态 |
| --- | --- | --- | --- | --- |
| ① | 需求分析报告 | [requirements/需求分析报告-v1.md](requirements/需求分析报告-v1.md) | 魏鸿 | 评审稿 |
| ① | 用例图 | [requirements/用例图-v1.md](requirements/用例图-v1.md) | 魏鸿 | 评审稿 |
| ① | 功能结构图 | [requirements/功能结构图-v1.md](requirements/功能结构图-v1.md) | 魏鸿 | 评审稿 |
| ① | 页面清单与交互说明 | [requirements/页面清单与交互说明-v1.md](requirements/页面清单与交互说明-v1.md) | 魏鸿（汇总） | 初稿，待前端确认 |
| ① | 详细设计说明书、E-R 图、数据库设计 | [design/](design/) | 魏鸿 | 待编写 |
| ① | 接口契约 | [api.md](api.md) | 魏鸿起草，邓裕达 / 陈益苇确认 | 草案 v0.1 |
| ③ | 系统测试报告 | [reports/](reports/) | 陈益苇 | 待编写 |
| ④ | 开发报告、答辩 PPT | [reports/](reports/) | 魏鸿 | 待编写 |
| ② | 后端核心模块交接文档 | [../backend/docs/HANDOVER.md](../backend/docs/HANDOVER.md) | 邓裕达 | 初稿 |

## 文档约定

- 正式文档可用 Word 或 Markdown，图表源文件（drawio、Visio）一并提交
- 文件名带版本号，例如 `需求分析报告-v1.md`
- 需求变更先改文档再改代码；分工以 小组分工.md 为准
- 报告中的数据要可追溯：接口测试对应 `tests/postman/`，缺陷对应用例编号
