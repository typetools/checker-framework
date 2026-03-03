package org.checkerframework.afu.annotator.tests;

public class LocalArray {

  public void foo() {
    Object[] o = null;
    System.out.println(o);
  }
}
