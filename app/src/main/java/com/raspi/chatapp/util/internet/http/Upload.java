package com.raspi.chatapp.util.internet.http;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.Toast;

import com.alexbbb.uploadservice.MultipartUploadRequest;
import com.alexbbb.uploadservice.UploadServiceBroadcastReceiver;
import com.raspi.chatapp.ui.util.message_array.ImageMessage;
import com.raspi.chatapp.ui.util.message_array.MessageArrayContent;
import com.raspi.chatapp.util.internet.XmppManager;
import com.raspi.chatapp.util.storage.MessageHistory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class Upload{
  public static final String SERVER_URL = "http://raspi-server.ddns" +
          ".net/ChatApp/upload.php";
  private static final String PARAM_FILE = "img_file";
  private static final String PARAM_TYPE = "type";

  private MessageHistory messageHistory;
  private XmppManager xmppManager;
  private Context context;

  public void uploadFile(final Context context, Task task){
    messageHistory = new MessageHistory(context);
    xmppManager = XmppManager.getInstance();
    this.context = context;
    final String uploadID = task.chatId + "|" + task.messageID;
    try{
      new MultipartUploadRequest(context, uploadID, SERVER_URL)
              .addFileToUpload(task.file.getAbsolutePath(), PARAM_FILE)
              .addParameter(PARAM_TYPE, PARAM_FILE)
              .setMaxRetries(2)
              .startUpload();
      uploadReceiver.register(context);
//      saveImageCopy(context, task.file.getAbsolutePath(), task.messageID, task
//              .chatId);
    }catch (Exception e){
      Log.e("AndroidUploadService", e.getMessage(), e);
    }
  }

  public static class Task{
    public File file;
    public String chatId;
    public long messageID;

    public Task(File file, String chatId, long
            messageID){
      this.file = file;
      this.chatId = chatId;
      this.messageID = messageID;
    }
  }

  private final UploadServiceBroadcastReceiver uploadReceiver =
          new UploadServiceBroadcastReceiver(){

            @Override
            public void onError(String uploadId, Exception exception){
              super.onError(uploadId, exception);
              Log.e("UPLOAD_DEBUG", "An error occured while uploading:" +
                      exception.toString());
              int index = uploadId.indexOf('|');
              String buddyId = uploadId.substring(0, index);
              String messageId = uploadId.substring(index + 1);
              messageHistory.updateMessageStatus(buddyId, Long.parseLong
                      (messageId), MessageHistory.STATUS_WAITING);
              this.unregister(context);
            }

            @Override
            public void onCompleted(String uploadId, int serverResponseCode, String serverResponseMessage){
              int index = uploadId.indexOf('|');
              String buddyId = uploadId.substring(0, index);
              long messageId = Long.parseLong(uploadId.substring(index + 1));
              //apparently the message was already sent by another thread,
              // e.g. if we switched connection while the image was sending
              // and this service restarted
              MessageArrayContent m = messageHistory.getMessage(buddyId,
                      messageId);
              if (MessageHistory.STATUS_SENT.equals(((ImageMessage) m)
                      .status))
                return;
              if (!"invalid".equals(serverResponseMessage)){
                MessageArrayContent mac = messageHistory.getMessage(buddyId,
                        messageId);
                try{
                  ImageMessage msg = (ImageMessage) mac;
                  if (xmppManager.sendImageMessage(serverResponseMessage, msg.description,
                          buddyId, msg._ID))
                    messageHistory.updateMessageStatus(buddyId, messageId,
                            MessageHistory.STATUS_SENT);
                }catch (ClassCastException e){
                  Log.e("UPLOAD", "Sending the uploaded image failed");
                  e.printStackTrace();
                }
              }else{
                messageHistory.updateMessageStatus(buddyId, messageId,
                        MessageHistory.STATUS_WAITING);
              }
              this.unregister(context);
            }
          };
}
