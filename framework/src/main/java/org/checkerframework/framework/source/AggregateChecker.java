package org.checkerframework.framework.source;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import java.util.Set;
import javax.tools.Diagnostic;

/**
 * An abstract {@link SourceChecker} that runs subcheckers and interleaves their messages.
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

  @Override
  protected abstract Set<Class<? extends SourceChecker>> getImmediateSubcheckerClasses();

  @Override
  protected SourceVisitor<?, ?> createSourceVisitor() {
    return new SourceVisitor<Void, Void>(this) {
      // aggregate checkers do not visit source,
      // the checkers in the aggregate checker do.
    };
  }
}
