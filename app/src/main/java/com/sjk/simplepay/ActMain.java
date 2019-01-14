package com.sjk.simplepay;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hdl.logcatdialog.LogcatDialog;
import com.sjk.simplepay.po.CommFunction;
import com.sjk.simplepay.po.Configer;
import com.sjk.simplepay.utils.HTTPSTrustManager;
import com.sjk.simplepay.utils.PayUtils;
import com.sjk.simplepay.utils.ReceiveUtils;
import com.sjk.simplepay.utils.SaveUtils;
import com.sjk.tpay.BuildConfig;
import com.sjk.tpay.R;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.sjk.simplepay.HookMain.UNIONPAY_CREAT_QR;

/**
 * @ Created by Dlg
 * @ <p>TiTle:  ActMain</p>
 * @ <p>Description: 启动首页，直接在xml绑定的监听
 * @ 其实我是不推荐这种绑定方式的，哈哈哈，为了项目简洁点还是就这样吧</p>
 * @ date:  2018/09/11
 */
public class ActMain extends AppCompatActivity {

    private EditText mEdtUrl;

    private EditText mEdtToken;

    private EditText mEdtSN;

    private EditText mEdtUserWechat;

    private EditText mEdtUserAlipay;

    private Button mBtnSubmit;

    private Button mBtnWechat;

    private Button mBtnAlipay;

    private Button mBtnUnionpay;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_main);
        mEdtUrl = ((TextInputLayout) findViewById(R.id.edt_act_main_url)).getEditText();
        mEdtToken = ((TextInputLayout) findViewById(R.id.edt_act_main_token)).getEditText();
        mEdtSN = ((TextInputLayout) findViewById(R.id.edt_act_main_sn)).getEditText();
        mEdtSN.setText(android.os.Build.SERIAL);
        mEdtUserWechat = ((TextInputLayout) findViewById(R.id.edt_act_main_user_wechat)).getEditText();
        mEdtUserAlipay = ((TextInputLayout) findViewById(R.id.edt_act_main_user_alipay)).getEditText();
        mBtnSubmit = findViewById(R.id.btn_submit);
        mBtnWechat = findViewById(R.id.btn_wechat);
        mBtnAlipay = findViewById(R.id.btn_alipay);
        mBtnUnionpay = findViewById(R.id.btn_unionpay);
        ((TextView) findViewById(R.id.txt_version)).setText("Ver：" + BuildConfig.VERSION_NAME);

	    HTTPSTrustManager.allowAllSSL();
        getPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBtnSubmit.setText(ServiceMain.mIsRunning ? "停止服务" : "确认配置并启动");
        mBtnWechat.setText(ServiceMain.mIsWechatRunning ? "停止微信服务" : "启动微信收款");
        mBtnAlipay.setText(ServiceMain.mIsAlipayRunning ? "停止支付服务" : "启动支付宝收款");
    }

    /**
     * 切换APP服务的运行状态
     *
     * @return
     */
    private boolean changeStatus() {
        ServiceMain.mIsRunning = !ServiceMain.mIsRunning;
        mBtnSubmit.setText(ServiceMain.mIsRunning ? "停止服务" : "确认配置并启动");

        if(!ServiceMain.mIsRunning)
        {
            if(ServiceMain.mIsWechatRunning) changeWechatStatus();
            if(ServiceMain.mIsAlipayRunning) changeAlipayStatus();
            if(ServiceMain.mIsUnionpayRunning) changeUnionpayStatus();
        }

        return ServiceMain.mIsRunning;
    }
    private boolean changeWechatStatus() {
        ServiceMain.mAccountChanged = true;
        ServiceMain.mIsWechatRunning = !ServiceMain.mIsWechatRunning;
        mBtnWechat.setText(ServiceMain.mIsWechatRunning ? "停止微信服务" : "启动微信收款");
        return ServiceMain.mIsWechatRunning;
    }
    private boolean changeAlipayStatus() {
        ServiceMain.mAccountChanged = true;
        ServiceMain.mIsAlipayRunning = !ServiceMain.mIsAlipayRunning;
        mBtnAlipay.setText(ServiceMain.mIsAlipayRunning ? "停止支付宝服务" : "启动支付宝收款");
        return ServiceMain.mIsAlipayRunning;
    }
    private boolean changeUnionpayStatus() {
        ServiceMain.mAccountChanged = true;
        ServiceMain.mIsUnionpayRunning = !ServiceMain.mIsUnionpayRunning;
        mBtnUnionpay.setText(ServiceMain.mIsUnionpayRunning ? "停止云闪付" : "启动云闪付收款");
        return ServiceMain.mIsUnionpayRunning;
    }


    private PackageInfo getPackageInfo(String packageName) {
        PackageInfo pInfo = null;
        try {
            //通过PackageManager可以得到PackageInfo
            PackageManager pManager = getPackageManager();
            pInfo = pManager.getPackageInfo(packageName, PackageManager.GET_CONFIGURATIONS);
            return pInfo;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pInfo;
    }

    /**
     * 点确认配置的操作
     *
     * @param view
     */
    public void clsSubmit(View view) {
        if (getPackageInfo(HookMain.WECHAT_PACKAGE) != null
                && !getPackageInfo(HookMain.WECHAT_PACKAGE).versionName.contentEquals("6.7.2")) {
            Toast.makeText(ActMain.this, "微信版本不对！官方下载版本号：6.7.2", Toast.LENGTH_SHORT).show();
        }
        if (getPackageInfo(HookMain.ALIPAY_PACKAGE) != null
                && !getPackageInfo(HookMain.ALIPAY_PACKAGE).versionName.contentEquals("10.1.35.828")) {
            Toast.makeText(ActMain.this, "支付宝版本不对！官方下载版本号：10.1.35.828", Toast.LENGTH_SHORT).show();
        }

        if (!changeStatus()) {
            return;
        }

        mEdtUrl.setText(mEdtUrl.getText().toString().trim());
        mEdtToken.setText(mEdtToken.getText().toString().trim());
        if (mEdtUrl.length() < 2 || mEdtToken.length() < 1
                || mEdtUserWechat.length() < 2 || mEdtUserAlipay.length() < 2) {
            Toast.makeText(ActMain.this, "请先输入正确配置！", Toast.LENGTH_SHORT).show();
            changeStatus();
            return;
        }
        if(!mEdtUrl.getText().toString().startsWith("http"))
        {
            Toast.makeText(ActMain.this, "请输入正确的网址！", Toast.LENGTH_SHORT).show();
            changeStatus();
            return;
        }
        if (!mEdtUrl.getText().toString().endsWith("/")) {
            mEdtUrl.setText(mEdtUrl.getText().toString() + "/");//保持以/结尾的网址
        }

        //下面开始获取最新配置并启动服务。
        Configer.getInstance()
                .setUrl(mEdtUrl.getText().toString());
        Configer.getInstance()
                .setToken(mEdtToken.getText().toString());
        Configer.getInstance()
                .setSN(mEdtSN.getText().toString());
        Configer.getInstance()
                .setUserWechat(mEdtUserWechat.getText().toString());
        Configer.getInstance()
                .setUserAlipay(mEdtUserAlipay.getText().toString());

        //保存配置
        new SaveUtils().putJson(SaveUtils.BASE, Configer.getInstance()).commit();


        //有的手机就算已经静态注册服务还是不行启动，再手动启动一下。
        startService(new Intent(this, ServiceMain.class));
        startService(new Intent(this, ServiceProtect.class));

        //广播也再次注册一下。。。机型兼容。。。
        ReceiveUtils.startReceive();
        addStatusBar();
    }

    /**
     * 测试微信获取二维码的功能
     *
     * @param view
     */
    public void clsWechatPay(View view) {
        if(!ServiceMain.mIsRunning)
        {
            Toast.makeText(ActMain.this, "请确认配置信息后，再启动微信收款功能", Toast.LENGTH_SHORT).show();
            return;
        }
        /*if (!changeWechatStatus()) {
            return;
        }*/
        changeWechatStatus();

        CommFunction.getInstance().postEventBus(CommFunction.getInstance().updateWechatStr(ServiceMain.mIsWechatRunning));
        //CommFunction.getInstance().postEventBus("updateActive");

        /*Intent broadCastIntent = new Intent();
        broadCastIntent.setAction(HookMain.RECEIVE_BILL_WECHAT);
        HKApplication.app.sendBroadcast(broadCastIntent);*/

//        String time = System.currentTimeMillis() / 1000 + "";
//        PayUtils.getInstance().creatWechatQr(this, 12, "test" + time);
    }


    /**
     * 测试支付宝获取二维码的功能
     *
     * @param view
     */
    public void clsAlipayPay(View view) {
        if(!ServiceMain.mIsRunning)
        {
            Toast.makeText(ActMain.this, "请确认配置信息后，再启动支付宝收款功能", Toast.LENGTH_SHORT).show();
            return;
        }
        /*if (!changeAlipayStatus()) {
            return;
        }*/
        changeAlipayStatus();

        CommFunction.getInstance().postEventBus(CommFunction.getInstance().updateAlipayStr(ServiceMain.mIsAlipayRunning));

        /*Intent broadCastIntent = new Intent();
        broadCastIntent.setAction(HookMain.RECEIVE_BILL_ALIPAY2);
        HKApplication.app.sendBroadcast(broadCastIntent);*/

//        String time = System.currentTimeMillis() / 1000 + "";
//        PayUtils.getInstance().creatAlipayQr(this, 12, "test" + time);
    }

    //云闪付
    public void clsUnionpayPay(View view) {
        if(!ServiceMain.mIsRunning)
        {
            Toast.makeText(ActMain.this, "请确认配置信息后，再启动云闪付收款功能", Toast.LENGTH_SHORT).show();
            return;
        }
        changeUnionpayStatus();

        CommFunction.getInstance().postEventBus(CommFunction.getInstance().updateUnionpayStr(ServiceMain.mIsUnionpayRunning));

        /*Intent intent = new Intent(UNIONPAY_CREAT_QR);
        String money ="12";
        String mark ="aaa";
        intent.putExtra("money",money);
        intent.putExtra("mark",mark);
        sendBroadcast(intent);*/
    }

    /**
     * 添加QQ群，保留版权哦。
     *
     * @param view
     */
    public void clsAddQq(View view) {
        LogcatDialog s= new LogcatDialog(this);
        s.searchTag="arik";
        s.show();
    }


    /**
     * 当获取到权限后才操作的事情
     */
    private void onPermissionOk() {
        mEdtUrl.setText(Configer.getInstance().getUrl());
        mEdtToken.setText(Configer.getInstance().getToken());
        mEdtSN.setText(Configer.getInstance().getSN());
        mEdtUserWechat.setText(Configer.getInstance().getUserWechat());
        mEdtUserAlipay.setText(Configer.getInstance().getUserAlipay());
        if (getIntent().hasExtra("auto")) {
            clsSubmit(null);
        }
    }


    /**
     * 在状态栏添加图标
     */
    private void addStatusBar() {
        NotificationManager manager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancelAll();

        PendingIntent pi = PendingIntent.getActivity(this, 0, getIntent(), 0);
        Notification noti = new Notification.Builder(this)
                .setTicker("程序启动成功")
                .setContentTitle("看到我，说明我在后台正常运行")
                .setContentText("始于：" + new SimpleDateFormat("MM-dd HH:mm:ss").format(new Date()))
                .setSmallIcon(R.mipmap.ic_launcher)//设置图标
                .setDefaults(Notification.DEFAULT_SOUND)//设置声音
                .setContentIntent(pi)//点击之后的页面
                .build();

        manager.notify(17952, noti);
    }


    /**
     * 获取权限。。有些手机很坑，明明是READ_PHONE_STATE权限，却问用户是否允许拨打电话，汗。
     */
    private void getPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            onPermissionOk();
            return;
        }
        List<String> sa = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            //申请READ_PHONE_STATE权限。。。。
            sa.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            sa.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            sa.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (sa.size() < 1) {
            onPermissionOk();
            return;
        }
        ActivityCompat.requestPermissions(this, sa.toArray(new String[]{}), 1);
    }


    /**
     * 获取到权限后的回调
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //获取到了权限之后才可以启动xxxx操作。
        for (int i = 0; i < grantResults.length; i++) {
            if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "部分权限未开启\n可能部分功能暂时无法工作。", Toast.LENGTH_SHORT).show();
                //如果被永久拒绝。。。那只有引导跳权限设置页
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!shouldShowRequestPermissionRationale(permissions[i])) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + getPackageName())); // 根据包名打开对应的设置界面
                        startActivity(intent);
                        onPermissionOk();
                        return;
                    }
                }
                break;
            }
        }
        onPermissionOk();
    }
}
