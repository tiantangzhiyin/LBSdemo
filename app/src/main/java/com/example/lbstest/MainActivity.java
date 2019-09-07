package com.example.lbstest;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public LocationClient mLocationClient;
    private TextView positionText;
    private int positionCount;
    private MapView mapView;
    private BaiduMap baiduMap;//地图的总控制器
    private boolean isFirstLocate=true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //初始化MapView，一定要在setContentView()前调用
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        positionCount=0;
        //getApplicationContext()获取一个全局的Context参数
        mLocationClient=new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(new MyLocationListener());//注册一个定位监听器
        positionText=(TextView)findViewById(R.id.position_text_view);
        mapView=(MapView)findViewById(R.id.bmapView);
        baiduMap= mapView.getMap();//获取地图控件引用
        //baiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);//显示卫星图层
        baiduMap.setMyLocationEnabled(true);//开启显示设备位置功能，程序退出时需关闭
        //运行时权限处理
        List<String> permissionList=new ArrayList<>();
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);//获取精准位置
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.READ_PHONE_STATE);//获取手机串码
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);//写SD卡
        }
        if(!permissionList.isEmpty()){
            String[] permissions=permissionList.toArray(new String[permissionList.size()]);//转换为字符串数组
            ActivityCompat.requestPermissions(MainActivity.this,permissions,1);
        }else{
            requestLocation();
        }
        //按钮改变视图
        Button change=(Button)findViewById(R.id.change);
        change.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(positionText.getVisibility()==View.VISIBLE){
                    positionText.setVisibility(View.GONE);
                    mapView.setVisibility(View.VISIBLE);
                }else{
                    positionText.setVisibility(View.VISIBLE);
                    mapView.setVisibility(View.GONE);
                }
            }
        });
    }

    private void requestLocation(){
        initLocation();
        //默认情况下，LocationClient的start()方法只会定位一次
        mLocationClient.start();
    }

    private void initLocation(){
        LocationClientOption option=new LocationClientOption();
        option.setScanSpan(5000);//设置定位更新间隔为五秒
        //定位模式有三种：Hight_Accuracy,Battery_Saving,Device_Sensors。默认高精度模式。
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);//选择定位模式
        option.setIsNeedAddress(true);//获取位置的详细信息
        mLocationClient.setLocOption(option);//修改默认设置
    }

    //public class MyLocationListener implements BDLocationListener {
    public class MyLocationListener extends BDAbstractLocationListener{
        @Override
        public void onReceiveLocation(final BDLocation location){
            positionCount++;
            //更新UI
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    StringBuilder currentPosition=new StringBuilder();
                    currentPosition.append("定位次数：").append(positionCount).append("\n");
                    currentPosition.append("纬度：").append(location.getLatitude()).append("\n");//getLatitude()获取纬度
                    currentPosition.append("经度：").append(location.getLongitude()).append("\n");//getLongitude()获取经度
                    currentPosition.append("国家：").append(location.getCountry()).append("\n");//getCountry()获取国家
                    currentPosition.append("省：").append(location.getProvince()).append("\n");//getProvince()获取省份
                    currentPosition.append("市：").append(location.getCity()).append("\n");//getCity()获取城市
                    currentPosition.append("区：").append(location.getDistrict()).append("\n");//getDistrict()获取地区
                    currentPosition.append("街道：").append(location.getStreet()).append("\n");//getStreet()获取街道
                    currentPosition.append("定位方式：");
                    if(location.getLocType()==BDLocation.TypeGpsLocation){//getLocType()获取定位方式
                        currentPosition.append("GPS");
                    }else if(location.getLocType()==BDLocation.TypeNetWorkLocation){
                        currentPosition.append("Internet");
                    }else {
                        currentPosition.append("Other");
                    }
                    positionText.setText(currentPosition);
                }
            });
            //进行地图的缩放和移动到指定经纬度
            if(mapView.getVisibility()==View.VISIBLE&&isFirstLocate){
                if(location.getLocType()==BDLocation.TypeGpsLocation||location.getLocType()==BDLocation.TypeNetWorkLocation){
                    navigateTo(location);
                    isFirstLocate=false;
                }
            }
            //在地图上显示设备位置
            MyLocationData.Builder locationBuilder=new MyLocationData.Builder();
            locationBuilder.latitude(location.getLatitude());//设置经度
            locationBuilder.longitude(location.getLongitude());//设置纬度
            MyLocationData locationData=locationBuilder.build();
            baiduMap.setMyLocationData(locationData);
        }
        @Override
        public void onConnectHotSpotMessage(String s,int i){

        }
    }

    private void navigateTo(BDLocation location){
        //将地图移动到指定的经纬度
        LatLng latLng=new LatLng(location.getLatitude(),location.getLongitude());//获取经纬度值
        MapStatusUpdate update= MapStatusUpdateFactory.newLatLng(latLng);
        baiduMap.animateMapStatus(update);
        //设置地图的缩放级别
        update=MapStatusUpdateFactory.zoomTo(16f);//值越大越详细
        baiduMap.animateMapStatus(update);
        isFirstLocate=false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        switch (requestCode){
            case 1:
                if(grantResults.length>0){
                    //逐个确认权限请求许可
                    for(int result:grantResults){
                        if(result!=PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(this,"必须同意所有权限才能使用本程序",Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                    requestLocation();
                }else{
                    Toast.makeText(this,"权限处理发生未知错误",Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                break;
        }
    }
    //使用百度地图SDK必须重写onResume(),onPause(),onDestroy()三个方法，进行资源管理
    @Override
    protected void onResume(){
        super.onResume();
        mapView.onResume();
    }
    @Override
    protected void onPause(){
        super.onPause();
        mapView.onPause();
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        mLocationClient.stop();//停止定位服务
        mapView.onDestroy();
        baiduMap.setMyLocationEnabled(false);//退出时关闭显示设备位置功能
    }
}
