package com.nei10u.tip.vo;

import lombok.Data;

/**
 * 统一响应对象
 */
@Data
public class ResponseVO<T> {

    private Integer code;
    private String message;
    private T data;

    public static <T> ResponseVO<T> success(T data) {
        ResponseVO<T> response = new ResponseVO<>();
        response.setCode(200);
        response.setMessage("success");
        response.setData(data);
        return response;
    }

    public static <T> ResponseVO<T> error(String message) {
        ResponseVO<T> response = new ResponseVO<>();
        response.setCode(500);
        response.setMessage(message);
        return response;
    }

    public static <T> ResponseVO<T> error(Integer code, String message) {
        ResponseVO<T> response = new ResponseVO<>();
        response.setCode(code);
        response.setMessage(message);
        return response;
    }
}
