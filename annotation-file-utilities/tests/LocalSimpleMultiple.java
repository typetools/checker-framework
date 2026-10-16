package org.checkerframework.afu.annotator.tests;

import java.util.List;

public class LocalSimpleMultiple {
  public void foo() {
    Object o = null;
    System.out.println(o);
    List list = null;
    bar(list);
    bar(o);
  }

  public void bar(Object o) {
    LocalSimpleMultiple second = null;
    bar(second);
  }
}
