package com.hust.generatingcapacity.config;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class ResponseMessage<T> {
    private Integer code;
    private String message;
    private T data;

    public ResponseMessage(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    //接口请求成功
    public static <T> ResponseMessage<T> success(T data) {
        return new ResponseMessage<>(200, "success", data);
    }

    //接口请求失败
    public static <T> ResponseMessage<T> error(String message) {
        return new ResponseMessage<>(400, message, null);
    }
}
