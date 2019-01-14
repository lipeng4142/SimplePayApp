package com.sjk.simplepay.po;

import com.alibaba.fastjson.JSON;
import com.sjk.simplepay.utils.SaveUtils;


/**
 * @ Created by Dlg
 * @ <p>TiTle:  Configer</p>
 * @ <p>Description: 用户的首页的配置Bean，单例模式类</p>
 * @ date:  2018/9/21
 */
public class Configer {

    private static Configer mConfiger;

    private String url = "http://p1.expal.io/";

    /**
     * 长度为8位，和服务端要设置为一样
     */
    private String token = "";

    /**
     * 服务器phone.php文件的真实文件名，改了的话，别人不方便恶意去访问
     */
    private String sn = android.os.Build.SERIAL;

    private String user_wechat = "wxid_"+(int) ((Math.random() * 9 + 1) * 1000);

    private String user_alipay = "2088****";

    private String user_unionpay = "姓名";

    public synchronized static Configer getInstance() {
        if (mConfiger == null) {
            mConfiger = new SaveUtils().getJson(SaveUtils.BASE, Configer.class);
            if (mConfiger == null) {
                mConfiger = new Configer();
            }
        }
        return mConfiger;
    }

    public String getUrl() {
        return url == null ? "" : url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getToken() {
        return token == null ? "" : token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getSN() {
        return sn == null ? "" : sn;
    }

    public void setSN(String sn) {
        this.sn = sn;
    }

    public String getUserWechat() {
        return user_wechat == null ? "" : user_wechat;
    }

    public void setUserWechat(String user_wechat) {
        this.user_wechat = user_wechat;
    }

    public String getUserAlipay() {
        return user_alipay == null ? "" : user_alipay;
    }

    public void setUser_unionpay(String user_alipay) {
        this.user_unionpay = user_unionpay;
    }

    public String getUser_unionpay() {
        return user_unionpay == null ? "" : user_unionpay;
    }

    public void setUserAlipay(String user_alipay) {
        this.user_alipay = user_alipay;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
