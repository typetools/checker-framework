package org.checkerframework.framework.source;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import javax.tools.Diagnostic;
import org.checkerframework.javacutil.TypeSystemError;

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

  /** Creates a new AggregateChecker. */
  protected AggregateChecker() {}

  /**
   * Returns the list of independent subcheckers to be run together. An aggregate checker overrides
   * this method to indicate its immediate subcheckers.
   *
   * @return the list of immediate subcheckers to be run
   */
  protected abstract Collection<Class<? extends SourceChecker>> getSupportedCheckers();

  @Override
  protected final Set<Class<? extends SourceChecker>> getImmediateSubcheckerClasses() {
    return new LinkedHashSet<>(getSupportedCheckers());
  }

  /**
   * {@inheritDoc}
   *
   * <p>An aggregate checker's messages also include those of its immediate subcheckers, because the
   * aggregate checker can report a diagnostic whose message key a subchecker defines. If the
   * aggregate checker and a subchecker define the same key, the aggregate checker's text is used.
   */
  @Override
  public Properties getMessagesProperties() {
    if (messagesProperties != null) {
      return messagesProperties;
    }

    Properties result = new Properties();
    // This creates new instances rather than calling `getSubcheckers()`, which requires a
    // processing environment and so fails outside a compilation, such as in a unit test.
    for (Class<? extends SourceChecker> subcheckerClass : getSupportedCheckers()) {
      SourceChecker subchecker;
      try {
        subchecker = subcheckerClass.getDeclaredConstructor().newInstance();
      } catch (Exception e) {
        throw new TypeSystemError("Could not create an instance of " + subcheckerClass, e);
      }
      result.putAll(subchecker.getMessagesProperties());
    }
    // `super.getMessagesProperties()` caches only the aggregate checker's own messages in the
    // field, so the field is overwritten below.
    result.putAll(super.getMessagesProperties());
    messagesProperties = result;
    return result;
  }

  @Override
  protected SourceVisitor<?, ?> createSourceVisitor() {
    return new SourceVisitor<Void, Void>(this) {
      // Aggregate checkers do not visit source,
      // the checkers in the aggregate checker do.
    };
  }
}
