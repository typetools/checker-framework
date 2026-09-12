package org.checkerframework.checker.testchecker.nullnesssubclass;

import javax.annotation.processing.SupportedOptions;
import org.checkerframework.checker.nullness.NullnessChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;

/**
 * A checker that behaves exactly like the Nullness Checker, but is a different class than {@link
 * NullnessChecker}. It exists so that tests can verify that features that are specific to the
 * Nullness Checker also work for checkers that are built on top of it.
 */
// NullnessChecker declares its supported options using
// javax.annotation.processing.SupportedOptions, which is not @Inherited, so this subclass must
// repeat them.
@SupportedOptions({"assumeKeyFor", "invocationPreservesArgumentNullness"})
public class NullnessSubclassChecker extends NullnessChecker {

  /** Creates a NullnessSubclassChecker. */
  public NullnessSubclassChecker() {}

  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    return new NullnessSubclassVisitor(this);
  }
}
