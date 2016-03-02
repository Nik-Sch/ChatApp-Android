package com.raspi.chatapp.ui.util.message_array;

/**
 * this is a message that should indicate that the following messages are new
 * to the user.
 */
public class NewMessage extends MessageArrayContent{
  /**
   * the text to be displayed because I want to differentiate between
   * singular and plural.
   */
  public String status;

  /**
   * creates an instance of the newMessage item, there should, for logics sake,
   * only be one in one chat.
   * @param status {@link #status}
   */
  public NewMessage(String status){
    this.status = status;
  }
}
