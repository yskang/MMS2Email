package com.studioY.mms2email;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootingReceiver extends BroadcastReceiver{
    public AppPreference appPreference;

    @Override
    public void onReceive(Context context, Intent intent) {
        appPreference = new AppPreference(context);

        if(appPreference.getValue(Commons.SERVICE_STATUS).equals(Commons.SERVICE_OFF)){
            return;
        }

        if(intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)){
            Intent serviceIntent = new Intent(Commons.COM_STUDIO_Y_MMS2EMAIL_START_MMSMONITOR);
            context.startService(serviceIntent);
        }
    }
}
