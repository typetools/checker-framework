package org.checkerframework.afu.annotator.tests;

public class SameName {
  void m() {
    if (5 == 6) {
      Object a = null;
    } else {
      Object a = null;
    }
  }
}
