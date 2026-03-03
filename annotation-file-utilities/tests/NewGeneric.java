package org.checkerframework.afu.annotator.tests;

import java.util.List;

public class NewGeneric {
  public void foo(Object o) {
    List<NewGeneric> varOne = (List<NewGeneric>) o;

    NewGeneric varTwo = (NewGeneric) varOne;

    varTwo.foo(varOne);
  }
}
