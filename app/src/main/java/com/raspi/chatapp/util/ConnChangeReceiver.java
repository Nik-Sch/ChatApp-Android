package com.raspi.chatapp.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.raspi.chatapp.ui.chatting.ChatActivity;
import com.raspi.chatapp.util.service.MessageService;

public class ConnChangeReceiver extends BroadcastReceiver{
  public ConnChangeReceiver(){
    Log.d("ConnectionChangeReceive", "CREATED CONN_CHANGE_RECEIVER");
  }

  @Override
  public void onReceive(Context context, Intent intent){
    Log.d("ConnectionChangeReceive", "starting service as reconnect");
    context.startService(new Intent(context, MessageService.class).setAction(ChatActivity.RECONNECT));
  }
}
