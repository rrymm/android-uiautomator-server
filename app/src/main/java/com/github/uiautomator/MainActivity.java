package com.github.uiautomator;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.uiautomator.util.MemoryManager;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends Activity {
    private final String TAG = "ATXMainActivity";

    private TextView tvInStorage;
    private TextView textViewIP;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.i(TAG, "service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(TAG, "service disconnected");

            // restart service
            Intent intent = new Intent(MainActivity.this, Service.class);
            startService(intent);
//            bindService(intent, connection, BIND_IMPORTANT | BIND_AUTO_CREATE);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent serviceIntent = new Intent(this, Service.class);
        startService(serviceIntent);
        bindService(serviceIntent, connection, BIND_IMPORTANT | BIND_AUTO_CREATE);

        Button btnFinish = (Button) findViewById(R.id.btn_finish);
        btnFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                unbindService(connection);
                stopService(new Intent(MainActivity.this, Service.class));
                finish();
            }
        });

        Button btnIdentify = (Button) findViewById(R.id.btn_identify);
        btnIdentify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, IdentifyActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("theme", "RED");
                intent.putExtras(bundle);
                startActivity(intent);
            }
        });

        ((Button) findViewById(R.id.accessibility)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });

        ((Button) findViewById(R.id.development_settings)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
            }
        });

        ((Button) findViewById(R.id.stop_uiautomator)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Request request = new Request.Builder()
                        .url("http://127.0.0.1:7912/uiautomator")
                        .delete()
                        .build();
                new OkHttpClient().newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        e.printStackTrace();
                        Looper.prepare();
                        Toast.makeText(MainActivity.this, "Uiautomator already stopped ", Toast.LENGTH_SHORT).show();
                        Looper.loop();
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        Looper.prepare();
                        Toast.makeText(MainActivity.this, "Uiautomator stopped", Toast.LENGTH_SHORT).show();
                        Looper.loop();
                    }
                });
            }
        });

        ((Button) findViewById(R.id.stop_atx_agent)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                atxAgentStopConfirm();
            }
        });

        Intent intent = getIntent();
        boolean isHide = intent.getBooleanExtra("hide", false);
        if (isHide) {
            Log.i(TAG, "launch args hide:true, move to background");
            moveTaskToBack(true);
        }
        textViewIP = (TextView) findViewById(R.id.ip_address);
        tvInStorage = (TextView) findViewById(R.id.in_storage);
    }

    private void atxAgentStopConfirm() {
        AlertDialog.Builder localBuilder = new AlertDialog.Builder(this);
        localBuilder.setTitle("Confirm");
        localBuilder.setMessage("Sure to stop ATX_AGENT?");
        localBuilder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                stopAtxAgent();
            }
        });
        localBuilder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        localBuilder.show();
    }

    private void stopAtxAgent() {
        Request request = new Request.Builder()
                .url("http://127.0.0.1:7912/stop")
                .get()
                .build();
        new OkHttpClient().newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Looper.prepare();
                Toast.makeText(MainActivity.this, "server already stopped", Toast.LENGTH_SHORT).show();
                Looper.loop();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Looper.prepare();
                Toast.makeText(MainActivity.this, "server stopped", Toast.LENGTH_SHORT).show();
                Looper.loop();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        tvInStorage.setText(Formatter.formatFileSize(this, MemoryManager.getAvailableInternalMemorySize()) + "/" + Formatter.formatFileSize(this, MemoryManager.getTotalExternalMemorySize()));
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        int ip = wifiManager.getConnectionInfo().getIpAddress();
        String ipStr = (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + ((ip >> 24) & 0xFF);
        textViewIP.setText(ipStr);
        textViewIP.setTextColor(Color.BLUE);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
