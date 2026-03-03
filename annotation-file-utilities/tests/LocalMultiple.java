package org.checkerframework.afu.annotator.tests;

import java.util.List;
import java.util.Set;

public class LocalMultiple {
  public void foo(Object o) {
    List myList = null;

    if (myList.size() != 0) { // equivalent to !myList.isEmpty()
      /* @UnderInitialization*/ Set localVar = null;
      myList.add(localVar);
    } else {
      /* @Tainted*/ Set localVar = null;
      myList.add(localVar);
    }
    foo(o);
  }
}
