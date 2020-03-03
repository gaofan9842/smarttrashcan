package com.example.administrator.smarttrashcan;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.net.ssl.HttpsURLConnection;

import static java.net.URLEncoder.*;


/**
 * 通过GET方式向服务器发送数据
 * @author jph
 * Date:2014.09.27
 */
//通过调此类 new SendDateToServer("2").SendDataToServer("","","");
public class SendDateToServer {
    private String tag;
    private String param_id="";
    private static String url="http://49.233.155.20//AndroidPosition/ServletForGETMethod";
    public static final int SEND_SUCCESS=0x123;
    public static final int SEND_FAIL=0x124;
//    private Handler handler;
    public SendDateToServer(String  tag) {
        // TODO Auto-generated constructor stub
       // this.handler=handler;
        this.tag=tag;
    }
    /**
     * 通过Get方式向服务器发送数据
     */
    public void SendDataToServer(String id,String lat,String lon ,String street) {
        // TODO Auto-generated method stub
        final Map<String, String> map =new HashMap<String, String >();
        map.put("tag",tag);
        map.put("id", id);
        map.put("lat", lat);
        map.put("lon",lon);
        param_id = id;
        try {
            street = encode(street,"utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        map.put("street",street);

       // System.out.println(street);
        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                try {
                    if (sendGetRequest(map,url,"utf-8")) {
                    //    handler.sendEmptyMessage(SEND_SUCCESS);//通知主线程数据发送成功
                    }else {
                        //将数据发送给服务器失败
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }).start();
    }
    /**
     * 发送GET请求

     * @return
     * @throws Exception
     */
    private  boolean sendGetRequest(Map<String, String> param, String url,String encoding) throws Exception {
        // TODO Auto-generated method stub
        StringBuffer sb=new StringBuffer(url);
        if (!url.equals("")&!param.isEmpty()) {
            sb.append("?");
            for (Map.Entry<String, String>entry:param.entrySet()) {
                sb.append(entry.getKey()+"=");
                sb.append(encode(entry.getValue(), encoding));
                sb.append("&");
            }
            sb.deleteCharAt(sb.length()-1);//删除字符串最后 一个字符“&”
        }

        URL myurl = new URL(sb.toString());
        Log.i("URL",sb.toString()+"????????????????????");
        // 获得连接
        HttpURLConnection conn = (HttpURLConnection) myurl.openConnection();
        conn.setRequestProperty("Content-Type","application/json");
        conn.setConnectTimeout(6000);//设置超时
        conn.setRequestMethod("GET");//设置请求方式为GET
        conn.connect();

        if (conn.getResponseCode()==200) {

            //需要返回数据的话就连接获取json
            //解析json数据。
            InputStream input = conn.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(input));
            String line = null;
            System.out.println(conn.getResponseCode());
            StringBuffer sb2 = new StringBuffer();
            while ((line = in.readLine()) != null) {
                sb2.append(line);
            }

            if (tag.equals("2")) {          //要获取位置数据
                MapActivity.ANDROID_ID=new ArrayList<String>();
                MapActivity.latitude=new ArrayList<Double>();
                MapActivity.longitude=new ArrayList<Double>();
                MapActivity.street=new ArrayList<String>();

                JSONArray array = new JSONArray(sb2.toString());
                StringBuffer buffer = new StringBuffer();
                for (int i = 0; i < array.length(); i++) {
                    JSONArray array2 = new JSONArray(array.get(i).toString());
                    for (int p = 0; p < array2.length(); p++) {
                        if(p==0){
                            MapActivity.ANDROID_ID.add(array2.getString(p));
                            // 这里传递数据实现了user界面的位置实时更新，之前该数据是从map界面传过去的，后来map界面加了finish，所以只能从这里传过去。
                            if(param_id.equals("userRouteActivity")){
                                if(array2.getString(p).equals(userRouteMapActivity.andoidId)){
                                    userRouteMapActivity.initLat=Double.parseDouble(array2.getString(1));
                                    userRouteMapActivity.initLon=Double.parseDouble(array2.getString(2));
                                }
                            }

                        }else if(p==1){
                            MapActivity.latitude.add(Double.parseDouble(array2.getString(p)));
                        }else if(p==2){
                            MapActivity.longitude.add(Double.parseDouble(array2.getString(p)));
                        }else{
                            MapActivity.street.add(array2.getString(p));
                        }
                        Log.i("信息", array2.getString(p));
                    }
                }
                if(param_id.equals("userRouteActivity")){
//                    Log.i("user线程通信",param_id);
                    Message message = userRouteMapActivity.handler2.obtainMessage();
                    message.what = 3;
                    userRouteMapActivity.handler2.sendMessage(message);
                }


            }else if(tag.equals("4")){              //查询poi附近用户数量（矩形范围）
                MapActivity.numOfUser=sb2.toString();
                Log.i("有多少人？",""+sb2.toString());
                //这里是利用百度提供的web API查询当前经纬度下的地址，这个过程没有影响到后端数据处理，地址是要显示在APP中的所以，该过程写在这里，没有写在后端。
                URL myurl0 = new URL("http://api.map.baidu.com/reverse_geocoding/v3/?ak=1oXVsfS7jhGYkzni2tgUbBO65GPXeEf7&output=json&coordtype=bd09ll&location="+param.get("lat")+","+param.get("lon")+"&mcode=31:73:65:A5:0E:3B:F1:88:43:96:72:1E:28:BF:09:CC:FC:31:2C:8A;com.example.administrator.smarttrashcan");
                // 获得连接
                HttpURLConnection conn1 = (HttpURLConnection) myurl0.openConnection();
                conn1.setRequestProperty("Content-Type","application/json");
                conn1.setConnectTimeout(6000);//设置超时
                conn1.setRequestMethod("GET");//设置请求方式为GET
                conn1.connect();

                input = conn1.getInputStream();
                in = new BufferedReader(new InputStreamReader(input));
                line = null;
                System.out.println(conn1.getResponseCode());
                sb2 = new StringBuffer();
                while ((line = in.readLine()) != null) {
                    sb2.append(line);
                }

                JSONObject jsonObject = new JSONObject(sb2.toString());
                Log.i("POI地址", sb2.toString());
                MapActivity.poiAddress=jsonObject.getString("result");
                JSONObject jsonObject1 = new JSONObject(MapActivity.poiAddress);
                MapActivity.poiAddress=jsonObject1.getString("formatted_address");

                Message message = MapActivity.handler.obtainMessage();
                message.what = 1;
                MapActivity.handler.sendMessage(message);
            }else if(tag.equals("5")){
                MapActivity.numOfUser=sb2.toString();

                URL myurl0 = new URL("http://api.map.baidu.com/reverse_geocoding/v3/?ak=1oXVsfS7jhGYkzni2tgUbBO65GPXeEf7&output=json&coordtype=bd09ll&location="+param.get("lat")+","+param.get("lon")+"&mcode=31:73:65:A5:0E:3B:F1:88:43:96:72:1E:28:BF:09:CC:FC:31:2C:8A;com.example.administrator.smarttrashcan");
                // 获得连接
                HttpURLConnection conn1 = (HttpURLConnection) myurl0.openConnection();
                conn1.setRequestProperty("Content-Type","application/json");
                conn1.setConnectTimeout(6000);//设置超时
                conn1.setRequestMethod("GET");//设置请求方式为GET
                conn1.connect();

                input = conn1.getInputStream();
                in = new BufferedReader(new InputStreamReader(input));
                line = null;
                System.out.println(conn1.getResponseCode());
                sb2 = new StringBuffer();
                while ((line = in.readLine()) != null) {
                    sb2.append(line);
                }
                JSONObject jsonObject = new JSONObject(sb2.toString());
                MapActivity.poiAddress=jsonObject.getString("result");
                JSONObject jsonObject1 = new JSONObject(MapActivity.poiAddress);
                MapActivity.poiAddress=jsonObject1.getString("addressComponent");
                JSONObject jsonObject2 = new JSONObject(MapActivity.poiAddress);
                MapActivity.poiAddress=jsonObject2.getString("street");

                // 线程间通信
                Message message = MapActivity.handler.obtainMessage();
                message.what = 1;
                MapActivity.handler.sendMessage(message);
            }else if(tag.equals("6")){
                Log.i("tag6",""+userRouteMapActivity.pointLat.size());
                userRouteMapActivity.pointLat.clear();
                userRouteMapActivity.pointLon.clear();
                userRouteMapActivity.pointTime.clear();

                JSONArray array = new JSONArray(sb2.toString());
                StringBuffer buffer = new StringBuffer();

                for (int i = 0; i < array.length(); i++) {
                  JSONObject jsonObject = new JSONObject(array.get(i).toString());
                  userRouteMapActivity.pointLat.add(jsonObject.getDouble("latitude"));
                  userRouteMapActivity.pointLon.add(jsonObject.getDouble("longitude"));
                  userRouteMapActivity.pointTime.add(jsonObject.getString("time"));
                }


                Message message = userRouteMapActivity.handler2.obtainMessage();
                message.what = 2;
                userRouteMapActivity.handler2.sendMessage(message);

            }



            return true;
        }else{
            System.out.print("连接失败"+conn.getResponseCode());
        }
        return false;
    }






}