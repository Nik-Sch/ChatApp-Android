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
 * this class contains any data needed for a textMessage
 */
public class TextMessage extends MessageArrayContent{
  /**
   * true if the message should be displayed on the left side, false otherwise
   */
  public boolean left;
  /**
   * the message of the textMessage
   */
  public String message;
  /**
   * the time of sending or receiving the image
   */
  public long time;
  /**
   * the message status, see:
   * <ul>
   * <li>{@link com.raspi.chatapp.util.storage.MessageHistory#STATUS_WAITING
   * STATUS_WAITING}</li>
   * <li>{@link com.raspi.chatapp.util.storage.MessageHistory#STATUS_SENDING
   * STATUS_SENDING}</li>
   * <li>{@link com.raspi.chatapp.util.storage.MessageHistory#STATUS_SENT
   * STATUS_SENT}</li>
   * <li>{@link com.raspi.chatapp.util.storage.MessageHistory#STATUS_RECEIVING
   * STATUS_RECEIVING}</li>
   * <li>{@link com.raspi.chatapp.util.storage.MessageHistory#STATUS_RECEIVED
   * STATUS_RECEIVED}</li>
   * <li>{@link com.raspi.chatapp.util.storage.MessageHistory#STATUS_READ
   * STATUS_READ}</li>
   * </ul>
   */
  public String status;
  /**
   * the messageId of the imageMessage
   */
  public long _ID;
  /**
   * the othersId of the imageMessage
   */
  public long othersId;

  /**
   * creates an imageMessage with all necessary data. See individual public
   * fields for details.
   * @param left {@link #left}
   * @param message {@link #message}
   * @param time {@link #time}
   * @param status {@link #status}
   * @param _ID {@link #_ID}
   * @param othersId {@link #othersId}
   */
  public TextMessage(boolean left, String message, long time, String status,
                     long _ID, long othersId){
    super();
    this.left = left;
    this.message = message;
    this.time = time;
    this.status = status;
    this._ID = _ID;
    this.othersId = othersId;
  }
}
