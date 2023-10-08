package org.checkerframework.checker.resourceleak;

import com.google.common.collect.ImmutableSet;
import com.sun.tools.javac.code.Type;
import java.util.Set;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.calledmethods.CalledMethodsAnalysis;
import org.checkerframework.checker.calledmethods.CalledMethodsAnnotatedTypeFactory;
import org.checkerframework.checker.signature.qual.CanonicalName;

/**
 * This variant of CFAnalysis extends the set of ignored exception types.
 *
 * @see ResourceLeakChecker#getIgnoredExceptions()
 */
public class ResourceLeakAnalysis extends CalledMethodsAnalysis {

  private final Set<@CanonicalName String> ignoredExceptions;

  /**
   * Creates a new {@code CalledMethodsAnalysis}.
   *
   * @param checker the checker
   * @param factory the factory
   */
  protected ResourceLeakAnalysis(
      ResourceLeakChecker checker, CalledMethodsAnnotatedTypeFactory factory) {
    super(checker, factory);
    this.ignoredExceptions = ImmutableSet.copyOf(checker.getIgnoredExceptions());
  }

  @Override
  public boolean isIgnoredExceptionType(TypeMirror exceptionType) {
    return ignoredExceptions.contains(((Type) exceptionType).tsym.getQualifiedName().toString());
  }
}
