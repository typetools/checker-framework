package org.checkerframework.framework.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Defines the path to the directory which holds test java files. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface TestRootDirectory {
  /**
   * Path, relative to the current/module directory, within which to search for test sources.
   *
   * @return tests root directory
   */
  String value();
}
