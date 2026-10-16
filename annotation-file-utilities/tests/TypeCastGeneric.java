package org.checkerframework.afu.annotator.tests;

import java.util.List;

public class TypeCastGeneric {
  public void foo(Object o) {
    List<Integer> i = (List<Integer>) o;
    System.out.println(i);
  }
}
