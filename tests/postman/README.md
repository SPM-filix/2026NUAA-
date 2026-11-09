# postman — 接口测试集合

| 文件 | 用途 |
| --- | --- |
| `2026NUAA-api.postman_collection.json` | 全量接口集合 |
| `local.postman_environment.json` | 本地环境变量 |

## 约定

- 集合按模块分文件夹：auth、user、delivery、exchange、mall、stats、upload、admin
- 环境文件只放 `baseUrl`、测试账号等非敏感变量；token 通过登录请求自动写入
- 不提交真实密码、JWT；测试账号在测试库中由脚本创建
- 接口变更后同步更新集合，并在 PR 中说明
- 阶段③开始时新增冒烟集合，覆盖「登录 → 投递 → 积分到账 → 兑换 → 核销」主流程
