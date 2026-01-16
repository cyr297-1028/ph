package cn.kmbeast.pojo.api;

import lombok.Getter;
import lombok.Setter;

/**
 * 通用响应 (修复版)
 * @param <T> 泛型
 */
@Setter
@Getter
public class ApiResult<T> extends Result<T> {

    /**
     * 数据总页，分页使用 (注意：这里已删除重复定义的 data 字段)
     */
    private Integer total;

    public ApiResult(Integer code) {
        super(code, "操作成功");
    }

    public ApiResult(Integer code, String msg) {
        super(code, msg);
    }

    public ApiResult(Integer code, String msg, T data) {
        // 调用父类构造方法，把数据存到父类的 data 字段里
        super(code, msg, data);
    }

    public ApiResult(Integer code, String msg, T data, Integer total) {
        super(code, msg, data);
        this.total = total;
    }

    public static <T> Result<T> success() {
        return new ApiResult<>(ResultCode.REQUEST_SUCCESS.getCode(), "操作成功", null);
    }

    public static <T> Result<T> success(T data) {
        return new ApiResult<>(ResultCode.REQUEST_SUCCESS.getCode(), "操作成功", data);
    }

    public static <T> Result<T> success(T data, Integer total) {
        return new ApiResult<>(ResultCode.REQUEST_SUCCESS.getCode(), "操作成功", data, total);
    }

    public static <T> Result<T> success(String msg) {
        return new ApiResult<>(ResultCode.REQUEST_SUCCESS.getCode(), msg, null);
    }

    public static <T> Result<T> success(String msg, T data) {
        return new ApiResult<>(ResultCode.REQUEST_SUCCESS.getCode(), msg, data);
    }

    public static <T> Result<T> error(String msg) {
        return new ApiResult<>(ResultCode.REQUEST_ERROR.getCode(), msg, null);
    }
}