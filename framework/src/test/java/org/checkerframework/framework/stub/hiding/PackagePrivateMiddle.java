package org.checkerframework.framework.stub.hiding;

/**
 * A class whose package-private member type hides {@link Base.N}, so that subclasses of this class
 * in other packages inherit neither. Used by {@code ToIndexFileConverterTest}.
 */
public class PackagePrivateMiddle extends Base {
  /** A package-private member type, which is not inherited outside this package. */
  static class N {}
}
