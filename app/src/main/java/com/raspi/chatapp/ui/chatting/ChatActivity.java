package com.raspi.chatapp.ui.chatting;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.alexbbb.uploadservice.UploadService;
import com.raspi.chatapp.BuildConfig;
import com.raspi.chatapp.R;
import com.raspi.chatapp.ui.password.PasswordActivity;
import com.raspi.chatapp.ui.settings.SettingsActivity;
import com.raspi.chatapp.util.Constants;
import com.raspi.chatapp.util.Notification;
import com.raspi.chatapp.util.internet.XmppManager;
import com.raspi.chatapp.util.service.MessageService;
import com.raspi.chatapp.util.storage.AndroidDatabaseManager;
import com.raspi.chatapp.util.storage.MessageHistory;
import com.raspi.chatapp.util.storage.file.MyFileUtils;

import org.jivesoftware.smack.roster.RosterEntry;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

/**
 * This activity is the main Activity which is being started and contains the
 * {@link ChatListFragment}, the {@link ChatFragment} and the {@link
 * SendImageFragment}.<br>
 * Here is the backstack managed and the callbacks from the fragments are
 * implemented.
 */
public class ChatActivity extends AppCompatActivity implements
        FragmentManager.OnBackStackChangedListener, ChatListFragment
        .OnFragmentInteractionListener, ChatFragment.OnChatFragmentInteractionListener, SendImageFragment.OnFragmentInteractionListener{

  /**
   * This constant references to the requestCode with which the activity is
   * started to return the image I want to send.
   */
  private static final int PHOTO_ATTACH_SELECTED = 42;

  /**
   * Here should the current buddy be stored. That means the buddyId of the
   * chat that is currently opened. If there is no open chat (e.g. I am in
   * the {@link ChatListFragment}) this should contain {@link
   * Constants#BUDDY_ID}.
   */
  private String currentBuddyId = Constants.BUDDY_ID;
  /**
   * Here should the current chatName be stored. That means the name of the
   * chat that is currently opened. If there is no open chat (e.g. I am in
   * the {@link ChatListFragment}) this should contain {@link
   * Constants#CHAT_NAME}.
   */
  private String currentChatName = Constants.CHAT_NAME;

  /**
   * if the user wants to share an image this imageUri is set in the onCreate.
   */
  private Uri imageUri = null;

  /**
   * this is a handler that might be assigned if needed. It may be used for
   * doing ui related tasks from a background thread
   */
  private Handler mHandler;


  @Override
  protected void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    // The namespace I am using in the UploadService is the app id
    UploadService.NAMESPACE = BuildConfig.APPLICATION_ID;

    // the layout contains just the toolbar and the FrameLayout which
    // contains every fragment
    setContentView(R.layout.activity_chat);
    // setting the actionbar
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    // manage the backstack, therefore, I got a function that checks whether I
    // can go back and sets the back button in the actionbar accordingly
    getSupportFragmentManager().addOnBackStackChangedListener(this);
    shouldDisplayHomeUp();

    // in case this is the first time the app launches or in case the user
    // deleted all application data set my buddyId and pwd I use to log into
    // the XMPP server
    setUserPwd();

    // if there is a calling intent and this contains a buddyId and chatName
    // do not open the chatFragmet instantly but only set the current
    // variables in order to be able to ask for a password if necessary, see
    // onResume.
    Intent callingIntent = getIntent();
    Bundle extras = callingIntent.getExtras();
    String type = callingIntent.getType();
    if (Intent.ACTION_SEND.equals(callingIntent.getAction()) && type != null){
      if (type.startsWith("image/")){
        imageUri = callingIntent.getParcelableExtra(Intent.EXTRA_STREAM);
      }
    }
    if (extras != null && extras.containsKey(Constants.BUDDY_ID) && extras
            .containsKey(Constants.CHAT_NAME)){
      currentBuddyId = extras.getString(Constants.BUDDY_ID);
      currentChatName = extras.getString(Constants.CHAT_NAME);
    }
  }

  @Override
  protected void onResume(){
    super.onResume();
    // start the messageService just in case it isn't already started or got
    // terminated (which should never happen as it boots with the system)
    startService(new Intent(getApplicationContext(), MessageService.class));
    //if the user wants a pwd protection and if we want to ask for a password
    // start the password activity for a result.
    if (PreferenceManager.getDefaultSharedPreferences(getApplication())
            .getBoolean(
                    getResources().getString(R.string.pref_key_enablepwd),
                    false) &&
            getSharedPreferences(Constants.PREFERENCES, 0)
                    .getBoolean(Constants.PWD_REQUEST, true)){
      startActivityForResult(new Intent(this, PasswordActivity.class),
              PasswordActivity.ASK_PWD_REQUEST);
    }else{
      // otherwise just initialize the activity which means opening the
      // correct fragment
      init();
    }
    XmppManager.getInstance(getApplicationContext()).setStatus(true, "0");
    // set the last presence I sent to online (0)
    getSharedPreferences(Constants.PREFERENCES, 0).edit().putLong
            (Constants.LAST_PRESENCE_SENT, 0).apply();
    // set the pwd request variable because we always want to ask for a pwd
    // if this activity gets resumed if it is not specified otherwise. That
    // means if something goes wrong we will ask for a pwd too much which is
    // better than to not ask for a pwd.
    getSharedPreferences(Constants.PREFERENCES, 0).edit().putBoolean
            (Constants.PWD_REQUEST, true).apply();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig){
    super.onConfigurationChanged(newConfig);
    // this method is actually called before onResume after I rotated the
    // device, therefore, this is for not asking for a pwd if I rotated.
    getSharedPreferences(Constants.PREFERENCES, 0).edit().putBoolean
            (Constants.PWD_REQUEST, false).apply();
  }

  @Override
  protected void onPause(){
    // for fun start the MessageService. Just making sure.
    startService(new Intent(getApplicationContext(), MessageService.class));
    // set my status to the current time as I am about to go offline.
    // Also set the last presence I sent to it.
    Long time = new Date().getTime();
    XmppManager.getInstance().setStatus(true, Long.toString(time));
    getSharedPreferences(Constants.PREFERENCES, 0).edit().putLong
            (Constants.LAST_PRESENCE_SENT, time).apply();
    super.onPause();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu){
    // "You must return true for the menu to be displayed;[...]"
    // thanks android :)
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item){
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    // noinspection SimplifiableIfStatement
    if (id == R.id.action_settings){
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  public void onSettingsClick(MenuItem menuItem){
    // clicking the settings entry in the optionsMenu will launch the
    // settingsActivity and also signal not to ask for a pwd the next time
    // someone asks for a pwd.
    // TODO: Yep for now if you close the app while in settings and the
    // relaunch it you won't be asked for a pwd... Maybe going to fix this
    Intent intent = new Intent(this, SettingsActivity.class);
    startActivity(intent);
    getSharedPreferences(Constants.PREFERENCES, 0).edit().putBoolean
            (Constants.PWD_REQUEST, false).apply();
  }

  public void onAddChatClick(final MenuItem menuItem){
    // this button in the optionsMenu will open a dialog containing every
    // buddy of your roster. That way you can contact each buddy even if you
    // never had contact with him.
    String title = getResources().getString(R.string.add_chat_title);
    // retrieving the roster and saving it in one rosterEntry[] for the full
    // roster and in one nameList[]. This is seperate in order to ensure
    // there is a name. If there is no name saved in the roster I will take
    // the buddyId but without the server and resource part.
    XmppManager xmppManager = XmppManager.getInstance();
    final RosterEntry[] rosterList = xmppManager.listRoster();
    final String[] nameList = new String[rosterList.length];
    for (int i = 0; i < rosterList.length; i++){
      nameList[i] = rosterList[i].getName();
      if (nameList[i] == null){
        nameList[i] = rosterList[i].getUser();
        int index = nameList[i].indexOf('@');
        if (index >= 0)
          nameList[i] = nameList[i].substring(0, index);
      }
    }
    // pretty straight forward setting the title and items and then showing it
    new AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(nameList, new DialogInterface.OnClickListener(){
              @Override
              public void onClick(DialogInterface dialog, int which){
                // clicking on one item will open the chatFragment with the
                // correct buddyId and name. First I save the buddy in the db
                // and afterwards I retrieve the buddy of the db, because if
                // the buddy already exists I want to show the name I
                // previously saved in the db and not the name of the roster
                // as these are not synced.
                MessageHistory messageHistory = new MessageHistory
                        (ChatActivity.this);
                messageHistory.addChat(rosterList[which].getUser(),
                        nameList[which]);
                onChatOpened(rosterList[which].getUser(), messageHistory
                        .getName(rosterList[which].getUser()));
              }
            }).show();
  }

  public void onUpdateClick(MenuItem menuItem){
    // save a handler for the background thread be able to do ui operations
    mHandler = new Handler();
    // start the update process
    new Thread(new updateRunnable()).start();
  }

  public void onAboutClick(MenuItem menuItem){
    // will show the user the versionName with a button to dismiss the dialog
    try{
      String versionName = getPackageManager().getPackageInfo(getPackageName(),
              0).versionName;
      new AlertDialog.Builder(this)
              .setTitle(R.string.action_about)
              .setMessage(versionName)
              .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialog, int which){
                  dialog.dismiss();
                }
              })
              .create().show();
    }catch (Exception e){
      e.printStackTrace();
    }
  }

  /**
   * this runnable will check for an update and if there is an update
   * available ask the user to download it and then start the download
   */
  private class updateRunnable implements Runnable{
    @Override
    public void run(){
      // this site will post the most current apk of the app
      final String getCurrentUrl = "http://raspi-server.ddns" +
              ".net/ChatApp/current.php";
      HttpURLConnection connection = null;
      try{
        // init the connection to the server
        URL url = new URL(getCurrentUrl);
        connection = (HttpURLConnection) url.openConnection();
        InputStream is = connection.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        int data = isr.read();
        String temp = "";
        // get the data char by char
        while (data != -1){
          char current = (char) data;
          temp += current;
          data = isr.read();
        }
        // extract the versionCode (delete the '.apk' at the end)
        final String result = temp.substring(0, temp.length() - 4);
        // check whether the version is more current than the one currently
        // installed one
        int version = getPackageManager().getPackageInfo(getPackageName(),
                0).versionCode;
        int targetVersion = Integer.valueOf(result);
        if (targetVersion > version){
          // if that is the case post a dialog to the ui thread to be shown
          // to ask the user whether he want's to download the app now
          mHandler.post(new Runnable(){
            @Override
            public void run(){
              new AlertDialog.Builder(ChatActivity.this)
                      .setTitle(R.string.update_available)
                      .setMessage(R.string.load_update)
                      .setPositiveButton(R.string.download, new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialog, int which){
                          // if he clicks yes start the download
                          downloadUpdate(result);
                          dialog.dismiss();
                        }
                      })
                      .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialog, int which){
                          // otherwise just dismiss the dialog
                          dialog.dismiss();
                        }
                      }).create().show();
            }
          });
        }else{
          mHandler.post(new Runnable(){
            @Override
            public void run(){
              // just post a dialog to the ui to indicate that the app is
              // up-to-date
              new AlertDialog.Builder(ChatActivity.this)
                      .setTitle(R.string.up_to_date)
                      .setNegativeButton(R.string.ok, new DialogInterface
                              .OnClickListener(){
                        @Override
                        public void onClick(DialogInterface dialog, int which){
                          dialog.dismiss();
                        }
                      }).create().show();
            }
          });
        }
      }catch (Exception e){
        e.printStackTrace();
        mHandler.post(new Runnable(){
          @Override
          public void run(){
            // if there was an error signal so.
            new AlertDialog.Builder(ChatActivity.this)
                    .setMessage(R.string.check_update_error)
                    .setNegativeButton(R.string.ok, new DialogInterface
                            .OnClickListener(){
                      @Override
                      public void onClick(DialogInterface dialog, int which){
                        dialog.dismiss();
                      }
                    }).create().show();
          }
        });
      }finally{
        // make sure to disconnect
        if (connection != null)
          connection.disconnect();
      }
    }
  }

  /**
   * will start the asyncTask to download the update
   *
   * @param version the version to download
   */
  private void downloadUpdate(String version){
    MyFileUtils mfu = new MyFileUtils();
    // if we have access to the external storage
    if (mfu.isExternalStorageWritable()){
      // get the default download location and execute the asyncTask
      UpdateAppAsyncTask asyncTask = new UpdateAppAsyncTask();
      File file = new File(Environment.getExternalStoragePublicDirectory
              (Environment.DIRECTORY_DOWNLOADS), version + ".apk");
      asyncTask.execute(new String[]{version + ".apk", file.getAbsolutePath()});
    }else{
      new AlertDialog.Builder(ChatActivity.this)
              .setTitle(R.string.download_failed)
              .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialog, int which){
                  dialog.dismiss();
                }
              }).show();
    }
  }

  /**
   * this AsyncTask will download the most current version of the app in the
   * background while displaying a ProgressDialog and after finishing
   * downloading it will install the app.
   */
  private class UpdateAppAsyncTask extends AsyncTask<String[], Integer,
          Boolean>{
    private ProgressDialog updateDownloadProgressDialog;
    private String fileLocation = "";

    @Override
    protected void onPreExecute(){
      super.onPreExecute();
      // initialize the progressDialog
      updateDownloadProgressDialog = new ProgressDialog(ChatActivity.this);
      updateDownloadProgressDialog.setMessage(getResources().getString(R.string
              .downloading_update));
      updateDownloadProgressDialog.setCancelable(false);
      updateDownloadProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
      updateDownloadProgressDialog.setProgress(0);
      updateDownloadProgressDialog.setButton(
              DialogInterface.BUTTON_NEGATIVE,
              getResources().getString(R.string.cancel),
              new DialogInterface.OnClickListener(){
                @Override
                public void onClick(DialogInterface dialog, int which){
                  // the cancel button will cancel the asyncTask and dismiss
                  // the progressDialog
                  UpdateAppAsyncTask.this.cancel(true);
                  updateDownloadProgressDialog.dismiss();
                }
              });
      updateDownloadProgressDialog.show();
    }

    @Override
    protected Boolean doInBackground(String[]... params){
      // the params contains the fileName to be downloaded and the location
      // where the file should be downloaded to.
      String urlToDownload = "http://raspi-server.ddns.net/ChatApp/binary/" +
              params[0][0];
      String fileLocation = params[0][1];
      this.fileLocation = fileLocation;
      try{
        // initialize the connection and input/outputStreams
        URL url = new URL(urlToDownload);
        URLConnection connection = url.openConnection();
        int fileLength = connection.getContentLength();
        InputStream input = new BufferedInputStream(connection.getInputStream());
        OutputStream output = new FileOutputStream(fileLocation);

        // start the download and every 20 ms publish a progress
        byte data[] = new byte[4096];
        long total = 0;
        int count;
        long start = new Date().getTime();
        while (!isCancelled() && (count = input.read(data)) != -1){
          total += count;
          output.write(data, 0, count);
          if ((new Date().getTime() - start) % 20 == 0){
            publishProgress((int) (total * 100 / fileLength));
          }
        }
        // close everything
        output.flush();
        output.close();
        input.close();
        return true;
      }catch (Exception e){
        e.printStackTrace();
      }
      return false;
    }

    @Override
    protected void onProgressUpdate(Integer... values){
      super.onProgressUpdate(values);
      Log.d("UPDATE PROGRESS", "Progress: " + values[0]);
      // just set the progress of the progressDialog
      updateDownloadProgressDialog.setProgress(values[0]);
    }


    @Override
    protected void onPostExecute(Boolean aBoolean){
      super.onPostExecute(aBoolean);
      // dismiss the dialog and if successful, start the install Activity,
      // otherwise, show an alert that downloading failed.
      updateDownloadProgressDialog.dismiss();
      if (aBoolean){
        Intent installIntent = new Intent(Intent.ACTION_VIEW);
        installIntent.setDataAndType(Uri.fromFile(new File(fileLocation)),
                "application/vnd.android.package-archive");
        installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(installIntent);
      }else{
        new AlertDialog.Builder(ChatActivity.this)
                .setTitle(R.string.download_failed)
                .setNegativeButton(R.string.ok, new DialogInterface.OnClickListener(){
                  @Override
                  public void onClick(DialogInterface dialog, int which){
                    dialog.dismiss();
                  }
                }).show();
      }
    }
  }

  public void onDatabaseDebug(MenuItem menuItem){
    //this opens the debug option to look and manage the databases. and of
    // course I don't want a pwd request
    Intent intent = new Intent(this, AndroidDatabaseManager.class);
    startActivity(intent);
    getSharedPreferences(Constants.PREFERENCES, 0).edit().putBoolean
            (Constants.PWD_REQUEST, false).apply();
  }

  /**
   * Sets myt username and password used to log into the XMPP server if they
   * aren't already set.<br>
   * The reason I am not hard coding these or making them constants is for
   * further improvements to be made easier. Probably I want the user to
   * choose his buddyId, so I can make a login from within the app possible.
   */
  private void setUserPwd(){
    //this is straight forward.
    SharedPreferences preferences = getSharedPreferences(Constants.PREFERENCES, 0);
    if (!preferences.contains(Constants.USERNAME))
      preferences.edit().putString(Constants.USERNAME, "aylin").apply();

    if (!preferences.contains(Constants.PASSWORD))
      preferences.edit().putString(Constants.PASSWORD, "passwdAylin").apply();
  }

  @Override
  public void onChatOpened(String buddyId, String name){
    // set the buddyId and the chat name as arguments for the ChatFragment,
    // replace it with the current one and add it to the backstack by its
    // classname. Also make sure the current variables are set correctly.
    ChatFragment fragment = new ChatFragment();
    Bundle extras = new Bundle();
    extras.putString(Constants.BUDDY_ID, buddyId);
    extras.putString(Constants.CHAT_NAME, name);
    if (imageUri != null)
      extras.putParcelable(Constants.IMAGE_URI, imageUri);
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
    // if the user chose a image to be sent to the current buddy
    if (requestCode == ChatActivity.PHOTO_ATTACH_SELECTED && resultCode ==
            Activity.RESULT_OK){
      sendImage(data.getData());
    }else if (requestCode == PasswordActivity.ASK_PWD_REQUEST){
      // if the user has entered a password or exited the passwordActivity
      // otherwise.
      if (resultCode == Activity.RESULT_OK){
        // if the pwd was correct do not request a new pwd. Yep this function
        // is called before onResume, therefore, this would end in an
        // infinity loop otherwise...
        getSharedPreferences(Constants.PREFERENCES, 0).edit().putBoolean
                (Constants.PWD_REQUEST, false).apply();
        // actually I am not sure, shouldn't the onResume function be called
        // which calls init?
        init();
      }else{
//        if (PreferenceManager.getDefaultSharedPreferences(getApplication())
//                .getBoolean(
//                        getResources().getString(R.string.pref_key_enablepwd),
//                        true))
//          startActivityForResult(new Intent(this, PasswordActivity.class),
//                  PasswordActivity.ASK_PWD_REQUEST);
        // I suppose I should finish the activity here as the user pressed
        // back. TODO: I am not sure whether on resume gets called and finish
        // will really finish if the passwordActivity is called...
        finish();
      }
    }
  }

  @Override
  public void sendImage(Uri imageUri){
    // open the sendImageFragment for the user to add a description and
    // probably scale the image or whatever I wanna add there.
    SendImageFragment fragment = new SendImageFragment();
    Bundle extras = new Bundle();
    // the image uri is for obvious reasons.
    extras.putString(Constants.IMAGE_URI, imageUri.toString());
    // the buddyId is needed due to the flow of sending images: The
    // sendImageFragment just copies the image and adds it to the message
    // db of the chat and notifies the chatFragment that there is a new
    // message. Therefore the chatFragment loads this message and sends it.
    extras.putString(Constants.BUDDY_ID, currentBuddyId);
    // the chat name is because the sendImageFragment shows it as subtitle
    // for the actionBar
    extras.putString(Constants.CHAT_NAME, currentChatName);
    fragment.setArguments(extras);
    // replace the fragment and also add it to the backstack by its name.
    getSupportFragmentManager().beginTransaction().replace(R.id
            .fragment_container, fragment).addToBackStack(SendImageFragment
            .class.getName()).commit();
  }

  @Override
  public void onAttachClicked(){
    // when clicking attack the user should at first select an application to
    // choose the image with and then choose an image.
    // this intent is for getting the image
    Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
    getIntent.setType("image/*");

    // and this for getting the application to get the image with
    Intent pickIntent = new Intent(Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
    pickIntent.setType("image/*");

    // and this finally is for opening the chooserIntent for opening the
    // getIntent for returning the image uri. Yep, thanks android
    Intent chooserIntent = Intent.createChooser(getIntent, getResources()
            .getString(R.string.select_image));
    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new
            Intent[]{pickIntent});
    startActivityForResult(chooserIntent, ChatActivity.PHOTO_ATTACH_SELECTED);
    // nope I don't want to be asked for a pwd when selected the image
    getSharedPreferences(Constants.PREFERENCES, 0).edit().putBoolean
            (Constants.PWD_REQUEST, false).apply();
  }

  @Override
  public void onBackStackChanged(){
    // every time I click back or add an item to the backstack I want to check
    // whether I want to display the home button in the actionBar or not.
    shouldDisplayHomeUp();
  }

  /**
   * chooses whether to display the back button in the actionBar and act
   * correspondingly (enabling/disabling the back button and resetting the
   * {@link #currentBuddyId} and {@link #currentChatName} if necessary.
   */
  private void shouldDisplayHomeUp(){
    // If I can back enable those two parameters for the actionBar, they
    // together enable the back button in the upper left corner.
    boolean canBack = getSupportFragmentManager().getBackStackEntryCount() > 0;
    getSupportActionBar().setDisplayHomeAsUpEnabled(canBack);
    getSupportActionBar().setHomeButtonEnabled(canBack);
    if (!canBack){
      currentBuddyId = Constants.BUDDY_ID;
      currentChatName = Constants.CHAT_NAME;
    }
  }

  /**
   * initialize the activity. That means opening the correct fragments,
   * propagating the backstack and canceling all active notifications.
   */
  private void init(){
    // if the buddyId equals the constant the chatListFragment should be
    // opened and if there are no open fragments I want to open the
    // listFragment and then open the chat in order to have a correct
    // backstack. Maybe this is possible in a more performant way with the
    // notfication backstack possibilities but well this works and is easy!
    if (Constants.BUDDY_ID.equals(currentBuddyId))
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
    //like what the fuck should the up button from the actionBar do
    // otherwise? Why do I need to implement this?
    getSupportFragmentManager().popBackStack();
    return true;
  }

  @Override
  public void onReturnClick(){
    //this function is for the sendImageFragment to pop the backstack when
    // clicking cancel or send or pressing back.
    getSupportFragmentManager().popBackStack();
  }
}
