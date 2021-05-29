// This class must be in a package.  If a class is in the default package, clients cannot static
// import the class, or static import its members.
package org.checkerframework.framework.testchecker.lib;

public class Issue3105Fields {
  public static final String FIELD1 = "foo";

  public static final String FIELD2;

  static {
    FIELD2 = "bar";
  }
}
