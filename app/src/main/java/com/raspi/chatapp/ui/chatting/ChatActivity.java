package com.raspi.chatapp.ui.chatting;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.raspi.chatapp.R;
import com.raspi.chatapp.ui.AddChatActivity;
import com.raspi.chatapp.ui.SettingsActivity;
import com.raspi.chatapp.util.service.MessageService;
import com.raspi.chatapp.util.sqlite.AndroidDatabaseManager;
import com.raspi.chatapp.util.Notification;

public class ChatActivity extends AppCompatActivity implements
        FragmentManager.OnBackStackChangedListener, ChatListFragment
        .OnFragmentInteractionListener, ChatFragment
        .OnFragmentInteractionListener, SendImageFragment.OnFragmentInteractionListener{

  public static final String PREFERENCES = "com.raspi.chatapp.ui" +
          ".ChatActivity.PREFERENCES";
  public static final String USERNAME = "com.raspi.chatapp.ui" +
          ".ChatActivity.USERNAME";
  public static final String PASSWORD = "com.raspi.chatapp.ui" +
          ".ChatActivity.PASSWORD";
  public static final String RECONNECT = "com.raspi.chatapp.ui" +
          ".ChatActivity.RECONNECT";
  public static final String APP_LAUNCHED = "con.raspi.chatapp.ui" +
          ".ChatActivity.APP_CREATED";
  public static final String APP_CLOSED = "con.raspi.chatapp.ui" +
          ".ChatActivity.APP_DESTROYED";
  public static final String BUDDY_ID = "com.raspi.chatapp.ui" +
          ".ChatActivity.BUDDY_ID";
  public static final String CHAT_NAME = "com.raspi.chatapp.ui" +
          ".ChatActivity.CHAT_NAME";
  public static final String MESSAGE_BODY = "com.raspi.chatapp.ui" +
          ".ChatActivity.MESSAGE_BODY";
  public static final String PRESENCE_CHANGED = "com.raspi.chatapp.ui" +
          ".ChatActivity.PRESENCE_CHANGED";
  public static final String PRESENCE_STATUS = "com.raspi.chatapp.ui" +
          ".ChatActivity.PRESENCE_STATUS";
  public static final String RECEIVE_MESSAGE = "com.raspi.chatapp.ui" +
          ".ChatActivity.RECEIVE_MESSAGE";
  public static final String CONN_ESTABLISHED = "com.raspi.chatapp.ui" +
          ".ChatActivity.CONN_ESTABLISHED";
  public static final String IMAGE_URI = "com.raspi.chatapp.ui" +
          ".ChatActivity.IMAGE_URI";

  public static final String IMAGE_DIR = "ChatApp Images";

  public static final String BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";

  public static final int PHOTO_ATTACH_SELECTED = 42;

  public String currentBuddyId = ChatActivity.BUDDY_ID;

  @Override
  protected void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    getSupportFragmentManager().addOnBackStackChangedListener(this);
    shouldDisplayHomeUp();

    setUserPwd();

    ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel
            (Notification.NOTIFICATION_ID);
    new Notification(this).reset();

    if (savedInstanceState == null)
      getSupportFragmentManager().beginTransaction().add(R.id
              .fragment_container, new ChatListFragment()).commit();

    Intent callingIntent = getIntent();
    if (callingIntent != null && Notification.NOTIFICATION_CLICK.equals
            (callingIntent.getAction())){
      Log.d("DEBUG", "received intend not click");
      Bundle extras = callingIntent.getExtras();
      if (extras != null && extras.containsKey(ChatActivity.BUDDY_ID) && extras
              .containsKey(ChatActivity.CHAT_NAME)){
        onChatOpened(extras.getString(ChatActivity.BUDDY_ID), extras
                .getString(ChatActivity.CHAT_NAME));
      }
    }
  }

  @Override
  protected void onResume(){
    super.onResume();
    this.startService(new Intent(this, MessageService.class).setAction(APP_LAUNCHED));
    new Notification(this).reset();
  }

  @Override
  protected void onPause(){
    this.startService(new Intent(this, MessageService.class).setAction(APP_CLOSED));
    super.onPause();
  }

  @Override
  protected void onDestroy(){
    //signal the service that the app is about to get destroyed
    this.startService(new Intent(this, MessageService.class).setAction(APP_CLOSED));
    super.onDestroy();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu){
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_chat_list, menu);
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
  }

  public void onAddChatClick(MenuItem menuItem){
    Intent intent = new Intent(this, AddChatActivity.class);
    startActivity(intent);
  }

  public void onDatabaseDebug(MenuItem menuItem){
    Intent intent = new Intent(this, AndroidDatabaseManager.class);
    startActivity(intent);
  }

  private void setUserPwd(){
    SharedPreferences preferences = getSharedPreferences(PREFERENCES, 0);
    //if (!preferences.contains(USERNAME))
    preferences.edit().putString(USERNAME, "aylin").apply();

    //if (!preferences.contains(PASSWORD))
    preferences.edit().putString(PASSWORD, "passwdAylin").apply();
  }

  @Override
  public void onChatOpened(String buddyId, String name){
    ChatFragment fragment = new ChatFragment();
    Bundle extras = new Bundle();
    extras.putString(ChatActivity.BUDDY_ID, buddyId);
    extras.putString(ChatActivity.CHAT_NAME, name);
    fragment.setArguments(extras);
    getSupportFragmentManager().beginTransaction().replace(R.id
            .fragment_container, fragment).addToBackStack(ChatFragment.class.getName())
            .commit();
    currentBuddyId = buddyId;
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
      fragment.setArguments(extras);
      getSupportFragmentManager().beginTransaction().replace(R.id
              .fragment_container, fragment).addToBackStack(SendImageFragment
              .class.getName()).commit();
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
  }

  @Override
  public void onBackStackChanged(){
    shouldDisplayHomeUp();
  }

  public void shouldDisplayHomeUp(){
    boolean canBack = getSupportFragmentManager().getBackStackEntryCount() > 0;
    getSupportActionBar().setDisplayHomeAsUpEnabled(canBack);
    getSupportActionBar().setHomeButtonEnabled(canBack);
    if (!canBack) currentBuddyId = ChatActivity.BUDDY_ID;
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

  //receiving boot intents
  public static class BootReceiver extends BroadcastReceiver{

    public BootReceiver(){
    }

    @Override
    public void onReceive(Context context, Intent intent){
      if (intent != null && intent.getAction() != null){
        if (intent.getAction().equals(BOOT_COMPLETED)){
          context.startService(new Intent(context, MessageService.class));
        }
      }
    }
  }
}
