package com.water.flowable.response;

import lombok.Getter;

/**
 * 返回码
 */
@Getter
public enum StatusCode {

    OK(20000, "成功"),
    ERROR(20001, "失败"),
    LOGIN_ERROR(20002, "用户名或密码错误"),
    ACCESS_ERROR(20003, "权限不足"),
    TOKEN_ERROR(20004, "令牌错误"),
    TOKEN_EXPIRED(20005, "令牌过期"),
    INVALID_DATA(20006, "无效数据"),
    Validated_fail(20007, "数据校验失败"),
    SQLIntegrityConstraintViolation(20008,"数据库约束或者冲突"),
    VERITY_CODE_ERROR(20009,"验证码异常"),
    FEIGN_CALL_ERROR(20010,"feign远程调用失败"),
    FLOW_LIMIT_ERROR(20011,"请求超过流量限制"),
    UNAUTHORIZED(24000,"未获得授权");

    private final int value;

    private final String message;

    StatusCode(int value, String message) {
        this.value = value;
        this.message = message;
    }
}
