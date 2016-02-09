//package com.raspi.chatapp.util.internet;
//
//import android.content.Context;
//import android.content.Intent;
//import android.support.annotation.Nullable;
//import android.support.v4.content.LocalBroadcastManager;
//import android.util.Log;
//
//import com.raspi.chatapp.ui.chatting.ChatActivity;
//import com.raspi.chatapp.util.storage.MessageHistory;
//
//import org.jivesoftware.smack.ConnectionListener;
//import org.jivesoftware.smack.XMPPConnection;
//import org.jivesoftware.smack.chat.Chat;
//import org.jivesoftware.smack.chat.ChatManager;
//import org.jivesoftware.smack.packet.Presence;
//import org.jivesoftware.smack.roster.Roster;
//import org.jivesoftware.smack.tcp.XMPPTCPConnection;
//import org.w3c.dom.Document;
//import org.w3c.dom.Element;
//
//import java.io.StringWriter;
//
//import javax.xml.parsers.DocumentBuilderFactory;
//import javax.xml.transform.Transformer;
//import javax.xml.transform.TransformerFactory;
//import javax.xml.transform.dom.DOMSource;
//import javax.xml.transform.stream.StreamResult;
//
///**
// * XmppManager is the wrapper Singleton for the xmppConnection which provides
// * all important functions. It is a Singleton to prohibit that one
// * message is received twice and to make sure it always exists :)
// */
//public class XmppManager{
//
//  private static class Holder {
//    static final XmppManager INSTANCE = new XmppManager(SERVER, SERVICE, PORT);
//  }
//
//
//  private String server;
//  private String service;
//  private int port;
//
//  /**
//   * returns an instance of the xmppManager
//   * @param context - the context with which to initialize a
//   *                LocalBroadCastManager, if this is not the first call of
//   *                this function it might also be null
//   * @return
//   */
//  @Nullable
//  public static XmppManager getInstance(Context context){
//    //yes this is the lazy implementation for the LBMgr but I think for the
//    // LBMgr it is not that important that there might be a second
//    // initialization
//    if (LBMgr == null && context != null)
//      LBMgr = LocalBroadcastManager.getInstance(context);
//    return Holder.INSTANCE;
//  }
//
//  /**
//   * creates a IM Manager with the given server ID
//   * @param server  host address
//   * @param service service name
//   * @param port port
//   */
//  protected XmppManager(String server, String service, int port){
//    this.server = server;
//    this.service = service;
//    this.port = port;
//    Log.d("DEBUG", "Success: created xmppManager");
//  }
//
//  /**
//   * returns the roster for the current connection
//   *
//   * @return the roster and null if the roster cannot be accessed
//   */
//  public Roster getRoster(){
//    if (connection != null && connection.isConnected()){
//      Log.d("DEBUG", "Success: returning roster.");
//      return Roster.getInstanceFor(connection);
//    }else{
//      Log.d("DEBUG", "Couldn't get the roster: No connection.");
//      return null;
//    }
//  }
//
////  /**
////   * disconnects
////   */
////  public void disconnect(){
////    if (connection != null && connection.isConnected()){
////      connection.disconnect();
////      Log.d("DEBUG", "Success: Disconnected.");
////    }else
////      Log.e("ERROR", "Disconnecting failed: No connection.");
////  }
//
//
//}
