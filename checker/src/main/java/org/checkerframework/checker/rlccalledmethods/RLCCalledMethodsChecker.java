package org.checkerframework.checker.rlccalledmethods;

import com.sun.source.tree.CompilationUnitTree;
import java.util.Set;
import org.checkerframework.checker.calledmethods.CalledMethodsChecker;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.checker.mustcall.MustCallNoCreatesMustCallForChecker;
import org.checkerframework.checker.resourceleak.ResourceLeakChecker;
import org.checkerframework.checker.resourceleak.SetOfTypes;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.qual.StubFiles;

/**
 * The entry point for the RLCCalledMethodsChecker. This checker is a modifed {@link
 * CalledMethodsChecker} used as a subchecker in the ResourceLeakChecker and never independently.
 * Runs the MustCallChecker as a subchecker in order to share cfg.
 */
@StubFiles("IOUtils.astub")
public class RLCCalledMethodsChecker extends CalledMethodsChecker {

  /** Creates a RLCCalledMethodsChecker. */
  public RLCCalledMethodsChecker() {}

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
    return ((RLCCalledMethodsAnnotatedTypeFactory) getTypeFactory())
        .getResourceLeakChecker()
        .getIgnoredExceptions();
  }

  @Override
  protected Set<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
    Set<Class<? extends BaseTypeChecker>> checkers = super.getImmediateSubcheckerClasses();

    if (this.processingEnv.getOptions().containsKey(MustCallChecker.NO_CREATES_MUSTCALLFOR)) {
      checkers.add(MustCallNoCreatesMustCallForChecker.class);
    } else {
      checkers.add(MustCallChecker.class);
    }

    return checkers;
  }

  /*
   * Required in the RLC, since the RLC doesn't have its root set. The likely reason is that it
   * doesn't have its own visitor. This way, the RLCCMC can set the root for the RLC.
   */
  @Override
  public void setRoot(CompilationUnitTree newRoot) {
    super.setRoot(newRoot);
    if (parentChecker != null) {
      parentChecker.setRoot(newRoot);
    }
  }
}
