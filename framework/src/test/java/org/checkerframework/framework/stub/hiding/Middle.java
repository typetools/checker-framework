package org.checkerframework.framework.stub.hiding;

/**
 * A class whose private member type hides {@link Base.N}, so that subclasses of this class inherit
 * neither. Used by {@code ToIndexFileConverterTest}.
 */
public class Middle extends Base {
  /** A private member type, which is not inherited. */
  @SuppressWarnings("UnusedNestedClass")
  private static class N {}
}
