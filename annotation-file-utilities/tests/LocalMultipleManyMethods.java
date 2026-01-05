package org.checkerframework.afu.annotator.tests;

import java.util.List;
import java.util.Set;

public class LocalMultipleManyMethods {
  public void foo(Object o) {
    List myList = null;

    if (myList.size() != 0) {
      Set localVar = null;
      myList.add(localVar);
    } else {
      Set localVar = null;
      myList.add(localVar);
    }
    foo(o);
  }

  public void foo(Object[] o) {
    List myList = null;

    if (myList.size() != 0) { // equivalent to !myList.isEmpty()
      Set localVar = null;
      myList.add(localVar);
    } else {
      Set localVar = null;
      myList.add(localVar);
    }
    foo(o);
  }
}
