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
package com.raspi.chatapp.ui.util.message_array;

/**
 * the DateMessage should be displayed as a simple date signaling that the
 * messages were sent on another day.
 */
public class Date extends MessageArrayContent{
  /**
   * the date in ms to be shown
   */
  public long date;

  /**
   * create a dateMessage with the date to be shown
   * @param date {@link #date}
   */
  public Date(long date){
    this.date = date;
  }
}
