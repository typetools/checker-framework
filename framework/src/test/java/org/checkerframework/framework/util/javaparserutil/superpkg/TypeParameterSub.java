package org.checkerframework.framework.util.javaparserutil.superpkg;

/**
 * Test data for {@code JavaParserUtilTest}: a class with a type parameter whose name is also the
 * name of a member type that the class inherits but does not declare.
 */
public class TypeParameterSub<Visible> extends Base {

  /** Creates a new TypeParameterSub. */
  public TypeParameterSub() {}
}
