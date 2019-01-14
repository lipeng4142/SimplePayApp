package com.sjk.simplepay;


import android.app.Activity;
import android.app.Application;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.sjk.simplepay.po.QrBean;
import com.sjk.simplepay.utils.PayUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.sjk.simplepay.HookMain.RECEIVE_BILL_UNIONPAY;
import static com.sjk.simplepay.HookMain.RECEIVE_QR_ALIPAY;
import static com.sjk.simplepay.HookMain.RECEIVE_QR_UNIONPAY;
import static com.sjk.simplepay.HookMain.UNIONPAY_CREAT_QR;

/**
 * @ Created by Dlg
 * @ <p>TiTle:  HookAlipay</p>
 * @ <p>Description:云闪的主要HOOK类</p>
 * @ date:  2018/9/28 22:57
 * @ QQ群：524901982
 */
public class HookUnionpay {

    public static boolean UNIONPAY_HOOK=false;
    public static    ClassLoader mClassLoader;
    public static Activity activity ;
    public static Service pushService;
    public static Application app ;
    public static Context context;

    public static MyHandler  handler;
    public static   Class UPPushService;
    public static boolean AUTO=false;
    public static boolean MIHOOK =false;

    public static final String checkOrder="com.android.unionpay.chexk";
    public static long lastCheckOrderTime = System.currentTimeMillis(); //最后查询收款列表时间
    public JSONArray qrList = new JSONArray(); //生成的二维码列表
    public static QRlist qRcreated = QRlist.getInstance();
    public static int getPayListLock = 0;

    /**
     * 主要的hook函数
     *
     * @param appClassLoader
     * @param context2
     */
    public void hook(ClassLoader appClassLoader, final Context context2) {
        context = context2;
        try {
            init(appClassLoader,context2);
            hookNotify(appClassLoader,context2);
        } catch (Error | Exception e) {
            e.printStackTrace();
        }
    }


    private void init(final ClassLoader appClassLoader,Context context) {
        try {
            if (!MIHOOK){
                XposedBridge.hookAllMethods(XposedHelpers.findClass("com.unionpay.push.receiver.miui.UPPushEventReceiverMiui", appClassLoader), "onNotificationMessageArrived", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        super.beforeHookedMethod(param);
                        XposedBridge.log("UPPushEventReceiverMiui onNotificationMessageArrived");
                        if (param.args!=null&&param.args.length>0){
                            mlog("onNotificationMessageArrived"+new Gson().toJson(param.args[1]));
                            String s = (String) XposedHelpers.getObjectField(param.args[1],"c");
                            JSONObject object =new JSONObject(s);
                            JSONObject body =object.optJSONObject("body");
                            String mTitle =body.optString("title");
                            String mContent =body.optString("alert");
                            if (("动账通知").equals(mTitle)&&mContent.contains("向您付款")){

                                String pre = mContent.split("元,")[0];
                                String parts[] = pre.split("通过扫码向您付款");
                                // Toast.makeText(activity, pre, Toast.LENGTH_SHORT).show();
                                if (parts.length == 2) {
                                    final String u = parts[0];
                                    final String m = parts[1];
                                    mlog("New Push Msg u:" + u + " m:" + m);
                                    Intent intent =new Intent(checkOrder);
                                    intent.putExtra("name",u);
                                    intent.putExtra("title",m);
                                    getContext().sendBroadcast(intent);
                                }
                            }
                        }
                    }
                });
                MIHOOK=true;
                mlog("MIUI 通知");
            }
        }catch (XposedHelpers.ClassNotFoundError e){
            mlog("非 MIUI 通知");
        }


        //XposedBridge.log("loadPackageParam.processName =");
        mClassLoader=appClassLoader;
        XposedHelpers.findAndHookMethod(ClassLoader.class, "loadClass", String.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                String cls_name = (String) param.args[0];
                if (cls_name.equals("com.unionpay.push.UPPushService")) {
                    if (UPPushService!=null)return;
                    UPPushService = (Class) param.getResult();
                    hookPushService(UPPushService);
                }
                if (cls_name.equals("com.unionpay.push.receiver.miui.UPPushEventReceiverMiui")) {

                    XposedBridge.hookAllMethods((Class ) param.getResult(), "onNotificationMessageArrived", new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            XposedBridge.log("UPPushEventReceiverMiui onNotificationMessageArrived");
                            if (param.args!=null&&param.args.length>0){
                                XposedBridge.log("onNotificationMessageArrived"+new Gson().toJson(param.args[1]));
                                String s = (String) XposedHelpers.getObjectField(param.args[1],"c");
                                JSONObject object =new JSONObject(s);
                                JSONObject body =object.optJSONObject("body");
                                String mTitle =body.optString("title");
                                String mContent =body.optString("alert");
                                if (("动账通知").equals(mTitle)&&mContent.contains("向您付款")){

                                    String pre = mContent.split("元,")[0];
                                    String parts[] = pre.split("通过扫码向您付款");
                                    // Toast.makeText(activity, pre, Toast.LENGTH_SHORT).show();
                                    if (parts.length == 2) {
                                        final String u = parts[0];
                                        final String m = parts[1];
                                        mlog("New Push Msg u:" + u + " m:" + m);
                                        Intent intent =new Intent(checkOrder);
                                        intent.putExtra("name",u);
                                        intent.putExtra("title",m);
                                        getContext().sendBroadcast(intent);
                                    }
                                }
                            }
                        }
                    });
                }
            }
        });
        XposedHelpers.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (param.thisObject.getClass().toString().contains("com.unionpay.activity.UPActivityMain")) {
                    if (UNIONPAY_HOOK) return;
                    UNIONPAY_HOOK = true;
                    activity = (Activity) param.thisObject;
                    app=activity.getApplication();
                    IntentFilter filter = new IntentFilter();
                    //filter.addAction("com.chuxin.socket.ACTION_CONNECT");
                    filter.addAction(UNIONPAY_CREAT_QR);
                    filter.addAction(checkOrder);
                    activity.registerReceiver(new MyBroadcastReceiver(), filter);
                    handler =new MyHandler(activity.getMainLooper());
                    // getVirtualCardNum(null);

                }
            }
        });
    }

    private void hookNotify(final ClassLoader appClassLoader,Context context) {
        final Class clazz=XposedHelpers.findClass("android.app.NotificationManager",appClassLoader);
        XposedHelpers.findAndHookMethod(clazz,"notify",String.class, int.class,Notification.class, new XC_MethodHook(){
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Notification a = (Notification)param.args[2];
                Log.d("arik", "捕获通知: "+param.toString());
                Log.d("arik", "捕获通知2: "+a.toString());
            }
        });
    }


    private void hookPushUPPushReceiver(Class upPushService) {
        XposedBridge.hookAllMethods(upPushService, "onReceive",  new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                if (param.args.length>0){
                    Object context =param.args[0];
                    Intent intent = (Intent) param.args[1];
                    XposedBridge.log(intent.getAction());

                }

            }
        });
    }


    private void hookPushService(Class upPushService) {
        XposedBridge.hookAllMethods(upPushService, "a",  new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                XposedBridge.log("UPPushService a");
                if (param.args!=null&&param.args.length>0){
                    Object uPPushMessage =param.args[0];
                    Object mText =XposedHelpers.callMethod(uPPushMessage,"getText");
                    String re = new Gson().toJson(mText) ;
                    XposedBridge.log("mText ="+re);
                    if (TextUtils.isEmpty(re)) return;
                    JSONObject object =new JSONObject(re);
                    String mTitle =object.optString("mTitle");
                    String mContent =object.optString("mContent");
                    if (("动账通知").equals(mTitle)&&mContent.contains("向您付款")){

                        String pre = mContent.split("元,")[0];
                        String parts[] = pre.split("通过扫码向您付款");
                        // Toast.makeText(activity, pre, Toast.LENGTH_SHORT).show();
                        if (parts.length == 2) {
                            final String u = parts[0];
                            final String m = parts[1];
                            mlog("New Push Msg u:" + u + " m:" + m);
                            Intent intent =new Intent(checkOrder);
                            intent.putExtra("name",u);
                            intent.putExtra("title",m);
                            getContext().sendBroadcast(intent);
                        }
                    }

                }

            }
        });


    }

    public static String encvirtualCardNo;
    public static Long time;
    public static void getVirtualCardNum(final GetCardNumListener listener) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mlog("GetVirtualCardNum");
                    String str2 = "https://pay.95516.com/pay-web/restlet/qr/p2pPay/getInitInfo?cardNo=&cityCode=" + Enc(getcityCd());
                    OkHttpClient client = new OkHttpClient();
                    mlog("GetVirtualCardNum...");
                    Request request = new Request.Builder()
                            .url(str2)
                            .header("X-Tingyun-Id", getXTid())
                            .header("X-Tingyun-Lib-Type-N-ST", "0;" + System.currentTimeMillis())
                            .header("sid", getSid())
                            .header("urid", geturid())
                            .header("cityCd", getcityCd())
                            .header("locale", "zh-CN")
                            .header("User-Agent", "Android CHSP")
                            .header("dfpSessionId", getDfpSessionId())
                            .header("gray", getgray())
                            .header("key_session_id", "")
                            .header("Host", "pay.95516.com")
                            .build();
                    mlog("访问获取二维码网址=>" + str2);
                    Response response = client.newCall(request).execute();
                    String RSP = response.body().string();
                    mlog("GetVirtualCardNum str2=>" + str2 + " RSP=>" + RSP);
                    String Rsp = Dec(RSP);
                    mlog("GetVirtualCardNum str2=>" + str2 + " RSP=>" + Rsp);
                    try {
                        encvirtualCardNo = Enc(new JSONObject(Rsp).getJSONObject("params").getJSONArray("cardList").getJSONObject(0).getString("virtualCardNo"));
                        mlog("encvirtualCardNo"+encvirtualCardNo);
                        if (listener!=null){
                            listener.success(encvirtualCardNo);
                        }
                    } catch (Throwable e) {
                        mlog(e);
                        if (listener!=null){
                            listener.error(e.getMessage()+e.getCause());
                        }
                    }



                } catch (Throwable e) {
                    mlog(e);
                    if (listener!=null){
                        listener.error(e.getMessage()+e.getCause());
                    }
                }
            }
        }).start();

    }

    public static String Dec(String src) {
        try {
            return (String) XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.unionpay.encrypt.IJniInterface",mClassLoader),"decryptMsg",src);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return "";
    }
    public static String Enc(String src) {
        try {
            return (String) XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.unionpay.encrypt.IJniInterface",mClassLoader),"encryptMsg",src);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return "";
    }
    private static String getXTid() {
        try {
            Class  m_s = XposedHelpers.findClass("com.networkbench.agent.impl.m.s",mClassLoader);
            Object f = XposedHelpers.callStaticMethod(m_s, "f");
            Object h = XposedHelpers.callMethod(f, "H");
            mlog("h=>" + h);
            Object i = XposedHelpers.callStaticMethod(m_s, "I");

            String xtid=       m_s.getDeclaredMethod("a", String.class, int.class).invoke(null, h, i).toString();
            //mlog("xtid:"+xtid+"");
            return xtid;
        } catch (Throwable e) {
            mlog(e);
        }
        return null;
    }
    private static String getSid() {
        String sid="";
        try {
            Object b = XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.unionpay.network.aa",mClassLoader), "b");
            sid=  XposedHelpers.callMethod(b, "e").toString();
        } catch (Throwable e) {
            mlog(e);
        }
        //mlog("sid:"+sid+"");
        return sid;
    }
    private static String geturid() {
        String Cacheurid="";
        try {
            Class data_d=XposedHelpers.findClass("com.unionpay.data.d",mClassLoader);
            Object o = XposedHelpers.callStaticMethod(data_d,"a",new Class[]{Context.class}, new Object[]{activity});
            String v1_2 = XposedHelpers.callMethod(XposedHelpers.callMethod(o, "A"), "getHashUserId").toString();
            if (!TextUtils.isEmpty(v1_2) && v1_2.length() >= 15) {
                Cacheurid= v1_2.substring(v1_2.length() - 15);
            }
        } catch (Throwable e) {
            mlog(e);
        }
        //mlog("Cacheurid:"+Cacheurid+"");
        return Cacheurid;
    }
    private static String getDfpSessionId() {
        String CacheDfpSessionId="";
        try {
            Object o = XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.unionpay.service.b",mClassLoader), "d");
            mlog("o=>" + o);
            CacheDfpSessionId = o.toString();
        } catch (Throwable e) {
            mlog(e);
        }
        //mlog("CacheDfpSessionId:"+CacheDfpSessionId+"");
        return CacheDfpSessionId;
    }
    private static String getcityCd() {
        String CachecityCd ="";
        try {
            CachecityCd = XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.unionpay.location.a",mClassLoader), "i").toString();
        } catch (Throwable e) {
            mlog(e);
        }
        //mlog("CachecityCd:"+CachecityCd+"");
        return CachecityCd;
    }
    private static String getgray() {
        String Cachegray ="";
        try {
            Object b = XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.unionpay.network.aa",mClassLoader), "b");
            Cachegray = XposedHelpers.callMethod(b, "d").toString();
        } catch (Throwable e) {
            mlog(e);
        }
        //mlog("Cachegray:"+Cachegray+"");
        return Cachegray;
    }


    private class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(UNIONPAY_CREAT_QR)){
                String money =intent.getStringExtra("money" );
                String mark =intent.getStringExtra("mark" );
                if (money==null){
                    money ="0.01";
                }
                if (mark==null){
                    mark ="测试";
                }
                mlog("money="+money+" mark="+mark);
                //  if (encvirtualCardNo==null||System.currentTimeMillis()-time>=60000){
                //   mlog("encvirtualCardNo"+encvirtualCardNo);
                final String finalMoney = money;
                final String finalMark = mark;
                getVirtualCardNum(new GetCardNumListener() {
                    @Override
                    public void success(String re) {
                        time=System.currentTimeMillis();
                        GenQrCode(finalMoney, finalMark);
                    }

                    @Override
                    public void error(String error) {
                        mlog("获取二维码网址错误="+error);
                    }
                });

                qRcreated.add(money,mark);
                mlog("vvvvvv"+qRcreated.get().toString());
                Message message =new Message();// 生成二维码过5秒再去找付款通知
                message.what=2;
                handler.sendMessageDelayed(message,5000);

            }else if (intent.getAction().equals(checkOrder)){
                if(System.currentTimeMillis()-lastCheckOrderTime>3000){
                    CheckNewOrder();
                }

                /*final String name =intent.getStringExtra("name");
                final String title =intent.getStringExtra("title");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //CheckNewOrder();
                        *//*Message message =new Message();
                        message.what=5;
                        message.obj=s;
                        handler.sendMessage(message);*//*
                    }
                }).start();*/
            }
        }
    }
    public static void GenQrCode(final String money, final String mark) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String mark1=mark;
                    String money1 =  new BigDecimal(money).setScale(2, RoundingMode.HALF_UP).toPlainString().replace(".", "");
                    mlog("FORRECODE GenQrCode:0 money:" + money1 + " mark:" + mark1);
                    String str2 = "https://pay.95516.com/pay-web/restlet/qr/p2pPay/applyQrCode?txnAmt=" + Enc(money1) + "&cityCode=" + Enc(getcityCd()) + "&comments=" + Enc(mark1) + "&virtualCardNo=" + encvirtualCardNo;
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder()
                            .url(str2)
                            .header("X-Tingyun-Id", getXTid())
                            .header("X-Tingyun-Lib-Type-N-ST", "0;" + System.currentTimeMillis())
                            .header("sid", getSid())
                            .header("urid", geturid())
                            .header("cityCd", getcityCd())
                            .header("locale", "zh-CN")
                            .header("User-Agent", "Android CHSP")
                            .header("dfpSessionId", getDfpSessionId())
                            .header("gray", getgray())
                            .header("key_session_id", "")
                            .header("Host", "pay.95516.com")
                            .build();

                    Response response = client.newCall(request).execute();
                    String RSP = response.body().string();
                    mlog("GenQrCode str2=>" + str2 + " RSP=>" + RSP);
                    String Rsp = Dec(RSP);
                    mlog( " RSP=>" + Rsp);

                    try {
                        JSONObject o = new JSONObject(Rsp);
                        o.put("mark", mark);
                        o.put("money", money);
                        Message message =new Message();
                        message.what=1;
                        message.obj=o.toString();
                        handler.sendMessage(message);
                    } catch (Throwable e) {
                        mlog(e);
                    }

                } catch (Throwable e) {
                    Message message =new Message();
                    message.what=4;
                    message.obj=money+"-"+mark;
                    handler.sendMessageDelayed(message,3000);
                    mlog(e);

                }

            }

        }).start();
    }

    public static void CheckNewOrder() {
        //mlog("当前lock:" + getPayListLock);
        //if(getPayListLock>1) return;
        //getPayListLock++;
        new Thread(new Runnable(){
            @Override
            public void run() {

        try {
            //mlog("FORRECODE CheckNewOrder user:" + user + " money:" + money);
            String str2 = "https://wallet.95516.com/app/inApp/order/list?currentPage=" + Enc("1") + "&month=" + Enc("0") + "&orderStatus=" + Enc("0") + "&orderType=" + Enc("A30000") + "&pageSize=" + Enc("10") + "";
            mlog("访问网址1:" + str2);
            OkHttpClient client =null;
            OkHttpClient.Builder builder = new OkHttpClient().newBuilder();
            builder.connectTimeout( 50, TimeUnit.SECONDS);
            builder.writeTimeout(50, TimeUnit.SECONDS);
            builder.readTimeout(50, TimeUnit.SECONDS);
            client=builder.build();
            Request request = new Request.Builder()
                    .url(str2)
                    .header("X-Tingyun-Id", getXTid())
                    .header("X-Tingyun-Lib-Type-N-ST", "0;" + System.currentTimeMillis())
                    .header("sid", getSid())
                    .header("urid", geturid())
                    .header("cityCd", getcityCd())
                    .header("locale", "zh-CN")
                    .header("User-Agent", "Android CHSP")
                    .header("dfpSessionId", getDfpSessionId())
                    .header("gray", getgray())
                    .header("Accept", "*/*")
                    .header("key_session_id", "")
                    .header("Host", "wallet.95516.com")
                    .build();
            mlog("访问网址1 client:" + str2.toString());

            Response response = client.newCall(request).execute();
            String RSP = response.body().string();
            mlog("CheckNewOrder str2=>" + str2 + " RSP=>" + RSP);
            String DecRsp = Dec(RSP);
            mlog("CheckNewOrder str2=>" + str2 + " DecRSP=>" + DecRsp);
            //这里有很多笔，可以自己调整同步逻辑s
            JSONArray o=null;
            o = new JSONObject(DecRsp).getJSONObject("params").getJSONArray("uporders");

            Boolean isexit = false;
            List<JSONObject> objlist = qRcreated.get();
            if(objlist==null || objlist.size()<1) return;
            for (int i = 0; i < o.length() && i<3; i++) { //只取最新的5条
                JSONObject p = o.getJSONObject(i);
                String orderid = p.getString("orderId");
                for (JSONObject s:objlist) {
                    //这里要转化成分，不然会出现0.1和0.10不相等的情况
                    if (PayUtils.formatMoneyToCent(p.getString("amount"))==PayUtils.formatMoneyToCent(s.getString("money"))) {
                        String v= DoOrderInfoGet(orderid,s.getString("mark"));
                        mlog("收到，准备发送:"+v);
                        Message message =new Message();
                        message.what=5;
                        message.obj=v;
                        handler.sendMessage(message);
                    }
                }
            }

                Message message =new Message();
                message.what=2;
                handler.sendMessageDelayed(message,20000);

        } catch (Throwable e) {
            Message message =new Message();
            message.what=2;
            handler.sendMessageDelayed(message,20000);

            mlog("cerror:"+e);

        }

            }
        }).start();
    }

    public static String DoOrderInfoGet(String orderid,String... param) {
        if (orderid.length() > 5) {
            try {
                String args = "{\"orderType\":\"21\",\"transTp\":\"simple\",\"orderId\":\"" + orderid + "\"}";
                String str2 = "https://wallet.95516.com/app/inApp/order/detail";
                mlog("访问网址2:" + str2);
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(str2)
                        .header("X-Tingyun-Id", getXTid())
                        .header("X-Tingyun-Lib-Type-N-ST", "0;" + System.currentTimeMillis())
                        .header("sid", getSid())
                        .header("urid", geturid())
                        .header("cityCd", getcityCd())
                        .header("locale", "zh-CN")
                        .header("User-Agent", "Android CHSP")
                        .header("dfpSessionId", getDfpSessionId())
                        .header("gray", getgray())
                        .header("Accept", "*/*")
                        .header("key_session_id", "")
                        .header("Content-Type", "application/json; charset=utf-8")
                        .header("Host", "wallet.95516.com")
                        .post(RequestBody.create(null, Enc(args)))
                        .build();
                Response response = client.newCall(request).execute();
                String RSP = response.body().string();
                mlog("DoOrderInfoGet str2=>" + str2 + " RSP=>" + RSP);
                String DecRsp = Dec(RSP);
                mlog("FORRECODE DoOrderInfoGet str2=>" + str2 + " DecRSP=>" + DecRsp);
                //这里有很多笔，可以自己调整同步逻辑s
                JSONObject params = new JSONObject(DecRsp).getJSONObject("params");
                String orderDetail = params.getString("orderDetail");
                mlog("FORRECODE DoOrderInfoGet str2=>" + str2 + " orderDetail=>" + orderDetail);
                JSONObject o = new JSONObject(orderDetail);
                String u = o.getString("payUserName");
                String mark = o.getString("postScript");
                String totalAmount = params.getString("totalAmount");
                mlog("qrlist======="+qRcreated.get().toString());
                mlog("FORRECODE DoOrderInfoGet str2=>" + str2 + " u:" + u + " mark:" + mark + " totalAmount:" + totalAmount);
                if(param!=null && param[0]!="" && param[0].equals(mark)){
                    mlog("mark相等，进入=======");
                    return  params.toString();
                }else {
                    return null;
                }

            } catch (Throwable e) {
                /*Message message =new Message();
                message.what=3;
                message.obj=orderid;
                handler.sendMessageDelayed(message,8000);*/

                mlog(e);
                return "ERR:" + e.getLocalizedMessage();
            }
        } else {
            return "ERROR_ORDER:" + orderid;
        }
    }
    private static class MyHandler extends Handler {
        public MyHandler(Looper mainLooper) {
            super(mainLooper);
        }
        public MyHandler( ) {

        }
        @Override
        public void handleMessage(Message msg) {
            if (msg.what==1){
                ///发送二维码
                String message = (String) msg.obj;
                Log.d("arik","云闪付二维码信息=>" + message);
                try {
                    JSONObject qrmessage = new JSONObject(message);
                    JSONObject params = (JSONObject) qrmessage.get("params");

                    QrBean qrBean = new QrBean();
                    qrBean.setChannel(QrBean.UNIONPAY);
                    qrBean.setUrl((String) params.getString("certificate"));
                    qrBean.setMark_sell((String) qrmessage.getString("mark"));
                    qrBean.setMoney(PayUtils.formatMoneyToCent(qrmessage.getString("money")));
                    Log.d("arik","云闪付二维码信息2=>" + qrBean.toString());
                    Intent intent = new Intent()
                            .putExtra("data", qrBean.toString())
                            .setAction(RECEIVE_QR_UNIONPAY);
                    context.sendBroadcast(intent);

                }catch (Exception e){
                    Log.e("arik","错误 =>" + e.toString());
                }

            }else if (msg.what==2){ //2是不断获取收款列表
                //String s = (String) msg.obj;
                //String amount =s.split("-")[0];
                //String name =s.split("-")[1];
                //getPayListLock--;
                if(System.currentTimeMillis() - lastCheckOrderTime >15000){
                    CheckNewOrder();
                    lastCheckOrderTime = System.currentTimeMillis();
                }
            }else if (msg.what==3){
                String s = (String) msg.obj;
                DoOrderInfoGet(s);
            }else if (msg.what==4){
                String s = (String) msg.obj;
                String amount =s.split("-")[0];
                String remark =s.split("-")[1];
                GenQrCode(amount,remark);
            }
            else if (msg.what==5){ //5为收到付款通知
                String message = (String) msg.obj;
                if(message==null || message=="" || message.contains("ERR")) return;
                Log.d("arik","云闪付付款通知=>" + message);
                try {
                    JSONObject qrmessage = new JSONObject(message);
                    String ss = qrmessage.getString("orderDetail");
                    JSONObject params = new JSONObject(ss);

                    QrBean qrBean = new QrBean();
                    qrBean.setChannel(QrBean.UNIONPAY);
                    qrBean.setMark_sell((String) params.getString("postScript"));
                    qrBean.setMoney(Integer.valueOf(qrmessage.getString("totalAmount")));
                    //Log.d("arik","云闪付付款通知2=>" + qrBean.toString());

                    Intent intent = new Intent()
                            .putExtra("data", qrBean.toString())
                            .setAction(RECEIVE_BILL_UNIONPAY);
                    context.sendBroadcast(intent);
                    Log.d("arik","已发送收款信息 =>" + qrBean.toString());

                    //清除
                    for (JSONObject s:qRcreated.qrList) {
                        if(Integer.valueOf(qrmessage.getString("totalAmount"))==PayUtils.formatMoneyToCent(s.getString("money")) &&
                                s.getString("mark").equals((String) params.get("postScript"))){
                            qRcreated.qrList.remove(s);
                        }
                    }

                }catch (Exception e){
                    Log.e("arik","错误 =>" + e.toString());
                }
            }
        }
    }
    public static void mlog(String s){
        XposedBridge.log(s);
    }
    public static void mlog(Throwable s){
        mlog(s.getMessage()+"------"+s.getCause());
    }
    public static Context getContext(){
        try {
            Class<?> ActivityThread =
                    Class.forName("android.app.ActivityThread");

            Method method = ActivityThread.getMethod("currentActivityThread");
            Object currentActivityThread = method.invoke(ActivityThread);//获取currentActivityThread 对象
            Method method2 = currentActivityThread.getClass().getMethod("getApplication");
            Context context =(Context)method2.invoke(currentActivityThread);//获取 Context对象
            XposedBridge.log("Context "+context);
            return context;
        } catch (Exception e) { e.printStackTrace(); }
        return null;

    }

}

class QRlist{
    private static final QRlist INSTANCE  = new QRlist();
    public static QRlist getInstance() {
        return INSTANCE ;
    }

    public List<JSONObject> qrList= new ArrayList<JSONObject>();

    public void add(String money,String mark){
        JSONObject s = new JSONObject();
        if(qrList!=null){
            for (JSONObject v:qrList) {
                try {
                    if(v.getString("money").equals(money)) return; //不重复添加相同金额
                }catch (Exception e){

                }
            }
        }

        try {
            s.put("money",money);
            s.put("mark",mark);
            s.put("addTime",System.currentTimeMillis());
            qrList.add(s);
            //if(qrList!=null)Log.e("arik","二维码列表qrList" + qrList.toString());
        }catch (Exception e){
            Log.e("arik","error:vvvvvv " + e);
        }
    }

    public List<JSONObject> get(){
        try {
            for (JSONObject s:qrList) {
                if(System.currentTimeMillis()-s.getLong("addTime")>10*60*1000){ //只存活10分钟
                    qrList.remove(s);
                }
            }
        }catch (Exception e){

        }
        if(qrList!=null)Log.e("arik","十分钟内二维码列表" + qrList.toString());
        return qrList;
    }
}