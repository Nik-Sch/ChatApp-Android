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
package com.raspi.chatapp.ui.util.emojicon.emoji;

import android.os.Parcel;
import android.os.Parcelable;

public class Emojicon implements Parcelable{

  public static final Creator<Emojicon> CREATOR = new Creator<Emojicon>(){
    @Override
    public Emojicon createFromParcel(Parcel source){
      return new Emojicon(source);
    }

    @Override
    public Emojicon[] newArray(int size){
      return new Emojicon[size];
    }
  };

  private int icon;
  private char value;
  private String emoji;

  public Emojicon(int icon, char value, String emoji){
    this.icon = icon;
    this.value = value;
    this.emoji = emoji;
  }

  public Emojicon(Parcel source){
    icon = source.readInt();
    value = (char) source.readInt();
    emoji = source.readString();
  }

  public Emojicon(String emoji){
    this.emoji = emoji;
  }

  public static Emojicon fromResource(int icon, int value){
    Emojicon emojicon = new Emojicon();
    emojicon.icon = icon;
    emojicon.value = (char) value;
    return emojicon;
  }

  public static Emojicon fromCodePoint(int codePoint){
    Emojicon emojicon = new Emojicon();
    emojicon.emoji = newString(codePoint);
    return emojicon;
  }

  public static Emojicon fromChars(String chars){
    Emojicon emojicon = new Emojicon();
    emojicon.emoji = chars;
    return emojicon;
  }

  public static Emojicon fromChar(char ch){
    Emojicon emojicon = new Emojicon();
    emojicon.emoji = Character.toString(ch);
    return emojicon;
  }

  private Emojicon(){
  }

  public static String newString(int codePoint){
    return (Character.charCount(codePoint) == 1)
            ? String.valueOf(codePoint)
            : new String(Character.toChars(codePoint));
  }


  @Override
  public int describeContents(){
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags){
    dest.writeInt(icon);
    dest.writeInt(value);
    dest.writeString(emoji);
  }

  public String getEmoji(){
    return emoji;
  }

  @Override
  public boolean equals(Object o){
    return o instanceof Emojicon && emoji.equals(((Emojicon) o).emoji);
  }

  @Override
  public int hashCode(){
    return emoji.hashCode();
  }
}
