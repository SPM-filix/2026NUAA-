package com.nuaa.recycle.common.error;

public enum ErrorCode {

    PARAM_INVALID(1001, "参数校验失败", 200),
    UNAUTHORIZED(1002, "未登录或令牌失效", 401),
    FORBIDDEN(1003, "当前角色无权访问该接口", 403),
    TOO_MANY_REQUESTS(1004, "请求过于频繁", 200),
    NOT_FOUND(1005, "资源不存在", 200),
    STATE_NOT_ALLOWED(1006, "当前状态不允许该操作", 200),

    BAD_CREDENTIALS(2001, "账号或密码错误", 200),
    ACCOUNT_DISABLED(2002, "账号已被禁用或未通过审核", 200),
    PHONE_EXISTS(2003, "手机号已注册", 200),
    RECYCLER_NOT_APPROVED(2004, "回收员申请未通过审核", 200),
    OLD_PASSWORD_WRONG(2005, "原密码错误", 200),
    NEW_PASSWORD_INVALID(2006, "新密码不符合策略", 200),
    TEMP_PASSWORD_EXPIRED(2007, "临时密码已过期", 200),

    POINTS_INSUFFICIENT(3001, "积分余额不足", 200),
    STOCK_INSUFFICIENT(3002, "商品库存不足", 200),
    PRODUCT_OFF_SHELF(3003, "商品已下架", 200),
    REDEEM_CODE_INVALID(3005, "核销码无效或不可核销", 200),
    DELIVERY_NOT_FOUND(3006, "投递流水不存在", 200),
    REVOKE_WINDOW_EXPIRED(3007, "已超过撤销时间窗口", 200),
    ALREADY_REVOKED(3008, "该流水已撤销", 200),
    CATEGORY_DISABLED(3009, "类别已停用", 200),
    WEIGHT_INVALID(3010, "重量超出允许范围", 200),
    BALANCE_NEGATIVE(3011, "操作会导致余额为负", 200),
    ORDER_STATE_INVALID(3012, "订单状态不允许该操作", 200),
    QUANTITY_INVALID(3013, "兑换数量非法", 200),
    IDEMPOTENCY_KEY_MISSING(3014, "幂等键缺失", 200),

    FILE_TYPE_UNSUPPORTED(4001, "文件类型不支持", 200),
    FILE_TOO_LARGE(4002, "文件超过大小限制", 200),

    INTERNAL_ERROR(5001, "系统内部错误", 200);

    private final int code;
    private final String message;
    private final int httpStatus;

    ErrorCode(int code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
