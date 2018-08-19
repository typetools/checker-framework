package org.checkerframework.checker.determinism;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * The Determinism Checker prevents non-determinism in single-threaded programs. It enables a
 * programmer to indicate which computations should be the same across runs of a program, and then
 * verifies that property.
 *
 * @checker_framework.manual #determinism-checker Determinism Checker
 */
public class DeterminismChecker extends BaseTypeChecker {}
