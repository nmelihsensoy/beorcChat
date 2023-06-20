package com.nmelihsensoy.beorc;

public class MessageData {
    private String message;
    private int type;

    public final static int TYPE_LEFT_BUBBLE = 1;
    public final static int TYPE_RIGHT_BUBBLE = 1;

    public MessageData(String message, int type) {
        this.message = message;
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
