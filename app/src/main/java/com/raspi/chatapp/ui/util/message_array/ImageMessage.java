package com.raspi.chatapp.ui.util.message_array;

/**
 * this class contain every data needed for an imageMessage
 */
public class ImageMessage extends MessageArrayContent{
  /**
   * true if the message should be displayed on the left side, false otherwise
   */
  public boolean left;
  /**
   * the local file containing the image that should be shown
   */
  public String file;
  /**
   * the images description
   */
  public String description;
  /**
   * the progress of downloading / uploading the image
   */
  public int progress;
  /**
   * the url of the server file of the image if it exists
   */
  public String url;
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
   * the id of the chat the imageMessage belongs to
   */
  public String chatId;
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
   * @param file {@link #file}
   * @param description {@link #description}
   * @param url {@link #url}
   * @param progress {@link #progress}
   * @param time {@link #time}
   * @param status {@link #status}
   * @param _ID {@link #_ID}
   * @param chatId {@link #chatId}
   * @param othersId {@link #othersId}
   */
  public ImageMessage(boolean left, String file, String description,
                      String url, int progress, long time, String status,
                      long _ID, String chatId, long othersId){
    this.left = left;
    this.file = file;
    this.description = description;
    this.progress = progress;
    this.url = url;
    this.time = time;
    this.status = status;
    this._ID = _ID;
    this.chatId = chatId;
    this.othersId = othersId;
  }
}
