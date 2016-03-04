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
package com.raspi.chatapp.util.storage;

import android.provider.BaseColumns;

public class MessageHistoryContract{
  public MessageHistoryContract(){
  }

  public static abstract class ChatEntry implements BaseColumns{
    public static final String TABLE_NAME_ALL_CHATS = "allchats";
    public static final String COLUMN_NAME_BUDDY_ID = "buddyid";
    public static final String COLUMN_NAME_NAME = "name";
    public static final String COLUMN_NAME_LAST_ONLINE = "online";

  }

  public static abstract class MessageEntry implements BaseColumns{
    public static final String COLUMN_NAME_BUDDY_ID = "buddyid";
    public static final String COLUMN_NAME_MESSAGE_TYPE = "type";
    public static final String COLUMN_NAME_MESSAGE_CONTENT = "content";
    public static final String COLUMN_NAME_MESSAGE_STATUS = "status";
    public static final String COLUMN_NAME_MESSAGE_TIMESTAMP = "timestamp";
    public static final String COLUMN_NAME_MESSAGE_PROGRESS = "progress";
    public static final String COLUMN_NAME_MESSAGE_URL = "url";
    public static final String COLUMN_NAME_OTHERS_ID = "othersId";
  }
}
