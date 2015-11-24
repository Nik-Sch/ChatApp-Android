package com.raspi.chatapp;

import android.app.Application;

/**
 * Created by gamer on 11/24/2015.
 */
public class Globals extends Application{

    private XmppManager xmppManager;

    public XmppManager getXmppManager(){
        return xmppManager;
    }

    public void setXmppManager(XmppManager xmppManager){
        this.xmppManager = xmppManager;
    }
}
