package org.checkerframework.framework.source;

import java.util.Set;

/**
 * An abstract {@link SourceChecker} that runs subcheckers and interleaves their messages.
 *
 * <p>There is no communication, interaction, or cooperation between the subcheckers, even to the
 * extent of being able to read one another's qualifiers. A composite checker is merely shorthand to
 * invoke a sequence of checkers.
 *
 * <p>This class delegates {@code AbstractTypeProcessor} responsibilities to each component checker.
 *
 * <p>Checker writers need to subclass this class and only override {@link
 * #getImmediateSubcheckerClasses()} ()} to indicate the classes of the checkers to be bundled.
 */
public abstract class CompositeChecker extends SourceChecker {

  /** Create a new CompositeChecker. */
  protected CompositeChecker() {}

  @Override
  protected abstract Set<Class<? extends SourceChecker>> getImmediateSubcheckerClasses();

  @Override
  protected SourceVisitor<?, ?> createSourceVisitor() {
    return new SourceVisitor<Void, Void>(this) {
      // composite checkers do not visit source,
      // the checkers in the composite checker do.
    };
  }
}
