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
import android.content.SharedPreferences;

import com.raspi.chatapp.ui.util.emojicon.emoji.Emojicon;

import java.util.ArrayList;
import java.util.StringTokenizer;

public class EmojiconRecentsManager extends ArrayList<Emojicon>{

  private static final String DELIMITER = ",";
  private static final String PREFERENCE_NAME = "emojicon";
  private static final String PREF_RECENTS = "emojicon_recents";
  private static final String PREF_PAGE = "emojicon_page";


  private static final Object LOCK = new Object();
  private static EmojiconRecentsManager instance;
  private static int maxSize = 40;

  private Context context;

  private EmojiconRecentsManager(Context context){
    this.context = context.getApplicationContext();
    loadRecents();
  }

  public static EmojiconRecentsManager getInstance(Context context){
    if (instance == null){
      synchronized (LOCK){
        if (instance == null)
          instance = new EmojiconRecentsManager(context);
      }
    }
    return instance;
  }

  public static void setMaxSize(int size){
    maxSize = size;
  }

  public int getRecentPage(){
    return getPreferences().getInt(PREF_PAGE, 0);
  }

  public void setRecentPage(int page){
    getPreferences().edit().putInt(PREF_PAGE, page).apply();
  }

  public void push(Emojicon emojicon){
    if (contains(emojicon))
      super.remove(emojicon);
    add(emojicon);
  }

  @Override
  public boolean add(Emojicon object){
    boolean r = super.add(object);
    while (size() > maxSize)
      super.remove(0);
    saveRecent();
    return r;
  }

  @Override
  public void add(int index, Emojicon object){
    super.add(index, object);

    if (index == 0)
      while (size() > maxSize)
        super.remove(maxSize);
    else
      while (size() > maxSize)
        super.remove(0);
    saveRecent();
  }

  @Override
  public boolean remove(Object object){
    boolean r = super.remove(object);
    saveRecent();
    return r;
  }

  private SharedPreferences getPreferences(){
    return context.getSharedPreferences(PREFERENCE_NAME, 0);
  }

  private void loadRecents(){
    String s = getPreferences().getString(PREF_RECENTS, " ");
    StringTokenizer tokenizer = new StringTokenizer(s, DELIMITER);
    while (tokenizer.hasMoreTokens())
      add(Emojicon.fromChars(tokenizer.nextToken()));
  }

  private void saveRecent(){
    StringBuilder builder = new StringBuilder();
    int c = size();
    for (int i = 0; i < c; i++){
      Emojicon emojicon = get(i);
      builder.append(emojicon.getEmoji());
      if (i < (c - 1))
        builder.append(DELIMITER);
    }
    getPreferences().edit().putString(PREF_RECENTS, builder.toString()).apply();
  }
}
