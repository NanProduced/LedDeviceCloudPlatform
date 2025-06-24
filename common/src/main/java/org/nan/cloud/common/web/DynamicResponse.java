package org.nan.cloud.common.web;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.nan.cloud.common.exception.ExceptionEnum;

import java.io.Serializable;
import java.util.Arrays;
import java.util.function.Supplier;

@Data
@Slf4j
public class DynamicResponse<T> implements Serializable {

    private Integer code;

    private String msg;

    private T data;

    public static <T> DynamicResponse<T> of(Supplier<T> supplier) {
        DynamicResponse<T> dynamicResponse = new DynamicResponse<>();
        T data = supplier.get();
        dynamicResponse.setData(data);
        dynamicResponse.setCode(ExceptionEnum.SUCCESS.getCode());
        dynamicResponse.setMsg(ExceptionEnum.SUCCESS.getMessage());
        return dynamicResponse;
    }

    public static <T> DynamicResponse<T> of(Supplier<T> supplier, Object... args) {
        DynamicResponse<T> dynamicResponse = new DynamicResponse<>();
        T data = supplier.get();
        dynamicResponse.setData(data);
        dynamicResponse.setCode(ExceptionEnum.SUCCESS.getCode());
        dynamicResponse.setMsg(Arrays.toString(args));
        return dynamicResponse;
    }

    public static <T> DynamicResponse<T> of(Runnable runnable) {
        DynamicResponse<T> dynamicResponse = new DynamicResponse<>();
        runnable.run();
        dynamicResponse.setCode(ExceptionEnum.SUCCESS.getCode());
        dynamicResponse.setMsg(ExceptionEnum.SUCCESS.getMessage());
        return dynamicResponse;
    }

    public static <T> DynamicResponse<T> of(Runnable runnable, Object... args) {
        DynamicResponse<T> dynamicResponse = new DynamicResponse<>();
        runnable.run();
        dynamicResponse.setCode(ExceptionEnum.SUCCESS.getCode());
        dynamicResponse.setMsg(Arrays.toString(args));
        return dynamicResponse;
    }

    public static <T> DynamicResponse<T> custom(T data, Object... args) {
        DynamicResponse<T> dynamicResponse = new DynamicResponse<>();
        dynamicResponse.setCode(ExceptionEnum.SUCCESS.getCode());
        dynamicResponse.setData(data);
        dynamicResponse.setMsg(Arrays.toString(args));
        return dynamicResponse;
    }

    public static <T> DynamicResponse<T> success(T data) {
        DynamicResponse<T> dynamicResponse = new DynamicResponse<>();
        dynamicResponse.setCode(ExceptionEnum.SUCCESS.getCode());
        dynamicResponse.setMsg(ExceptionEnum.SUCCESS.getMessage());
        dynamicResponse.setData(data);
        return dynamicResponse;
    }

    public static <T> DynamicResponse<T> fail(T data, int code, Object... args) {
        DynamicResponse<T> dynamicResponse = new DynamicResponse<>();
        dynamicResponse.setCode(code);
        dynamicResponse.setData(data);
        dynamicResponse.setMsg(Arrays.toString(args));
        return dynamicResponse;
    }
}
