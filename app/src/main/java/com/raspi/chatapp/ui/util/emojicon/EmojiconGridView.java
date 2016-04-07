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
package com.raspi.chatapp.ui.util.emojicon;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import com.raspi.chatapp.R;
import com.raspi.chatapp.ui.util.emojicon.emoji.Emojicon;
import com.raspi.chatapp.ui.util.emojicon.emoji.People;

public class EmojiconGridView implements AdapterView.OnItemClickListener{
  public View rootView;
  EmojiconRecents recents;
  Emojicon[] data;
  OnEmojiconClickedListener onEmojiconClickedListener;

  public void setOnEmojiconClickedListener(OnEmojiconClickedListener onEmojiconClickedListener){
    this.onEmojiconClickedListener = onEmojiconClickedListener;
  }

  public EmojiconGridView(Context context, Emojicon[] data, EmojiconRecents recents){
    this(context, data, recents, null);
  }

  public EmojiconGridView(Context context, Emojicon[] data, EmojiconRecents recents,
                          OnEmojiconClickedListener onEmojiconClickedListener){
    LayoutInflater inflater = LayoutInflater.from(context);
    rootView = inflater.inflate(R.layout.emojicon_grid, null);
    this.recents = recents;
    this.onEmojiconClickedListener = onEmojiconClickedListener;
    GridView gridView = (GridView) rootView.findViewById(R.id.emojicon_grid_view);
    if (data == null)
      this.data = People.DATA;
    else{
      this.data = new Emojicon[data.length];
      System.arraycopy(data, 0, this.data, 0, data.length);
    }
    gridView.setAdapter(new EmojiconAdapter(context, this.data));
    gridView.setOnItemClickListener(this);
  }


  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id){
    Emojicon emojicon = (Emojicon) parent.getItemAtPosition(position);
    if (onEmojiconClickedListener != null)
      onEmojiconClickedListener.OnEmojiconClicked(emojicon);
    if (recents != null)
      recents.addRecentEmoji(view.getContext(), emojicon);
  }

  public interface OnEmojiconClickedListener{
    void OnEmojiconClicked(Emojicon emojicon);
  }
}
