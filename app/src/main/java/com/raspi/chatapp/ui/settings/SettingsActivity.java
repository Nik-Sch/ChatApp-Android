package com.raspi.chatapp.ui.settings;


import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Toast;

import com.raspi.chatapp.R;
import com.raspi.chatapp.ui.chatting.ChatActivity;
import com.raspi.chatapp.ui.password.PasswordActivity;
import com.raspi.chatapp.ui.util.AppCompatPreferenceActivity;
import com.raspi.chatapp.util.storage.file.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p/>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity{
  /**
   * A preference value change listener that updates the preference's summary
   * to reflect its new value.
   */
  private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener(){
    @Override
    public boolean onPreferenceChange(Preference preference, Object value){
      String stringValue = value.toString();

      if (preference instanceof ListPreference){
        // For list preferences, look up the correct display value in
        // the preference's 'entries' list.
        ListPreference listPreference = (ListPreference) preference;
        int index = listPreference.findIndexOfValue(stringValue);

        // Set the summary to reflect the new value.
        preference.setSummary(
                index >= 0
                        ? listPreference.getEntries()[index]
                        : null);

      }else if (preference instanceof RingtonePreference){
        // For ringtone preferences, look up the correct display value
        // using RingtoneManager.
        if (TextUtils.isEmpty(stringValue)){
          // Empty values correspond to 'silent' (no ringtone).
          preference.setSummary(R.string.pref_ringtone_silent);

        }else{
          Ringtone ringtone = RingtoneManager.getRingtone(
                  preference.getContext(), Uri.parse(stringValue));

          if (ringtone == null){
            // Clear the summary if there was a lookup error.
            preference.setSummary(null);
          }else{
            // Set the summary to reflect the new ringtone display
            // name.
            String name = ringtone.getTitle(preference.getContext());
            preference.setSummary(name);
          }
        }

      }else{
        // For all other preferences, set the summary to the value's
        // simple string representation.
        preference.setSummary(stringValue);


      }
      return true;
    }
  };

  /**
   * Helper method to determine if the device has an extra-large screen. For
   * example, 10" tablets are extra-large.
   */
  private static boolean isXLargeTablet(Context context){
    return (context.getResources().getConfiguration().screenLayout
            & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
  }

  /**
   * Binds a preference's summary to its value. More specifically, when the
   * preference's value is changed, its summary (line of text below the
   * preference title) is updated to reflect the value. The summary is also
   * immediately updated upon calling this method. The exact display format is
   * dependent on the type of preference.
   *
   * @see #sBindPreferenceSummaryToValueListener
   */
  private static void bindPreferenceSummaryToValue(Preference preference){
    // Set the listener to watch for value changes.
    preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

    // Trigger the listener immediately with the preference's
    // current value.
    sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
            PreferenceManager
                    .getDefaultSharedPreferences(preference.getContext())
                    .getString(preference.getKey(), ""));
  }

  @Override
  protected void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    setupActionBar();
  }

  /**
   * Set up the {@link android.app.ActionBar}, if the API is available.
   */
  private void setupActionBar(){
    ActionBar actionBar = getSupportActionBar();
    if (actionBar != null){
      // Show the Up button in the action bar.
      actionBar.setDisplayHomeAsUpEnabled(true);
    }
  }

  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item){
    int id = item.getItemId();
    if (id == android.R.id.home){
      if (!super.onMenuItemSelected(featureId, item)){
        NavUtils.navigateUpFromSameTask(this);
      }
      return true;
    }
    return super.onMenuItemSelected(featureId, item);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean onIsMultiPane(){
    return isXLargeTablet(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public void onBuildHeaders(List<Header> target){
    loadHeadersFromResource(R.xml.pref_headers, target);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item){
    if (item.getItemId() == android.R.id.home){
      onBackPressed();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  /**
   * This method stops fragment injection in malicious applications.
   * Make sure to deny any unknown fragments here.
   */
  protected boolean isValidFragment(String fragmentName){
    return PreferenceFragment.class.getName().equals(fragmentName)
            || PasswordPreferenceFragment.class.getName().equals(fragmentName)
            || NotificationPreferenceFragment.class.getName().equals(fragmentName)
            || ChatPreferenceFragment.class.getName().equals(fragmentName);
  }

  /**
   * This fragment shows password preferences only. It is used when the
   * activity is showing a two-pane settings UI.
   */
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public static class PasswordPreferenceFragment extends PreferenceFragment{
    @Override
    public void onCreate(Bundle savedInstanceState){
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.pref_password);
      setHasOptionsMenu(true);
      findPreference(getResources().getString(R.string.pref_key_enablepwd))
              .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){
                @Override
                public boolean onPreferenceChange(Preference preference,
                                                  Object value){
                  if (((SwitchPreference) preference).isChecked())
                    startActivityForResult(
                            new Intent(getActivity(), PasswordActivity.class),
                            PasswordActivity.ASK_PWD_REQUEST);
                  else{
                    Intent intent = new Intent(getActivity(), ChangePasswordActivity
                            .class);
                    intent.putExtra(ChangePasswordActivity.ASK_PWD, false);
                    startActivityForResult(intent, ChangePasswordActivity
                            .CHANGE_PWD_REQUEST);
                  }
                  return true;
                }
              });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
      super.onActivityResult(requestCode, resultCode, data);
      switch (requestCode){
        case PasswordActivity.ASK_PWD_REQUEST:
          if (resultCode != Activity.RESULT_OK){
            ((SwitchPreference) findPreference(getResources().getString(R.string
                    .pref_key_enablepwd))).setChecked(true);
          }
          break;
        case ChangePasswordActivity.CHANGE_PWD_REQUEST:
          if (resultCode != Activity.RESULT_OK)
            ((SwitchPreference) findPreference(getResources().getString(R.string
                    .pref_key_enablepwd))).setChecked(false);
          break;
      }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
      int id = item.getItemId();
      if (id == android.R.id.home){
        startActivity(new Intent(getActivity(), SettingsActivity.class));
        return true;
      }
      return super.onOptionsItemSelected(item);
    }
  }

  /**
   * This fragment shows notification preferences only. It is used when the
   * activity is showing a two-pane settings UI.
   */
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public static class NotificationPreferenceFragment extends PreferenceFragment{
    @Override
    public void onCreate(Bundle savedInstanceState){
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.pref_notification);
      setHasOptionsMenu(true);

      // Bind the summaries of EditText/List/Dialog/Ringtone preferences
      // to their values. When their values change, their summaries are
      // updated to reflect the new value, per the Android Design
      // guidelines.
      bindPreferenceSummaryToValue(findPreference("notifications_new_message_ringtone"));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
      int id = item.getItemId();
      if (id == android.R.id.home){
        startActivity(new Intent(getActivity(), SettingsActivity.class));
        return true;
      }
      return super.onOptionsItemSelected(item);
    }
  }

  /**
   * This fragment shows chat specific preferences only. It is used when the
   * activity is showing a two-pane settings UI.
   */
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  public static class ChatPreferenceFragment extends PreferenceFragment{

    private static final int WALLPAPER_CHOSEN = 4242;

    @Override
    public void onCreate(Bundle savedInstanceState){
      super.onCreate(savedInstanceState);
      addPreferencesFromResource(R.xml.pref_chats);
      setHasOptionsMenu(true);
      findPreference(getResources().getString(R.string.pref_key_wallpaper))
              .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
                @Override
                public boolean onPreferenceClick(Preference preference){

                  Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
                  getIntent.setType("image/*");

                  Intent pickIntent = new Intent(Intent.ACTION_PICK,
                          MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                  pickIntent.setType("image/*");

                  Intent chooserIntent = Intent.createChooser(getIntent, getResources()
                          .getString(R.string.select_image));
                  chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new
                          Intent[]{pickIntent});
                  startActivityForResult(chooserIntent, WALLPAPER_CHOSEN);
                  return true;
                }
              });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
      super.onActivityResult(requestCode, resultCode, data);
      if (requestCode == WALLPAPER_CHOSEN){
        if (resultCode == Activity.RESULT_OK){
          String imageUriPath = FileUtils.getPath(getActivity(), data.getData());
          File file = new File(getActivity().getFilesDir(), ChatActivity
                  .WALLPAPER_NAME);
          try{
            copyImage(imageUriPath, file);
            Toast.makeText(getActivity(), R.string.wallpaper_changed, Toast
                    .LENGTH_LONG).show();
          }catch (Exception e){
            e.printStackTrace();
            Toast.makeText(getActivity(), R.string.wallpaper_not_changed, Toast
                    .LENGTH_LONG).show();
          }
        }else{
          Toast.makeText(getActivity(), R.string.wallpaper_not_changed, Toast
                  .LENGTH_LONG).show();
        }
      }
    }


    private void copyImage(String sourcePath, File destFile) throws IOException{
      OutputStream out = new FileOutputStream(destFile);
      final BitmapFactory.Options options = new BitmapFactory.Options();
      options.inDensity = 96;
      Bitmap image = BitmapFactory.decodeFile(sourcePath, options);
      image.compress(Bitmap.CompressFormat.JPEG, 42, out);
      out.close();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
      int id = item.getItemId();
      if (id == android.R.id.home){
        startActivity(new Intent(getActivity(), SettingsActivity.class));
        return true;
      }
      return super.onOptionsItemSelected(item);
    }
  }
}
