package org.checkerframework.afu.annotator.tests;

import java.util.List;

public class TypeCastSimple {
  public void foo(Object o) {
    List myList = (/* @UnderInitialization*/ List) o;
    System.out.println(o);
  }
}
