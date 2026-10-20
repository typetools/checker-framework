package org.checkerframework.afu.annotator.tests;

public class MethodNew {
  void m() {
    Object l = new MethodNew();
  }

  void m(Object p) {
    Object x = new MethodNew();
  }
}
