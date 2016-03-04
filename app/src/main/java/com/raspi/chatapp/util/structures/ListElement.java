package com.raspi.chatapp.util.structures;

public class ListElement<E>{
  public ListElement<E> next;
  public ListElement<E> previous;
  public E data;

  public ListElement(E object){
    next = null;
    previous = null;
    data = object;
  }

  public void destroy(){
    next = null;
    previous = null;
    data = null;
  }
}
