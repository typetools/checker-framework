package org.checkerframework.framework.stub.inheritancea;

/** A class on the classpath, used by {@code ToIndexFileConverterTest}, with member types. */
public class ProtectedBase {
  /** A protected member type, inherited by subclasses in other packages. */
  protected static class ProtectedInner {}

  /** A package-private member type, not inherited by subclasses in other packages. */
  static class Integer {}
}
