package com.lmk.mqtt.exception;

public class NullChannelIdException extends RuntimeException{
    public NullChannelIdException() {
    }

    public NullChannelIdException(String message) {
        super(message);
    }
}
