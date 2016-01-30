package com.raspi.chatapp.util.internet.http;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DownloadService extends IntentService{
  public static int UPDATE_PROGRESS = 54242;
  public static String DOWNLOAD_ACTION = "com.raspi.chatapp.util.internet" +
          ".http.DowloadService.DOWNLOAD_ACTION";
  public static String PARAM_URL = "com.raspi.chatapp.util.internet" +
          ".http.DowloadService.PARAM_URL";
  public static String PARAM_FILE = "com.raspi.chatapp.util.internet" +
          ".http.DowloadService.PARAM_FILE";
  public static String PARAM_RECEIVER = "com.raspi.chatapp.util.internet" +
          ".http.DowloadService.PARAM_RECEIVER";
  public static String PARAM_PROGRESS = "com.raspi.chatapp.util.internet" +
          ".http.DowloadService.PARAM_PROGRESS";
  public static String PARAM_MESSAGE_ID = "com.raspi.chatapp.util.internet" +
          ".http.DowloadService.PARAM_MESSAGE_ID";


  public static final String DELETE_URL = "http://raspi-server.ddns" +
          ".net/ChatApp/delete.php";
  public DownloadService(){
    super("DowloadService");
  }

  @Override
  protected void onHandleIntent(Intent intent){
    if (intent != null && intent.getAction().equals(DOWNLOAD_ACTION)){
      Bundle extras = intent.getExtras();
      String urlToDownload = extras.getString(PARAM_URL);
      String fileLocation = extras.getString(PARAM_FILE);
      Long messageId = extras.getLong(PARAM_MESSAGE_ID);
      ResultReceiver receiver = intent.getParcelableExtra(PARAM_RECEIVER);
      try{
        URL url = new URL(urlToDownload);
        URLConnection connection = url.openConnection();
        int fileLength = connection.getContentLength();

        //download the file
        InputStream input = new BufferedInputStream(connection.getInputStream());
        OutputStream output = new FileOutputStream(fileLocation);

        byte data[] = new byte[4096];
        long total = 0;
        int count;
        long start = new Date().getTime();
        while ((count = input.read(data)) != -1){
          total += count;
          if ((new Date().getTime() - start) % 20 == 0){
            Bundle resultData = new Bundle();
            resultData.putInt(PARAM_PROGRESS, (int) (total * 100 / fileLength));
            resultData.putLong(PARAM_MESSAGE_ID, messageId);
            receiver.send(UPDATE_PROGRESS, resultData);
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

      }catch (Exception e){
        e.printStackTrace();
      }

      Bundle resultData = new Bundle();
      resultData.putInt(PARAM_PROGRESS, 100);
      resultData.putLong(PARAM_MESSAGE_ID, messageId);
      receiver.send(UPDATE_PROGRESS, resultData);
    }
  }

  private String performPostCall(String requestURL, HashMap<String, String>
          postDataParams){
    URL url;
    String response = "";
    try{
      url = new URL(requestURL);

      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setReadTimeout(15000);
      conn.setConnectTimeout(15000);
      conn.setRequestMethod("POST");
      conn.setDoInput(true);
      conn.setDoOutput(true);

      OutputStream os = conn.getOutputStream();
      BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os,
              "UTF-8"));
      writer.write(getPostDataString(postDataParams));
      writer.flush();
      writer.close();
      os.close();

      int responseCode = conn.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK){
        String line;
        BufferedReader br = new BufferedReader(new InputStreamReader(conn
                .getInputStream()));
        while ((line = br.readLine()) != null)
          response += line;
      }
    }catch (Exception e){
      e.printStackTrace();
    }
    return response;
  }

  private String getPostDataString(HashMap<String, String> params) throws
          UnsupportedEncodingException{
    StringBuilder result = new StringBuilder();
    boolean first = true;
    for (Map.Entry<String, String> entry : params.entrySet()){
      if (first)
        first = false;
      else
        result.append("&");

      result.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
      result.append("=");
      result.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
    }
    return result.toString();
  }
}
