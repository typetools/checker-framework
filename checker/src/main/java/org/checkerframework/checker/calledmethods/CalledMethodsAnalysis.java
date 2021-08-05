package org.checkerframework.checker.calledmethods;

import com.sun.tools.javac.code.Type;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAnalysis;

public class CalledMethodsAnalysis extends CFAnalysis {

  protected CalledMethodsAnalysis(
      BaseTypeChecker checker, CalledMethodsAnnotatedTypeFactory factory) {
    super(checker, factory);
  }

  private static final String NPE_NAME = NullPointerException.class.getCanonicalName();

  @Override
  protected boolean isIgnoredExceptionType(TypeMirror typeMirror) {
    return ((Type) typeMirror).tsym.getQualifiedName().contentEquals(NPE_NAME);
  }
}
