package com.studioY.mms2email;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class MMSMonitorService extends Service {
    private MMSMonitor mmsMonitor;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("ysakng", "service started");

        Notification notification = new Notification(R.drawable.ic_launcher, "MMS 전달 서비스", System.currentTimeMillis());
        notification.setLatestEventInfo(getApplicationContext(), "MMS 전달 서비스", "MMS가 수신되면 지정된 e-mail로 전송됩니다.", null);
        startForeground(1, notification);
        startForeground(startId, notification);

        mmsMonitor = new MMSMonitor(this);
        mmsMonitor.startMMSMonitoring();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d("ysakng", "service stopped");

        mmsMonitor.stopMMSMonitoring();
        super.onDestroy();
    }
}
