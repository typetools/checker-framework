package org.checkerframework.afu.annotator.tests;

import java.util.List;
import java.util.Map;

public class ComplexLocationOne {
  public List<Map<Integer, String[]>> field;
  public List<Outer<Integer, String[]>.Inner<Integer, String[]>> entries;

  class Outer<W, X> {
    class Inner<Y, Z> {}
  }
}
