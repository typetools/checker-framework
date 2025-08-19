package org.checkerframework.framework.testchecker.sideeffectsonly;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * Toy checker used to test whether dataflow analysis correctly type-refines methods annotated with
 * {@link org.checkerframework.dataflow.qual.SideEffectsOnly}.
 */
public class SideEffectsOnlyToyChecker extends BaseTypeChecker {}
