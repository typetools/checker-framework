package org.checkerframework.framework.util.javacparse;

import java.net.URI;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

/** A JavaFileObject constructed from a string. */
class StringJavaFileObject extends SimpleJavaFileObject {

  /** Java code for a file (= a compilation unit). */
  private final String javaCode;

  /**
   * Creates a StringJavaFileObject for the given file contents.
   *
   * @param javaCode the contents of a Java file (= a compilation unit)
   */
  public StringJavaFileObject(String javaCode) {
    super(URI.create("string"), JavaFileObject.Kind.SOURCE);
    this.javaCode = javaCode;
  }

  @Override
  public CharSequence getCharContent(boolean ignoreEncodingErrors) {
    return javaCode;
  }
}
