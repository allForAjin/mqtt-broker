package com.lmk.mqtt.exception;

public class NullChannelException extends RuntimeException{
    public NullChannelException() {
        super();
    }

    public NullChannelException(String message) {
        super(message);
    }


}
