package com.xmliu.locationdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity {

    TextView textView;
    TextView textView2;

    LocationManager locationManager;
    GPSStatusImpl mGPSStatusImpl;
    Location lastLocation = null;

    int stars;
    float minDistance = 1;
    long minSecond = 2 * 1000;
    double speed = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.main_text);
        textView2 = findViewById(R.id.main_text2);

        Utils.initDir(Utils.BASE_PROJECT_IMAGE_PATH);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))) {
            Toast.makeText(this, "请打开网络或GPS定位功能!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, 0);
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    }, 100);
        } else {
            initLocation();
        }
    }

    @SuppressLint("MissingPermission")
    void initLocation() {
        Log.i("xml", "权限已取得，开始获取经纬度");
        try {
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            updateLocation(location);

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minSecond, minDistance, listener);//LocationManager.GPS_PROVIDER

            mGPSStatusImpl = new GPSStatusImpl();
            locationManager.addGpsStatusListener(mGPSStatusImpl);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateLocation(Location location) {
        String result;
        double lat;
        double lon;

        if (location != null) {
            lat = location.getLatitude();
            lon = location.getLongitude();
            if (location.hasSpeed()) {
                speed = location.getSpeed() * 3.6;
            } else {
                if( lastLocation != null) {
                    speed = getMySpeed(lastLocation, location);
                }
            }

            result = "纬度：" + lat + "\n经度：" + lon + "\n速度：" + String.format("%.2f", speed) + " km/h\n卫星数量：" + stars;
            Log.i("xml", "显示结果 = " +  result);
            lastLocation = location;
        } else {
            result = "无法获取经纬度信息";
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat dateFormatDate = new SimpleDateFormat("yyyyMMdd");
        String curTime = dateFormat.format(new Date());
        String curDate = dateFormatDate.format(new Date());

        // 把结果展示在界面上
        textView.setText(curTime + "\n" + result);

        // 把结果写入文件
        Utils.writeFileAppend(Utils.BASE_PROJECT_IMAGE_PATH + "log_" + curDate + ".txt", curTime + "\n" + result.replace("\n", " "));
    }

    private class GPSStatusImpl implements GpsStatus.Listener {
        @Override
        public void onGpsStatusChanged(int event) {
            switch (event) {
                case GpsStatus.GPS_EVENT_FIRST_FIX:  //第一次定位
                    break;

                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:  //卫星状态改变
                {
                    //获取当前状态
                    @SuppressLint("MissingPermission")
                    GpsStatus gpsStatus = locationManager.getGpsStatus(null);
                    //获取卫星颗数的默认最大值
                    int maxSatellites = gpsStatus.getMaxSatellites();
                    //创建一个迭代器保存所有卫星
                    Iterator<GpsSatellite> iters = gpsStatus.getSatellites().iterator();
                    int count = 0;
                    while (iters.hasNext() && count <= maxSatellites) {
                        GpsSatellite s = iters.next();
                        // 只有信噪比不为0时才算合格的卫星
                        if (s.getSnr() != 0) {
                            count++;
                        }
                    }

                    stars = count;

                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String curTime = dateFormat.format(new Date());
                    System.out.println(curTime + "-----卫星数量nun----" + stars);
                    textView2.setText("实时卫星数量：" + stars);
                    break;
                }

                case GpsStatus.GPS_EVENT_STARTED:   //定位启动
                    break;

                case GpsStatus.GPS_EVENT_STOPPED:   //定位结束
                    break;
            }
        }
    }

    public final LocationListener listener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            updateLocation(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                // GPS状态为可见时
                case LocationProvider.AVAILABLE:
                    Log.i("xml", "当前GPS状态为可见状态");
                    break;
                // GPS状态为服务区外时
                case LocationProvider.OUT_OF_SERVICE:
                    Log.i("xml", "当前GPS状态为服务区外状态");
                    break;
                // GPS状态为暂停服务时
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Log.i("xml", "当前GPS状态为暂停服务状态");
                    break;
            }
        }

        @Override
        public void onProviderEnabled(String provider) {
            @SuppressLint("MissingPermission")
            Location location = locationManager.getLastKnownLocation(provider);
            updateLocation(location);
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            // 相机权限
            case 100:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //用户点击了同意授权
                    initLocation();
                } else {
                    //用户拒绝了授权
                    Toast.makeText(MainActivity.this, "权限被拒绝", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    protected void onDestroy() {
        try {
            locationManager.removeUpdates(listener);
            locationManager.removeGpsStatusListener(mGPSStatusImpl);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    /**
     * 根据两个位置计算速度
     * @param lastLocation
     * @param curLocation
     * @return
     */
    private double getMySpeed(Location lastLocation, Location curLocation) {
        double mSpeed = 0;
        double lat1 = lastLocation.getLatitude();
        double lon1 = lastLocation.getLongitude();
        double lat2 = curLocation.getLatitude();
        double lon2 = curLocation.getLongitude();
        long timeDelta = (curLocation.getTime() - lastLocation.getTime())/1000;//单位秒
        if (timeDelta > 0) {
            double distanceInMeters = Utils.getDistance(lon1, lat1, lon2, lat2);
            System.out.println("真实距离 == " + distanceInMeters);
            mSpeed = (distanceInMeters / timeDelta) * 3.6; // 转换成km/h
            System.out.println("真实速度 == " + mSpeed);
        }
        return mSpeed;
    }

}
