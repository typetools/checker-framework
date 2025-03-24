package org.checkerframework.checker.rlccalledmethods;

import java.util.Set;
import org.checkerframework.checker.calledmethods.CalledMethodsChecker;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.checker.mustcall.MustCallNoCreatesMustCallForChecker;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.checker.resourceleak.ResourceLeakUtils;
import org.checkerframework.checker.resourceleak.SetOfTypes;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.qual.StubFiles;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.javacutil.TypeSystemError;
import org.checkerframework.javacutil.UserError;

/**
 * The entry point for the RLCCalledMethodsChecker. This checker is a modifed {@link
 * CalledMethodsChecker} used as a subchecker in the ResourceLeakChecker, and never independently.
 * Runs the MustCallChecker as a subchecker in order to share the CFG.
 */
@StubFiles("IOUtils.astub")
public class RLCCalledMethodsChecker extends CalledMethodsChecker {

  /** Creates a RLCCalledMethodsChecker. */
  public RLCCalledMethodsChecker() {}

  /** The parent resource leak checker */
  private @MonotonicNonNull ResourceLeakChecker rlc;

  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    return new RLCCalledMethodsVisitor(this);
  }

  /**
   * Get the set of exceptions that should be ignored. This set comes from the {@link
   * ResourceLeakChecker#IGNORED_EXCEPTIONS} option if it was provided, or {@link
   * ResourceLeakChecker#DEFAULT_IGNORED_EXCEPTIONS} if not.
   *
   * @return the set of exceptions to ignore
   */
  public SetOfTypes getIgnoredExceptions() {
    return getResourceLeakChecker().getIgnoredExceptions();
  }

  /**
   * Disable the Returns Receiver Checker unless it has been explicitly enabled with the {@link
   * ResourceLeakChecker#ENABLE_RETURNS_RECEIVER} option.
   */
  @Override
  protected boolean isReturnsReceiverDisabled() {
    return !getResourceLeakChecker().hasOption(ResourceLeakChecker.ENABLE_RETURNS_RECEIVER)
        || super.isReturnsReceiverDisabled();
  }

  @Override
  protected Set<Class<? extends SourceChecker>> getImmediateSubcheckerClasses() {
    Set<Class<? extends SourceChecker>> checkers = super.getImmediateSubcheckerClasses();

    if (hasOptionNoSubcheckers(MustCallChecker.NO_CREATES_MUSTCALLFOR)) {
      checkers.add(MustCallNoCreatesMustCallForChecker.class);
    } else {
      checkers.add(MustCallChecker.class);
    }

    return checkers;
  }

  /**
   * Finds the {@link ResourceLeakChecker} in the checker hierarchy, caches it in a field, and
   * returns it.
   *
   * @return the {@link ResourceLeakChecker} in the checker hierarchy
   */
  private ResourceLeakChecker getResourceLeakChecker() {
    if (this.rlc == null) {
      try {
        this.rlc = ResourceLeakUtils.getResourceLeakChecker(this);
      } catch (TypeSystemError e) {
        throw new UserError(
            "Cannot find ResourceLeakChecker in checker hierarchy. The RLCCalledMethods checker shouldn't be invoked directly, it is only a subchecker of the ResourceLeakChecker. Use the ResourceLeakChecker or the CalledMethodsChecker instead.");
      }
    }

    return this.rlc;
  }
}
