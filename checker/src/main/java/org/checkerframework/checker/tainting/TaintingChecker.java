package org.checkerframework.checker.tainting;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.source.SuppressWarningsPrefix;

/**
 * A type-checker plug-in for the Tainting type system qualifier that finds (and verifies the
 * absence of) trust bugs.
 *
 * <p>It verifies that only verified values are trusted and that user-input is sanitized before use.
 *
 * @checker_framework.manual #tainting-checker Tainting Checker
 */
@SuppressWarningsPrefix({"untainted", "tainting"})
public class TaintingChecker extends BaseTypeChecker {}
