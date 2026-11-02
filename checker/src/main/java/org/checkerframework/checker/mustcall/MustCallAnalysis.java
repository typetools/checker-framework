package org.checkerframework.checker.mustcall;

import com.google.common.collect.ImmutableSet;
import com.sun.tools.javac.code.Type;
import java.util.Set;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.signature.qual.CanonicalName;
import org.checkerframework.framework.flow.CFAnalysis;

/**
 * The analysis for the Must Call Checker. The analysis is specialized to ignore certain exception
 * types; see {@link #isIgnoredExceptionType(TypeMirror)}.
 */
public class MustCallAnalysis extends CFAnalysis {

  /**
   * Constructs an MustCallAnalysis.
   *
   * @param checker the checker
   * @param factory the type factory
   */
  public MustCallAnalysis(MustCallChecker checker, MustCallAnnotatedTypeFactory factory) {
    super(checker, factory);
  }

  /**
   * The fully-qualified names of the exception types that are ignored by this checker when
   * computing dataflow stores.
   */
  protected static final Set<@CanonicalName String> ignoredExceptionTypes =
      ImmutableSet.of(
          // Use the Nullness Checker instead.
          NullPointerException.class.getCanonicalName(),
          // Ignore run-time errors, which cannot be predicted statically. Doing
          // so is unsound in the sense that they could still occur - e.g., the
          // program could run out of memory - but if they did, the checker's
          // results would be useless anyway.
          Error.class.getCanonicalName(),
          RuntimeException.class.getCanonicalName());

  @Override
  public boolean isIgnoredExceptionType(TypeMirror exceptionType) {
    return ignoredExceptionTypes.contains(
        ((Type) exceptionType).tsym.getQualifiedName().toString());
  }
}
