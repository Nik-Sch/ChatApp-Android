package com.raspi.chatapp.util.internet.http;

import android.content.Context;
import android.util.Log;

import com.alexbbb.uploadservice.MultipartUploadRequest;
import com.alexbbb.uploadservice.UploadServiceBroadcastReceiver;
import com.raspi.chatapp.ui.util.message_array.ImageMessage;
import com.raspi.chatapp.ui.util.message_array.MessageArrayContent;
import com.raspi.chatapp.util.internet.XmppManager;
import com.raspi.chatapp.util.storage.MessageHistory;

import java.io.File;

/**
 * this class manages everything concerning uploading stuff to the server
 */
public class Upload{
  /**
   * this is the url to the server script that manages the data to be uploaded
   */
  public static final String SERVER_URL = "http://raspi-server.ddns" +
          ".net/ChatApp/upload.php";
  /**
   * the key of the parameter containing the file to be uploaded.
   */
  private static final String PARAM_FILE = "img_file";
  /**
   * the key of the parameter containing the type of the content to be uploaded
   */
  private static final String PARAM_TYPE = "type";

  private MessageHistory messageHistory;
  private XmppManager xmppManager;
  private Context context;

  /**
   * performs the uploadTask
   * @param context the applicationContext
   * @param task the task to be executed
   */
  public void uploadFile(final Context context, Task task){
    // get necessary data
    messageHistory = new MessageHistory(context);
    xmppManager = XmppManager.getInstance();
    this.context = context;
    final String uploadID = task.chatId + "|" + task.messageID;
    try{
      // create the multiPartUploadRequest
      new MultipartUploadRequest(context, uploadID, SERVER_URL)
              // add the file
              .addFileToUpload(task.file.getAbsolutePath(), PARAM_FILE)
              // add the file type
              .addParameter(PARAM_TYPE, PARAM_FILE)
              .setMaxRetries(2)
              .startUpload();
      // register the receiver to receiver upload updates
      uploadReceiver.register(context);
    }catch (Exception e){
      Log.e("AndroidUploadService", e.getMessage(), e);
    }
  }

  /**
   * this class is a wrapper for all the data that is needed for an upload
   */
  public static class Task{
    /**
     * this is the file that should be uploaded
     */
    public File file;
    /**
     * this is the chat to which the file should be uploaded
     */
    public String chatId;
    /**
     * this is the messageId that the message has
     */
    public long messageID;

    /**
     *
     * @param file {@link #file}
     * @param chatId {@link #chatId}
     * @param messageID {@link #messageID}
     */
    public Task(File file, String chatId, long messageID){
      this.file = file;
      this.chatId = chatId;
      this.messageID = messageID;
    }
  }

  /**
   * this field receives the upload progress
   */
  private final UploadServiceBroadcastReceiver uploadReceiver =
          new UploadServiceBroadcastReceiver(){

            @Override
            public void onError(String uploadId, Exception exception){
              super.onError(uploadId, exception);
              // update the messageStatus in the messageHistory
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
              // if the message was already sent by another thread return.
              // e.g. if we switched connection while the image was sending and this service
              // restarted
              MessageArrayContent m = messageHistory.getMessage(buddyId,
                      messageId);
              if (MessageHistory.STATUS_SENT.equals(((ImageMessage) m)
                      .status))
                return;
              // if the respond is not invalid, send the imageMessage
              if (!"invalid".equals(serverResponseMessage)){
                MessageArrayContent mac = messageHistory.getMessage(buddyId,
                        messageId);
                try{
                  // send the imageMessage and update the messageStatus
                  ImageMessage msg = (ImageMessage) mac;
                  if (xmppManager.sendImageMessage(serverResponseMessage, msg.description,
                          buddyId, msg._ID))
                    messageHistory.updateMessageStatus(buddyId, messageId,
                            MessageHistory.STATUS_SENT);
                }catch (ClassCastException e){
                  Log.e("UPLOAD", "Sending the uploaded image failed");
                  e.printStackTrace();
                  messageHistory.updateMessageStatus(buddyId, messageId,
                          MessageHistory.STATUS_WAITING);
                }
              }else{
                messageHistory.updateMessageStatus(buddyId, messageId,
                        MessageHistory.STATUS_WAITING);
              }
              this.unregister(context);
            }
          };
}
