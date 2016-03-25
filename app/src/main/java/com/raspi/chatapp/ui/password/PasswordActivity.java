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
package com.raspi.chatapp.ui.password;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;

import com.raspi.chatapp.R;

import java.security.NoSuchAlgorithmException;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class PasswordActivity extends AppCompatActivity implements
        PinFragment.OnFragmentInteractionListener,
        AGBFragment.OnFragmentInteractionListener{

  /**
   * the preference where the passwords hash and salt is stored
   */
  public static final String PREFERENCES = "com.raspi.chatapp.ui.password" +
          ".PasswordActivity.PREFERENCES";
  /**
   * The key to the password hash
   */
  public static final String HASH = "com.raspi.chatapp.ui.password" +
          ".PasswordActivity.HASH";
  /**
   * the key to the password salt
   */
  public static final String SALT = "com.raspi.chatapp.ui.password" +
          ".PasswordActivity.SALT";

  /**
   * the requestCode of the activity when started as result
   */
  public static final int ASK_PWD_REQUEST = 1;

  /**
   * the amount of iterations
   */
  public static final int ITERATIONS = 1024;
  /**
   * the length of the salt
   */
  public static final int SALT_LENGTH = 32;

  /**
   * a boolean storing whether access is granted or not
   */
  private boolean access = false;

  public static SecretKeyFactory getSecretKeyFactory() throws
          NoSuchAlgorithmException, NullPointerException{
    SecretKeyFactory f;
    // depending on the android version there are two different keyFactories
    // that should be used
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
              .fragment_container, AGBFragment.newInstance()).commit();
  }

  /**
   * sets the activity result and finishes the activity
   */
  private void grantAccess(){
    access = true;
    setResult(Activity.RESULT_OK);
    finish();
  }

  @Override
  protected void onPause(){
    // if access was granted set the result to ok, otherwise to canceled
    setResult(access ? Activity.RESULT_OK : Activity.RESULT_CANCELED);
    super.onPause();
  }

  private void checkPassword(final char[] pwd){
    //the dialog will be dismissed in the thread
    //and the thread will also display the error message if logging in was
    // not successful.
    ProgressDialog dialog = ProgressDialog.show(this, "", getResources()
                    .getString(R.string.logging_in), true);
    LoginThread loginThread = new LoginThread(dialog, pwd);
    loginThread.start();
  }

  @Override
  public void agbAccepted(){
    // if the agb gets accepted you may continue to the "real" pwd request
    getSupportFragmentManager().beginTransaction().replace(R.id
            .fragment_container, PinFragment.newInstance()).commit();

  }

  private class LoginThread extends Thread{

    private ProgressDialog dialog;
    private char[] pwd;

    /**
     * creates an instance of the LoginThread
     * @param dialog the dialog indicating that the password is being checked
     * @param pwd the pwd that the user entered
     */
    public LoginThread(ProgressDialog dialog, final char[] pwd){
      this.dialog = dialog;
      this.pwd = pwd;
    }

    @Override
    public void run(){
      try{
        // get the salt and hash that are correct from the preferences
        SharedPreferences preferences = getSharedPreferences(PREFERENCES, 0);
        // the salt and the hash is a byte array and therefore stored Base64
        // encoded
        byte[] salt = Base64.decode(preferences.getString(SALT,
                "0123456789ABCDEF0123456789ABCDEF"), Base64.DEFAULT);
        byte[] real_hash = Base64.decode(preferences.getString(HASH,
                "invalid"), Base64.DEFAULT);
        // create the keySpec
        KeySpec spec = new PBEKeySpec(pwd, salt, ITERATIONS, SALT_LENGTH);
        // get the keyFactory
        SecretKeyFactory factory = getSecretKeyFactory();
        // and generate the hash with the password the user entered and the
        // salt that was stored
        byte[] gen_hash = factory.generateSecret(spec).getEncoded();

        // if those byte arrays (the hashes) equal grantAccess and dismiss
        // the dialog
        if (Arrays.equals(real_hash, gen_hash)){
          grantAccess();
          dialog.dismiss();
          return;
        }
        // otherwise dismiss the dialog and show the invalid pwd sign
        dialog.dismiss();
        runOnUiThread(new Runnable(){
          @Override
          public void run(){
            try{
              findViewById(R.id.password_invalid).setVisibility(View.VISIBLE);
            }catch (Exception e){
              e.printStackTrace();
            }
          }
        });
      }catch (Exception e){
        e.printStackTrace();
      }
    }
  }

  @Override
  public void onPasswordEntered(char[] pwd){
    checkPassword(pwd);
  }
}
