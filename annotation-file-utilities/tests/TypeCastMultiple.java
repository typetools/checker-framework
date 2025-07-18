package org.checkerframework.afu.annotator.tests;

import java.util.List;

public class TypeCastMultiple {
  public void foo(Object o) {
    List myList = (List) o;
    Integer i = (Integer) o;
    String s = (String) ((CharSequence) o);
    Object n = (String & Comparable<String> & CharSequence) null;
    System.out.println(myList);
    System.out.println(i);
  }
}
