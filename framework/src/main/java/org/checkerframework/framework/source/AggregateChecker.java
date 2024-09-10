package org.checkerframework.framework.source;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import java.util.Set;
import javax.tools.Diagnostic;

/**
 * An abstract {@link SourceChecker} that runs independent subcheckers and interleaves their
 * messages.
 *
 * <p>There is no communication, interaction, or cooperation between the component checkers, even to
 * the extent of being able to read one another's qualifiers. An aggregate checker is merely
 * shorthand to invoke a sequence of checkers.
 *
 * <p>Though each checker is run on a whole compilation unit before the next checker is run, error
 * and warning messages are collected and sorted based on the location in the source file before
 * being printed. (See {@link #printOrStoreMessage(Diagnostic.Kind, String, Tree,
 * CompilationUnitTree)}.)
 *
 * <p>This class delegates {@code AbstractTypeProcessor} responsibilities to each component checker.
 *
 * <p>Checker writers need to subclass this class and only override {@link
 * #getImmediateSubcheckerClasses()} ()} to indicate the classes of the checkers to be bundled.
 */
public abstract class AggregateChecker extends SourceChecker {

  /** Create a new AggregateChecker. */
  protected AggregateChecker() {}

  /**
   * Returns the set of independent subchecker classes run by this checker.
   *
   * <p>If a checker should be added or not based on a command line option, use {@link
   * #getOptionsNoSubcheckers()} or {@link #hasOptionNoSubcheckers(String)} to avoid recursively
   * calling this method.
   *
   * <p>Each subchecker of this checker may depend on other checkers. If this checker and one of its
   * subcheckers both run a third checker, that checker will only be instantiated once.
   *
   * <p>Though each checker is run on a whole compilation unit before the next checker is run, error
   * and warning messages are collected and sorted based on the location in the source file before
   * being printed. (See {@link #printOrStoreMessage(Diagnostic.Kind, String, Tree,
   * CompilationUnitTree)}.)
   *
   * <p>WARNING: Circular dependencies are not supported. (In other words, if checker A depends on
   * checker B, checker B cannot depend on checker A.) The Checker Framework does not check for
   * circularity. Make sure no circular dependencies are created when overriding * this method.
   *
   * <p>This method is protected so it can be overridden, but it should only be called internally by
   * {@link SourceChecker}.
   *
   * @return the independent subchecker classes; will be modified by callees
   */
  @Override
  protected abstract Set<Class<? extends SourceChecker>> getImmediateSubcheckerClasses();

  @Override
  protected SourceVisitor<?, ?> createSourceVisitor() {
    return new SourceVisitor<Void, Void>(this) {
      // Aggregate checkers do not visit source,
      // the checkers in the aggregate checker do.
    };
  }
}
