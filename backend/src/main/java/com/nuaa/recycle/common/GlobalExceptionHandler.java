package com.nuaa.recycle.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public R<Void> handleBiz(BizException e) {
        return R.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler({BindException.class, ConstraintViolationException.class, IllegalArgumentException.class})
    public R<Void> handleParam(Exception e) {
        String msg = e.getMessage();
        if (e instanceof BindException be && be.getFieldError() != null) {
            msg = be.getFieldError().getDefaultMessage();
        }
        return R.error(1001, msg != null ? msg : "参数校验失败");
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(UnauthorizedException.class)
    public R<Void> handleUnauthorized(UnauthorizedException e) {
        return R.error(1002, e.getMessage());
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(ForbiddenException.class)
    public R<Void> handleForbidden(ForbiddenException e) {
        return R.error(1003, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public R<Void> handleOther(Exception e) {
        log.error("系统内部错误", e);
        return R.error(5001, "系统内部错误");
    }
}
