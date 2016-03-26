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
package com.raspi.chatapp.ui.util.chat_array;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.ankushsachdeva.emojicon.EmojiconTextView;
import com.raspi.chatapp.R;
import com.raspi.chatapp.util.storage.MessageHistory;

import java.util.ArrayList;
import java.util.List;

/**
 * this array adapter serves for showing a list of {@link ChatEntry ChatEntries}.
 */
public class ChatArrayAdapter extends ArrayAdapter<ChatEntry>{
  private List<ChatEntry> chatList = new ArrayList<>();

  public ChatArrayAdapter(Context context, int textViewResourceId){
    super(context, textViewResourceId);
  }

  @Override
  public void add(ChatEntry chatEntry){
    chatList.add(chatEntry);
    notifyDataSetChanged();
  }

  @Override
  public int getCount(){
    return chatList.size();
  }

  @Override
  public ChatEntry getItem(int i){
    return chatList.get(i);
  }

  @Override
  public View getView(int position, View ConvertView, ViewGroup parent){
    View v = ConvertView;
    // if the view doesn't exist create it.
    if (v == null){
      LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      v = inflater.inflate(R.layout.chat_list_entry, parent, false);
    }

    // get the corresponding object
    ChatEntry chatObj = getItem(position);


    ((TextView) v.findViewById(R.id.chat_list_entry_name)).setText(chatObj.name);
    // if there is data for this object then show it
    if (!chatObj.lastMessageStatus.equals("")){
      ((TextView) v.findViewById(R.id.chat_list_entry_time)).setText(chatObj.lastMessageDate);
      // show the last message
      EmojiconTextView msg = ((EmojiconTextView) v.findViewById(R.id.chat_list_entry_mess));
      msg.setExpandedSize(true);
      msg.setText(chatObj.lastMessageMessage);
      // if it is an imageMessage show the image icon, otherwise hide it
      if (MessageHistory.TYPE_IMAGE.equals(chatObj.lastMessageType))
        msg.setCompoundDrawablesWithIntrinsicBounds(R.drawable
                .ic_photo_camera_black_18dp, 0, 0, 0);
      else
        msg.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
      // if I sent the message show the status
      if (chatObj.sent){
        v.findViewById(R.id.chat_list_entry).setBackgroundColor(0xFFFFFF);
        switch (chatObj.lastMessageStatus){
          // show every possible status
          case MessageHistory.STATUS_WAITING:
            ((ImageView) v.findViewById(R.id.chat_list_entry_status))
                    .setImageResource(R.drawable.ic_hourglass_empty_black_48dp);
            break;
          case MessageHistory.STATUS_SENT:
            ((ImageView) v.findViewById(R.id.chat_list_entry_status)).setImageResource(R.drawable.single_grey_hook);
            break;
          case MessageHistory.STATUS_RECEIVED:
            ((ImageView) v.findViewById(R.id.chat_list_entry_status)).setImageResource(R.drawable.two_grey_hook);
            break;
          case MessageHistory.STATUS_READ:
            ((ImageView) v.findViewById(R.id.chat_list_entry_status)).setImageResource(R.drawable.two_blue_hook);
            break;
        }
        // otherwise it might be read or received, if received a blue background should be shown
      }else{
        ((ImageView) v.findViewById(R.id.chat_list_entry_status)).setImageDrawable(null);
        if (!chatObj.sent && !chatObj.read)
          v.findViewById(R.id.chat_list_entry).setBackgroundColor(0xFF55AAFF);
        else
          v.findViewById(R.id.chat_list_entry).setBackgroundColor(0xFFFFFF);
      }
      // if there is no data set everything to blank
    }else{
      ((ImageView) v.findViewById(R.id.chat_list_entry_status)).setImageDrawable(null);
      ((TextView) v.findViewById(R.id.chat_list_entry_time)).setText("");
      ((TextView) v.findViewById(R.id.chat_list_entry_mess)).setText("");
    }
    return v;
  }
}
