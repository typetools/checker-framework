package org.checkerframework.framework.stub.inheritancea;

/**
 * A class whose package-private member type is not inherited by subclasses in other packages. Used
 * by {@code ToIndexFileConverterTest}.
 */
public class Base {
  /** A package-private member type. */
  static class Inner {}
}
