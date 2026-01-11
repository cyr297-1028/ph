package cn.kmbeast.pojo.api;

/**
 * 通用响应 (修复版)
 *
 * @param <T> 泛型
 */
public class ApiResult<T> extends Result<T> {

    /**
     * 数据总页，分页使用 (data字段已在父类定义，此处只需定义特有的)
     */
    private Integer total;

    public ApiResult(Integer code, String msg) {
        super(code, msg);
    }

    public ApiResult(Integer code, String msg, T data) {
        super(code, msg, data);
    }

    // 带分页信息的构造
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

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }
}