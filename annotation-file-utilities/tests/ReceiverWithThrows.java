package org.checkerframework.afu.annotator.tests;

public class ReceiverWithThrows {
  /* @UnderInitialization ReceiverWithThrows this */
  public void foo() {}

  /* @Tainted ReceiverWithThrows this */
  public void bar() throws Exception {}
}
