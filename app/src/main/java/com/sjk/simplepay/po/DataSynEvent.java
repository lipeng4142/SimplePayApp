package com.sjk.simplepay.po;

import org.json.JSONObject;

public class DataSynEvent {
    private String message;
    private JSONObject obj;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        clear();
        this.message = message;
    }

    public JSONObject getObj() {
        return obj;
    }

    public void setObj(JSONObject objmessage) {
        clear();
        this.obj = objmessage;
    }

    public void clear(){
        message = null;
        obj = null;
    }

}
