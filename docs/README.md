# docs — 文档索引

所有文档按开发阶段归档。阶段①结束前 [api.md](api.md) 必须冻结 v0.1，之后前后端才能开始并行开发。

| 阶段 | 文档 | 位置 | 负责人 | 状态 |
| --- | --- | --- | --- | --- |
| ① | 需求分析报告 | [requirements/](requirements/) | 魏鸿 | 待编写 |
| ① | 用例图、功能结构图 | [requirements/](requirements/) | 魏鸿 | 待编写 |
| ① | 详细设计说明书、E-R 图、数据库设计 | [design/](design/) | 魏鸿 | 待编写 |
| ① | 接口契约 | [api.md](api.md) | 邓裕达 | 草案 |
| ③ | 系统测试报告 | [reports/](reports/) | 陈益苇 | 待编写 |
| ④ | 开发报告、答辩 PPT | [reports/](reports/) | 魏鸿 | 待编写 |

## 文档约定

- 正式文档可用 Word 或 Markdown，图表源文件（drawio、Visio）一并提交
- 文件名带版本号，例如 `需求分析报告-v1.md`
- 需求变更先改文档再改代码；分工以 小组分工.md 为准
- 报告中的数据要可追溯：接口测试对应 `tests/postman/`，缺陷对应用例编号
