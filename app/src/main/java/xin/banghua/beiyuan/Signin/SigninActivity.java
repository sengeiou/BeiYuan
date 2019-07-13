package xin.banghua.beiyuan.Signin;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import xin.banghua.beiyuan.MainActivity;
import xin.banghua.beiyuan.OkHttp.OkHttpHelper;
import xin.banghua.beiyuan.ParseJSON.ParseJSONObject;
import xin.banghua.beiyuan.R;
import xin.banghua.beiyuan.SharedPreferences.SharedHelper;

import static io.rong.imkit.fragment.ConversationFragment.TAG;

public class SigninActivity extends Activity {
    private Context mContext;
    Response response = null;

    //登录账号密码
    EditText userAccount;
    EditText userPassword;

    //三个按钮
    private Button signIn,signUp,findPassword;

    //okhttp


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signin);

        mContext = this;

        signIn = (Button) findViewById(R.id.signin_btn);
        signUp = (Button) findViewById(R.id.signup_btn);
        findPassword = (Button) findViewById(R.id.findPassword_btn);


        signUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SigninActivity.this,SignupActivity.class);
                startActivity(intent);
            }
        });


        signIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                userAccount = (EditText) findViewById(R.id.userAccount);
                userPassword = (EditText) findViewById(R.id.userPassword);
                if(userAccount.getText().toString()==""||userPassword.getText().toString()==""){
                    Toast.makeText(mContext, "请输入账号密码", Toast.LENGTH_LONG).show();
                }else{
                    postSignIn("https://applet.banghua.xin/app/index.php?i=99999&c=entry&a=webapp&do=signin&m=socialchat",userAccount.getText().toString(),userPassword.getText().toString());
                }
            }
        });
    }

    @SuppressLint("HandlerLeak")
    private Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    String resultJson = msg.obj.toString();
                    try {
                        JSONObject object = new JSONObject(resultJson);
                        Log.d("进入handler",object.getString("error"));
                        if (object.getString("error").equals("0")){
                            //通知
                            Log.d("发送通知",object.getString("info"));
                            Toast.makeText(mContext,object.getString("info"),Toast.LENGTH_LONG);
                            //保存用户数据
                            Log.d("保存用户数据",object.getString("userID"));
                            mContext = getApplicationContext();
                            SharedHelper sh = new SharedHelper(mContext);
                            sh.saveUserInfo(object.getString("userID"),object.getString("userNickName"),object.getString("userPortrait"),object.getString("userAge"),object.getString("userGender"),object.getString("userProperty"),object.getString("userRegion"));
                            //判断token是否存在

                            postRongyunUserRegister("https://rongyun.banghua.xin/RongCloud/example/User/userregister.php",object.getString("userID"),object.getString("userNickName"),object.getString("userPortrait"));

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case 2:
                    String resultJson1 = msg.obj.toString();
                    try {
                        JSONObject object1 = new JSONObject(resultJson1);
                        Log.d("融云token获取",object1.getString("code"));
                        if (object1.getString("code").equals("200")){

                            //保存融云token
                            Log.d("融云token",object1.getString("token"));
                            mContext = getApplicationContext();
                            SharedHelper sh = new SharedHelper(mContext);
                            sh.saveRongtoken(object1.getString("token"));

                            //跳转首页
                            //Log.d("跳转首页",object1.getString("userNickName"));
                            Intent intent = new Intent(SigninActivity.this, MainActivity.class);
                            startActivity(intent);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
            }

        }
    };

    //TODO 登录 form形式的post
    public void postSignIn(final String url, final String userAccount, final String userPassword){
        new Thread(new Runnable() {
            @Override
            public void run(){
                OkHttpClient client = new OkHttpClient();
                RequestBody formBody = new FormBody.Builder()
                        .add("userAccount", userAccount)
                        .add("userPassword",userPassword)
                        .build();
                Request request = new Request.Builder()
                        .url(url)
                        .post(formBody)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    Log.d(TAG, "run: 看看"+response.toString());
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                    //Log.d("form形式的post",response.body().string());
                    //格式：{"error":"0","info":"登陆成功"}
                    Message message=handler.obtainMessage();
                    message.obj=response.body().string();
                    message.what=1;
                    JSONObject jsonObject = new ParseJSONObject(message.obj.toString()).getParseJSON();
                    Log.d("登录信息",jsonObject.getString("info"));
                    handler.sendMessageDelayed(message,10);
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //TODO 登录 form形式的post
    public void postRongyunUserRegister(final String url, final String userID, final String userNickName,final String userPortrait){
        new Thread(new Runnable() {
            @Override
            public void run(){
                Log.d("融云注册信息","进入融云注册");
                OkHttpClient client = new OkHttpClient();
                RequestBody formBody = new FormBody.Builder()
                        .add("userID", userID)
                        .add("userNickName",userNickName)
                        .add("userPortrait",userPortrait)
                        .build();
                Request request = new Request.Builder()
                        .url(url)
                        .post(formBody)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    //Log.d(TAG, "run: 融云注册信息返回"+response.toString());
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                    //Log.d("融云注册信息返回",response.body().string());
                    //格式：{"error":"0","info":"登陆成功"}
                    Message message=handler.obtainMessage();
                    message.obj=response.body().string();
                    Log.d("融云注册信息返回",message.obj.toString());
                    message.what=2;
                    JSONObject jsonObject = new ParseJSONObject(message.obj.toString()).getParseJSON();
                    Log.d("融云注册信息token",jsonObject.getString("token"));
                    handler.sendMessageDelayed(message,10);
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
