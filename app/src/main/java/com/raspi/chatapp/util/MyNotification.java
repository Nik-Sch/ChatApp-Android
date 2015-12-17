package com.raspi.chatapp.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.raspi.chatapp.R;
import com.raspi.chatapp.activities.MainActivity;
import com.raspi.chatapp.activities.ChatActivity;

import org.json.JSONArray;

import java.util.Arrays;

public class MyNotification{
  public static final int NOTIFICATION_ID = 42;
  public static final String NOTIFICATION_CLICK = "com.raspi.chatapp.util.MyNotification" +
          ".NOTIFICATION_CLICK";
  public static final String NOTIFICATION_OLD_BUDDY = "com.raspi.chatapp.util.MyNotification" +
          ".NOTIFICATION_OLD_BUDDY";
  public static final String CURRENT_NOTIFICATIONS = "com.raspi.chatapp.util.MyNotification" +
          ".CURRENT_NOTIFICATIONS";

  Context context;

  public MyNotification(Context context){
    this.context = context;
  }

  public void createNotification(String buddyId, String name, String message){
    int index = buddyId.indexOf("@");
    if (index != -1)
      buddyId = buddyId.substring(0, index);
    if (name == null)
      name = buddyId;
    Log.d("DEBUG", "creating notification: " + buddyId + "|" + name + "|" + message);
    Intent resultIntent = new Intent(context, MainActivity.class);
    resultIntent.setAction(NOTIFICATION_CLICK);
    String oldBuddyId = getOldBuddyId();
    Log.d("DEBUG", (oldBuddyId == null) ? ("oldBuddy is null (later " + buddyId) : ("oldBuddy: " +
            oldBuddyId));
    if (oldBuddyId == null || oldBuddyId.equals("")){
      oldBuddyId = buddyId;
      setOldBuddyId(buddyId);
    }
    if (oldBuddyId.equals(buddyId)){
      resultIntent.putExtra(MainActivity.BUDDY_ID, buddyId);
      resultIntent.putExtra(MainActivity.CHAT_NAME, name);
    }

    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
    stackBuilder.addParentStack(ChatActivity.class);
    stackBuilder.addNextIntent(resultIntent);
    PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
            PendingIntent.FLAG_UPDATE_CURRENT);

    NotificationManager nm = ((NotificationManager) context.getSystemService(Context
            .NOTIFICATION_SERVICE));
    NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

    String[] previousNotifications = readJSONArray(CURRENT_NOTIFICATIONS);
    String[] currentNotifications = Arrays.copyOf(previousNotifications,
            previousNotifications.length + 1);
    currentNotifications[currentNotifications.length - 1] = name + ": " + message;
    for (String s : currentNotifications)
      if (s != null && !"".equals(s))
        inboxStyle.addLine(s);
    inboxStyle.setSummaryText((currentNotifications.length > 2) ? ("+" + (currentNotifications
            .length - 2) + " more") : null);
    inboxStyle.setBigContentTitle((currentNotifications.length > 1) ? "New messages" : "New " +
            "message");
    writeJSONArray(currentNotifications, CURRENT_NOTIFICATIONS);


    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
            .setContentTitle("New Message")
            .setContentText(currentNotifications[currentNotifications
                    .length - 1])
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setStyle(inboxStyle)
            .setAutoCancel(true)
            .setVibrate(new long[]{500, 300, 500, 300})
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setLights(Color.BLUE, 500, 500)
            .setContentIntent(resultPendingIntent);

    nm.notify(NOTIFICATION_ID, mBuilder.build());
  }

  public void reset(){
    SharedPreferences preferences = context.getSharedPreferences(MainActivity.PREFERENCES, 0);
    preferences.edit().putString(NOTIFICATION_OLD_BUDDY, "").putString(CURRENT_NOTIFICATIONS,
            "").apply();
  }

  private String getOldBuddyId(){
    SharedPreferences preferences = context.getSharedPreferences(MainActivity.PREFERENCES, 0);
    return preferences.getString(NOTIFICATION_OLD_BUDDY, null);
  }

  private void setOldBuddyId(String buddyId){
    SharedPreferences preferences = context.getSharedPreferences(MainActivity.PREFERENCES, 0);
    preferences.edit().putString(NOTIFICATION_OLD_BUDDY, buddyId).apply();
  }

  private void writeJSONArray(String[] arr, String arr_name){
    SharedPreferences preferences = context.getSharedPreferences(MainActivity.PREFERENCES, 0);
    JSONArray jsonArray = new JSONArray();
    for (String s : arr)
      jsonArray.put(s);
    preferences.edit().putString(arr_name, jsonArray.toString()).apply();
  }

  private String[] readJSONArray(String arr_name){
    SharedPreferences preferences = context.getSharedPreferences(MainActivity.PREFERENCES, 0);
    try{
      JSONArray jsonArray = new JSONArray(preferences.getString(arr_name, ""));
      String[] result = new String[jsonArray.length()];
      for (int i = 0; i < result.length; i++)
        result[0] = jsonArray.getString(i);
      return result;
    }catch (Exception e){
      e.printStackTrace();
      return new String[]{};
    }
  }
}
