package org.checkerframework.afu.annotator.tests;

public class LocalVariables {
  public void foo() {
    /*Mut*/ Object a = null;
    Object b = null;
    Object c = null;
  }
}
