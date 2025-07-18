package org.checkerframework.afu.annotator.tests;

import java.util.List;

public class ConstructorParamMultiple {
  public ConstructorParamMultiple(
      /* @Tainted*/ Object a,
      /* @Tainted*/ List</* @UnderInitialization*/ Integer> b,
      /* @Tainted*/ int c) {}
}
