package org.checkerframework.framework.util.javaparserutil.subpkg;

import org.checkerframework.framework.util.javaparserutil.superpkg.Base;

/**
 * Test data for {@code JavaParserUtilTest}: a top-level type whose simple name is also the simple
 * name of a package-private member type of {@link Base}. A class in this package does not inherit
 * {@code Base.Shadowed}, so the simple name {@code Shadowed} refers to this type.
 */
public class Shadowed {

  /** Creates a new Shadowed. */
  public Shadowed() {}
}
