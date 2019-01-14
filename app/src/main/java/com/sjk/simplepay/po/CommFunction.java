package com.sjk.simplepay.po;

import android.content.Intent;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;
import org.json.JSONObject;
import com.sjk.simplepay.ServiceMain;

public class CommFunction {
    private static final CommFunction INSTANCE  = new CommFunction();
    public static CommFunction getInstance() {
        return INSTANCE ;
    }

    public static String formateMessage(String func, Object... args){
        JSONObject msgObj = new JSONObject();
        try {
            msgObj.put("type", 1);
            msgObj.put("target", func);
            JSONArray arguments = new JSONArray();
            for (Object i:args){
                arguments.put(i);
            }
            msgObj.put("arguments",arguments);
            return msgObj.toString()+(char)0x1e;
        }catch (Exception e){

        }
        return null;
    }

    public static JSONArray getArgument(String message, String target){
        try {
            JSONObject obj = new JSONObject(message);
            if(obj.getString("target").equals(target)){
                return obj.getJSONArray("arguments");
            }
        }catch (Exception e){

        }
        return null;
    }

    public static int getType(String message){
        try {
            JSONObject obj = new JSONObject(message);
            return obj.getInt("type");
        }catch (Exception e){

        }
        return 0;
    }

    public static String getState(String message){
        if(message=="{}"){
            Log.d("收到空包", message);
            return null;
        }
        try {
            JSONObject receiveJson = new JSONObject(message);
            if(receiveJson!=null){
                return receiveJson.getString("arguments");
            }
        }catch (Exception e){

        }
        return null;
    }

    public String getLoginString(){
        String deviceAddress = Configer.getInstance().getSN();//android.os.Build.SERIAL;
        String password = Configer.getInstance().getToken();
        return formateMessage("DeviceLogin",deviceAddress,deviceAddress,password);
    }

    public String updateWechatStr(Boolean active){
        return formateMessage("DeviceAccountUpdate","wechat",active.toString(),Configer.getInstance().getUserWechat());
    }

    public String updateAlipayStr(Boolean active){
        return formateMessage("DeviceAccountUpdate","alipay",active.toString(),Configer.getInstance().getUserAlipay());
    }

    public String updateUnionpayStr(Boolean active){
        return formateMessage("DeviceAccountUpdate","unionpay",active.toString(),Configer.getInstance().getUser_unionpay());
    }

    public void postEventBus(String v){
        DataSynEvent dataSynEvent = new DataSynEvent();//创建事件对象
        dataSynEvent.setMessage(v);//給类对象设置数据
        EventBus.getDefault().post(dataSynEvent);//发送事件
    }

}
