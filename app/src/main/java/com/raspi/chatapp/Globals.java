package com.raspi.chatapp;

import android.app.Application;

public class Globals extends Application{
    public Globals(){
        xmppManager = null;
    }

    private XmppManager xmppManager;

    public XmppManager getXmppManager(){
        return xmppManager;
    }

    public void setXmppManager(XmppManager xmppManager){
        this.xmppManager = xmppManager;
    }
}
