# mocks — Mock 数据（阶段②）

前端在后端接口未完成时，依据 [docs/api.md](../docs/api.md) 与这里的示例响应并行开发。

## 约定

- 文件路径与接口对应，例如 `POST /api/delivery` → `mocks/delivery/create.json`
- 响应必须是完整的统一响应体，字段、类型、枚举与 `api.md` 一致
- 每个接口至少覆盖：正常响应、业务错误（如积分不足）、空列表
- 接口变更时先改 `api.md`，再同步 Mock，并在群里通知前端
- Mock 工具在前端脚手架确定后写入各模块 README（可选 Vite 插件、MSW 或 Postman Mock Server）

## 示例

见 [_example.json](_example.json)。

## 阶段③

联调开始后不再以 Mock 为准，转为真实接口；[tests/postman/](../tests/postman/) 负责验证真实响应与契约一致。
