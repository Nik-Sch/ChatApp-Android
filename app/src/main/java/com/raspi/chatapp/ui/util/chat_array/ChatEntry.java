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

public class ChatEntry{

  public String buddyId;
  public String name;
  public String lastMessageType;
  public String lastMessageStatus;
  public String lastMessageDate;
  public String lastMessageMessage;
  public boolean read;
  public boolean sent;

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
