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
package com.raspi.chatapp.ui.image;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.raspi.chatapp.R;
import com.raspi.chatapp.ui.util.message_array.ImageMessage;
import com.raspi.chatapp.util.Constants;
import com.raspi.chatapp.util.storage.MessageHistory;

import java.util.ArrayList;

public class ImageViewActivity extends AppCompatActivity implements
        SingleImageFragment.OnFragmentInteractionListener,
        OverviewImageFragment.OnFragmentInteractionListener{

  private String chatId;
  private long messageId;
  private int current;
  private int count;
  private ArrayList<ImageMessage> images;


  @Override
  protected void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_image_view);
    Bundle extras = getIntent().getExtras();
    chatId = extras.getString(Constants.BUDDY_ID);
    MessageHistory messageHistory = new MessageHistory(this);
    images = messageHistory.getImageMessages(chatId);
    if (extras != null && extras.containsKey(Constants.BUDDY_ID) && extras
            .containsKey(Constants.MESSAGE_ID)){
      messageId = extras.getLong(Constants.MESSAGE_ID);
      if (savedInstanceState == null)
        getSupportFragmentManager().beginTransaction().add(
                R.id.fragment_container,
                SingleImageFragment.newInstance()).commit();
      count = images.size();
      for (current = 0; current < count; current++)
        if (images.get(current)._ID == messageId)
          break;
    }else if (extras != null && extras.containsKey(Constants.BUDDY_ID)){
      messageId = -1;
      if (savedInstanceState == null)
        getSupportFragmentManager().beginTransaction().add(
                R.id.fragment_container,
                OverviewImageFragment.newInstance(chatId)).commit();
    }
  }

  @Override
  public int getCount(){
    return count;
  }

  @Override
  public ImageMessage getImageAtIndex(int index){
    current = index;
    return images.get(index);
  }

  @Override
  public int getCurrent(){
    return current;
  }

  @Override
  public String getChatName(){
    MessageHistory messageHistory = new MessageHistory(this);
    return messageHistory.getName(chatId);
  }
}
