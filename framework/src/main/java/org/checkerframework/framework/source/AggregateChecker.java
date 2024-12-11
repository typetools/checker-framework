package org.checkerframework.framework.source;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import java.util.Collection;
import java.util.LinkedHashSet;
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
 * <p>Checker writers need to subclass this class and only override {@link #getSupportedCheckers()}
 * to indicate the classes of the checkers to be bundled.
 */
public abstract class AggregateChecker extends SourceChecker {

  /** Create a new AggregateChecker. */
  protected AggregateChecker() {}

  /**
   * Returns the list of independent subcheckers to be run together. Subclasses need to override
   * this method.
   *
   * @return the list of checkers to be run
   */
  // These are immediate subcheckers.
  protected abstract Collection<Class<? extends SourceChecker>> getSupportedCheckers();

  @Override
  protected final Set<Class<? extends SourceChecker>> getImmediateSubcheckerClasses() {
    return new LinkedHashSet<>(getSupportedCheckers());
  }

  @Override
  protected SourceVisitor<?, ?> createSourceVisitor() {
    return new SourceVisitor<Void, Void>(this) {
      // Aggregate checkers do not visit source,
      // the checkers in the aggregate checker do.
    };
  }
}
