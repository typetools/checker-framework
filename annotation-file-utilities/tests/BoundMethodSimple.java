package org.checkerframework.afu.annotator.tests;

import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class BoundMethodSimple {
  public <T extends Date> void foo(T t) {
    System.out.println(t);
  }

  public <T> void foo2(T t) {
    System.out.println(t);
  }

  public static <T> void foo3(List<T> list, Comparator<? super T> c) {}
}
