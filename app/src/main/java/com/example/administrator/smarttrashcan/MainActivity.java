package com.example.administrator.smarttrashcan;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.icu.text.UnicodeSetSpanner;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.nfc.Tag;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {
    //private Timer timer;  //用于设置执行频率
    private   String ANDROID_ID;
    private Socket socket;
    private LocationClient mLocationClient = null;
    private BDLocationListener myListener = new MainActivity.MyLocationListener();
    private  double lat,lon;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);




        getAndroid_ID();
        mLocationClient = new LocationClient(this);
        //注册监听函数
        mLocationClient.registerLocationListener(myListener);
        initLocation();
        //开始定位
        mLocationClient.start();


        //设置更新位置频率
//        timer=new Timer();
//        timer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                //使用handler发送消息
//                Message message=new Message();
//                message.what=0;
//                mHandler.sendMessage(message);
//            }
//        },0,8000);


        Button btn = (Button) this.findViewById(R.id.mapButton);


        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO Auto-generated method stub
                Intent intent = new Intent();
                intent.setClass(MainActivity.this,MapActivity.class);
                startActivity(intent);

              //  new SendDateToServer("1").SendDataToServer("1234","12.123","123.0987");

            }
        });



    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        new SendDateToServer("3").SendDataToServer(ANDROID_ID,"","","");

    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("这里","LALALALALALAAL");
        new SendDateToServer("3").SendDataToServer(ANDROID_ID,"","","");
    }

    private void initLocation() {
        LocationClientOption option = new LocationClientOption();
        //可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
        //可选，默认gcj02，设置返回的定位结果坐标系
        option.setCoorType("bd09ll");
        //可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        int span = 5000;
        option.setScanSpan(span);
        //可选，设置是否需要地址信息，默认不需要
        option.setIsNeedAddress(true);
        //可选，默认false,设置是否使用gps
        option.setOpenGps(true);
        //可选，默认false，设置是否当GPS有效时按照1S/1次频率输出GPS结果
        option.setLocationNotify(true);
        //可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        option.setIsNeedLocationDescribe(true);
        //可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        option.setIsNeedLocationPoiList(true);
        //可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        option.setIgnoreKillProcess(false);
        //可选，默认false，设置是否收集CRASH信息，默认收集
        option.SetIgnoreCacheException(false);
        //可选，默认false，设置是否需要过滤GPS仿真结果，默认需要
        option.setEnableSimulateGps(false);
        mLocationClient.setLocOption(option);
    }

//设置循环执行的事件
//    private Handler mHandler = new Handler() {
//        @Override
//        public void handleMessage(Message msg) {
//            if (msg.what == 0) {
//
//            }
//        }
//    };
//


    //动态监听器，位置有所改变就会调用该方法
    public class MyLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            //经纬度
            lat = location.getLatitude();
            lon = location.getLongitude();
            //Toast.makeText(MainActivity.this,"经纬度："+lat+"??????>>>>"+lon,Toast.LENGTH_LONG).show();
            new SendDateToServer("1").SendDataToServer(ANDROID_ID,lat+"",lon+"",location.getStreet());
            Log.i("街道信息是：：：：：：", location.getStreet()+"++++"+location.getStreetNumber());
        }
    }


    //获取设备识别码ANDROID_ID
    public String getAndroid_ID() {
        ANDROID_ID = Settings.System.getString(getContentResolver(), Settings.System.ANDROID_ID);
        return ANDROID_ID;
    }

}
