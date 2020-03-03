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
import com.baidu.mapapi.model.LatLng;
import com.suke.widget.SwitchButton;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

import static android.content.ContentValues.TAG;


@RuntimePermissions
public class MapActivity extends AppCompatActivity {
   public static List<String>ANDROID_ID = new ArrayList<String>();
   public static List<Double>latitude = new ArrayList<Double>();
   public static List<Double>longitude = new ArrayList<Double>();
   public static List<String>street = new ArrayList<String>();
    //地图视图控件
    private MapView mapView;
    //百度地图
    private BaiduMap baiduMap;
    //防止每次定位都重新设置中心点和marker
    //private Button btn;
    private com.suke.widget.SwitchButton switchButton,switchButton1,switchButton2,switchButton3,switchButton4;
    public static boolean recheck=false,jiaocheck=true,weicheck=false,dingcheck=false,lacheck=false;
    public static float zoom=18.0f;
    //初始化LocationClient定位类
    private LocationClient mLocationClient = null;
    //BDAbstractLocationListener为7.2版本新增的Abstract类型的监听接口，原有BDLocationListener接口
    private BDLocationListener myListener = new MyLocationListener();
    private BDLocation bdLocationTemp = new BDLocation();
    //经纬度
    private double lat;
    private double lon;

    // 用于解决行走时 中心点的问题
    private int flag_center=0;

    //用于解决匿名定为marker连续多次点击问题
    public static int marker_click_flag=0;


    public static Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };


    //marker
    private ConstraintLayout rl_marker;
    ArrayList<MarkerInfo> infos;
    public static  List<MarkerInfo> infos1;
    public static String numOfUser="";
    public static String  poiAddress="";

    @Nullable
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
// 归位按钮
        ImageButton imb = findViewById(R.id.mylocation);
        imb.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View arg0) {
                setPosition2Center(baiduMap, bdLocationTemp, true,zoom);
            }
        });

        mapView = (MapView) findViewById(R.id.bmapView);
        rl_marker=(ConstraintLayout)findViewById(R.id.rl_marker);
        rl_marker.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent();
                intent.setClass(MapActivity.this,trashcanDetailActivity.class);
                startActivity(intent);
                rl_marker.setVisibility(View.GONE);
            }
        });



        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            initMap();
        } else {
            MapActivityPermissionsDispatcher.ApplySuccessWithCheck(this);
        }


        //调用switchButton;
        switchButton1Do();

        //地图点击事件
        baiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng arg0) {
                if(rl_marker.getVisibility()!=View.GONE)  rl_marker.setVisibility(View.GONE);
                else {
                    double la=arg0.latitude;
                    double lo=arg0.longitude;

                    new SendDateToServer("4").SendDataToServer("",""+la,""+lo,"");
                    Log.i("触摸点非(POI)信息", la+"  "+lo);
                    //Toast.makeText(MapActivity.this,"?",Toast.LENGTH_LONG).show();
                    handler = new Handler() {
                        @Override
                        public void handleMessage(Message msg) {
                            super.handleMessage(msg);
                            if (msg.what == 1) {
                                Toast.makeText(MapActivity.this,"地址是："+poiAddress+"   "+"附近有"+numOfUser+"个用户。",Toast.LENGTH_LONG).show();
                            }
                        }
                    };
                }
            }
            @Override
            public void onMapPoiClick(MapPoi poi) {
                LatLng ll = poi.getPosition();
                double la=ll.latitude;
                double lo=ll.longitude;
                new SendDateToServer("4").SendDataToServer("",""+la,""+lo,"");
                Log.i("POI信息", la+"  "+lo);

 //  http://api.map.baidu.com/reverse_geocoding/v3/?ak=1oXVsfS7jhGYkzni2tgUbBO65GPXeEf7&output=json&coordtype=bd09ll&location=39.00605855237172,117.32279585642641&mcode=31:73:65:A5:0E:3B:F1:88:43:96:72:1E:28:BF:09:CC:FC:31:2C:8A;com.example.administrator.smarttrashcan

                //测试的时候明显发现这里存在线程通信问题。
                 handler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        if (msg.what == 1) {
                            Toast.makeText(MapActivity.this,"地址是："+poiAddress+"   "+poi.getName()+"附近有"+numOfUser+"个用户。",Toast.LENGTH_LONG).show();
                        }
                    }
                };

        }
        });

        //地图长按事件
        //这里是监测某条路上的用户数量
        baiduMap.setOnMapLongClickListener(new BaiduMap.OnMapLongClickListener() {
            //地图长按事件监听回调函数
            public void onMapLongClick(LatLng arg0) {
                double la=arg0.latitude;
                double lo=arg0.longitude;
                new SendDateToServer("5").SendDataToServer("",""+la,""+lo,"");
                //测试的时候明显发现这里存在线程通信问题。
                handler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        if (msg.what == 1) {
                            Toast.makeText(MapActivity.this,"道路名称： "+poiAddress+"  用户数量"+numOfUser+" 人",Toast.LENGTH_LONG).show();
                        }
                    }
                };


            }
        });




    }


    private void switchButton1Do(){
        switchButton = (com.suke.widget.SwitchButton)findViewById(R.id.switch_button);
        switchButton1 = (com.suke.widget.SwitchButton)findViewById(R.id.switch_button1);
        switchButton2 = (com.suke.widget.SwitchButton)findViewById(R.id.switch_button2);
        switchButton3 = (com.suke.widget.SwitchButton)findViewById(R.id.switch_button3);
        switchButton4 = (com.suke.widget.SwitchButton)findViewById(R.id.switch_button4);


        switchButton.setChecked(recheck);//设置为真，即默认为真
        switchButton.isChecked();//被选中
        switchButton.toggle();     //开关状态
        switchButton.toggle(true);//开关有动画
        switchButton.setShadowEffect(false);//禁用阴影效果
        switchButton.setEnabled(true);//false为禁用按钮
        switchButton.setEnableEffect(true);//false为禁用开关动画
        switchButton.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                Log.i("widgetDemo", "switchbutton！！！！！！！！！！！！！。");
                recheck=!recheck;
                if(!recheck){
                    baiduMap.setBaiduHeatMapEnabled(false);
                }else{
                    baiduMap.setBaiduHeatMapEnabled(true);
                }
            }
        });

        switchButton1.setChecked(jiaocheck);//设置为真，即默认为真
        switchButton1.isChecked();//被选中
        switchButton1.toggle();     //开关状态
        switchButton1.toggle(true);//开关有动画
        switchButton1.setShadowEffect(false);//禁用阴影效果
        switchButton1.setEnabled(true);//false为禁用按钮
        switchButton1.setEnableEffect(true);//false为禁用开关动画
        switchButton1.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
               jiaocheck=!jiaocheck;
               if(!jiaocheck) {
                   baiduMap.setTrafficEnabled(false);
               }else{
                   baiduMap.setTrafficEnabled(true);
               }

            }
        });

        switchButton2.setChecked(weicheck);//设置为真，即默认为真
        switchButton2.isChecked();//被选中
        switchButton2.toggle();     //开关状态
        switchButton2.toggle(true);//开关有动画
        switchButton2.setShadowEffect(false);//禁用阴影效果
        switchButton2.setEnabled(true);//false为禁用按钮
        switchButton2.setEnableEffect(true);//false为禁用开关动画
        switchButton2.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                Log.i("widgetDemo", "switchbutton！！！！！！！！！！！！！。");
                weicheck  = !weicheck;
                if(!weicheck){
                    baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
                }else{
                    baiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
                }
            }
        });

        switchButton3.setChecked(lacheck);//设置为真，即默认为真
        switchButton3.isChecked();//被选中
        switchButton3.toggle();     //开关状态
        switchButton3.toggle(true);//开关有动画
        switchButton3.setShadowEffect(false);//禁用阴影效果
        switchButton3.setEnabled(true);//false为禁用按钮
        switchButton3.setEnableEffect(true);//false为禁用开关动画
        switchButton3.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                lacheck=!lacheck;
                if(dingcheck==true) setMarker1();
                if(lacheck==true) setMarker();
                if(lacheck==false&&dingcheck==false)baiduMap.clear();
                else if(lacheck==false&&dingcheck==true) setMarker1();
            }
        });

        switchButton4.setChecked(dingcheck);//设置为真，即默认为真
        switchButton4.isChecked();//被选中
        switchButton4.toggle();     //开关状态
        switchButton4.toggle(true);//开关有动画
        switchButton4.setShadowEffect(false);//禁用阴影效果
        switchButton4.setEnabled(true);//false为禁用按钮
        switchButton4.setEnableEffect(true);//false为禁用开关动画
        switchButton4.setOnCheckedChangeListener(new SwitchButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchButton view, boolean isChecked) {
                dingcheck=!dingcheck;
                if(dingcheck==true) {
                    setMarker1();
                    if(lacheck==true) setMarker();
                }
                if(lacheck==false&&dingcheck==false)baiduMap.clear();
                else if(dingcheck==false&&lacheck==true){
                    baiduMap.clear();
                    setMarker();
                }

            }
        });



    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("这里","LALALALALALAAL");
        flag_center=0;
        new SendDateToServer("3").SendDataToServer(Settings.System.getString(getContentResolver(), Settings.System.ANDROID_ID),"","","");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        flag_center=0;
        new SendDateToServer("3").SendDataToServer(Settings.System.getString(getContentResolver(), Settings.System.ANDROID_ID),"","","");
        // 退出时销毁定位
        mLocationClient.unRegisterLocationListener(myListener);
        mLocationClient.stop();
        // 关闭定位图层
        baiduMap.setMyLocationEnabled(false);
        mapView.onDestroy();
        mapView = null;
    }
    @Override
    public void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mapView.onResume();

    }
    @Override
    public void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mapView.onPause();
    }
    /**
     * 初始化地图
     */
    public void initMap(){
        //得到地图实例
        baiduMap = mapView.getMap();

        setPosition2Center(baiduMap, bdLocationTemp, true,zoom);
        //设置地图类型,普通地图
        baiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        //开启交通图
        baiduMap.setTrafficEnabled(true);
        //关闭缩放按钮
        mapView.showZoomControls(true);
        // 开启定位图层
        baiduMap.setMyLocationEnabled(true);
        //热力图
        baiduMap.setBaiduHeatMapEnabled(false);//开启
        //声明LocationClient类
        mLocationClient = new LocationClient(this);
        //注册监听函数
        mLocationClient.registerLocationListener(myListener);
        initLocation();
        //开始定位
        mLocationClient.start();
        //覆盖物
        //setMarker();
    }
    /**
     * 配置定位参数
     */
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
    /**
     * 实现定位监听 位置一旦有所改变就会调用这个方法
     * 可以在这个方法里面获取到定位之后获取到的一系列数据
     */
    public class MyLocationListener implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {

            Log.i("测试测试", location.getFloor()+"??");
            if (location.getFloor() != null) {
                Log.i("测试测试", "here!");
                // 当前支持高精度室内定位
                String buildingID = location.getBuildingID();// 百度内部建筑物ID
                String buildingName = location.getBuildingName();// 百度内部建筑物缩写
                String floor = location.getFloor();// 室内定位的楼层信息，如 f1,f2,b1,b2
                mLocationClient.startIndoorMode();// 开启室内定位模式（重复调用也没问题），开启后，定位SDK会融合各种定位信息（GPS,WI-FI，蓝牙，传感器等）连续平滑的输出定位结果；
            }


            //获取定位结果
            location.getTime();    //获取定位时间
            location.getLocationID();    //获取定位唯一ID，v7.2版本新增，用于排查定位问题
            location.getLocType();    //获取定位类型
            location.getLatitude();    //获取纬度信息
            location.getLongitude();    //获取经度信息
            location.getRadius();    //获取定位精准度
            location.getAddrStr();    //获取地址信息
            location.getCountry();    //获取国家信息
            location.getCountryCode();    //获取国家码
            location.getCity();    //获取城市信息
            location.getCityCode();    //获取城市码
            location.getDistrict();    //获取区县信息
            location.getStreet();    //获取街道信息
            location.getStreetNumber();    //获取街道码
            location.getLocationDescribe();    //获取当前位置描述信息
            location.getPoiList();    //获取当前位置周边POI信息
            location.getBuildingID();    //室内精准定位下，获取楼宇ID
            location.getBuildingName();    //室内精准定位下，获取楼宇名称
            location.getFloor();    //室内精准定位下，获取当前位置所处的楼层信息

          //  Log.i("街道信息是：：：：：：", location.getStreet()+"++++"+location.getStreetNumber());


            //经纬度
            lat = location.getLatitude();
            lon = location.getLongitude();

            //查询所有人定位，实时更新每个人位置
            new SendDateToServer("2").SendDataToServer("","","","");
            //Log.i("个数",ANDROID_ID.size()+" "+latitude.size()+" "+longitude.size());
            //这里需要对动态点、静态点全部实现重标，因为baidumap在实时刷新，之前标过的点不再存在。
          if(dingcheck==true)  setMarker1();
          if(lacheck==true)  setMarker();


          //  Log.i(TAG, "onReceiveLocation: 经纬度："+lat+"??????"+lon);
            //这个判断是为了防止每次定位都重新设置中心点（只有位置改变时会重新标注中心点，但不会改变缩放比例）
            zoom= baiduMap.getMapStatus().zoom;
//            if (!(bdLocationTemp.getLatitude()==lat&&bdLocationTemp.getLongitude()==lon)) {
//                //isFirstLocation = false;
//                //设置并显示中心点
//                setPosition2Center(baiduMap, location, true,zoom);
//            }

            //这里保证只有第一次打开APP时才会设置中心点
            // 防止每次刷新中心点（就是指位置变化时，页面划至别处时不会瞬间闪回到中心位置），但可以保证自己位置实时更新
            if(flag_center==0){
                flag_center++;
                setPosition2Center(baiduMap, location, true,zoom);
            }else{
                setPosition2Center(baiduMap, location, false,zoom);
            }



        }
    }

    /**
     * 设置中心点和添加marker，添加参数zoomNow,是为了保存缩放信息。
     */
    public void setPosition2Center(BaiduMap map, BDLocation bdLocation, Boolean isShowLoc,Float zoomNow) {
        MyLocationData locData = new MyLocationData.Builder()
                .accuracy(bdLocation.getRadius())
                .direction(bdLocation.getRadius()).latitude(bdLocation.getLatitude())
                .longitude(bdLocation.getLongitude()).build();
        map.setMyLocationData(locData);

        if (isShowLoc) {
            LatLng ll = new LatLng(bdLocation.getLatitude(), bdLocation.getLongitude());
            MapStatus.Builder builder = new MapStatus.Builder();
            builder.target(ll).zoom(zoomNow);
            map.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));

        }
        bdLocationTemp = bdLocation;

    }


    //设置marker
      private  void setMarkerInfo() {
        infos = new ArrayList<MarkerInfo>();
        infos.add(new MarkerInfo(39.001912, 117.3239,"天津站",R.drawable.marker,"俗称天津东站"));
        infos.add(new MarkerInfo(39.011345,117.306955,"南开大学",R.drawable.marker,"综合性大学。"));
        infos.add(new MarkerInfo(39.094994,117.174081,"天津水上公园",R.drawable.marker,"原称青龙潭。"));
    }

    //静态的marker标志，垃圾桶不会动，所以标注一次就好。
    public void setMarker() {
        setMarkerInfo();

        //构建Marker图标
        View markerView = LayoutInflater.from(this).inflate(R.layout.drawadapt,mapView,false);
        BitmapDescriptor bitmap = BitmapDescriptorFactory.fromView(markerView);

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
//            if(lacheck==false)marker.setAlpha(0.0f);
//            else marker.setAlpha(1.0f);
           // Log.i("marker 信息",marker.getAlpha()+"");
            //使用marker携带info信息，当点击事件的时候可以通过marker获得info信息
            Bundle bundle = new Bundle();
            //info必须实现序列化接口
            bundle.putSerializable("info", info);
            marker.setExtraInfo(bundle);

        //    Log.i("zezeze","啧啧啧"+info.getLatitude()+"  "+info.getLongitude());
        }

        //添加marker点击事件的监听
        baiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                //从marker中获取info信息
                Bundle bundle = marker.getExtraInfo();
                MarkerInfo getinfo = (MarkerInfo) bundle.getSerializable("info");
                //将信息显示在界面上
                if(getinfo.getImgId()==R.drawable.marker){
                    TextView item_name = (TextView)rl_marker.findViewById(R.id.item_name);
                    item_name.setText(getinfo.getName());

                    //将布局显示出来
                    rl_marker.setVisibility(View.VISIBLE);

                    CountDownTimer timer = new CountDownTimer(5000, 1000){
                        @Override
                        public void onTick(long sin) {
                        }
                        @Override
                        public void onFinish() {
                            rl_marker.setVisibility(View.GONE);
                        }
                    };
                    timer.start();


                }
                return true;
            }
        });
    }


    private  void setMarkerInfo1() {
        infos1 = new ArrayList<MarkerInfo>();
        for(int i=0;i<=ANDROID_ID.size()-1;i++){
            if(!ANDROID_ID.get(i).equals(Settings.System.getString(getContentResolver(), Settings.System.ANDROID_ID)))
            infos1.add(new MarkerInfo(latitude.get(i),longitude.get(i),ANDROID_ID.get(i),R.drawable.people_gps,ANDROID_ID.get(i)));
        }
    }

    private void setMarker1() {
        baiduMap.clear();
        setMarkerInfo1();
        Log.i("测试","here!!!!!!!");
        //构建Marker图标
        View markerView = LayoutInflater.from(this).inflate(R.layout.drawpeople,mapView,false);
        BitmapDescriptor bitmap = BitmapDescriptorFactory.fromView(markerView);
        LatLng point = null;
        Marker marker;
        OverlayOptions options;
        for(MarkerInfo info1:infos1){
            //获取经纬度
            point= new LatLng(info1.getLatitude(),info1.getLongitude());
            //设置marker
            options = new MarkerOptions()
                    .position(point)//设置位置
                    .icon(bitmap);//设置图标样式
            //添加marker
            marker = (Marker) baiduMap.addOverlay(options);
            //使用marker携带info信息，当点击事件的时候可以通过marker获得info信息
            Bundle bundle = new Bundle();
            //info必须实现序列化接口
            bundle.putSerializable("info", info1);
            marker.setExtraInfo(bundle);

        }

      //  添加marker点击事件的监听
        baiduMap.setOnMarkerClickListener(new BaiduMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                //获得marker 的info
                Bundle bundle = marker.getExtraInfo();
                MarkerInfo getinfo = (MarkerInfo) bundle.getSerializable("info");

                //这里由于界面实时刷新，所以会造成marker 连续点击，利用marker_click_flag解决。
                if(getinfo.getImgId()==R.drawable.people_gps&&marker_click_flag==0){
                    //这里可以传递该marker 的信息，比如之后需要将marker同学号等绑定，可以在此实现传递
                    userRouteMapActivity.initLat=getinfo.getLatitude();
                    userRouteMapActivity.initLon=getinfo.getLongitude();
                    userRouteMapActivity.andoidId=getinfo.getName();
             //       Toast.makeText(MapActivity.this,  userRouteMapActivity.initLat+""+ userRouteMapActivity.initLon+userRouteMapActivity.andoidId, Toast.LENGTH_SHORT).show();
                    marker_click_flag++;
                    userRouteMapActivity.dingcheck = dingcheck;
                    userRouteMapActivity.weicheck = weicheck;
                    userRouteMapActivity.jiaocheck = jiaocheck;
                    userRouteMapActivity.lacheck = lacheck;
                    userRouteMapActivity.recheck = recheck;
                    userRouteMapActivity.zoom = zoom;

                    Intent intent = new Intent();
                    intent.setClass(MapActivity.this,userRouteMapActivity.class);
                    startActivity(intent);
                    finish();
                }
                return true;
            }
        });
       // onResume();
    }



    /**
     * 以下为获取权限内容
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        MapActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
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
                intent.setData(Uri.parse("package:" + MapActivity.this.getPackageName())); // 根据包名打开对应的设置界面
                startActivity(intent);
            }
        });
        builder.create().show();
    }

}
