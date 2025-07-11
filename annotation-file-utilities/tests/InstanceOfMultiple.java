package org.checkerframework.afu.annotator.tests;

import java.util.List;

public class InstanceOfMultiple {
  public void foo(Object o) {
    if (o instanceof List) {
      if (o instanceof InstanceOfMultiple) {
        if (o instanceof Object) {
          System.out.println(o);
        }
      }
    }

    if (o instanceof List<?>) {
      System.out.println(o);
    }
  }
}
