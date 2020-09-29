package com.water.flowable.response;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 返回结果封装
 * 状态码使用StatusCode
 */
@Data
@NoArgsConstructor
public class Result<T> implements Serializable {

    private boolean success;//是否成功
    private Integer code;//返回码，使用StatusCode
    private String message;//返回消息

    private T data;//返回数据

    public Result(boolean success, StatusCode code, String message, T data) {
        this.success = success;
        this.code = code.getValue();
        this.message = message;
        this.data = data;
    }

    public Result(boolean success, StatusCode code, String message) {
        this.success = success;
        this.code = code.getValue();
        this.message = message;
    }

    public Result(T data) {
        this.success = true;
        this.code = StatusCode.OK.getValue();
        this.message =StatusCode.OK.getMessage();
        this.data = data;
    }

    public static <T> Result<T> ok(T data){
        return new Result<>(true,StatusCode.OK,StatusCode.OK.getMessage(),data);
    }

    public static <T> Result<T> ok(){
        return new Result<>(true,StatusCode.OK,StatusCode.OK.getMessage());
    }

    public static <T> Result<T> error(){
        return new Result<>(false,StatusCode.ERROR,StatusCode.ERROR.getMessage());
    }

    public static <T> Result<T> error(T data){
        return new Result<>(false,StatusCode.ERROR,StatusCode.ERROR.getMessage(),data);
    }
}
