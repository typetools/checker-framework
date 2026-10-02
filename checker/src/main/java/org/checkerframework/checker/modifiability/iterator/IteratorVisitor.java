package org.checkerframework.checker.modifiability.iterator;

import org.checkerframework.checker.modifiability.ModifiabilityBaseVisitor;
import org.checkerframework.common.basetype.BaseTypeChecker;

/** Visitor for the {@link IteratorChecker}. */
public class IteratorVisitor extends ModifiabilityBaseVisitor {

  /**
   * Creates a visitor for the Iterator Checker.
   *
   * @param checker the Iterator Checker
   */
  public IteratorVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  // @IteratorPolyMod does not follow the override capability preservation rules.
  // Even if a supertype has @IteratorPolyMod on the receiver, its subtype can override it with an
  // implementation that does not use an iterator.
  @Override
  protected boolean shouldCheckReceiverOverrideCapabilityPreservation() {
    return false;
  }
}
