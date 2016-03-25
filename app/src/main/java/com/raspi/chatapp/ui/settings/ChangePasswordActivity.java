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
package com.raspi.chatapp.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.raspi.chatapp.R;
import com.raspi.chatapp.ui.password.PasswordActivity;

import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class ChangePasswordActivity extends AppCompatActivity{

  public static final String ASK_PWD = "com.raspi.chatapp.ui.settings" +
          ".ChangePasswordActivity.ASK_PWD";

  /**
   * the request code when starting this activity as a result
   */
  public static final int CHANGE_PWD_REQUEST = 4242;

  /**
   * holds the value of whether the changing process was completed or not
   */
  private boolean changed = false;
  /**
   * this will only become true if the passwordActivity returns ok
   */
  private boolean active = false;

  @Override
  protected void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);

    Bundle extras = getIntent().getExtras();
    setResult(Activity.RESULT_CANCELED);
    // if I should ask for a password before changing it (when the pwd gets
    // activated, there should be no request for the old (non-existent) pwd)
    if (extras.getBoolean(ASK_PWD, true)){
      // start the pwdActivity with some special flags to not make it
      // possible to get back to it via the back button or similar
      Intent intent = new Intent(this, PasswordActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_NO_HISTORY);
      startActivityForResult(intent, PasswordActivity.ASK_PWD_REQUEST);
      // otherwise just launch onActivityResult with the result ok
    }else
      onActivityResult(PasswordActivity.ASK_PWD_REQUEST, Activity.RESULT_OK,
              null);
  }

  @Override
  protected void onResume(){
    super.onResume();
    initUI();
  }

  private void initUI(){
    final EditText np = (EditText) findViewById(R.id.new_pin);
    final EditText cp = (EditText) findViewById(R.id.confirm_pin);
    if (np != null)
      np.addTextChangedListener(new TextWatcher(){
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after){
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count){
        }

        @Override
        public void afterTextChanged(Editable s){
          // if the length of the newPinEditText equals 4 focus the
          // confirmEditText
          if (s.length() == 4)
            cp.requestFocus();
          // otherwise make sure the the no match sign is gone
          else
            findViewById(R.id.pwd_no_match).setVisibility(View.GONE);
        }
      });

    if (cp != null)
      cp.addTextChangedListener(new TextWatcher(){
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after){
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count){
        }

        @Override
        public void afterTextChanged(Editable s){
          s.length();
          // if the length equals 4, check the entered pwds for equality
          if (s.length() == 4){
            if (checkEqual(s, np.getText())){
              // if they equal, change the password and finish the activity
              // with the result ok
              changePwd(s.toString().toCharArray());
              changed = true;
              setResult(Activity.RESULT_OK);
              finish();
            }else{
              // otherwise clear the editTexts and show the no match warning
              s.clear();
              np.getText().clear();
              np.requestFocus();
              findViewById(R.id.pwd_no_match).setVisibility(View.VISIBLE);
            }
          }
        }
      });
  }

  private boolean checkEqual(Editable c, Editable n){
    // get the char arrays
    char[] newPwd = new char[n.length()];
    n.getChars(0, n.length(), newPwd, 0);

    char[] conPwd = new char[c.length()];
    c.getChars(0, c.length(), conPwd, 0);
    // check the arrays for equality
    return Arrays.equals(newPwd, conPwd);
  }

  private void changePwd(char[] pwd){
    try{
      // generate a salt
      byte[] salt = new byte[PasswordActivity.SALT_LENGTH];
      Random random = new Random();
      random.nextBytes(salt);
      // create the keySpec
      KeySpec spec = new PBEKeySpec(pwd, salt, PasswordActivity.ITERATIONS,
              PasswordActivity.SALT_LENGTH);
      // encode the password
      SecretKeyFactory factory = PasswordActivity.getSecretKeyFactory();
      byte[] hash = factory.generateSecret(spec).getEncoded();

      // save the hash and the salt
      getSharedPreferences(PasswordActivity.PREFERENCES, 0).edit()
              .putString(PasswordActivity.SALT, Base64.encodeToString(salt,
                      Base64.DEFAULT))
              .putString(PasswordActivity.HASH, Base64.encodeToString(hash,
                      Base64.DEFAULT))
              .commit();
    }catch (Exception e){
      e.printStackTrace();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data){
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == PasswordActivity.ASK_PWD_REQUEST && resultCode ==
            Activity.RESULT_OK){
      //access was granted -> change pwd
      setContentView(R.layout.content_change_pwd_pin);
      ActionBar actionBar = getSupportActionBar();
      actionBar.setTitle(R.string.change_pwd);
      actionBar.setDisplayHomeAsUpEnabled(true);
      actionBar.setHomeButtonEnabled(true);
      getWindow().setSoftInputMode(WindowManager.LayoutParams
              .SOFT_INPUT_STATE_ALWAYS_VISIBLE);
      active = true;
    }
  }

  @Override
  protected void onDestroy(){
    super.onDestroy();
    // only if active show the toast (otherwise the changePwd was not started
    if (active){
      // show the toast for either successful change or unsuccessful change
      Toast toast = Toast.makeText(getApplicationContext(), changed ? R.string
              .pwd_changed : R.string.pwd_not_changed, Toast.LENGTH_LONG);
      toast.show();
    }
  }

  @Override
  public boolean onSupportNavigateUp(){
    finish();
    return true;
  }
}
