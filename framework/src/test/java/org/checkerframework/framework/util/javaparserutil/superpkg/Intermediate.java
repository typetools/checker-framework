package org.checkerframework.framework.util.javaparserutil.superpkg;

/**
 * Test data for {@code JavaParserUtilTest}: a subtype that hides a public member type of {@link
 * Base} with a package-private one of the same name.
 */
public class Intermediate extends Base {

  /** Creates a new Intermediate. */
  public Intermediate() {}

  /** Hides {@code Base.Visible}, which no subtype of this class inherits as a result. */
  static class Visible {}
}
