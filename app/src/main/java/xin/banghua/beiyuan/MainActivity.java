package xin.banghua.beiyuan;

import android.Manifest;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.SyncStateContract;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import io.rong.imkit.RongIM;
import io.rong.imlib.RongIMClient;
import io.rong.imlib.model.Message;
import io.rong.push.RongPushClient;
import io.rong.push.pushconfig.PushConfig;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import xin.banghua.beiyuan.Main3Branch.RongyunConnect;
import xin.banghua.beiyuan.SharedPreferences.SharedHelper;
import xin.banghua.beiyuan.Signin.SigninActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public Map<String,String> userInfo;

    //super果 18673668974
    private static final String token1 = "FnqsejuE+d830KfLY+PQ/r2N7KVbcvzutnIpZ+Sh9wDFjYfKqXxBctd4ec0OeFSRUAuX4eO3BltHQ60BVeZ2HA==";
    //贝吉塔 18673668975
    private String token2 = "";
    //希特 18673668976
    private static final String token3 = "PxvEuLbavYH5sNKqsxX7lb2N7KVbcvzutnIpZ+Sh9wDFjYfKqXxBcvx+1j1Oq44Au22Gnok3QTNHQ60BVeZ2HA==";


    private SharedHelper sh;
    private Context mContext = this;


    private TextView mTextMessage;


    //微信
    // APP_ID 替换为你的应用从官方网站申请到的合法appID
    private static final String APP_ID = "wx88888888";
    // IWXAPI 是第三方app和微信通信的openApi接口
    private IWXAPI api;
    private void regToWx() {
        // 通过WXAPIFactory工厂，获取IWXAPI的实例
        api = WXAPIFactory.createWXAPI(this, APP_ID, true);

        // 将应用的appId注册到微信
        api.registerApp(APP_ID);

        //建议动态监听微信启动广播进行注册到微信
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                // 将该app注册到微信
                api.registerApp(APP_ID);
            }
        }, new IntentFilter(ConstantsAPI.ACTION_REFRESH_WXAPP));

    }
    //初始化底部导航按钮监听
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_shenbian:

                    //Intent intent1 = new Intent(MainActivity.this, MainActivity.class);
                    //startActivity(intent1);
                    return true;
                case R.id.navigation_haoyou:

                    Intent intent2 = new Intent(MainActivity.this, Main2Activity.class);
                    startActivity(intent2);
                    return true;
                case R.id.navigation_xiaoxi:

                    //Intent intent3 = new Intent(MainActivity.this, Main3Activity.class);
                    //startActivity(intent3);
                    //启动会话列表
                    startActivity(new Intent(MainActivity.this, Main3Activity.class));
                    return true;
                case R.id.navigation_dongtai:

                    Intent intent4 = new Intent(MainActivity.this, Main4Activity.class);
                    startActivity(intent4);
                    return true;
                case R.id.navigation_wode:

                    Intent intent5 = new Intent(MainActivity.this, Main5Activity.class);
                    startActivity(intent5);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //获取融云token
        SharedHelper sh = new SharedHelper(mContext);
        token2 = sh.readRongtoken().get("Rongtoken");
        RongyunConnect rongyunConnect = new RongyunConnect();
        rongyunConnect.connect(token2);
        //判断是否登录
        ifSignin();

        //底部导航初始化和配置监听，默认选项
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        navigation.setSelectedItemId(R.id.navigation_shenbian);


        //定位问题
        localization();

        //融云
        PushConfig config = new PushConfig.Builder()
                .enableHWPush(true)
                .enableMiPush("123123", "2342342342")
                .enableMeiZuPush("234234234", "234234234")
                .enableFCM(true)
                .build();
        RongPushClient.setPushConfig(config);
        RongIM.setOnReceiveMessageListener(new RongIMClient.OnReceiveMessageListener() {
            @Override
            public boolean onReceived(Message message, int i) {
                return false;
            }
        });
    }

    public void ifSignin(){
        sh = new SharedHelper(getApplicationContext());
        userInfo = sh.readUserInfo();
        //Toast.makeText(mContext, userInfo.toString(), Toast.LENGTH_SHORT).show();
        if(userInfo.get("userID")==""){
            Intent intentSignin = new Intent(MainActivity.this, SigninActivity.class);
            startActivity(intentSignin);
        }else{
            //登录后，更新定位信息，包括经纬度和更新时间
            //获取用户id和定位值
            SharedHelper shlocation = new SharedHelper(getApplicationContext());
            Map<String,String> locationInfo = shlocation.readLocation();
            postLocationInfo(userInfo.get("userID"),locationInfo.get("latitude"),locationInfo.get("longitude"),"https://applet.banghua.xin/app/index.php?i=99999&c=entry&a=webapp&do=updatelocation&m=socialchat");
        }
    }

    //定位
    private void localization() {
        Location location = getLastKnownLocation();

        if (location!=null){
            Log.d(TAG, "localization: latitude:"+location.getLatitude());
            Log.d(TAG, "localization: longitude:"+location.getLongitude());
            //保存定位值
            SharedHelper shlocation = new SharedHelper(getApplicationContext());
            Float latitude = (float)(Math.round(location.getLatitude()*10))/10;
            Float longitude = (float)(Math.round(location.getLongitude()*10))/10;
            shlocation.saveLocation(latitude+"",longitude+"");
        }
    }

    private Location getLastKnownLocation() {
        Location l=null;
        LocationManager mLocationManager = (LocationManager)getApplicationContext().getSystemService(LOCATION_SERVICE);
        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED) {
                l = mLocationManager.getLastKnownLocation(provider);
            }
            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                bestLocation = l;
            }
        }
        return bestLocation;
    }


    //更新定位信息
    public void postLocationInfo(final String userID,final String latitude, final String longitude,final String url){
        new Thread(new Runnable() {
            @Override
            public void run(){
                OkHttpClient client = new OkHttpClient();
                RequestBody formBody = new FormBody.Builder()
                        .add("userID", userID)
                        .add("latitude", latitude)
                        .add("longitude",longitude)
                        .build();
                Request request = new Request.Builder()
                        .url(url)
                        .post(formBody)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                    //Log.d("form形式的post",response.body().string());
                    //不需要返回值
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
