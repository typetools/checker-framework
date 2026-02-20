package org.checkerframework.common.subtyping;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotationClassLoader;

public class SubtypingAnnotationClassLoader extends AnnotationClassLoader {

  public SubtypingAnnotationClassLoader(BaseTypeChecker checker) {
    super(checker);
  }
}
