package org.checkerframework.framework.stub.hiding;

/**
 * A class whose public member type hides {@link Base.N}. Used by {@code ToIndexFileConverterTest}.
 */
public class PublicMiddle extends Base {
  /** A public member type. */
  public static class N {}
}
