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
      parser.require(XmlPullParser.START_TAG, null, "message");
      String type = parser.getAttributeValue(null, "type");
      if (MessageHistory.TYPE_TEXT.equals(type)){
        msg = new Message(MessageHistory.TYPE_TEXT);
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
        while (parser.next() != XmlPullParser.END_TAG){
          if (parser.getEventType() != XmlPullParser.START_TAG)
            continue;
          if ("file".equals(parser.getName())){
            parser.require(XmlPullParser.START_TAG, null, "file");
            msg.file = parser.nextText();
            parser.require(XmlPullParser.END_TAG, null, "file");
          }else if("description".equals(parser.getName())){
            parser.require(XmlPullParser.START_TAG, null, "description");
            msg.description = parser.nextText();
            parser.require(XmlPullParser.END_TAG, null, "description");
          }
        }

      }
    }catch (Exception e){

    }
    return msg;
  }

  public static class Message{
    public String type;
    public String content = null;
    public String file = null;
    public String description = null;

    public Message(String type){
      this.type = type;
    }
  }

}
