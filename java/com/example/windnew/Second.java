package com.example.windnew;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Set;

public class Second extends AppCompatActivity {

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private UsbService usbService;

    private MyHandler mHandler;

    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };


    LineGraphSeries<DataPoint> MGSeri;
    LineGraphSeries<DataPoint> PSeerie, MiniGSeries, popGserie, point1, point2;
    GraphView MGraph, miniGraph, popGrah;

    ImageButton seekBtn;
    VideoView video;
    TextView powerT, windT, torbuneT, torqueT, timeT;
    ImageButton load, delet, doubl, start, stop;


    Uri uri;

    int one, two, i;

    Thread thread;

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;


    DataPoint[] mainData = new DataPoint[700];
    DataPoint[] tempData = new DataPoint[5000];
    DataPoint[] progress = new DataPoint[20];
    DataPoint[] miniData;

    int wind = 0, maxY = 0, point = 157, count = 0, c = 0;

    float r, v, w = 0, l, cp, po;
    float playSpeed = 1;

    boolean isLoaded = false, isSarted = false, isPlaying = false, yes = false, isFinished = false, isDoulble = false, isThread = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.second);

        sharedPreferences = getSharedPreferences("data", Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
        r = sharedPreferences.getFloat("rad", (float) 1.2);

        mHandler = new MyHandler(this);
        prepare();

    }


    @Override
    public void onResume() {
        super.onResume();
        video.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.a));
        video.requestFocus();
        video.start();
        setFilters();
        startService(UsbService.class, usbConnection, null);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }


    void prepare() {


        powerT = (TextView) findViewById(R.id.powerLabel);
        windT = (TextView) findViewById(R.id.windlabel);
        torbuneT = (TextView) findViewById(R.id.speedLabel);
        torqueT = (TextView) findViewById(R.id.torqueLabel);
        timeT = (TextView) findViewById(R.id.time);
        MGraph = (GraphView) findViewById(R.id.graph);
        miniGraph = (GraphView) findViewById(R.id.mini);
        seekBtn = (ImageButton) findViewById(R.id.seekbtn);
        video = (VideoView) findViewById(R.id.videoView);
        load = (ImageButton) findViewById(R.id.loaddelete);
        start = (ImageButton) findViewById(R.id.playpause);
        stop = (ImageButton) findViewById(R.id.stop);

        load.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isLoaded) {
                    delet();
                    Toast.makeText(Second.this, "File Deleted", Toast.LENGTH_SHORT).show();
                } else {
                    try {
                        fileDialoge();

                    } catch (Exception e) {
                        Toast.makeText(Second.this, e.toString(), Toast.LENGTH_SHORT).show();
                    }
                }
//                if (!isThread) {
//                    thread.start();
//                    isThread = true;
//                }

            }
        });

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isLoaded) {
                    if (isFinished) {
                        miniGraph.removeAllSeries();
                        miniGraph.addSeries(MiniGSeries);
                        yes = true;
                        isSarted = true;
                        isPlaying = true;
                        isFinished = false;
                    } else {
                        if (isSarted) {
                            if (isPlaying) {
                                pause();
                            } else {
                                resume();
                            }
                        } else {
                            start();
                        }
                    }

                } else {
                    Toast.makeText(Second.this, "File is not loaded", Toast.LENGTH_SHORT).show();
                }

            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isLoaded) {
                    rest();

                } else {
                    Toast.makeText(Second.this, "File is not loaded", Toast.LENGTH_SHORT).show();
                }
            }
        });


        seekBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Second.this, First.class));
            }
        });


        video.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {

                video.setVideoURI(uri);
                video.requestFocus();
                video.start();
            }
        });
        video.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {

                mediaPlayer.setLooping(true);
            }
        });
        video.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                optionsDialoge();
                return false;
            }
        });
        video.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.a));
        video.requestFocus();
        video.start();
        ChooseVideo(0);


        miniGraph.getGridLabelRenderer().setGridColor(Color.TRANSPARENT);
        miniGraph.getGridLabelRenderer().setHorizontalAxisTitleColor(Color.TRANSPARENT);
        miniGraph.getViewport().setBorderColor(Color.BLACK);
        miniGraph.getGridLabelRenderer().setTextSize(1);
        miniGraph.animate();
        miniGraph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog();
            }
        });


        MGraph.getGridLabelRenderer().setGridColor(Color.BLACK);
        MGraph.getGridLabelRenderer().setHorizontalAxisTitleColor(Color.BLACK);
        MGraph.getViewport().setBorderColor(Color.BLACK);
        MGraph.setTitleColor(Color.BLACK);
        MGraph.getGridLabelRenderer().setHorizontalLabelsColor(Color.BLACK);
        MGraph.getGridLabelRenderer().setVerticalLabelsColor(Color.BLACK);
        MGraph.getLegendRenderer().setVisible(true);
        MGraph.getLegendRenderer().setTextColor(Color.BLACK);
        MGraph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
        MGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    // show normal x values
                    if (value != 0) return super.formatLabel(value, isValueX) + " RPM";
                    else return "0";
                } else {
                    // show currency for y values
                    if (value != 0) return super.formatLabel(value, isValueX) + " W";
                    else return "";
                }
            }
        });
        preparThread();

    }

    void ChooseVideo(int a) {


        if (a == 0) {
            uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.a);
            one = 1;
        } else if (a < 5) {
            uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.b);
            one = 2;
        } else if (a < 10) {
            uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.c);
            one = 3;
        } else if (a < 15) {
            uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.d);
            one = 4;
        } else if (a < 20) {
            uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.e);
            one = 5;
        } else if (a < 25) {
            uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.f);
            one = 6;
        } else {
            uri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.j);
            one = 7;
        }

        if (one != two) {
            video.setVideoURI(uri);
            two = one;

        }

    }

    void read(String fileName) {


        try {
            File file = null;

            try {

                file = new File("/mnt/sdcard/"+fileName);
            } catch (Exception e) {
                Toast.makeText(Second.this, e.toString(), Toast.LENGTH_SHORT).show();
            }
            FileInputStream fis = new FileInputStream(file);

            if (fis != null) {
                InputStreamReader isr = new InputStreamReader(fis);
                BufferedReader buff = new BufferedReader(isr);
                String line;


                while ((line = buff.readLine()) != null) {
                    String[] data = line.split(",");
                    int x = Integer.parseInt(data[0]);
                    int y = Integer.parseInt(data[1]);
                    tempData[c] = new DataPoint(x, y);
                    c++;
                    if (y > maxY) {
                        maxY = y;
                    }

                }
                fis.close();
            } else {
                Toast.makeText(Second.this, "file not found 0 ", Toast.LENGTH_SHORT).show();
            }


            if (tempData != null) {
                miniData = new DataPoint[c];
                for (int i = 0; i < c; i++) {
                    miniData[i] = tempData[i];
                }

                MiniGSeries = new LineGraphSeries<>(miniData);
                MiniGSeries.setThickness(2);
                MiniGSeries.setDataPointsRadius(2);
                MiniGSeries.setColor(Color.BLACK);

                //miniGraph.removeAllSeries();
                miniGraph.addSeries(MiniGSeries);
                miniGraph.getViewport().setXAxisBoundsManual(true);
                miniGraph.getViewport().setYAxisBoundsManual(true);
                miniGraph.getViewport().setMinX(0);
                miniGraph.getViewport().setMaxX(c - 2);
                miniGraph.getViewport().setMaxY(maxY + 3);
                miniGraph.setPadding(-100, -100, -100, -100);

                inti((int) miniData[0].getY(), point);


                wind = (int) miniData[0].getY();
                ChooseVideo(wind);
                setTexts(count, (int) miniData[count].getY());

                Toast.makeText(Second.this, "The file is loaded", Toast.LENGTH_SHORT).show();
                isLoaded = true;
                isPlaying = false;
                yes = false;
                load.setImageURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.rubbish));

            }

            if (!isThread) {
                thread.start();
                isThread = true;
            }


        } catch (IOException e) {
            Toast.makeText(Second.this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    public float current(float o, int k) {
        v = (int) k;
        w = o;
        l = w * r / v;
        cp = (float) ((float) 0.22 * (116 / l - 5) * Math.exp(-12.5 / l));
        po = (float) (0.5 * 1.23 * cp * Math.pow(v, 3));
        return po;
    }

    void inti(int a, int point) {


        MGraph.removeAllSeries();


        DataPoint[] hori = new DataPoint[(int) point];
        for (int e = 0; e < point; e++) {
            hori[e] = new DataPoint(e, current(point, a));
        }


        int max = 4000;
        int max2 = 400;
        DataPoint[] dataPoints = new DataPoint[700];
        for (int i = 0; i < 700; i++) {
            dataPoints[i] = new DataPoint(i, (int) current(i, a));
            if (current(i, a) > max) {
                max = (int) current(i, a) + 10;
            }
            if (current(i, a) > 0 && i > max2) {
                max2 = i + 10;
            }
        }

        DataPoint[] dataPoint = new DataPoint[1];
        for (int j = 0; j < 1; j++) {
            dataPoint[j] = new DataPoint(point, (int) current(point, a));
        }

        DataPoint[] dataPoint1 = new DataPoint[1];
        for (int k = 0; k < 1; k++) {
            dataPoint1[k] = new DataPoint(point, (int) current(point, a));
        }

        MGSeri = new LineGraphSeries<>(dataPoints);
        point1 = new LineGraphSeries<>(dataPoint);
        point2 = new LineGraphSeries<>(dataPoint1);

        MGSeri.setThickness(2);
        MGSeri.setDrawDataPoints(true);
        MGSeri.setDataPointsRadius(2);
        MGSeri.setTitle("P,RPM Curve");

        point1.setColor(Color.BLACK);
        point1.setThickness(6);
        point1.setDrawDataPoints(true);
        point1.setDataPointsRadius(6);
        point1.setTitle("Config point");

        point2.setColor(Color.RED);
        point2.setThickness(4);
        point2.setDrawDataPoints(true);
        point2.setDataPointsRadius(4);

        MGraph.getViewport().setXAxisBoundsManual(true);
        MGraph.getViewport().setYAxisBoundsManual(true);
        MGraph.getViewport().setMaxX(max2);
        MGraph.getViewport().setMaxY(1000 + max);
//        MGraph.getViewport().setMaxY(8200);

        MGraph.getGridLabelRenderer().setGridColor(Color.BLACK);
        MGraph.getGridLabelRenderer().setHorizontalAxisTitleColor(Color.BLACK);
        MGraph.getViewport().setBorderColor(Color.BLACK);
        MGraph.setTitleColor(Color.BLACK);
        MGraph.getGridLabelRenderer().setHorizontalLabelsColor(Color.BLACK);
        MGraph.getGridLabelRenderer().setVerticalLabelsColor(Color.BLACK);

        MGraph.addSeries(MGSeri);
        MGraph.addSeries(point1);
        MGraph.addSeries(point2);

        MGraph.getLegendRenderer().setVisible(true);
        MGraph.getLegendRenderer().setTextColor(Color.BLACK);
        MGraph.getLegendRenderer().setBackgroundColor(Color.TRANSPARENT);
        MGraph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
        MGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    // show normal x values
                    if (value != 0) return super.formatLabel(value, isValueX) + " RPM";
                    else return "0";
                } else {
                    // show currency for y values
                    if (value != 0) return super.formatLabel(value, isValueX) + " W";
                    else return "";
                }
            }
        });

    }

    void preparThread() {
        thread = new Thread(new Runnable() {
            @Override
            public void run() {

                while (count <= miniData.length) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (count == miniData.length) {
                                count = 0;
                                pause();
                                isFinished = true;
                                Toast.makeText(Second.this, "finish", Toast.LENGTH_SHORT).show();
                            } else {
                                if (yes) {
                                    inti((int) miniData[count].getY(), point);
                                    progres(count);
                                    wind = (int) miniData[count].getY();
                                    ChooseVideo(wind);
                                    setTexts(count, (int) miniData[count].getY());
                                    send(String.valueOf((int) miniData[count].getY()));
                                    count++;

                                }
                            }
                        }

                    });
                    try {
                        Thread.sleep((long) (1000 / playSpeed));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }


                }


            }
        });
    }

    void setTexts(int time, int windS) {
        String t = String.format(" %02d:%02d", (time / 60), (time % 60));
        //timeT.setText(((int)(time/60))+":"+((int)(time%60)));
        timeT.setText(t);
        windT.setText(String.valueOf(windS));
        torqueT.setText(String.valueOf(current(point, (int) windS) / point));
        torbuneT.setText(String.valueOf(point));
        powerT.setText(String.valueOf(current(point, windS)));


    }

    void progres(int p) {

        for (int i = 0; i < 20; i++) {
            progress[i] = new DataPoint((p + (i / 10)), 300);
        }

        PSeerie = new LineGraphSeries<>(progress);

        PSeerie.setDrawBackground(true);
        PSeerie.setBackgroundColor(Color.parseColor("#597290"));

        miniGraph.addSeries(PSeerie);
        miniGraph.addSeries(MiniGSeries);

    }

    void dialog() {
        if (isLoaded) {
            AlertDialog.Builder builder = new AlertDialog.Builder(Second.this);
            View view = getLayoutInflater().inflate(R.layout.layout, null);
            GraphView big = (GraphView) view.findViewById(R.id.popgraph);
            popGserie = new LineGraphSeries<>(miniData);

            popGserie.setThickness(4);
            popGserie.setDataPointsRadius(4);

            big.addSeries(popGserie);
            big.getViewport().setXAxisBoundsManual(true);
            big.getViewport().setYAxisBoundsManual(true);
            big.getViewport().setMinX(0);
            big.getViewport().setMaxX(c);
            big.getViewport().setMaxY(maxY);
            big.getViewport().setScrollable(true);
            big.getViewport().setScalable(true);
            big.getGridLabelRenderer().setGridColor(Color.BLACK);
            big.getGridLabelRenderer().setHorizontalAxisTitleColor(Color.BLACK);
            big.getViewport().setBorderColor(Color.BLACK);
            big.setTitleColor(Color.BLACK);
            big.getGridLabelRenderer().setHorizontalLabelsColor(Color.BLACK);
            big.getGridLabelRenderer().setVerticalLabelsColor(Color.BLACK);
            big.animate();
            // big.setBackgroundColor(Color.rgb(37,37,37));
            big.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
                @Override
                public String formatLabel(double value, boolean isValueX) {
                    if (isValueX) {
                        // show normal x values
                        if (value != 0) return super.formatLabel(value, isValueX) + " s";
                        else return "0";
                    } else {
                        // show currency for y values
                        if (value != 0) return super.formatLabel(value, isValueX) + " m/s";
                        else return "";
                    }
                }
            });


            builder.setView(view);
            AlertDialog dialog = builder.create();
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.show();
            pause();
        }

    }

    public void send(String g) {
        if (!g.equals("")) {
            String data = g;
            int windSpeed = Integer.parseInt(data);

            byte[] rec = new byte[2];
            rec[0] = (byte) (windSpeed / 256);
            rec[1] = (byte) (windSpeed % 256);
            if (usbService != null) { // if UsbService was correctly binded, Send data
                //usbService.write(data.getBytes());
                usbService.write(rec);
            } else Toast.makeText(Second.this, "usb service is null", Toast.LENGTH_SHORT).show();
        }
    }

    void start() {
        // preparThread();
        // thread.start();
        isPlaying = true;
        isSarted = true;
        yes = true;
        start.setImageURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.pause));
    }

    void pause() {
        yes = false;
        isPlaying = false;
        start.setImageURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.music));
    }

    void resume() {
        isPlaying = true;
        yes = true;
        start.setImageURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.pause));

    }

    void rest() {
        yes = false;
        isPlaying = false;
        isSarted = false;
        miniGraph.removeAllSeries();
        miniGraph.addSeries(MiniGSeries);
        count = 0;
        setTexts(count, (int) miniData[count].getY());
        ChooseVideo((int) miniData[0].getY());
        inti((int) miniData[0].getY(), point);
        start.setImageURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.music));
    }

    void delet() {

        isLoaded = false;
        isSarted = false;
        isPlaying = false;
        yes = false;
        isFinished = false;
        isDoulble = false;


        MGraph.removeAllSeries();
        miniGraph.removeAllSeries();

        tempData = new DataPoint[5000];
        count = 0;
        maxY = 0;
        c = 0;
        setTexts(0, 0);
        ChooseVideo(0);

        start.setImageURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.music));
        load.setImageURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.drawable.text));

        Toast.makeText(Second.this, "File Deleted", Toast.LENGTH_SHORT).show();

    }

    private void optionsDialoge() {

        AlertDialog.Builder builder = new AlertDialog.Builder(Second.this);
        View view = getLayoutInflater().inflate(R.layout.options, null);

        Button save = view.findViewById(R.id.saveBtn);
        Button cancle = view.findViewById(R.id.cancelBtn);
        EditText valueTxt = view.findViewById(R.id.value);

        float val = sharedPreferences.getFloat("rad", (float) 1.2);
        valueTxt.setText(String.valueOf(val));

        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                float val2 = Float.parseFloat(valueTxt.getText().toString());
                if (!valueTxt.getText().toString().isEmpty() && val2 > 1 && val2 < 10) {
                    r = val2;
                    editor.putFloat("rad", val2);
                    editor.commit();
                    dialog.hide();
                }
            }
        });


        cancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.hide();
            }
        });

        dialog.show();
        pause();

    }

    private static class MyHandler extends Handler {
        private final WeakReference<Second> mActivity;

        public MyHandler(Second activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    if (mActivity.get().isLoaded) {
                        String data = (String) msg.obj;
                        mActivity.get().point = Integer.parseInt(data);
                        int count = mActivity.get().count;
                        mActivity.get().setTexts(count, (int) mActivity.get().miniData[count].getY());
                        mActivity.get().inti((int) mActivity.get().miniData[count].getY(), Integer.parseInt(data));
                    }

                    break;
                case UsbService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE", Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    void fileDialoge(){
        AlertDialog.Builder builder = new AlertDialog.Builder(Second.this);
        View view = getLayoutInflater().inflate(R.layout.dialogfile, null);

        ListView listView =view.findViewById(R.id.list);

        File directory = new File("/mnt/sdcard");
        File[] files = directory.listFiles();

        final ArrayList<String> arry = new ArrayList<>();

        for (int i = 0; i < files.length; i++)
        {
            arry.add(files[i].getName());
        }


        ArrayAdapter arrayAdapter=new ArrayAdapter(this,android.R.layout.simple_list_item_1,arry);
        listView.setAdapter(arrayAdapter);


        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
        pause();


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String fileName= arry.get(i).toString();
                if(fileName.endsWith(".csv")){
                    read(fileName);
                    dialog.hide();
                }else {
                    Toast.makeText(Second.this,"this file can't be loaded",Toast.LENGTH_SHORT);
                }

            }
        });
    }


}

