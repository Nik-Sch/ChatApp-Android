/*
 * Copyright 2016 Niklas Schelten
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.raspi.chatapp.util;

import android.content.Context;

/**
 * These are the main constants used all over the application.
 */
public class Constants{
  /**
   * The string referring the the {@link android.content.SharedPreferences
   * SharedPreferences} I am using for everything that is not related to
   * settings. The settings are managed in the default {@link android.content
   * .SharedPreferences SharedPreferences}.
   */
  public static final String PREFERENCES = "com.raspi.chatapp.util.Constants.PREFERENCES";
  /**
   * This string references to everything related to the user name which I
   * use to log into the XMPP Server.
   */
  public static final String USERNAME = "com.raspi.chatapp.util.Constants.USERNAME";
  /**
   * This string references to everything related to my password which I use
   * to log into the XMPP Server.
   */
  public static final String PASSWORD = "com.raspi.chatapp.util.Constants.PASSWORD";
  /**
   * This string references to everything related to the buddyId of the
   * chat partner. This is not the {@link Constants#CHAT_NAME chatName}.
   */
  public static final String BUDDY_ID = "com.raspi.chatapp.util.Constants.BUDDY_ID";
  /**
   * This string references to everything related to the name of the
   * chat partner. This is not the {@link Constants#BUDDY_ID buddId}.
   */
  public static final String CHAT_NAME = "com.raspi.chatapp.util.Constants.CHAT_NAME";
  /**
   * This string references to everything related to the body of a sent or
   * received message. That means the actual content and not the metadata.
   */
  public static final String MESSAGE_BODY = "com.raspi.chatapp.util.Constants.MESSAGE_BODY";
  /**
   * This string references to everything related to the "presenceChanged"
   * event that is fired every time the presence of anyone in my roster has
   * changed. The presence signals the last time the buddy was online.
   */
  public static final String PRESENCE_CHANGED = "com.raspi.chatapp.util.Constants.PRESENCE_CHANGED";
  /**
   * This string references to everything related to the presence status.
   * That means the actual content of the new presence that has changed.
   */
  public static final String PRESENCE_STATUS = "com.raspi.chatapp.util.Constants.PRESENCE_STATUS";
  /**
   * This string references to everything related to the "messageReceived"
   * event which is fired as soon as someone sends me a message.
   */
  public static final String MESSAGE_RECEIVED = "com.raspi.chatapp.util.Constants.MESSAGE_RECEIVED";
  /**
   * This string references to everything related to the "reconnectionEvent"
   * which is fired every time I reconnected to the XMPP server.
   */
  public static final String RECONNECTED = "com.raspi.chatapp.util.Constants.RECONNECTED";
  /**
   * This string references to everything related to the type of a message
   * which, at the moment, are the following.
   * <ul>
   * <li>{@link com.raspi.chatapp.util.storage.MessageHistory#TYPE_TEXT
   * TEXT}</li>
   * <li>{@link com.raspi.chatapp.util.storage.MessageHistory#TYPE_IMAGE IMAGE
   * }</li>
   * </ul>
   */
  public static final String MESSAGE_TYPE = "com.raspi.chatapp.util.Constants.MESSAGE_TYPE";
  /**
   * This string references to everything related to the uri of the image to
   * be sent.
   */
  public static final String IMAGE_URI = "com.raspi.chatapp.util.Constants.IMAGE_URI";
  /**
   * This string references to everything related to the id of a message
   */
  public static final String MESSAGE_ID = "com.raspi.chatapp.util.Constants.MESSAGE_ID";
  /**
   * This string references to everything related to the id that the corresponding chat partner
   * has of this message. That means every message has an messageId, which is mine and an
   * othersId which is the message id of the buddy.
   */
  public static final String MESSAGE_OTHERS_ID = "com.raspi.chatapp.util.Constants.MESSAGE_OTHERS_ID";
  /**
   * This string references to everything related to the event
   * "messageStatusChanged" which is fired when the buddy sent a acknowledge
   * message. Currently are the following
   * {@link com.raspi.chatapp.util.internet.XmppManager#sendAcknowledgement(String, long, String) acknowledgements}
   * supported:
   * <ul>
   * <li>{@link com.raspi.chatapp.util.storage.MessageHistory#STATUS_RECEIVED RECEIVED}</li>
   * <li>{@link com.raspi.chatapp.util.storage.MessageHistory#STATUS_READ READ
   * }</li>
   * </ul>
   */
  public static final String MESSAGE_STATUS_CHANGED = "com.raspi.chatapp.util.Constants.MESSAGE_STATUS_CHANGED";
  /**
   * This string references to the {@link #PREFERENCES preference} whether a
   * password request should be made or not.
   */
  public static final String PWD_REQUEST = "com.raspi.chatapp.util.Constants.PWD_REQUEST";
  /**
   * This string references to everything related to the last presence I
   * sent. This needs to be saved because every time I reconnect I want to
   * send my presence to "initiate" the connection. I don't really get why
   * this should be necessary but it is...
   */
  public static final String LAST_PRESENCE_SENT = "com.raspi.chatapp.util.Constants.LAST_PRESENCE_SENT";
  /**
   * references to something to do with pressing back
   */
  public static final String PRESSED_BACK = "com.raspi.chatapp.util.Constants.PRESSED_BACK";
  /**
   * This string references to the file name of the wallpaper image.
   */
  public static final String WALLPAPER_NAME = "wallpaper.jpg";
  /**
   * This string references to the directory name the images I save are
   * stored. (Not the full path, only the directory!)
   */
  public static final String IMAGE_DIR = "ChatApp Images";

  public static int dipToPixel(Context context, int dip){
    final float scale = context.getResources().getDisplayMetrics().density;
    return (int) (dip * scale + 0.5f);
  }
}
