package com.raspi.chatapp.util.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.raspi.chatapp.util.internet.XmppManager;


public class ConnChangeReceiver extends BroadcastReceiver{
  @Override
  public void onReceive(Context context, Intent intent){
    try{
      XmppManager.getInstance(context).getConnection().connect();
    }catch (Exception e){}
  }
}
