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

import com.raspi.chatapp.util.storage.MessageHistory;

/**
 * this class is a wrapper for all data that is needed for displaying an ChatEntry
 */
public class ChatEntry{

  /**
   * this is the chatId the chatEntry references to
   */
  public String buddyId;
  /**
   * this is the name of the chat that is displayed
   */
  public String name;
  /**
   * this is the type of the last message which is needed for eventually displaying an icon
   * indicating the message type
   */
  public String lastMessageType;
  /**
   * this is the status of the new message, when the message was sent it is directly shown to the
   * user via an icon, otherwise it is indicated that the message was not be read.
   */
  public String lastMessageStatus;
  /**
   * the string representing the date of the last message. See:
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
  public String lastMessageDate;
  /**
   * the string representation of the last message.
   */
  public String lastMessageMessage;
  /**
   * true if the message was read
   */
  public boolean read;
  /**
   * true if the message was sent, false otherwise.
   */
  public boolean sent;

  /**
   * Creates an ChatEntry with all necessary data. See individual fields for further information.
   * @param buddyId {@link #buddyId}
   * @param name {@link #name}
   * @param lastMessageType {@link #lastMessageType}
   * @param lastMessageStatus {@link #lastMessageStatus}
   * @param lastMessageDate {@link #lastMessageDate}
   * @param lastMessageMessage {@link #lastMessageMessage}
   * @param sent {@link #sent}
   */
  public ChatEntry(String buddyId, String name, String lastMessageType, String
          lastMessageStatus, String lastMessageDate, String
                           lastMessageMessage, boolean sent){
    this.buddyId = buddyId;
    this.name = name;
    this.lastMessageType = lastMessageType;
    this.lastMessageStatus = lastMessageStatus;
    this.lastMessageDate = lastMessageDate;
    this.lastMessageMessage = lastMessageMessage;
    this.read = MessageHistory.STATUS_READ.equals(lastMessageStatus);
    this.sent = sent;
  }
}
