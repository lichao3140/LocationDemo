package com.xmliu.locationdemo;

import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class Utils {

    public final static String BASE_SD_PATH = Environment.getExternalStorageDirectory().getAbsolutePath();
    public final static String BASE_PROJECT_IMAGE_PATH = BASE_SD_PATH + "/ALocation/log/";

    public static void initDir(String dirPath) {
        try {
            File dir = new File(dirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 追加文件：使用FileWriter
     *
     * @param fileName
     * @param content
     */
    public static void writeFileAppend(String fileName, String content) {
        FileWriter writer = null;
        try {
            // 打开一个写文件器，构造函数中的第二个参数true表示以追加形式写文件
            File file = new File(fileName);
            if(!file.exists()){
                file.createNewFile();
            }
            writer = new FileWriter(fileName, true);
            writer.write(content);
            writer.write("\n");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if(writer != null){
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 地球半径，单位米
    private static final double EARTH_RADIUS2 = 6378137.0;
    // 返回单位是米，准确
    public static double getDistance(double longitude1, double latitude1,
                                     double longitude2, double latitude2) {
        double Lat1 = rad(latitude1);
        double Lat2 = rad(latitude2);
        double a = Lat1 - Lat2;
        double b = rad(longitude1) - rad(longitude2);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2)
                + Math.cos(Lat1) * Math.cos(Lat2)
                * Math.pow(Math.sin(b / 2), 2)));
        s = s * EARTH_RADIUS2;
        s = Math.round(s * 10000) / 10000;
        return s;
    }
    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }
}
