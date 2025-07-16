package org.checkerframework.afu.annotator.tests;

import java.util.List;

public class InstanceOfSimple {
  public void foo(Object o) {
    if (o instanceof List) {
      o = new Object();
    }
    System.out.println(o);
  }
}
