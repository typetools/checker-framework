package org.checkerframework.checker.resourceleak;

import org.checkerframework.checker.calledmethods.CalledMethodsAnalysis;
import org.checkerframework.checker.calledmethods.CalledMethodsAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * This variant of CFAnalysis extends the set of ignored exception types to include all those
 * ignored by the {@link MustCallConsistencyAnalyzer}. See {@link
 * MustCallConsistencyAnalyzer#ignoredExceptionTypes}.
 */
public class ResourceLeakAnalysis extends CalledMethodsAnalysis {
  /**
   * Creates a new {@code CalledMethodsAnalysis}.
   *
   * @param checker the checker
   * @param factory the factory
   */
  protected ResourceLeakAnalysis(
      BaseTypeChecker checker, CalledMethodsAnnotatedTypeFactory factory) {
    super(checker, factory);
    ignoredExceptionTypes.addAll(MustCallConsistencyAnalyzer.ignoredExceptionTypes);
  }
}
