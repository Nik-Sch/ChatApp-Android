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

  public static final int CHANGE_PWD_REQUEST = 4242;

  private boolean changed = false;
  private boolean active = false;

  @Override
  protected void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);

    Bundle extras = getIntent().getExtras();
    setResult(Activity.RESULT_CANCELED);
    if (extras.getBoolean(ASK_PWD, true)){
      Intent intent = new Intent(this, PasswordActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_NO_HISTORY);
      startActivityForResult(intent, PasswordActivity.ASK_PWD_REQUEST);
    }else
      onActivityResult(PasswordActivity.ASK_PWD_REQUEST, Activity.RESULT_OK,
              null);
  }

  @Override
  protected void onResume(){
    super.onResume();
    ui();
  }

  private void ui(){
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
          if (s.length() == 4){
            cp.requestFocus();
            s = null;
          }
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
          if (s.length() == 4){
            if (checkEqual(s, np.getText())){
              changePwd(s.toString().toCharArray());
              s = null;
              changed = true;
              setResult(Activity.RESULT_OK);
              finish();
            }else{
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
    char[] newPwd = new char[n.length()];
    n.getChars(0, n.length(), newPwd, 0);

    char[] conPwd = new char[c.length()];
    c.getChars(0, c.length(), conPwd, 0);
    boolean ret = Arrays.equals(newPwd, conPwd);
    conPwd = null;
    newPwd = null;
    n = null;
    c = null;
    return ret;
  }

  private void changePwd(char[] pwd){
    try{
      byte[] salt = new byte[32];
      Random random = new Random();
      random.nextBytes(salt);
      KeySpec spec = new PBEKeySpec(pwd, salt, PasswordActivity.ITERATIONS, 32);
      pwd = null;
      SecretKeyFactory factory = PasswordActivity.getSecretKeyFactory();
      byte[] hash = factory.generateSecret(spec).getEncoded();

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
    if (active){
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
