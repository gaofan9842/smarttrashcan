package com.example.administrator.smarttrashcan;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import com.baidu.mapapi.*;
import android.graphics.Point;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.Poi;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Polyline;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;
import com.suke.widget.SwitchButton;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

import static android.content.ContentValues.TAG;
import static java.lang.Math.abs;


public class userRouteMapActivity extends AppCompatActivity {

    public static Handler handler2 = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    private MapView mapView = null;
    private BaiduMap baiduMap;
    private Polyline mPolyline;
    private Timer timer;  //用于设置执行频率
    public  static double initLat;
    public  static  double initLon;
    public  static  String andoidId;

    // 为了返回map界面时保持原样
    // 由于map界面调用了finish，所以需要通过捕获返回按键手动返回
    public static boolean recheck=false,jiaocheck=true,weicheck=false,dingcheck=false,lacheck=false;
    public static  float zoom = 18.0f;

    List<LatLng> points = new ArrayList<LatLng>();//用于储存所有信息点
    List<LatLng> dotPoints = new ArrayList<LatLng>(); //绘制虚线（用户下线时造成的路径间断）
    List<LatLng> linePoints = new ArrayList<LatLng>(); //绘制实线

    //接收服务器返回的数据
    public static List<Double> pointLat = new ArrayList<Double>();
    public static List<Double> pointLon = new ArrayList<Double>();
    public static List<String> pointTime = new ArrayList<String>(); //记录经过节点的时间

    ArrayList<MarkerInfo> infos = new ArrayList<MarkerInfo>();
    private com.suke.widget.SwitchButton switchButton; //开启轨迹
    private boolean route_flag=false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_route_map);
        //获取地图控件引用
        MapActivity.marker_click_flag=0;
        mapView = (MapView) findViewById(R.id.bmapView2);

        // 归位按钮
        ImageButton imb = findViewById(R.id.mylocation);
        imb.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View arg0) {
                zoom=baiduMap.getMapStatus().zoom;
                LatLng center_point = new LatLng(initLat,initLon);
                setPosition2Center(baiduMap, center_point, zoom);
            }
        });



        initMap();
        setMarker();

        //设置更新位置频率,利用timer实现实时查询marker的最新位置，实现实时更新
        timer=new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                //使用handler发送消息
                Message message=new Message();
                message.what=0;
                mHandler.sendMessage(message);
            }
        },0,1000);

        switchButtonDo();


        //地图点击事件
//        baiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
//            @Override
//            public void onMapClick(LatLng arg0) {
//                Toast.makeText(userRouteMapActivity.this,">>>>>",Toast.LENGTH_LONG).show();
//            }
//            @Override
//            public void onMapPoiClick(MapPoi poi) {
//                Toast.makeText(userRouteMapActivity.this,"???????",Toast.LENGTH_LONG).show();
//
//            }
//        });

        //添加marker点击事件的监听
        baiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                //获得marker 的info
                Bundle bundle = marker.getExtraInfo();
                MarkerInfo getinfo = (MarkerInfo) bundle.getSerializable("info");

                String text="";
                text=getinfo.getName();
               // ToastUtil.showToast(userRouteMapActivity.this,"???....???");
                Toast.makeText(userRouteMapActivity.this,text,Toast.LENGTH_LONG).show();
                return true;
            }
        });


    }


//    //设置循环执行的事件，查询marker实时位置
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                if(route_flag==false) setMarker();
                else drawRoute();
            }
        }
    };


    public void initMap(){
        //得到地图实例
        baiduMap = mapView.getMap();

        LatLng center_point = new LatLng(initLat,initLon);
        setPosition2Center(baiduMap, center_point, zoom);
        //设置地图类型,普通地图
        baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        //开启交通图
        baiduMap.setTrafficEnabled(false);
        //关闭缩放按钮
        mapView.showZoomControls(true);
        // 开启定位图层
        baiduMap.setMyLocationEnabled(true);
        //热力图
        baiduMap.setBaiduHeatMapEnabled(false);//开启



    }

    // 定义中心点以及缩放等级
    public void setPosition2Center(BaiduMap map,  LatLng center_point,Float zoomNow) {
        //定义地图状态
        MapStatus mMapStatus = new MapStatus.Builder()
                .target(center_point)
                .zoom(zoomNow)
                .build();
        //定义MapStatusUpdate对象，以便描述地图状态将要发生的变化
        MapStatusUpdate mMapStatusUpdate = MapStatusUpdateFactory
                .newMapStatus(mMapStatus);
        //改变地图状态
        map.setMapStatus(mMapStatusUpdate);


    }



    public void setMarker() {
        infos.clear();
        new SendDateToServer("2").SendDataToServer("userRouteActivity","","","");

        //构建Marker图标
        View markerView = LayoutInflater.from(this).inflate(R.layout.drawpeople,mapView,false);
        BitmapDescriptor bitmap = BitmapDescriptorFactory.fromView(markerView);

        //线程间通信
        handler2 = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == 3) {
                    baiduMap.clear();
//                    Log.i("userceshi","????/"+initLon+""+initLon);
                    infos.add(new MarkerInfo(initLat, initLon,andoidId,R.drawable.people,"终点"));
                    //  infos.add(new MarkerInfo(39.011345,117.306955,"南开大学",R.drawable.marker,"综合性大学。"));

                    LatLng point = null;
                    Marker marker;

                    OverlayOptions options;
                    for(MarkerInfo info:infos){
                        //获取经纬度
                        point= new LatLng(info.getLatitude(),info.getLongitude());
                        //设置marker
                        options = new MarkerOptions()
                                .position(point)//设置位置
                                .icon(bitmap);//设置图标样式
                        //添加marker
                        marker = (Marker) baiduMap.addOverlay(options);

                        //使用marker携带info信息，当点击事件的时候可以通过marker获得info信息
                        Bundle bundle = new Bundle();
                        //info必须实现序列化接口
                        bundle.putSerializable("info", info);
                        marker.setExtraInfo(bundle);
                        marker.setClickable(true);
                        marker.setToTop();

                        //    Log.i("zezeze","啧啧啧"+info.getLatitude()+"  "+info.getLongitude());
                    }
                }
            }
        };



    }

    public void drawRoute(){
        new SendDateToServer("6").SendDataToServer(andoidId,"","","");

        //构建Marker图标 ， 标出用户经过每个节点的时间
        View markerView = LayoutInflater.from(this).inflate(R.layout.drawhongdian,mapView,false);
        BitmapDescriptor bitmap = BitmapDescriptorFactory.fromView(markerView);
        //起点图标
        View markerView_qidian = LayoutInflater.from(this).inflate(R.layout.drawqidian,mapView,false);
        BitmapDescriptor bitmap_qidian = BitmapDescriptorFactory.fromView(markerView_qidian);
        //终点图标
        View markerView_zhongdian = LayoutInflater.from(this).inflate(R.layout.drawzhongdian,mapView,false);
        BitmapDescriptor bitmap_zhongdian = BitmapDescriptorFactory.fromView(markerView_zhongdian);


        //线程间通信
        handler2 = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == 2) {

                    baiduMap.clear();
                    points.clear();
                    for(int i=0;i<=pointLat.size()-1;i++){
                        LatLng p = new LatLng(pointLat.get(i), pointLon.get(i));
                        points.add(p);
                    }

                    //划线
                    for(int i=0;i<=pointLat.size()-2;i++){
                        dotPoints.clear();
                        double lat1= points.get(i).latitude;
                        double lon1= points.get(i).longitude;
                        double lat2= points.get(i+1).latitude;
                        double lon2= points.get(i+1).longitude;
                        if(abs(lat1-lat2)>=0.1||abs(lon1-lon2)>=0.1){
                            if(linePoints.size()>0){
                              //  Log.i("pointLat",""+"!?");
                                OverlayOptions ooPolyline = new PolylineOptions()
                                        .width(10)
                                        .color(0xAAFF0000).points(linePoints);
                                mPolyline = (Polyline) baiduMap.addOverlay(ooPolyline);
                                linePoints.clear();
                            }
                            dotPoints.add(points.get(i));
                            dotPoints.add(points.get(i+1));
                            OverlayOptions ooPolyline2 = new PolylineOptions()
                                    .width(10)
                                    .color(0xAAFF0000).points(dotPoints);
                            mPolyline = (Polyline) baiduMap.addOverlay(ooPolyline2);
                            mPolyline.setDottedLine(true);
                            dotPoints.clear();
                        }else{
                            linePoints.add(points.get(i));
                            linePoints.add(points.get(i+1));
                        }

                    }

                    if(linePoints.size()>0){
                        OverlayOptions ooPolyline = new PolylineOptions()
                                .width(10)
                                .color(0xAAFF0000).points(linePoints);
                        mPolyline = (Polyline) baiduMap.addOverlay(ooPolyline);
                        linePoints.clear();
                    }




                    //标记节点marker
                    ArrayList<MarkerInfo> infos = new ArrayList<MarkerInfo>();
                    for(int i=0;i<=pointTime.size()-1;i++){
                        if(i==0){
                          //  infos.add(new MarkerInfo(points.get(i).latitude, points.get(i).longitude,pointTime.get(i),R.drawable.people_gps,"中间节点"));
                        }else if(i==pointTime.size()-1){
                           // infos.add(new MarkerInfo(points.get(i).latitude, points.get(i).longitude,pointTime.get(i),R.drawable.people_gps,"中间节点"));
                        }else{
                            infos.add(new MarkerInfo(points.get(i).latitude, points.get(i).longitude,pointTime.get(i),R.drawable.people_gps,"中间节点"));
                        }

                    }

                    LatLng point = null;
                    Marker marker;

                    OverlayOptions options;
                    for(MarkerInfo info:infos){
                        //获取经纬度
                        point= new LatLng(info.getLatitude(),info.getLongitude());
                        //设置marker
                        options = new MarkerOptions()
                                .position(point)//设置位置
                                .icon(bitmap);//设置图标样式
                        //添加marker
                        marker = (Marker) baiduMap.addOverlay(options);
                        Bundle bundle = new Bundle();
                        //info必须实现序列化接口
                        bundle.putSerializable("info", info);
                        marker.setExtraInfo(bundle);

                    }

                    //画起点
                    point= new LatLng(points.get(0).latitude,points.get(0).longitude);
                    //设置marker
                    options = new MarkerOptions()
                            .position(point)//设置位置
                            .icon(bitmap_qidian);//设置图标样式
                    //添加marker
                    marker = (Marker) baiduMap.addOverlay(options);
                    Bundle bundle = new Bundle();
                    //info必须实现序列化接口
                    bundle.putSerializable("info", new MarkerInfo(points.get(0).latitude, points.get(0).longitude,pointTime.get(0),R.drawable.qidian,"起点"));
                    marker.setExtraInfo(bundle);

                    //画终点
                    point= new LatLng(points.get(pointTime.size()-1).latitude,points.get(pointTime.size()-1).longitude);
                    //设置marker
                    options = new MarkerOptions()
                            .position(point)//设置位置
                            .icon(bitmap_zhongdian);//设置图标样式
                    //添加marker
                    marker = (Marker) baiduMap.addOverlay(options);
                    Bundle bundle2 = new Bundle();
                    //info必须实现序列化接口
                    bundle2.putSerializable("info", new MarkerInfo(points.get(pointTime.size()-1).latitude, points.get(pointTime.size()-1).longitude,pointTime.get(pointTime.size()-1),R.drawable.zhongdian,"终点"));
                    marker.setExtraInfo(bundle2);


                }
            }
        };



    }


    public void switchButtonDo(){
        switchButton = (com.suke.widget.SwitchButton)findViewById(R.id.switch_button1);

        switchButton.setChecked(false);//设置为真，即默认为真
        switchButton.isChecked();//被选中
        switchButton.toggle();     //开关状态
        switchButton.toggle(true);//开关有动画
        switchButton.setShadowEffect(false);//禁用阴影效果
        switchButton.setEnabled(true);//false为禁用按钮
        switchButton.setEnableEffect(true);//false为禁用开关动画
        switchButton.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                route_flag=!route_flag;
                LatLng center_point = new LatLng(initLat,initLon);
                zoom=baiduMap.getMapStatus().zoom;
                setPosition2Center(baiduMap, center_point, zoom);
                if(!route_flag){
                    setMarker();
                }else{
                  baiduMap.clear();
                  drawRoute();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mapView.onResume();
    }
    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mapView.onPause();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mapView.onDestroy();
    }


    // 由于map界面调用了finish，所以需要通过捕获返回按键手动返回
    @Override
    public void onBackPressed(){
        super.onBackPressed();
        Log.i("捕获返回键", "onBackPressed: 按下了返回键");
        MapActivity.dingcheck = dingcheck;
        MapActivity.recheck = recheck;
        MapActivity.weicheck = weicheck;
        MapActivity.jiaocheck = jiaocheck;
        MapActivity.lacheck = lacheck;
        MapActivity.zoom = zoom;
        Intent intent = new Intent();
        intent.setClass(userRouteMapActivity.this,MapActivity.class);
        startActivity(intent);
        finish();

    }


    /**
     * 以下为获取权限内容
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
//        userRouteMapActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }
    //申请权限成功时
    @NeedsPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
    void ApplySuccess() {
        initMap();
    }
    // 申请权限告诉用户原因时
    @OnShowRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
    void showRationaleForMap(PermissionRequest request) {
        showRationaleDialog("使用此功能需要打开定位的权限", request);
    }
    //申请权限被拒绝时
    @OnPermissionDenied(Manifest.permission.ACCESS_COARSE_LOCATION)
    void onMapDenied() {
        Toast.makeText(this,"你拒绝了权限，该功能不可用",Toast.LENGTH_LONG).show();
    }
    //申请权限被拒绝并勾选不再提醒时
    @OnNeverAskAgain(Manifest.permission.ACCESS_COARSE_LOCATION)
    void onMapNeverAskAgain() {
        AskForPermission();
    }
    //告知用户具体需要权限的原因
    private void showRationaleDialog(String messageResId, final PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(@NonNull DialogInterface dialog, int which) {
                        request.proceed();//请求权限
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(@NonNull DialogInterface dialog, int which) {
                        request.cancel();
                    }
                })
                .setCancelable(false)
                .setMessage(messageResId)
                .show();
    }
    //被拒绝并且不再提醒,提示用户去设置界面重新打开权限
    private void AskForPermission() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("当前应用缺少定位权限,请去设置界面打开\n打开之后按两次返回键可回到该应用哦");
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                return;
            }
        });
        builder.setPositiveButton("设置", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + userRouteMapActivity.this.getPackageName())); // 根据包名打开对应的设置界面
                startActivity(intent);
            }
        });
        builder.create().show();
    }


}
