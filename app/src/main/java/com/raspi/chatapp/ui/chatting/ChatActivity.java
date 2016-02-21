package com.raspi.chatapp.ui.chatting;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.alexbbb.uploadservice.UploadService;
import com.raspi.chatapp.BuildConfig;
import com.raspi.chatapp.R;
import com.raspi.chatapp.ui.password.PasswordActivity;
import com.raspi.chatapp.ui.settings.SettingsActivity;
import com.raspi.chatapp.util.Notification;
import com.raspi.chatapp.util.internet.XmppManager;
import com.raspi.chatapp.util.service.MessageService;
import com.raspi.chatapp.util.storage.AndroidDatabaseManager;
import com.raspi.chatapp.util.storage.MessageHistory;

import org.jivesoftware.smack.roster.RosterEntry;

import java.util.Date;

public class ChatActivity extends AppCompatActivity implements
        FragmentManager.OnBackStackChangedListener, ChatListFragment
        .OnFragmentInteractionListener, ChatFragment
        .OnFragmentInteractionListener, SendImageFragment.OnFragmentInteractionListener{

  public static final String PREFERENCES = "com.raspi.chatapp.ui.chatting" +
          ".ChatActivity.PREFERENCES";
  public static final String USERNAME = "com.raspi.chatapp.ui.chatting" +
          ".ChatActivity.USERNAME";
  public static final String PASSWORD = "com.raspi.chatapp.ui.chatting" +
          ".ChatActivity.PASSWORD";
  public static final String BUDDY_ID = "com.raspi.chatapp.ui.chatting" +
          ".ChatActivity.BUDDY_ID";
  public static final String CHAT_NAME = "com.raspi.chatapp.ui.chatting" +
          ".ChatActivity.CHAT_NAME";
  public static final String MESSAGE_BODY = "com.raspi.chatapp.ui.chatting" +
          ".ChatActivity.MESSAGE_BODY";
  public static final String PRESENCE_CHANGED = "com.raspi.chatapp.ui.chatting" +
          ".ChatActivity.PRESENCE_CHANGED";
  public static final String PRESENCE_STATUS = "com.raspi.chatapp.ui.chatting" +
          ".ChatActivity.PRESENCE_STATUS";
  public static final String RECEIVE_MESSAGE = "com.raspi.chatapp.ui.chatting" +
          ".ChatActivity.RECEIVE_MESSAGE";
  public static final String RECONNECTED = "com.raspi.chatapp.ui.chatting" +
          ".ChatActivity.RECONNECTED";
  public static final String MESSAGE_TYPE = "com.raspi.chatapp.ui.chatting" +
          ".ChatActivity.MESSAGE_TYPE";
  public static final String IMAGE_URI = "com.raspi.chatapp.ui.chatting" +
          ".ChatActivity.IMAGE_URI";
  public static final String PWD_REQUEST = "com.raspi.chatapp.ui.chatting" +
          ".ChatActivity.PWD_REQUEST";
  public static final String MESSAGE_STATUS_CHANGED = "com.raspi.chatapp.ui.chatting" +
          ".ChatActivity.MESSAGE_STATUS_CHANGED";
  public static final String LAST_SENT_PRESENCE = "com.raspi.chatapp.ui.chatting" +
          ".ChatActivity.LAST_SENT_PRESENCE";
  public static final String WALLPAPER_NAME = "wallpaper.jpg";

  public static final String IMAGE_DIR = "ChatApp Images";

  public static final int PHOTO_ATTACH_SELECTED = 42;

  public String currentBuddyId = ChatActivity.BUDDY_ID;
  public String currentChatName = ChatActivity.CHAT_NAME;

  @Override
  protected void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    UploadService.NAMESPACE = BuildConfig.APPLICATION_ID;

    setContentView(R.layout.activity_chat);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    getSupportFragmentManager().addOnBackStackChangedListener(this);
    shouldDisplayHomeUp();

    setUserPwd();

    Intent callingIntent = getIntent();
    if (callingIntent != null){
      Bundle extras = callingIntent.getExtras();
      if (extras != null && extras.containsKey(ChatActivity.BUDDY_ID) && extras
              .containsKey(ChatActivity.CHAT_NAME)){
        currentBuddyId = extras.getString(ChatActivity.BUDDY_ID);
        currentChatName = extras.getString(ChatActivity.CHAT_NAME);
      }
    }
  }

  @Override
  protected void onResume(){
    super.onResume();
    startService(new Intent(getApplicationContext(), MessageService.class));
    if (getSharedPreferences(ChatActivity.PREFERENCES, 0)
            .getBoolean(ChatActivity.PWD_REQUEST, true)
            && PreferenceManager.getDefaultSharedPreferences(getApplication())
            .getBoolean(
                    getResources().getString(R.string.pref_key_enablepwd),
                    false)){
      startActivityForResult(new Intent(this, PasswordActivity.class),
              PasswordActivity.ASK_PWD_REQUEST);
    }else{
      init();
    }
    XmppManager.getInstance(getApplicationContext()).setStatus(true, "0");
    getSharedPreferences(ChatActivity.PREFERENCES, 0).edit().putLong
            (ChatActivity.LAST_SENT_PRESENCE, 0).apply();
    getSharedPreferences(ChatActivity.PREFERENCES, 0).edit().putBoolean
            (ChatActivity.PWD_REQUEST, true).apply();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig){
    super.onConfigurationChanged(newConfig);
    getSharedPreferences(ChatActivity.PREFERENCES, 0).edit().putBoolean
            (ChatActivity.PWD_REQUEST, false).apply();
  }

  @Override
  protected void onPause(){
    startService(new Intent(getApplicationContext(), MessageService.class));
    Long time = new Date().getTime();
    XmppManager.getInstance().setStatus(true, Long.toString(time));
    getSharedPreferences(ChatActivity.PREFERENCES, 0).edit().putLong
            (ChatActivity.LAST_SENT_PRESENCE, time).apply();
    super.onPause();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu){
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item){
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings){
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  public void onSettingsClick(MenuItem menuItem){
    Intent intent = new Intent(this, SettingsActivity.class);
    startActivity(intent);
    getSharedPreferences(ChatActivity.PREFERENCES, 0).edit().putBoolean
            (ChatActivity.PWD_REQUEST, false).apply();
  }

  public void onAddChatClick(final MenuItem menuItem){
    String title = getResources().getString(R.string.add_chat_title);
    XmppManager xmppManager = XmppManager.getInstance();
    final RosterEntry[] rosterList = xmppManager.listRoster();
    final String[] nameList = new String[rosterList.length];
    for (int i=0;i<rosterList.length;i++){
      nameList[i] = rosterList[i].getName();
      if (nameList[i] == null){
        nameList[i] = rosterList[i].getUser();
        int index = nameList[i].indexOf('@');
        if (index >= 0)
          nameList[i] = nameList[i].substring(0, index);
      }
    }
    new AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(nameList, new DialogInterface.OnClickListener(){
              @Override
              public void onClick(DialogInterface dialog, int which){
                MessageHistory messageHistory = new MessageHistory
                        (ChatActivity.this);
                messageHistory.addChat(rosterList[which].getUser(),
                        nameList[which]);
                onChatOpened(rosterList[which].getUser(), messageHistory
                        .getName(rosterList[which].getUser()swit));
              }
            }).show();
  }

  public void onDatabaseDebug(MenuItem menuItem){
    Intent intent = new Intent(this, AndroidDatabaseManager.class);
    startActivity(intent);
    getSharedPreferences(ChatActivity.PREFERENCES, 0).edit().putBoolean
            (ChatActivity.PWD_REQUEST, false).apply();
  }

  private void setUserPwd(){
    SharedPreferences preferences = getSharedPreferences(PREFERENCES, 0);
    if (!preferences.contains(USERNAME))
      preferences.edit().putString(USERNAME, "niklas").apply();

    if (!preferences.contains(PASSWORD))
      preferences.edit().putString(PASSWORD, "passwdNiklas").apply();
  }

  @Override
  public void onChatOpened(String buddyId, String name){
    ChatFragment fragment = new ChatFragment();
    Bundle extras = new Bundle();
    extras.putString(ChatActivity.BUDDY_ID, buddyId);
    extras.putString(ChatActivity.CHAT_NAME, name);
    fragment.setArguments(extras);
    getSupportFragmentManager().beginTransaction().replace(R.id
            .fragment_container, fragment).addToBackStack(ChatFragment.class
            .getName()).commit();
    currentBuddyId = buddyId;
    currentChatName = name;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data){
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == ChatActivity.PHOTO_ATTACH_SELECTED && resultCode ==
            Activity.RESULT_OK){
      SendImageFragment fragment = new SendImageFragment();
      Bundle extras = new Bundle();
      extras.putString(ChatActivity.IMAGE_URI, data.getData().toString());
      extras.putString(ChatActivity.BUDDY_ID, currentBuddyId);
      extras.putString(ChatActivity.CHAT_NAME, currentChatName);
      fragment.setArguments(extras);
      getSupportFragmentManager().beginTransaction().replace(R.id
              .fragment_container, fragment).addToBackStack(SendImageFragment
              .class.getName()).commit();
    }else if (requestCode == PasswordActivity.ASK_PWD_REQUEST){
      getSharedPreferences(ChatActivity.PREFERENCES, 0).edit().putBoolean
              (ChatActivity.PWD_REQUEST, false).apply();
      if (resultCode == Activity.RESULT_OK){
        init();
      }else{
        if (PreferenceManager.getDefaultSharedPreferences(getApplication())
                .getBoolean(
                        getResources().getString(R.string.pref_key_enablepwd),
                        true))
          startActivityForResult(new Intent(this, PasswordActivity.class),
                  PasswordActivity.ASK_PWD_REQUEST);
      }
    }
  }

  @Override
  public void onAttachClicked(){
    Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
    getIntent.setType("image/*");

    Intent pickIntent = new Intent(Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    pickIntent.setType("image/*");

    Intent chooserIntent = Intent.createChooser(getIntent, getResources()
            .getString(R.string.select_image));
    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new
            Intent[]{pickIntent});
    startActivityForResult(chooserIntent, ChatActivity.PHOTO_ATTACH_SELECTED);
    getSharedPreferences(ChatActivity.PREFERENCES, 0).edit().putBoolean
            (ChatActivity.PWD_REQUEST, false).apply();
  }

  @Override
  public void onBackStackChanged(){
    shouldDisplayHomeUp();
  }

  public void shouldDisplayHomeUp(){
    boolean canBack = getSupportFragmentManager().getBackStackEntryCount() > 0;
    getSupportActionBar().setDisplayHomeAsUpEnabled(canBack);
    getSupportActionBar().setHomeButtonEnabled(canBack);
    if (!canBack){
      currentBuddyId = ChatActivity.BUDDY_ID;
      currentChatName = ChatActivity.CHAT_NAME;
    }
  }

  private void init(){
    if (ChatActivity.BUDDY_ID.equals(currentBuddyId))
      getSupportFragmentManager().beginTransaction().replace(R.id
              .fragment_container, new ChatListFragment()).commit();
    else if (getSupportFragmentManager().getFragments() == null){
      //for propagating the backstack...
      getSupportFragmentManager().beginTransaction().replace(R.id
              .fragment_container, new ChatListFragment()).commit();
      onChatOpened(currentBuddyId, currentChatName);
    }

    ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel
            (Notification.NOTIFICATION_ID);
    new Notification(this).reset();
  }

  @Override
  public boolean onSupportNavigateUp(){
    getSupportFragmentManager().popBackStack();
    return true;
  }

  @Override
  public void onReturnClick(){
    getSupportFragmentManager().popBackStack();
  }
}
