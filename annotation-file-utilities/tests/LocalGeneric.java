package org.checkerframework.afu.annotator.tests;

import java.util.List;

public class LocalGeneric {
  public void foo() {
    List<Integer> var = null;
    System.out.println(var);
  }
}
