package org.checkerframework.afu.annotator.tests;

import java.util.LinkedList;
import java.util.List;

public class TypeCastMultiple {
  public void foo(Object o) {
    List myList = (List) o;
    myList = new LinkedList();
    if (myList instanceof List) {}
    Integer i = (Integer) o;
    System.out.println(myList);
    System.out.println(i);
  }
}
