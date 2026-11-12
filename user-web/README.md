# user-web — 用户端（居民 / 学生）

## 职责

注册登录、个人资料与密码、身份二维码、投递记录、积分余额与明细、积分商城与兑换核销码、公告与环保科普。

## 负责人

赵哲禹

## 技术栈

Vue 3 + Vite + Element Plus（2026-11-12 确定）。

## 协作约定

- 分支：`feature/user-web-<描述>`、`fix/user-web-<描述>`
- commit scope：`user-web`
- 阶段②开发期间只依赖 [docs/api.md](../docs/api.md) 与 [mocks/](../mocks/)，接口未就绪时不要等待后端
- 页面清单与交互在阶段①确认后补充到本文件
- 权限制约：用户不能修改自己的积分余额，不能查看他人数据；前端隐藏入口不等于权限控制，后端必须校验
- 不提交 `node_modules/`、`dist/`

## 计划结构

脚手架落地后参考：

```text
user-web/
└── src/
    ├── api/        # 按模块封装的请求
    ├── views/      # 页面
    ├── components/ # 复用组件
    ├── router/
    └── stores/
```
