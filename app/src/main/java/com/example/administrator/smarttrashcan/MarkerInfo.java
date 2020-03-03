package com.example.administrator.smarttrashcan;

import java.io.Serializable;

/**
 * Created by Administrator on 2019/10/27 0027.
 */

public class MarkerInfo implements Serializable {
    private static final long serialVersionUID = 8633299996744734593L;

    private double latitude;//纬度
    private double longitude;//经度
    private String name;//名字
    private int imgId;//图片
    private String capacity;//装载量
    //构造方法
    public MarkerInfo() {};
    public MarkerInfo(double latitude, double longitude, String name, int imgId, String description) {
        super();
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
        this.imgId = imgId;
        this.capacity = description;
    }
    //toString方法
    @Override
    public String toString() {
        return "MarkerInfoUtil [latitude=" + latitude + ", longitude=" + longitude + ", name=" + name + ", imgId="
                + imgId + ", description=" + capacity + "]";
    }
    //getter setter
    public double getLatitude() {
        return latitude;
    }
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    public double getLongitude() {
        return longitude;
    }
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public int getImgId() {
        return imgId;
    }
    public void setImgId(int imgId) {
        this.imgId = imgId;
    }
    public String getCapacity() {
        return capacity;
    }
    public void setCapacity(String capacity) {
        this.capacity = capacity;
    }

}
