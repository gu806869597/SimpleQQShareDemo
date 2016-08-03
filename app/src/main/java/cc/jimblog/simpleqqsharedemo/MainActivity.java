package cc.jimblog.simpleqqsharedemo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.connect.UserInfo;
import com.tencent.connect.common.Constants;
import com.tencent.connect.share.QQShare;
import com.tencent.connect.share.QzoneShare;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * @author JimHao
 * @version 1.1 第二次重新修改
 *
 */
public class MainActivity extends AppCompatActivity {
    //这里是注解框架
    @BindView(R.id.nickname)
    TextView nickName;
    @BindView(R.id.headimage)
    ImageView headimage;
    @BindView(R.id.login_to_qq)
    Button loginToQq;
    @BindView(R.id.share_to_qq)
    Button shareToQq;
    @BindView(R.id.share_to_qzone)
    Button shareToQzone;
    @BindView(R.id.logOut_to_qq)
    Button logOutToQq;
    @BindView(R.id.show_user_info)
    TextView showUserInfo;
    private UserInfo mInfo = null;  //用户类
    private static Tencent mTencent;  //主类
    private static final String APP_KEY = "222222"; //用户信息主键

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this); //注解初始化
        if (mTencent == null) {
            mTencent = Tencent.createInstance(APP_KEY, this);
        }
    }

    @OnClick({R.id.login_to_qq, R.id.share_to_qq, R.id.share_to_qzone, R.id.logOut_to_qq})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.login_to_qq:
                if (!mTencent.isSessionValid()) {
                    mTencent.login(this, "all", loginListener);
                    Log.d("SDKQQAgentPref", "FirstLaunch_SDK:" + SystemClock.elapsedRealtime());
                }else{
                    Toast.makeText(MainActivity.this, "用户已经登陆了", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.share_to_qq:
                shareToQQ();
                break;
            case R.id.share_to_qzone:
                shareToQzone();
                break;
            case R.id.logOut_to_qq:
                if (mTencent.isSessionValid()) {
                    mTencent.logout(this);
                    Toast.makeText(MainActivity.this, "退出登陆成功", Toast.LENGTH_SHORT).show();
                    return ; 
                }
                Toast.makeText(MainActivity.this, "用户还没有登录", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    /**
     * 处理登录完成的监听
     */
    IUiListener loginListener = new BaseUiListener() {
        @Override
        protected void doComplete(JSONObject values) {
            Log.d("SDKQQAgentPref", "AuthorSwitch_SDK:" + SystemClock.elapsedRealtime());
            Toast.makeText(MainActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
            initOpenidAndToken(values);
            updateUserInfo();
        }
    };

    /**
     * @param jsonObject  成功回调得到的jsonObject
     */
    public void initOpenidAndToken(JSONObject jsonObject) {
        try {
            String token = jsonObject.getString(Constants.PARAM_ACCESS_TOKEN);
            String expires = jsonObject.getString(Constants.PARAM_EXPIRES_IN);
            String openId = jsonObject.getString(Constants.PARAM_OPEN_ID);
            String time = "" + (System.currentTimeMillis() + Long.parseLong(expires) * 1000);
            if (!TextUtils.isEmpty(token) && !TextUtils.isEmpty(expires)
                    && !TextUtils.isEmpty(openId)) {
                mTencent.setAccessToken(token, expires);
                mTencent.setOpenId(openId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //得到用户信息的方法
    private void updateUserInfo() {
        if (mTencent != null && mTencent.isSessionValid()) {
            class BaseUIListener implements IUiListener {
                private String mScope;
                public BaseUIListener(String mScope) {
                    super();
                    this.mScope = mScope;
                }
                @Override
                public void onError(UiError e) {
                    //发生错误
                }
                @Override
                public void onComplete(final Object response) {
                    Message msg = new Message();
                    msg.obj = response;
                    msg.what = 0;
                    mHandler.sendMessage(msg);
                    new Thread() {
                        @Override
                        public void run() {
                            JSONObject json = (JSONObject) response;
                            if (json.has("figureurl")) {
                                Bitmap bitmap = null;
                                try {
                                    bitmap = getbitmap(json.getString("figureurl_qq_2"));
                                } catch (JSONException e) {

                                }
                                Message msg = new Message();
                                msg.obj = bitmap;
                                msg.what = 1;
                                mHandler.sendMessage(msg);
                            }
                        }
                    }.start();
                }
                @Override
                public void onCancel() {
                    //用户取消了
                }
            }
            BaseUIListener listener = new BaseUIListener("get_simple_userinfo");
            mInfo = new UserInfo(this, mTencent.getQQToken());  //这两个方法为必要的
            mInfo.getUserInfo(listener);
        }
    }

    /**
     * 处理得到的用户信息的Handler
     */
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                JSONObject response = (JSONObject) msg.obj;
                String rmsg = response.toString().replace(",", "\n");
                if (response.has("nickname")) {
                    try {
                        showUserInfo.setText(response.toString());
                        nickName.setText(response.getString("nickname"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } else if (msg.what == 1) {
                Bitmap bitmap = (Bitmap) msg.obj;
                headimage.setImageBitmap(bitmap);
            }
        }
    };

    /**
     * 从网络下载图片
     * @param imageUri
     * @return
     */
    public static Bitmap getbitmap(String imageUri) {
        // 显示网络上的图片
        Bitmap bitmap = null;
        try {
            URL myFileUrl = new URL(imageUri);
            HttpURLConnection conn = (HttpURLConnection) myFileUrl
                    .openConnection();
            conn.setDoInput(true);
            conn.connect();
            InputStream is = conn.getInputStream();
            bitmap = BitmapFactory.decodeStream(is);
            is.close();
            //Log.v(TAG, "image download finished." + imageUri);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return bitmap;
    }

    /**Activity回调
     * @param requestCode
     * @param resultCode
     * @param data
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == Constants.REQUEST_LOGIN ||
                resultCode == Constants.ACTIVITY_OK) {
            Tencent.onActivityResultData(requestCode,resultCode,data,loginListener);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * 分享到QQ
     */
    public void shareToQQ(){
        Bundle bundle = new Bundle();
        bundle.clear();
        bundle.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE,
                QQShare.SHARE_TO_QQ_TYPE_DEFAULT);
        bundle.putInt(QQShare.SHARE_TO_QQ_EXT_INT,
                QQShare.SHARE_TO_QQ_FLAG_QZONE_ITEM_HIDE);
        bundle.putString(QQShare.SHARE_TO_QQ_TITLE, "JimHao的个人博客");
        bundle.putString(QQShare.SHARE_TO_QQ_SUMMARY, "记录在指间的冒险物语");
        bundle.putString(
                QQShare.SHARE_TO_QQ_TARGET_URL,"http://121.41.82.6/wordpress/");
        ArrayList<String> imageUrls = new ArrayList<String>();
        imageUrls.add("http://121.41.82.6/wordpress/w3g.png");
        imageUrls.add("http://121.41.82.6/wordpress/w3g.png");
//	        bundle.putStringArrayList(QQShare.SHARE_TO_QQ_IMAGE_URL, imageUrls);
        bundle.putStringArrayList(QQShare.SHARE_TO_QQ_IMAGE_URL,imageUrls);
        mTencent.shareToQQ(this, bundle, new BaseUiListener());
    }

    /**
     * 分享到QQ空间的方法
     */
    public void shareToQzone(){
        final Bundle params = new Bundle();
        params.putInt(QzoneShare.SHARE_TO_QZONE_KEY_TYPE,QzoneShare.SHARE_TO_QZONE_TYPE_IMAGE_TEXT );
        params.putString(QzoneShare.SHARE_TO_QQ_TITLE, "JimHao的个人博客");
        params.putString(QzoneShare.SHARE_TO_QQ_TARGET_URL, "http://121.41.82.6/wordpress/");
        params.putString(QzoneShare.SHARE_TO_QQ_SUMMARY, "记录在指间的冒险物语");
        ArrayList<String> imageUrls = new ArrayList<String>();
        imageUrls.add("http://121.41.82.6/wordpress/w3g.png");
        imageUrls.add("http://www.opensnap.com.cn/images/300x250.jpg");
        params.putStringArrayList(QzoneShare.SHARE_TO_QQ_IMAGE_URL, imageUrls);
        doShareToQzone(params); //这里将封装好的参数发送出去
    }
    /**
     * 从线程中执行分享到QQ空间的方法
     * @param params 将分享事件的参数
     */
    private void doShareToQzone(final Bundle params) {
        final Activity activity =MainActivity.this;
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                MainActivity.mTencent.shareToQzone(activity, params, qZoneShareListener);
            }
        }).start();
    }

    /**
     * 分享到QQ空间的接口回调
     */
    IUiListener qZoneShareListener = new IUiListener() {
        @Override
        public void onCancel() {
            //Util.toastMessage(QZoneShareActivity.this, "onCancel: ");
            Toast.makeText(getApplicationContext(), "取消分享到空间", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onError(UiError e) {
            // TODO Auto-generated method stub
            //Util.toastMessage(QZoneShareActivity.this, "onError: " + e.errorMessage, "e");
        }

        @Override
        public void onComplete(Object response) {
            // TODO Auto-generated method stub
            //Util.toastMessage(QZoneShareActivity.this, "onComplete: " + response.toString());
            Toast.makeText(getApplicationContext(), "分享到空间成功", Toast.LENGTH_SHORT).show();
        }

    };

    /**
     * 接口基类
     */
    private class BaseUiListener implements IUiListener {

        @Override
        public void onComplete(Object response) {
            if (null == response) {
                Log.e("ResponseError", "返回结果为空，登陆失败");
                return;
            }
            JSONObject jsonResponse = (JSONObject) response;
            if (null != jsonResponse && jsonResponse.length() == 0) {
                Log.e("ResponseError", "返回结果为空，登陆失败");
                return;
            }
            doComplete((JSONObject) response);
        }
        protected void doComplete(JSONObject values) {
        }
        @Override
        public void onError(UiError e) {
        }
        @Override
        public void onCancel() {
        }
    }
}