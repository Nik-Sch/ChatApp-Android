package com.raspi.chatapp.util.structures;


/**
 * This is a list that is linked both ways. Therefore, iterating forward and
 * backwards is the most efficient way of accessing data. However, it is also
 * possible to access items at certain position via {@link #setIndex(int)},
 * {@link #getIndex()} and {@link #current()}.
 * @param <E> This is the data type the list will be holding.
 */
public class LinkedList<E>{

  private ListElement<E> root;
  private int size;
  // current will always point to the first element, or if there is no element
  // in the list to the root.
  private ListElement<E> current;

  /**
   * Creates and initializes an empty list.
   */
  public LinkedList(){
    size = 0;
    root = new ListElement<>(null);
    root.next = root;
    root.previous = root;
    current = root;
  }

  /**
   * Add objects at the end of the list.
   * @param objects the objects to add
   */
  public void add(E... objects){
    for (E object : objects){
      addAtEnd(object);
    }
  }

  /**
   * add the object at the end.
   * @param object the object to be added.
   */
  private void addAtEnd(E object){
    ListElement<E> newElement = new ListElement<>(object);
    if (root.previous != null){
      newElement.previous = root.previous;
      root.previous.next = newElement;
      newElement.next = root;
      root.previous = newElement;
    }else{
      root.next = newElement;
      root.previous = newElement;
      newElement.next = root;
      newElement.previous = root;
      current = newElement;
    }
    size++;
  }

  /**
   * Empty the list.
   */
  public void clear(){
    current = root;
    while (size >= 0){
      try{
        remove();
      }catch (EmptyLinkedListException e){
        e.printStackTrace();
      }
    }
  }

  /**
   * removes the current element. Afterwards, the current element will be the
   * element that followed the removed one.
   * @return the element, that has been removed.
   */
  public E remove(){
    if (size == 0)
      throw new EmptyLinkedListException("There is no element to be removed " +
              "in the list.");
    // just making sure
    if (current != root){
      E result = current.data;
      ListElement<E> p = current.previous;
      ListElement<E> n = current.next;
      p.next = n;
      n.previous = p;
      current.destroy();
      current = n;
      size--;
      return result;
    }
    return null;
  }

  /**
   * returns the current element.
   * @return the current element
   */
  public E current(){
    if (current == root)
      throw new EmptyLinkedListException();
    return current.data;
  }

  /**
   * returns the next element if there is one and moves the current element.
   * Meaning {@link #getIndex()} will return{@code oldIndex + 1}.
   * @return the next element. Or null if there is no next element.
   */
  public E next(){
    if (current.next == root)
      return null;
    current = current.next;
    return current.data;
  }

  /**
   * returns the previous element if there is one and moves the current element.
   * Meaning {@link #getIndex()} will return{@code oldIndex - 1}.
   * @return the previous element. Or null if there is no previous element.
   */
  public E previous(){
    if (current.previous == root)
      return null;
    current = current.previous;
    return current.data;
  }

  /**
   * Sets the current element to the element with this index. As this is a
   * linked list, this is not the most efficient way of accessing.
   * @param index the new index of the current element.
   */
  public void setIndex(int index){
    if (index >= size || index < 0)
      throw new IllegalArgumentException("The index must be greater or equal" +
              "than zero and smaller than the size of the LinkedList.");
    current = root;
    if (index < size / 2)
      for (int i = 0; i <= index; i++)
        current = current.next;
    else
      for (int i = 0; i <= index; i++)
        current = current.previous;
  }

  /**
   * returns the current index. This structure does not keep track of the
   * index but calculates it each time this method is called.
   * @return the current index. If there is no element in the list, -1.
   */
  public int getIndex(){
    int i = -1;
    ListElement<E> temp = root;
    while (temp != current){
      temp = temp.next;
      i++;
    }
    return i;
  }

  /**
   * returns the total size of the list.
   * @return the list size.
   */
  public int getSize(){
    return size;
  }

  public void moveToFirst(){
    setIndex(0);
  }

  /**
   * This Exception is thrown if you try to access the list while it is empty.
   */
  public static class EmptyLinkedListException extends RuntimeException{

    public EmptyLinkedListException(){
    }

    public EmptyLinkedListException(String message){
      super(message);
    }
  }
}
