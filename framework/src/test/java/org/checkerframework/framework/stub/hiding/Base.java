package org.checkerframework.framework.stub.hiding;

/**
 * A class whose public member types are hidden in some of its subclasses, such as {@link Middle}.
 * Used by {@code ToIndexFileConverterTest}.
 */
public class Base {
  /** A public member type. */
  public static class N {}

  /** Another public member type. */
  public static class M {}
}
