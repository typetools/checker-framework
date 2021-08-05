package org.checkerframework.checker.calledmethods;

import com.sun.tools.javac.code.Type;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAnalysis;

/**
 * The analysis for the Called Methods Checker. The analysis is specialized to ignore certain
 * exception types; see {@link #isIgnoredExceptionType(TypeMirror)}.
 */
public class CalledMethodsAnalysis extends CFAnalysis {

  /**
   * Creates a new {@code CalledMethodsAnalysis}.
   *
   * @param checker the checker
   * @param factory the factory
   */
  protected CalledMethodsAnalysis(
      BaseTypeChecker checker, CalledMethodsAnnotatedTypeFactory factory) {
    super(checker, factory);
  }

  /**
   * Ignore exceptional control flow due to {@code NullPointerException}s, as this checker assumes
   * the Nullness Checker will be used to prevent such exceptions.
   *
   * @param exceptionType exception type
   * @return {@code true} if {@code exceptionType} is {@code NullPointerException}, {@code false}
   *     otherwise
   */
  @Override
  protected boolean isIgnoredExceptionType(TypeMirror exceptionType) {
    return ((Type) exceptionType)
        .tsym
        .getQualifiedName()
        .contentEquals("java.lang.NullPointerException");
  }
}
