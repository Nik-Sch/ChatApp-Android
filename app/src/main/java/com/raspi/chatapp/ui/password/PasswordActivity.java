package com.raspi.chatapp.ui.password;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;

import com.raspi.chatapp.R;

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

  public static final int ASK_PWD_REQUEST = 1;

  public static final int ITERATIONS = 1024;
  public static final int SALT_LENGTH = 32;

  private boolean access = false;

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
  protected void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_password);
    if (savedInstanceState == null)
      getSupportFragmentManager().beginTransaction().add(R.id
              .fragment_container, new PinFragment()).commit();
  }

  private void grantAccess(){
    access = true;
    setResult(Activity.RESULT_OK);
    finish();
  }

  @Override
  protected void onPause(){
    setResult(access ? Activity.RESULT_OK : Activity.RESULT_CANCELED);
    super.onPause();
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

  @Override
  public boolean onPasswordEntered(char[] pwd){
    return checkPassword(pwd);
  }
}
