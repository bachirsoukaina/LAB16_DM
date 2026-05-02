package com.example.lab16_dm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView displayTime;
    private Button btnLaunch, btnHalt;
    private TimerService timerService;
    private boolean connected = false;
    private Handler uiHandler;
    private Runnable uiUpdater;

    private final ServiceConnection serviceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            TimerService.TimerBinder tb = (TimerService.TimerBinder) binder;
            timerService = tb.getInstance();
            connected = true;
            beginUiRefresh();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            connected = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        displayTime = findViewById(R.id.displayTime);
        btnLaunch   = findViewById(R.id.btnLaunch);
        btnHalt     = findViewById(R.id.btnHalt);

        uiHandler = new Handler(Looper.getMainLooper());

        btnLaunch.setOnClickListener(v -> launchTimer());
        btnHalt.setOnClickListener(v -> haltTimer());
    }

    private void launchTimer() {
        Intent svc = new Intent(this, TimerService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(svc);
        } else {
            startService(svc);
        }
        bindService(svc, serviceConn, Context.BIND_AUTO_CREATE);
    }

    private void haltTimer() {
        Intent svc = new Intent(this, TimerService.class);
        svc.setAction("CMD_STOP");
        stopService(svc);
        if (connected) {
            unbindService(serviceConn);
            connected = false;
        }
        stopUiRefresh();
        displayTime.setText("00:00");
    }

    private void beginUiRefresh() {
        uiUpdater = new Runnable() {
            @Override
            public void run() {
                if (connected && timerService != null) {
                    int sec = timerService.getElapsedSeconds();
                    displayTime.setText(String.format(
                            java.util.Locale.getDefault(), "%02d:%02d", sec / 60, sec % 60));
                }
                uiHandler.postDelayed(this, 500);
            }
        };
        uiHandler.post(uiUpdater);
    }

    private void stopUiRefresh() {
        if (uiUpdater != null) uiHandler.removeCallbacks(uiUpdater);
    }

    @Override
    protected void onDestroy() {
        stopUiRefresh();
        if (connected) {
            unbindService(serviceConn);
        }
        super.onDestroy();
    }
}