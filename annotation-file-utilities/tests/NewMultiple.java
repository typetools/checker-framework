package org.checkerframework.afu.annotator.tests;

import java.util.List;

public class NewMultiple {
  void foo(Object o) {
    List var = (/* @Tainted*/ List) o;
    System.out.println(var);
  }

  void bar(Object o) {
    List var = (/* @UnderInitialization*/ List) o;
    System.out.println(var);
  }
}
