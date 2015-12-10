package com.raspi.chatapp.util;

import android.app.Application;

public class Globals extends Application{
  private XmppManager xmppManager;

  public Globals(){
    xmppManager = null;
  }

  public XmppManager getXmppManager(){
    return xmppManager;
  }

  public void setXmppManager(XmppManager xmppManager){
    this.xmppManager = xmppManager;
  }
}
