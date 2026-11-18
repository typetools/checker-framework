package org.checkerframework.framework.util.javaparserutil.superpkg;

/**
 * Test data for {@code JavaParserUtilTest}: a class that is not a subtype of {@link Base}, so that
 * a test of a local or an anonymous class within it cannot accidentally resolve a name through the
 * enclosing class.
 */
public class Outer {

  /** Creates a new Outer. */
  public Outer() {}
}
