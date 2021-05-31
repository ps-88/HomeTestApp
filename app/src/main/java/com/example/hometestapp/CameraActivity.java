package com.example.hometestapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Camera;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class CameraActivity extends AppCompatActivity {

    public static final int CAMERA_REQUEST_CODE = 101;
    Camera camera;
    FrameLayout frameLayout;
    ShowCamera showCamera;
    View viewDisplayColor1, viewDisplayColor2, viewDisplayColor3, viewDisplayColor4, viewDisplayColor5;
    TextView viewRgb, viewRgb2, viewRgb3, viewRgb4, viewRgb5;
    TextView viewPercent1, viewPercent2, viewPercent3, viewPercent4, viewPercent5;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        frameLayout = findViewById(R.id.frameLayout);
        viewDisplayColor1 = findViewById(R.id.cameraLiveDisplayColor1);
        viewDisplayColor2 = findViewById(R.id.cameraLiveDisplayColor2);
        viewDisplayColor3 = findViewById(R.id.cameraLiveDisplayColor3);
        viewDisplayColor4 = findViewById(R.id.cameraLiveDisplayColor4);
        viewDisplayColor5 = findViewById(R.id.cameraLiveDisplayColor5);

        viewRgb = findViewById(R.id.cameraLiveRGB);
        viewRgb2 = findViewById(R.id.cameraLiveRGB2);
        viewRgb3 = findViewById(R.id.cameraLiveRGB3);
        viewRgb4 = findViewById(R.id.cameraLiveRGB4);
        viewRgb5 = findViewById(R.id.cameraLiveRGB5);

        viewPercent1 = findViewById(R.id.percent1);
        viewPercent2 = findViewById(R.id.percent2);
        viewPercent3 = findViewById(R.id.percent3);
        viewPercent4 = findViewById(R.id.percent4);
        viewPercent5 = findViewById(R.id.percent5);

        askCameraPermissions();
    }

    private void askCameraPermissions() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            //permission is not granted, request it
            String[] permission = {Manifest.permission.CAMERA};
            requestPermissions(permission, CAMERA_REQUEST_CODE);
        } else {
            startCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show();
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startCamera() {
        camera = Camera.open();
        showCamera = new ShowCamera(this, camera);
        frameLayout.addView(showCamera);
        new Thread(() -> camera.setPreviewCallback((bytes, camera) -> {

            //getting sizes
            int frameWidth = camera.getParameters().getPreviewSize().width;
            int frameHeight = camera.getParameters().getPreviewSize().height;


            //number of pixels
            int[] rgb = new int[frameWidth * frameHeight];
            //transforms NV21 pixel data into RGB pixels
            //I took it from internet because it is boilerplate and i don't think we can improve here something
            //http://www.java2s.com/example/android-utility-method/color-convert/decodeyuv420sp-int-rgb-byte-yuv420sp-int-width-int-height-2c094.html
            decodeYUV420SP(rgb, bytes, frameWidth, frameHeight);


            Bitmap bmp = Bitmap.createBitmap(rgb, frameWidth, frameHeight, Bitmap.Config.ARGB_8888);



            int totalPixels = bmp.getWidth() * bmp.getHeight();
            int pixelColor;

            //add and calculate amount of colors
            Map m = new HashMap();
            for (int i = 0; i < bmp.getHeight(); i++) {
                for (int j = 0; j < bmp.getWidth(); j++) {

                    pixelColor = bmp.getPixel(j, i);

                    Integer counter = (Integer) m.get(pixelColor);
                    if (counter == null)
                        counter = 0;
                    counter++;
                    m.put(pixelColor, counter);
                }
            }

            //sorted a list of our colors for further reuse
            List sortedList = sortMap(m);

            HashMap<Integer, int[]> mostCommonColours = getMostCommonColour(sortedList);
            List<Double> percentOfColors = getPercentOfColors(totalPixels, sortedList);

            //getting most popular colors from the list
            int[] color1 = mostCommonColours.get(0);
            int[] color2 = mostCommonColours.get(1);
            int[] color3 = mostCommonColours.get(2);
            int[] color4 = mostCommonColours.get(3);
            int[] color5 = mostCommonColours.get(4);


            //check color, if it too much light we change text percent to black
            checkColor(color1, viewPercent1);
            checkColor(color2, viewPercent2);
            checkColor(color3, viewPercent3);
            checkColor(color4, viewPercent4);
            checkColor(color5, viewPercent5);

            viewDisplayColor1.setBackgroundColor(Color.rgb(color1[0], color1[1], color1[2]));
            viewDisplayColor2.setBackgroundColor(Color.rgb(color2[0], color2[1], color2[2]));
            viewDisplayColor3.setBackgroundColor(Color.rgb(color3[0], color3[1], color3[2]));
            viewDisplayColor4.setBackgroundColor(Color.rgb(color4[0], color4[1], color4[2]));
            viewDisplayColor5.setBackgroundColor(Color.rgb(color5[0], color5[1], color5[2]));


            viewPercent1.setText(String.format("%.2f", percentOfColors.get(0)) + " %");
            viewPercent2.setText(String.format("%.2f", percentOfColors.get(1)) + " %");
            viewPercent3.setText(String.format("%.2f", percentOfColors.get(2)) + " %");
            viewPercent4.setText(String.format("%.2f", percentOfColors.get(3)) + " %");
            viewPercent5.setText(String.format("%.2f", percentOfColors.get(4)) + " %");


            viewRgb.setText("R:" + color1[0] + " G:" + color1[1] + "\n" + "B:" + color1[2]);
            viewRgb2.setText("R:" + color2[0] + " G:" + color2[1] + "\n" + "B:" + color2[2]);
            viewRgb3.setText("R:" + color3[0] + " G:" + color3[1] + "\n" + "B:" + color3[2]);
            viewRgb4.setText("R:" + color4[0] + " G:" + color4[1] + "\n" + "B:" + color4[2]);
            viewRgb5.setText("R:" + color5[0] + " G:" + color5[1] + "\n" + "B:" + color5[2]);

        })).start();
    }

    private void checkColor(int[] color, TextView viewPercent) {

        if (color[0] > 200 && color[1] > 200 && color[2] > 200) {
            viewPercent.setTextColor(Color.BLACK);
        } else {
            viewPercent.setTextColor(Color.WHITE);
        }
    }

    private List<Double> getPercentOfColors(int totalPixels, List list) {

        int sizeList = list.size();
        List<Double> percentList = new ArrayList<>();

        Map.Entry me;
        for (int i = 0; i < 5; i++) {
            if (sizeList < 5) {
                me = (Map.Entry) list.get(sizeList - 1);

            } else {
                me = (Map.Entry) list.get(sizeList - (i + 1));
            }

            double value = (((Integer) me.getValue()).floatValue() * 100) / totalPixels;
            percentList.add(value);
        }
        return percentList;
    }

    private void decodeYUV420SP(int[] rgb, byte[] yuv420sp, int width, int height) {
        final int frameSize = width * height;

        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & ((int) yuv420sp[yp])) - 16;
                if (y < 0)
                    y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }

                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0)
                    r = 0;
                else if (r > 262143)
                    r = 262143;
                if (g < 0)
                    g = 0;
                else if (g > 262143)
                    g = 262143;
                if (b < 0)
                    b = 0;
                else if (b > 262143)
                    b = 262143;

                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
    }

    public static int[] getRGBArr(int pixel) {
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = (pixel) & 0xff;
        return new int[]{red, green, blue};

    }

    public HashMap<Integer, int[]> getMostCommonColour(List list) {

        int sizeList = list.size();
        HashMap<Integer, int[]> hashMap = new HashMap<>();

        Map.Entry me;
        for (int i = 0; i < 5; i++) {
            if (sizeList < 5) {
                me = (Map.Entry) list.get(sizeList - 1);
            } else {
                me = (Map.Entry) list.get(sizeList - (i + 1));
            }
            int[] rgb = getRGBArr((Integer) me.getKey());
            hashMap.put(i, rgb);
        }

        return hashMap;
    }

    private List sortMap(Map map) {
        List list = new LinkedList(map.entrySet());
        Collections.sort(list, (Comparator) (o1, o2) -> ((Comparable) ((Map.Entry) (o1)).getValue())
                .compareTo(((Map.Entry) (o2)).getValue()));
        return list;
    }


}