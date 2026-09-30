package org.checkerframework.framework.util.javaparserutil.superpkg;

/**
 * Test data for {@code JavaParserUtilTest}: a class that declares a member type whose name is also
 * the name of a type parameter and of an inherited member type.
 */
public class Shadowing<Visible> extends Base {

  /** Creates a new Shadowing. */
  public Shadowing() {}

  /** Shadows the type parameter {@code Visible}, and hides {@code Base.Visible}. */
  public static class Visible {

    /** A member type of a member type that shadows a type parameter. */
    public static class Inner {}
  }
}
