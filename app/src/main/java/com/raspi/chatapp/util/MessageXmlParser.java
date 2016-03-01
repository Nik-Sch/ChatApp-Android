package com.raspi.chatapp.util;

import android.util.Xml;

import com.raspi.chatapp.util.storage.MessageHistory;

import org.xmlpull.v1.XmlPullParser;

import java.io.ByteArrayInputStream;
import java.io.InputStream;


public class MessageXmlParser{

  public static Message parse(String str){
    Message msg = null;
    try{
      XmlPullParser parser = Xml.newPullParser();
      parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
      InputStream in = new ByteArrayInputStream(str.getBytes("UTF-8"));
      parser.setInput(in, null);
      parser.nextTag();
      //parser.require(XmlPullParser.START_TAG, null, "message");
      String name = parser.getName();
      if ("message".equals(name)){
        String type = parser.getAttributeValue(null, "type");
        long othersId = Long.parseLong(parser.getAttributeValue(null, "id"));
        if (MessageHistory.TYPE_TEXT.equals(type)){
          msg = new Message(MessageHistory.TYPE_TEXT);
          msg.id = othersId;
          while (parser.next() != XmlPullParser.END_TAG){
            if (parser.getEventType() != XmlPullParser.START_TAG)
              continue;
            if ("content".equals(parser.getName())){
              parser.require(XmlPullParser.START_TAG, null, "content");
              msg.content = parser.nextText();
              parser.require(XmlPullParser.END_TAG, null, "content");
            }
          }
        }else if (MessageHistory.TYPE_IMAGE.equals(type)){
          msg = new Message(MessageHistory.TYPE_IMAGE);
          msg.id = othersId;
          while (parser.next() != XmlPullParser.END_TAG){
            if (parser.getEventType() != XmlPullParser.START_TAG)
              continue;
            if ("file".equals(parser.getName())){
              parser.require(XmlPullParser.START_TAG, null, "file");
              msg.url = parser.nextText();
              parser.require(XmlPullParser.END_TAG, null, "file");
            }else if ("description".equals(parser.getName())){
              parser.require(XmlPullParser.START_TAG, null, "description");
              msg.description = parser.nextText();
              parser.require(XmlPullParser.END_TAG, null, "description");
            }
          }
        }
      }else if ("acknowledgement".equals(name)){
        msg = new Message("acknowledgement");
        msg.content = parser.getAttributeValue(null, "type");
        msg.id = Long.parseLong(parser.getAttributeValue(null, "id"));
      }
    }catch (Exception e){

    }
    return msg;
  }

  public static class Message{
    public String type;
    public String content = null;
    public String url = null;
    public String description = null;
    /**
     * if used as an received acknowledgement this refers to the id I got
     * sent by the other buddy, that means, my messageId. If used as a
     * message I received, this refers to the othersId.
     */
    public long id = -1;

    public Message(String type){
      this.type = type;
    }
  }

}
