package com.raspi.chatapp.ui.password;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;

import com.alexbbb.uploadservice.UploadService;
import com.raspi.chatapp.BuildConfig;
import com.raspi.chatapp.R;
import com.raspi.chatapp.ui.chatting.ChatActivity;
import com.raspi.chatapp.ui.settings.ChangePasswordActivity;
import com.raspi.chatapp.util.Notification;

import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PasswordActivity extends AppCompatActivity implements PinFragment.OnFragmentInteractionListener{

  public static final String PREFERENCES = "com.raspi.chatapp.ui.password" +
          ".PasswordActivity.PREFERENCES";
  public static final String HASH = "com.raspi.chatapp.ui.password" +
          ".PasswordActivity.HASH";
  public static final String SALT = "com.raspi.chatapp.ui.password" +
          ".PasswordActivity.SALT";

  public static final int ITERATIONS = 1024;
  public static final int SALT_LENGTH = 32;

  private String buddyId = null, chatName = null;
  private boolean not_click = false, change_pwd = false;

  @Override
  protected void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);

    Intent callingIntent = getIntent();
    if (callingIntent != null){
      String action = callingIntent.getAction();
      if (Notification.NOTIFICATION_CLICK.equals
              (action)){
        not_click = true;
        Bundle extras = callingIntent.getExtras();
        if (extras != null && extras.containsKey(ChatActivity.BUDDY_ID) && extras
                .containsKey(ChatActivity.CHAT_NAME)){
          buddyId = extras.getString(ChatActivity.BUDDY_ID);
          chatName = extras.getString(ChatActivity.CHAT_NAME);
        }
      }else if (ChangePasswordActivity.CHANGE_PWD.equals(action)){
        change_pwd = true;
      }
    }
    UploadService.NAMESPACE = BuildConfig.APPLICATION_ID;
    setContentView(R.layout.activity_password);
    if (savedInstanceState == null)
      getSupportFragmentManager().beginTransaction().add(R.id
              .fragment_container, new PinFragment()).commit();
  }

  private void grantAccess(){
    Intent intent;
    if (change_pwd){
      intent = new Intent(this, ChangePasswordActivity.class);
      intent.setAction(ChangePasswordActivity.CHANGE_PWD);
    }else{
      intent = new Intent(this, ChatActivity.class);
      if (buddyId != null){
        intent.putExtra(ChatActivity.BUDDY_ID, buddyId);
        intent.putExtra(ChatActivity.CHAT_NAME, chatName);
        if (not_click)
          intent.setAction(Notification.NOTIFICATION_CLICK);
      }
    }
    startActivity(intent);
    finish();
  }

  private boolean checkPassword(char[] pwd){
    try{
      SharedPreferences preferences = getSharedPreferences(PREFERENCES, 0);
      byte[] salt = Base64.decode(preferences.getString(SALT,
              "0123456789ABCDEF0123456789ABCDEF"), Base64.DEFAULT);

      //default hash for init
      KeySpec tmpspec = new PBEKeySpec("0000".toCharArray(), salt,
              ITERATIONS, SALT_LENGTH);
      SecretKeyFactory f = getSecretKeyFactory();
      byte[] init_hash = f.generateSecret(tmpspec).getEncoded();

      byte[] real_hash = Base64.decode(preferences.getString(HASH, Base64
              .encodeToString(init_hash, Base64.DEFAULT)), Base64.DEFAULT);
      KeySpec spec = new PBEKeySpec(pwd, salt, ITERATIONS, SALT_LENGTH);
      SecretKeyFactory factory = getSecretKeyFactory();
      byte[] gen_hash = factory.generateSecret(spec).getEncoded();

      if (Arrays.equals(real_hash, gen_hash)){
        grantAccess();
        return true;
      }
    }catch (Exception e){
    }
    return false;
  }

  public static SecretKeyFactory getSecretKeyFactory() throws
          NoSuchAlgorithmException, NullPointerException{
    SecretKeyFactory f;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
      f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1And8bit");
    else
      f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
    return f;
  }

  @Override
  public boolean onPasswordEntered(char[] pwd){
    return checkPassword(pwd);
  }
}
