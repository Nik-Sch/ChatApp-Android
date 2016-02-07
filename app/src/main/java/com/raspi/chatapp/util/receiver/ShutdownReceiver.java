package com.raspi.chatapp.util.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.raspi.chatapp.util.internet.XmppManager;

public class ShutdownReceiver extends BroadcastReceiver{
  @Override
  public void onReceive(Context context, Intent intent){
    Log.d("SHUTDOWN", "Received a shutdown event...");
    try{
      XmppManager.getInstance(context).disconnect();
    }catch (Exception e){}
  }
}
