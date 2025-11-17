package org.checkerframework.afu.annotator.tests;

import java.util.List;
import java.util.Set;

public class LocalMultipleManyMethodsShifted {

  public void foo(Object o) {
    List myList = null;

    myList.add(myList);
    myList.remove(myList);

    if (myList.size() != 0) { // equivalent to !myList.isEmpty()
      Set localVar = null;
      foo(localVar);
      System.out.println(localVar);
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
