package com.raspi.chatapp.util.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.raspi.chatapp.util.service.MessageService;

public class BootReceiver extends BroadcastReceiver{
  @Override
  public void onReceive(Context context, Intent intent){
    Log.d("BOOT", "Received a boot event...");
    context.startService(new Intent(context, MessageService.class));
  }
}
