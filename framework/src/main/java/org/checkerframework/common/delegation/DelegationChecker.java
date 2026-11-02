package org.checkerframework.common.delegation;

import org.checkerframework.common.delegation.qual.Delegate;
import org.checkerframework.common.delegation.qual.DelegatorMustOverride;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.source.SourceVisitor;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.BasicAnnotationProvider;

/**
 * The Delegation Checker verifies that a class that is written as a delegator really does delegate:
 * that it implements each of its methods by calling the same method on the field that is annotated
 * with {@link Delegate}.
 *
 * <p>In a class that has a field annotated with {@link Delegate}, this checker enforces the
 * following:
 *
 * <ul>
 *   <li>A class may have at most one field annotated with the {@link Delegate} annotation.
 *   <li>The body of a method that overrides a supertype's method is exactly a call to the same
 *       method on the delegate field, passing the overriding method's formal parameters as the
 *       arguments. Alternately, the body may throw {@code UnsupportedOperationException}.
 *   <li>The class overrides every method of a supertype that is annotated with {@link
 *       DelegatorMustOverride}.
 * </ul>
 *
 * <p>The Delegation Checker is not a type system: it defines no type qualifiers and no subtyping
 * relationship, so it extends {@link SourceChecker} rather than {@code BaseTypeChecker}. Its
 * purpose is to justify the reasoning that other checkers perform: a delegator inherits not only an
 * implementation but also a specification, and it satisfies that specification only if it really
 * does delegate. Run the Delegation Checker together with the type-checkers whose specifications
 * the delegator depends on.
 *
 * @checker_framework.manual #delegation-checker Delegation Checker
 */
public class DelegationChecker extends SourceChecker {

  /** The annotation provider, which reads declaration annotations. */
  private final AnnotationProvider annotationProvider = new BasicAnnotationProvider();

  /** Creates a DelegationChecker. */
  public DelegationChecker() {}

  @Override
  protected SourceVisitor<?, ?> createSourceVisitor() {
    return new DelegationVisitor(this);
  }

  @Override
  public AnnotationProvider getAnnotationProvider() {
    return annotationProvider;
  }
}
