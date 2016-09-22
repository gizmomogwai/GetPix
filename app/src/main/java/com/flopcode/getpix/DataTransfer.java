package com.flopcode.getpix;

import java.util.concurrent.CountDownLatch;

public class DataTransfer<T> extends CountDownLatch {

    private T data;
    private Exception exception;

    public DataTransfer(int count) {
        super(count);
    }

    public void setData(T data) {
        this.data = data;
        countDown();
    }

    public T getData() {
        if (data == null) {
            throw new RuntimeException("no data available");
        }
        return data;
    }

    public void exception(Exception e) {
        this.exception = e;
        countDown();
    }
}
