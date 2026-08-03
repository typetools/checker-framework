package org.checkerframework.framework.testchecker.typeinference8;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;

/**
 * A checker whose only purpose is to run {@link Typeinference8InvariantVisitor}, which tests
 * invariants of the classes in {@code org.checkerframework.framework.util.typeinference8}.
 *
 * <p>The qualifier hierarchy of this checker (in the {@code qual} subpackage) is irrelevant; it
 * exists only because every {@code BaseTypeChecker} needs one.
 */
public class Typeinference8InvariantChecker extends BaseTypeChecker {

  /** Creates a {@code Typeinference8InvariantChecker}. */
  public Typeinference8InvariantChecker() {}

  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    return new Typeinference8InvariantVisitor(this);
  }
}
