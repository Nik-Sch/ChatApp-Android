package com.raspi.chatapp.util;

import android.app.Application;

import com.raspi.chatapp.util.internet.XmppManager;

public class ChatApp extends Application{
  @Override
  public void onCreate(){
    super.onCreate();
    XmppManager.getInstance(getApplicationContext());
  }
}
