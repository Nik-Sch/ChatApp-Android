package com.raspi.chatapp.util.internet.http;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.ResultReceiver;

import com.raspi.chatapp.util.internet.XmppManager;
import com.raspi.chatapp.util.storage.MessageHistory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * This service will download the message and publicise the progress
 */
public class MessageDownloadService extends IntentService{
  /**
   * the progress will be published with this id.
   */
  public static int UPDATE_PROGRESS = 54242;
  /**
   * this is the action with which this service is to be started.
   */
  public static String DOWNLOAD_ACTION = "com.raspi.chatapp.util.internet" +
          ".http.MessageDownloadService.DOWNLOAD_ACTION";
  /**
   * the key for the parameter containing the url string
   */
  public static String PARAM_URL = "com.raspi.chatapp.util.internet" +
          ".http.MessageDownloadService.PARAM_URL";
  /**
   * the key for the parameter containing the full path to where the message should be downloaded.
   */
  public static String PARAM_FILE = "com.raspi.chatapp.util.internet" +
          ".http.MessageDownloadService.PARAM_FILE";
  /**
   * the key for the parameter containing the parcelable of the receiver which should receive
   * progress updates.
   */
  public static String PARAM_RECEIVER = "com.raspi.chatapp.util.internet" +
          ".http.MessageDownloadService.PARAM_RECEIVER";
  /**
   * the key to the parameter containing the progress when publishing progress.
   */
  public static String PARAM_PROGRESS = "com.raspi.chatapp.util.internet" +
          ".http.MessageDownloadService.PARAM_PROGRESS";
  /**
   * the key to the parameter containing the
   * {@link com.raspi.chatapp.util.Constants#MESSAGE_ID messageId}.
   */
  public static String PARAM_MESSAGE_ID = "com.raspi.chatapp.util.internet" +
          ".http.MessageDownloadService.PARAM_MESSAGE_ID";
  /**
   * the key to the parameter containing the
   * {@link com.raspi.chatapp.util.Constants#MESSAGE_OTHERS_ID othersId}.
   */
  public static String PARAM_OTHERS_MSG_ID = "com.raspi.chatapp.util.internet" +
          ".http.MessageDownloadService.PARAM_OTHERS_MSG_ID";
  /**
   * the key to the parameter containing the
   * {@link com.raspi.chatapp.util.Constants#BUDDY_ID chatId}.
   */
  public static String PARAM_CHAT_ID = "com.raspi.chatapp.util.internet" +
          ".http.MessageDownloadService.PARAM_CHAT_ID";


  private static final String DELETE_URL = "http://raspi-server.ddns" +
          ".net/ChatApp/delete.php";

  /**
   * creates an instance of the downloadService
   */
  public MessageDownloadService(){
    super("MessageDownloadService");
  }

  @Override
  protected void onHandleIntent(Intent intent){
    // only if the action is the correct one
    if (intent != null && intent.getAction().equals(DOWNLOAD_ACTION)){
      // retrieve all necessary data
      Bundle extras = intent.getExtras();
      String urlToDownload = extras.getString(PARAM_URL);
      String fileLocation = extras.getString(PARAM_FILE);
      Long messageId = extras.getLong(PARAM_MESSAGE_ID);
      Long othersId = extras.getLong(PARAM_OTHERS_MSG_ID);
      ResultReceiver receiver = intent.getParcelableExtra(PARAM_RECEIVER);
      String chatId = extras.getString(PARAM_CHAT_ID);
      // send a progress update of 0
      Bundle resultData = new Bundle();
      resultData.putInt(PARAM_PROGRESS, 0);
      resultData.putLong(PARAM_MESSAGE_ID, messageId);
      receiver.send(UPDATE_PROGRESS, resultData);
      // try to download the message
      try{
        // establish the connection
        URL url = new URL(urlToDownload);
        URLConnection connection = url.openConnection();
        int fileLength = connection.getContentLength();

        //download the file
        InputStream input = new BufferedInputStream(connection.getInputStream());
        OutputStream output = new FileOutputStream(fileLocation);

        byte data[] = new byte[4096];
        long total = 0;
        int count;
        long lastUpdate = 0;
        // copy the streams until there is no data left in the input stream
        while ((count = input.read(data, 0, 4096)) != -1){
          total += count;
          // "only" publish the progress every 16kB
          if (total - lastUpdate >= 16384){
            resultData = new Bundle();
            resultData.putInt(PARAM_PROGRESS, (int) (total * 100 / fileLength));
            resultData.putLong(PARAM_MESSAGE_ID, messageId);
            receiver.send(UPDATE_PROGRESS, resultData);
            lastUpdate = total;
          }
          output.write(data, 0, count);
        }
        output.flush();
        output.close();
        input.close();

        //signal the server that the file is no longer needed
        int i;
        String file;
        if ((i = urlToDownload.indexOf("files/")) != -1)
          file = urlToDownload.substring(i);
        else
          throw new Exception("incorrect url");
        HashMap<String, String> params = new HashMap<>();
        params.put("f", file);
        performPostCall(DELETE_URL, params);

        //send the read acknowledgement
        try{
          new MessageHistory(getApplication()).updateMessageStatus(chatId,
                  messageId, MessageHistory.STATUS_READ);
          XmppManager.getInstance().sendAcknowledgement(chatId,
                  othersId, MessageHistory.STATUS_READ);
        }catch (Exception e){
          e.printStackTrace();
        }

        //save a copy of the image in low quality for pre rendering in local storage
        saveImageCopy(fileLocation, messageId, chatId);

      }catch (Exception e){
        e.printStackTrace();
      }

      // publish a final progress making sure the image is completely downloaded
      resultData = new Bundle();
      resultData.putInt(PARAM_PROGRESS, 100);
      resultData.putLong(PARAM_MESSAGE_ID, messageId);
      receiver.send(UPDATE_PROGRESS, resultData);
    }
  }

  /**
   * saves a downscaled copy of the image
   * @param fileLocation the location where the image is stored
   * @param id the messageId of the message needed to calculate the file name of the downscaled
   *           image
   * @param chatId the chatId of the message needed to calculate the file name of the downscaled
   *           image
   */
  private void saveImageCopy(String fileLocation, Long id, String chatId){
    try{
      //old images bitmap
      Bitmap oldImg = BitmapFactory.decodeFile(fileLocation);
      // new width and height
      float height = 50;
      float x = oldImg.getHeight() / height;
      float width = oldImg.getWidth() / x;

      File destFile = new File(getApplication().getFilesDir(), chatId + "-" +
              id + ".jpg");
      OutputStream out = new FileOutputStream(destFile);
      Bitmap image = Bitmap.createScaledBitmap(oldImg, (int) width, (int)
              height, true);
      image.compress(Bitmap.CompressFormat.JPEG, 20, out);
      out.close();
    }catch (Exception e){
      e.printStackTrace();
    }
  }

  /**
   * sends a post request to the url containing the dataParams.
   * @param requestURL the url to which to send the postRequest
   * @param postDataParams the hashmap containing the data the server should get as a post request
   * @return the server response
   */
  private String performPostCall(String requestURL, HashMap<String, String>
          postDataParams){
    URL url;
    String response = "";
    try{
      url = new URL(requestURL);

      // create the connection
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setReadTimeout(15000);
      conn.setConnectTimeout(15000);
      conn.setRequestMethod("POST");
      conn.setDoInput(true);
      conn.setDoOutput(true);

      // set the data as output and send the url request
      OutputStream os = conn.getOutputStream();
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os,
              "UTF-8"));
      writer.write(getPostDataString(postDataParams));
      writer.flush();
      writer.close();
      os.close();

      // retrieve the response, if the responseCode is HTTP_OK, retrieve the responseString
      int responseCode = conn.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK){
        String line;
        BufferedReader br = new BufferedReader(new InputStreamReader(conn
                .getInputStream()));
        // read the data to the response
        while ((line = br.readLine()) != null)
          response += line;
      }
    }catch (Exception e){
      e.printStackTrace();
    }
    return response;
  }

  /**
   * turns a hashmap into the String that represents the post format for sending the data
   * @param params the hashmap to be converted
   * @return the string that can be appended to a HttpRequest
   * @throws UnsupportedEncodingException
   */
  private String getPostDataString(HashMap<String, String> params) throws
          UnsupportedEncodingException{
    StringBuilder result = new StringBuilder();
    boolean first = true;
    // loop through all hashmap entries
    for (Map.Entry<String, String> entry : params.entrySet()){
      // always append an '&' before adding the new data but for the first data.
      if (first)
        first = false;
      else
        result.append("&");

      // append the data in the following format: <key1>=<value2>&<key2>=<value1>&<key3>=<value3>...
      result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
      result.append("=");
      result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
    }
    return result.toString();
  }
}
