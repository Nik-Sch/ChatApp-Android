package com.raspi.chatapp.ui.util.message_array;

/**
 * the DateMessage should be displayed as a simple date signaling that the
 * messages were sent on another day.
 */
public class Date extends MessageArrayContent{
  /**
   * the date in ms to be shown
   */
  public long date;

  /**
   * create a dateMessage with the date to be shown
   * @param date {@link #date}
   */
  public Date(long date){
    this.date = date;
  }
}
