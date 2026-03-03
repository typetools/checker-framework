package org.checkerframework.afu.annotator.tests;

import java.util.Date;

public class TypeParamMethod {

  public <T> void foo(T t) {
    System.out.println(t);
  }

  public <T extends Date> void foo2(T t) {
    System.out.println(t);
  }

  public <T, U> void foo(T t, U u) {
    System.out.println(t);
    System.out.println(u);
  }

  public <T extends Date, U extends Date> void foo2(T t, U u) {
    System.out.println(t);
    System.out.println(u);
  }
}
