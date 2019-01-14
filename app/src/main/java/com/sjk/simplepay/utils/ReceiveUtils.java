package com.sjk.simplepay.utils;

import android.content.IntentFilter;

import com.sjk.simplepay.HKApplication;
import com.sjk.simplepay.ReceiverMain;

import static com.sjk.simplepay.HookMain.RECEIVE_BILL_ALIPAY;
import static com.sjk.simplepay.HookMain.RECEIVE_BILL_ALIPAY2;
import static com.sjk.simplepay.HookMain.RECEIVE_BILL_WECHAT;
import static com.sjk.simplepay.HookMain.RECEIVE_QR_ALIPAY;
import static com.sjk.simplepay.HookMain.RECEIVE_QR_UNIONPAY;
import static com.sjk.simplepay.HookMain.RECEIVE_QR_WECHAT;
import static com.sjk.simplepay.HookMain.RECEIVE_BILL_UNIONPAY;

/**
 * @ Created by Dpc
 * @ <p>TiTle:  ReceiveUtils</p>
 * @ <p>Description:</p>
 * @ date:  2018/10/14 12:02
 */
public class ReceiveUtils {

    public static void startReceive() {
        if (!ReceiverMain.mIsInit) {
            IntentFilter filter = new IntentFilter(RECEIVE_QR_WECHAT);
            filter.addAction(RECEIVE_QR_ALIPAY);
            filter.addAction(RECEIVE_QR_UNIONPAY);
            filter.addAction(RECEIVE_BILL_WECHAT);
            filter.addAction(RECEIVE_BILL_ALIPAY);
            filter.addAction(RECEIVE_BILL_ALIPAY2);
            filter.addAction(RECEIVE_BILL_UNIONPAY);
            HKApplication.app.registerReceiver(new ReceiverMain(), filter);
        }
    }

}
