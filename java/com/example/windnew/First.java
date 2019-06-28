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
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.lang.ref.WeakReference;
import java.util.Set;


public class First extends AppCompatActivity {


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

    GraphView graph;
    SeekBar seek;
    VideoView video;
    TextView windtxt, powertxt, speedtxt, torquetxt;
    ImageButton  fileBtn;


    LineGraphSeries<DataPoint> ser;
    LineGraphSeries<DataPoint> ser1, ser2;
    LineGraphSeries<DataPoint> hor, ver;

    int prg = 15;

    float r, v, w = 0, l, cp, po;
    int point = 157;
    Uri uri = null;
    int one, two;

    SharedPreferences sharedPreferences ;
    SharedPreferences.Editor editor;



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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.first);


        sharedPreferences = getSharedPreferences("data",Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();

        r = sharedPreferences.getFloat("rad", (float) 1.2);



        mHandler = new MyHandler(this);

        Context context = this;

        prepar();

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int densityDpi = (int) (metrics.density * 160f);
        Log.i("First", String.valueOf(densityDpi) + "is your xdpi nd i dont know ");


        inti(20, point);


        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                prg = progress;
                inti((int) prg, point);
                ChooseVideo((int) prg);
                windtxt.setText(String.valueOf(prg));
                powertxt.setText(String.valueOf(current(point, (int) prg)));
                torquetxt.setText(String.valueOf(current(point, (int) prg) / point));

                /*if (!windtxt.getText().toString().equals("")) {
                    String data = windtxt.getText().toString();
                    if (usbService != null) { // if UsbService was correctly binded, Send data
                        usbService.write(data.getBytes());
                    }
                }*/
                send(String.valueOf(prg)+"/"+String.valueOf( (int) current(point, (int) prg) / point));


            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                inti((int) prg, point);
                ChooseVideo((int) prg);
                windtxt.setText(String.valueOf(prg));
                powertxt.setText(String.valueOf(current(point, (int) prg)));

                /*if (!windtxt.getText().toString().equals("")) {
                    String data = windtxt.getText().toString();
                    if (usbService != null) { // if UsbService was correctly binded, Send data
                        usbService.write(data.getBytes());
                    }
                }*/

                send(String.valueOf(prg)+"/"+String.valueOf( (int) current(point, (int) prg) / point));



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

        video.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.b));
        video.requestFocus();
        video.start();
    }

    void prepar() {
        graph = (GraphView) findViewById(R.id.graph);
        seek = (SeekBar) findViewById(R.id.seekBar);
        video = (VideoView) findViewById(R.id.videoView);
        windtxt = (TextView) findViewById(R.id.windlabel);
        powertxt = (TextView) findViewById(R.id.powerLabel);
        speedtxt = (TextView) findViewById(R.id.speedLabel);
        torquetxt = (TextView) findViewById(R.id.torqueLabel);
        fileBtn = (ImageButton) findViewById(R.id.filebtn);

        seek.setMax(30);
        seek.setProgress(10);

        windtxt.setText("10");
        powertxt.setText(String.valueOf(current(point, 10)));
        speedtxt.setText("157");
        torquetxt.setText(String.valueOf(current(point, 10) / point));

        ser = new LineGraphSeries<DataPoint>();
        ser1 = new LineGraphSeries<DataPoint>();
        ser2 = new LineGraphSeries<DataPoint>();
        hor = new LineGraphSeries<DataPoint>();
        ver = new LineGraphSeries<DataPoint>();

        fileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(First.this, Second.class));
            }
        });

        graph.setPadding(-10,-10,-10,-10);
    }


    void inti(int a, int point) {


        graph.removeAllSeries();


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


        ser = new LineGraphSeries<>(dataPoints);
        ser1 = new LineGraphSeries<>(dataPoint);
        ser2 = new LineGraphSeries<>(dataPoint1);
        hor = new LineGraphSeries<>(hori);
        // ver = new LineGraphSeries<>(veri);


        ser.setThickness(2);
        ser.setDrawDataPoints(true);
        ser.setDataPointsRadius(2);


        ser1.setColor(Color.BLACK);
        ser1.setThickness(6);
        ser1.setDrawDataPoints(true);
        ser1.setDataPointsRadius(6);

        ser2.setColor(Color.RED);
        ser2.setThickness(4);
        ser2.setDrawDataPoints(true);
        ser2.setDataPointsRadius(4);

        hor.setThickness(0);
        hor.setColor(Color.BLACK);
        hor.setDrawDataPoints(false);
        hor.setDataPointsRadius(1);


        /*ver.setThickness(0);
        ver.setColor(Color.RED);
        ver.setDrawDataPoints(true);
        ver.setDataPointsRadius(1);*/


        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setMaxX(max2);
        graph.getViewport().setMaxY(1000 + max);

        //graph.addSeries(hor);
        //graph.addSeries(ver);
        graph.addSeries(ser);
        graph.addSeries(ser1);
        graph.addSeries(ser2);


        graph.getGridLabelRenderer().setGridColor(Color.BLACK);
        graph.getGridLabelRenderer().setHorizontalAxisTitleColor(Color.BLACK);
        graph.getViewport().setBorderColor(Color.BLACK);
        graph.setTitleColor(Color.BLACK);
        graph.getGridLabelRenderer().setHorizontalLabelsColor(Color.BLACK);
        graph.getGridLabelRenderer().setVerticalLabelsColor(Color.BLACK);


        ser.setTitle("P,RPM Curve");
        ser2.setTitle("Config point");

        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setTextColor(Color.BLACK);
        graph.getLegendRenderer().setBackgroundColor(Color.TRANSPARENT);
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);

        graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
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

    public float current(float o, int k) {
        v = (int) k;
        w = o;
        l = w * r / v;
        cp = (float) ((float) 0.22 * (116 / l - 5) * Math.exp(-12.5 / l));
        po = (float) (0.5 * 1.23 * cp * Math.pow(v, 3));
        return po;
    }

    @Override
    public void onResume() {
        super.onResume();
        video.setVideoURI(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.b));
        video.requestFocus();
        video.start();
        setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
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

    private void optionsDialoge(){
        AlertDialog.Builder builder = new AlertDialog.Builder(First.this);
        View view = getLayoutInflater().inflate(R.layout.options,null);

        Button save = view.findViewById(R.id.saveBtn);
        Button cancle = view.findViewById(R.id.cancelBtn);
        EditText valueTxt = view.findViewById(R.id.value);

        float val = sharedPreferences.getFloat("rad",(float)1.2);
        valueTxt.setText(String.valueOf(val));

        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                float val2= Float.parseFloat(valueTxt.getText().toString());
                if(!valueTxt.getText().toString().isEmpty() && val2>1 && val2 <10 ){
                    r=val2;
                    editor.putFloat("rad",val2);
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
    }


    public void send(String g) {
        if (!g.equals("")) {
            String[] data = g.split("/");
            int windSpeed = Integer.parseInt(data[0]);
            int torque=Integer.parseInt(data[1]);


            byte[] rec = new byte[2];
            rec[0] = (byte) (windSpeed);
            rec[1] = (byte) torque;
            if (usbService != null) { // if UsbService was correctly binded, Send data
                //usbService.write(data.getBytes());
                usbService.write(rec);
            } else Toast.makeText(First.this, "usb service is null", Toast.LENGTH_SHORT).show();
        }
    }

    private static class MyHandler extends Handler {
        private final WeakReference<First> mActivity;

        public MyHandler(First activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    String data = (String) msg.obj;
                    mActivity.get().speedtxt.setText(data);
                    mActivity.get().point = Integer.parseInt(data);
                    mActivity.get().inti((int) mActivity.get().prg, Integer.parseInt(data));
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
}
