package com.raspi.chatapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.raspi.chatapp.service.MessageService;

public class ConnChangeReceiver extends BroadcastReceiver{
    public ConnChangeReceiver(){
        Log.d("ConnectionChangeReceive", "CREATED CONN_CHANGE_RECEIVER");
    }

    @Override
    public void onReceive(Context context, Intent intent){
        Log.d("ConnectionChangeReceive", "received conn change");
        NetworkInfo info = ((ConnectivityManager) context.getSystemService(Context
                .CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (info != null){
            if (info.isConnected()){
                Log.d("ConnectionChangeReceive", "I am connected");
                context.startService(new Intent(context, MessageService.class));
            } else {
                Log.d("ConnectionChangeReceive", "I am not connected");
                context.stopService(new Intent(context, MessageService.class));
            }
        }
    }
}
