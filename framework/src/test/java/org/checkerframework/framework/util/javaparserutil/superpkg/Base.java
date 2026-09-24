package org.checkerframework.framework.util.javaparserutil.superpkg;

/**
 * Test data for {@code JavaParserUtilTest}: a class with member types of differing accessibility.
 */
public class Base {

  /** Creates a new Base. */
  public Base() {}

  /** A package-private member type, which only a subtype in this package inherits. */
  static class Hidden {}

  /** A public member type, which every subtype inherits. */
  public static class Visible {

    /** A member type that a type that hides {@code Visible} does not have. */
    public static class OnlyInBase {}
  }

  /** A private member type, which no subtype inherits. */
  private static class Secret {}

  /**
   * Returns null. This method exists so that {@code Secret} is used, which avoids a compiler
   * warning.
   *
   * @return null
   */
  Secret secret() {
    return null;
  }
}
