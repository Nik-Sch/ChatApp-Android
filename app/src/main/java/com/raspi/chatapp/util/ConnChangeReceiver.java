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
  public ConnChangeReceiver(){
    Log.d("ConnectionChangeReceive", "CREATED CONN_CHANGE_RECEIVER");
  }

  @Override
  public void onReceive(Context context, Intent intent){
    Log.d("ConnectionChangeReceive", "starting service as reconnect");
    context.startService(new Intent(context, MessageService.class).setAction(MainActivity.RECONNECT));
  }
}
