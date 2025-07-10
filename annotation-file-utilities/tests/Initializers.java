package org.checkerframework.afu.annotator.tests;

public class Initializers {
  static {
    String s1 = new String();
  }

  static {
    String s2 = new String();
  }

  {
    Object o1 = new Object();
  }

  {
    Object o2 = new Object();
  }

  enum MyEnum {
    A;

    static {
      String s = new String();
    }

    {
      Object o = new Object();
    }
  }
}
