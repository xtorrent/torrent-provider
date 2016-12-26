package com.gko3.torrentprovider.common;

/**
 * return message wrapper
 *
 * @author Chaobin He<hechaobin1988@163.com>
 * @since JDK1.6
 */
public class ReturnMessage<T> {

    private boolean success = true;
    private String message = "";
    private T data;

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setData(T data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
