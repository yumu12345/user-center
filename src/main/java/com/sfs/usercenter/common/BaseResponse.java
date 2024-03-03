package com.sfs.usercenter.common;

import lombok.Data;

/***
 * 返回通用类
 * @param <T>
 */
@Data
public class BaseResponse<T> {
    private int code;
    private String message;
    private T data;
    private String description;

    public BaseResponse(int code, T data,String message,String description) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.description=description;
    }

    public BaseResponse(ErrorCode errorCode){
        this(errorCode.getCode(),null,errorCode.getMessage(),errorCode.getDescription());
    }
}
