package com.raspi.chatapp.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.TextPaint;
import android.util.Log;
import android.util.TypedValue;

import com.raspi.chatapp.R;
import com.raspi.chatapp.ui.chatting.ChatActivity;
import com.raspi.chatapp.util.storage.MessageHistory;

import org.json.JSONArray;

import java.util.Arrays;
import java.util.Random;

public class Notification{
  public static final int NOTIFICATION_ID = 42;
  public static final String NOTIFICATION_CLICK = "com.raspi.chatapp.util.Notification" +
          ".NOTIFICATION_CLICK";
  public static final String NOTIFICATION_OLD_BUDDY = "com.raspi.chatapp.util.Notification" +
          ".NOTIFICATION_OLD_BUDDY";
  public static final String CURRENT_NOTIFICATIONS = "com.raspi.chatapp.util.Notification" +
          ".CURRENT_NOTIFICATIONS";

  Context context;

  public Notification(Context context){
    this.context = context;
  }

  public void createNotification(String buddyId, String name, String message,
                                 String type){
    SharedPreferences defaultSharedPreferences = PreferenceManager
            .getDefaultSharedPreferences(context);

    if (defaultSharedPreferences.getBoolean(context.getResources().getString(R.string
            .pref_key_new_message_notifications), true)){
      int index = buddyId.indexOf("@");
      if (index != -1)
        buddyId = buddyId.substring(0, index);
      if (name == null)
        name = buddyId;
      Log.d("DEBUG", "creating notification: " + buddyId + "|" + name + "|" + message);
      Intent resultIntent = new Intent(context, ChatActivity.class);
      resultIntent.setAction(NOTIFICATION_CLICK);
      String oldBuddyId = getOldBuddyId();
      Log.d("DEBUG", (oldBuddyId == null) ? ("oldBuddy is null (later " + buddyId) : ("oldBuddy: " +
              oldBuddyId));
      if (oldBuddyId == null || oldBuddyId.equals("")){
        oldBuddyId = buddyId;
        setOldBuddyId(buddyId);
      }
      if (oldBuddyId.equals(buddyId)){
        resultIntent.putExtra(Constants.BUDDY_ID, buddyId);
        resultIntent.putExtra(Constants.CHAT_NAME, name);
      }

      TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
      stackBuilder.addParentStack(ChatActivity.class);
      stackBuilder.addNextIntent(resultIntent);
      PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0,
              PendingIntent.FLAG_UPDATE_CURRENT);

      NotificationManager nm = ((NotificationManager) context.getSystemService(Context
              .NOTIFICATION_SERVICE));
      NotificationCompat.Style style;

      String[] previousNotifications = readJSONArray(CURRENT_NOTIFICATIONS);
      String title;
      String[] currentNotifications = Arrays.copyOf(previousNotifications,
              previousNotifications.length + 1);
      currentNotifications[currentNotifications.length - 1] = name + ": " +
              message;
      if (previousNotifications.length == 0){
        style = new NotificationCompat.BigTextStyle();
        NotificationCompat.BigTextStyle bigTextStyle = ((NotificationCompat
                .BigTextStyle) style);
        title = context.getResources().getString(R.string.new_message) + " " +
                context.getResources().getString(R.string.from) + " " + name;
        bigTextStyle.bigText
                (currentNotifications[0]);
        bigTextStyle.setBigContentTitle(title);
      }else{
        style = new NotificationCompat.InboxStyle();
        NotificationCompat.InboxStyle inboxStyle = (NotificationCompat
                .InboxStyle) style;
        title = (previousNotifications.length + 1) + " " + context
                .getResources().getString(R.string.new_messages);
        for (String s : currentNotifications)
          if (s != null && !"".equals(s))
            inboxStyle.addLine(s);
        inboxStyle.setSummaryText((currentNotifications.length > 2) ? ("+" +
                (currentNotifications.length - 2) + " more") : null);
        inboxStyle.setBigContentTitle(title);
      }
      writeJSONArray(currentNotifications, CURRENT_NOTIFICATIONS);

      NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
              .setContentTitle(title)
              .setContentText(currentNotifications[currentNotifications
                      .length - 1])
              .setSmallIcon(MessageHistory.TYPE_TEXT.equals(type)
                      ? R.drawable.ic_forum_white_48dp
                      : R.drawable.ic_photo_camera_white_48dp)
              .setLargeIcon(getLargeIcon(Character.toUpperCase(name
                      .toCharArray()[0])))
              .setStyle(style)
              .setAutoCancel(true)
              .setPriority(5)
              .setContentIntent(resultPendingIntent);

      String str = context.getResources().getString(R.string.pref_key_privacy);
      mBuilder.setVisibility(context.getResources().getString(R.string
              .pref_value1_privacy).equals(str)
              ? NotificationCompat.VISIBILITY_SECRET
              : context.getResources().getString(R.string.pref_value2_privacy)
              .equals(str)
              ? NotificationCompat.VISIBILITY_PRIVATE
              : NotificationCompat.VISIBILITY_PUBLIC);

      str = context.getResources().getString(R.string.pref_key_vibrate);
      if (defaultSharedPreferences.getBoolean(str, true))
        mBuilder.setVibrate(new long[]{800, 500, 800, 500});

      str = context.getResources().getString(R.string.pref_key_led);
      if (defaultSharedPreferences.getBoolean(str, true))
        mBuilder.setLights(Color.BLUE, 500, 500);

      str = defaultSharedPreferences.getString(context.getResources().getString(R
              .string.pref_key_ringtone), "");
      mBuilder.setSound("".equals(str) ? RingtoneManager
              .getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) : Uri
              .parse(str));

      nm.notify(NOTIFICATION_ID, mBuilder.build());
      str = context.getResources().getString(R.string.pref_key_banner);
      if (!defaultSharedPreferences.getBoolean(str, true)){
        try{
          Thread.sleep(1500);
        }catch (InterruptedException e){
        }
        reset();
      }
    }
  }

  private Bitmap getLargeIcon(char letter){
    Random random = new Random();
    int widthDP = 64;
    int bgColor = Color.rgb(random.nextInt(256), random.nextInt(256), random
            .nextInt(256));
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
      return getLargeIcon(bgColor, letter, TypedValue.applyDimension
              (TypedValue.COMPLEX_UNIT_DIP, widthDP, context.getResources()
                      .getDisplayMetrics()), true);
    else
      return getLargeIcon(bgColor, letter, TypedValue.applyDimension
              (TypedValue.COMPLEX_UNIT_DIP, widthDP, context.getResources()
                      .getDisplayMetrics()), false);
  }

  private Bitmap getLargeIcon(int bgColor, char letter, float
          width, boolean round){
    Bitmap b = Bitmap.createBitmap((int) width, (int) width, Bitmap.Config
            .ARGB_8888);
    Canvas c = new Canvas(b);

    RectF mInnerRectF = new RectF();
    mInnerRectF.set(0, 0, width, width);
    mInnerRectF.offset(0, 0);

    Paint mBgPaint = new Paint();
    mBgPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
    mBgPaint.setStyle(Paint.Style.FILL);
    mBgPaint.setColor(bgColor);

    TextPaint mTitleTextPaint = new TextPaint();
    mTitleTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
    mTitleTextPaint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
    mTitleTextPaint.setTextAlign(Paint.Align.CENTER);
    mTitleTextPaint.setLinearText(true);
    mTitleTextPaint.setColor(Color.WHITE);
    mTitleTextPaint.setTextSize(width * 0.8f);

    float centerX = mInnerRectF.centerX();
    float centerY = mInnerRectF.centerY();

    int xPos = (int) centerX;
    int yPos = (int) (centerY - (mTitleTextPaint.descent() + mTitleTextPaint
            .ascent()) / 2);

    if (round)
      c.drawOval(mInnerRectF, mBgPaint);
    else
      c.drawRect(mInnerRectF, mBgPaint);
    c.drawText(String.valueOf(letter), xPos, yPos, mTitleTextPaint);

    return b;
  }

  public void reset(){
    SharedPreferences preferences = context.getSharedPreferences(Constants.PREFERENCES, 0);
    preferences.edit().putString(NOTIFICATION_OLD_BUDDY, "").putString(CURRENT_NOTIFICATIONS,
            "").apply();
    ((NotificationManager) context.getSystemService(Context
            .NOTIFICATION_SERVICE)).cancel(Notification.NOTIFICATION_ID);
  }

  private String getOldBuddyId(){
    SharedPreferences preferences = context.getSharedPreferences(Constants.PREFERENCES, 0);
    return preferences.getString(NOTIFICATION_OLD_BUDDY, null);
  }

  private void setOldBuddyId(String buddyId){
    SharedPreferences preferences = context.getSharedPreferences(Constants.PREFERENCES, 0);
    preferences.edit().putString(NOTIFICATION_OLD_BUDDY, buddyId).apply();
  }

  private void writeJSONArray(String[] arr, String arr_name){
    SharedPreferences preferences = context.getSharedPreferences(Constants.PREFERENCES, 0);
    JSONArray jsonArray = new JSONArray();
    for (String s : arr)
      jsonArray.put(s);
    preferences.edit().putString(arr_name, jsonArray.toString()).apply();
  }

  private String[] readJSONArray(String arr_name){
    SharedPreferences preferences = context.getSharedPreferences(Constants.PREFERENCES, 0);
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
