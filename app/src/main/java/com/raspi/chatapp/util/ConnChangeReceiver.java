package com.raspi.chatapp.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.raspi.chatapp.activities.MainActivity;
import com.raspi.chatapp.service.MessageService;

import java.util.Date;

public class ConnChangeReceiver extends BroadcastReceiver{
    private static int CONN_TIMEOUT = 5000;

    public ConnChangeReceiver(){
        Log.d("ConnectionChangeReceive", "CREATED CONN_CHANGE_RECEIVER");
    }

    @Override
    public void onReceive(Context context, Intent intent){
        SharedPreferences preferences = context.getSharedPreferences(MainActivity.PREFERENCES, 0);
        long now = new Date().getTime();
        long lastConnection = preferences.getLong(MainActivity.CONN_TIMEOUT, now - CONN_TIMEOUT -
                1000);
        Log.d("ConnectionChangeReceive", "received conn change with time difference: " +
                (now - lastConnection));
        if (lastConnection + CONN_TIMEOUT < now){
            Log.d("ConnectionChangeReceive", "starting service as reconnect");
            preferences.edit().putLong(MainActivity.CONN_TIMEOUT, now).apply();
            context.startService(new Intent(context, MessageService.class).setAction(MainActivity.RECONNECT));
        }
    }
}
