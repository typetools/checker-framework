package org.checkerframework.afu.annotator.tests;

@SuppressWarnings({"deprecation", "removal"})
public class StaticInit {
  static void blabla() {}

  static {
    Object o = new Integer(5);
    if (o instanceof Integer) {
      Object o2 = new Object();
    }
  }

  void m() {
    if (true) {
    } else {
    }
  }

  static {
    StaticInit si = new StaticInit();
  }
}
