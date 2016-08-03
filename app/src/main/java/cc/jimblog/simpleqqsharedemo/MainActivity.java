package cc.jimblog.simpleqqsharedemo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mob.tools.utils.UIHandler;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cn.sharesdk.framework.Platform;
import cn.sharesdk.framework.PlatformActionListener;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.tencent.qq.QQ;
import cn.sharesdk.tencent.qzone.QZone;

/**
 *
 */
public class MainActivity extends AppCompatActivity implements Callback, PlatformActionListener {
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

    private static final int IS_USER = 1;           //用户已经登陆了
    private static final int USER_LOGIN = 2;        //登陆完成
    private static final int USER_CANCEL = 3;       //登录被取消
    private static final int USER_ERROR = 4;        //登录错误
    private static final int USER_OK = 5;           //认证完成

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this); //注解初始化
        ShareSDK.initSDK(this); //初始化SDK

    }

    @OnClick({R.id.login_to_qq, R.id.share_to_qq, R.id.share_to_qzone})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.login_to_qq:
                authorize(new QQ(this));    //登录方法
                break;
            case R.id.share_to_qq:
                shareToQQ();
            case R.id.share_to_qzone:
                setShareToQzone();
                break;
        }
    }

    /**
     * 分享到QQ
     */
    public void shareToQQ(){
        QQ.ShareParams sp = new QQ.ShareParams();   //生成一个QQParams变量
        sp.setTitle("JimHao的个人博客");
        sp.setText("听风小栈\t记录在指间的冒险物语");
        sp.setImageUrl("http://121.41.82.6/wordpress/w3g.png");//网络图片rul
        sp.setTitleUrl("http://121.41.82.6/wordpress/");  //网友点进链接后，可以看到分享的详情
        //3、非常重要：获取平台对象
        Platform qq = ShareSDK.getPlatform(QQ.NAME);
        qq.setPlatformActionListener(MainActivity.this); // 设置分享事件回调,如果不设置回调则传入Activity对象
        // 执行分享
        qq.share(sp);
    }

    /**
     * 分享到QQ空间
     */
    public void setShareToQzone(){
        QZone.ShareParams sp = new QZone.ShareParams();
        sp.setTitle("JimHao的个人博客");
        sp.setText("听风小栈\t记录在指间的冒险物语");
        sp.setImageUrl("http://121.41.82.6/wordpress/w3g.png");//网络图片rul
        sp.setTitleUrl("http://121.41.82.6/wordpress/");  //网友点进链接后，可以看到分享的详情
        Platform qzone = ShareSDK.getPlatform(QZone.NAME);
        qzone.setPlatformActionListener(MainActivity.this);
        qzone.share(sp);
    }
    //登录完成回调
    @Override
    public void onComplete(Platform platform, int action, HashMap<String, Object> hashMap) {
        if (action == Platform.ACTION_USER_INFOR) {
            UIHandler.sendEmptyMessage(USER_OK, this);
            login(platform.getName(), platform.getDb().getUserId(), hashMap);
            Log.i("Brust Link", hashMap.toString());
        }
    }

    @OnClick(R.id.logOut_to_qq)
    public void onClick() {
        Platform pf3=ShareSDK.getPlatform(MainActivity.this,QQ.NAME);
        if (pf3.isValid()) {
            pf3.removeAccount();//删除授权信息
            Toast.makeText(this,"退出成功，测试请点击上方登陆按钮，如果出现重新登陆界面则算成功", Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(MainActivity.this, "用户尚未登陆", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onError(Platform platform, int action, Throwable throwable) {
        if (action == Platform.ACTION_USER_INFOR) {   //当用户授权发送错误时
            UIHandler.sendEmptyMessage(USER_ERROR, this);
        }
        throwable.printStackTrace();
    }

    @Override
    public void onCancel(Platform platform, int i) {
        if (i == Platform.ACTION_USER_INFOR) {    //当用户取消操作时
            UIHandler.sendEmptyMessage(USER_CANCEL, this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ShareSDK.stopSDK(this); //当Activity关闭时关闭ShareSDK的服务
    }
    //在Handler中对消息类型进行处理
    @Override
    public boolean handleMessage(Message message) {
        switch (message.what) {
            case IS_USER:
                Toast.makeText(this, "用户信息已存在，正在跳转登录操作…", Toast.LENGTH_SHORT).show();
                break;
            case USER_LOGIN:
                //处理登陆后返回的消息
                Toast.makeText(MainActivity.this, "登陆成功", Toast.LENGTH_SHORT).show();
                HashMap<String, Object> userInfo = (HashMap<String, Object>) message.obj;
                String nicknameStr = (String) userInfo.get("nickname");
                String urlStr = (String) userInfo.get("figureurl_qq_1");
                Log.i("URLStr",urlStr);
                getUserBitmapThread thread = new getUserBitmapThread(urlStr);
                thread.start();
                nickName.setText(nicknameStr);
                break;
            case USER_CANCEL:
                Toast.makeText(this, "授权操作已取消", Toast.LENGTH_SHORT).show();
                break;
            case USER_ERROR:
                Toast.makeText(this, "授权操作遇到错误，请阅读Logcat输出", Toast.LENGTH_SHORT).show();
                break;
            case USER_OK:
                Toast.makeText(this, "授权成功，正在跳转登录操作…", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
        return false;
    }

    /**
     * 从线程中得到Bitmap用户头像
     */
    class getUserBitmapThread extends Thread{
        private String imageUrl ;
        public getUserBitmapThread(String url){
            imageUrl = url ;
        }

        @Override
        public void run() {
            Bitmap bitmap = getHttpBitmap(imageUrl);
            Message msg = getUserBitmapHandler.obtainMessage();
            if(bitmap != null){
                msg.obj = bitmap ;
                msg.arg1 = 1 ;
                msg.sendToTarget();
            }else{
                msg.obj  = null ;
                msg.arg1 = 0 ;
                msg.sendToTarget();
            }
        }
    }

    /**
     * 与线程对应的Handler处理器
     */
    Handler getUserBitmapHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            if(msg.arg1 == 1 && msg.obj != null){
                Bitmap bitmap = (Bitmap) msg.obj;
                headimage.setImageBitmap(bitmap);
            }
        }
    };
    //  authorize()方法将引导用户在授权页面输入帐号密码，然后目标平台将验证此用户(此方法在此例子中仅仅是QQ账号登陆时候使用)
    public void authorize(Platform plat) {
        if (plat.isValid()) {
            String userId = plat.getDb().getUserId();//获取id
            if (!TextUtils.isEmpty(userId)) {   //如果用户已经存在了
                UIHandler.sendEmptyMessage(IS_USER, this);
                login(plat.getName(), userId, null);//不等于null执行登陆，讲用户id发送至目标平台进行验证
                return;
            }
        }
        plat.setPlatformActionListener(this);
        plat.SSOSetting(true);
        plat.showUser(null);
    }

    /**
     * 发送到Handler处理消息的方法
     *
     * @param plat     数据库用户基类
     * @param userId   用户ID
     * @param userInfo 用户数据库中的数据
     */
    public void login(String plat, String userId, HashMap<String, Object> userInfo) {
        if(userInfo != null){
            Message msg = new Message();
            msg.what = USER_LOGIN;
            msg.obj = userInfo;
            UIHandler.sendMessage(msg, this);
        }
    }
    //从URL中得到图片的方法
    public static Bitmap getHttpBitmap(String url){
        URL myFileURL;
        Bitmap bitmap=null;
        try{
            myFileURL = new URL(url);
            //获得连接
            HttpURLConnection conn=(HttpURLConnection)myFileURL.openConnection();
            //设置超时时间为6000毫秒，conn.setConnectionTiem(0);表示没有时间限制
            conn.setConnectTimeout(6000);
            //连接设置获得数据流
            conn.setDoInput(true);
            //不使用缓存
            conn.setUseCaches(false);
            //这句可有可无，没有影响
            //conn.connect();
            //得到数据流
            InputStream is = conn.getInputStream();
            //解析得到图片
            bitmap = BitmapFactory.decodeStream(is);
            //关闭数据流
            is.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        return bitmap;
    }
}
