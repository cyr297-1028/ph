package cn.kmbeast.pojo.api;

/**
 * 响应基类 (修复版：已强制添加 data 字段)
 * @param <T>
 */
public class Result<T> {
    /**
     * 响应状态码
     */
    private Integer code;
    /**
     * 响应消息
     */
    private String msg;

    /**
     * 【核心修复】必须在这里定义 data，否则前端收不到数据！
     */
    private T data;

    public Result() {
    }

    public Result(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    // ================== ↓↓↓ 语音识别静态方法↓↓↓ ==================

    /**
     * 成功响应（带数据）
     * 对应 VoiceController 中的 Result.success(...)
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data); // 假设 200 为成功码
    }

    /**
     * 成功响应（不带数据）
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功");
    }

    /**
     * 失败响应
     * 对应 VoiceController 中的 Result.error(...)
     */
    public static <T> Result<T> error(String msg) {
        return new Result<>(500, msg); // 假设 500 为通用错误码
    }

    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, msg);
    }

    // ================== ↑↑↑语音识别结束 ↑↑↑ ==================

    // ↓↓↓↓↓ 这两个方法是 Jackson 序列化拿到数据的关键 ↓↓↓↓↓
    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
    // ↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑↑

    @Override
    public String toString() {
        return "Result{" +
                "code=" + code +
                ", msg='" + msg + '\'' +
                ", data=" + data +
                '}';
    }
}