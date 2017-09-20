package org.checkerframework.checker.nullness;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * A type-checker for determining which values are keys for which maps. Typically used as part of
 * the compound checker for the nullness type system.
 *
 * @checker_framework.manual #map-key-checker Map Key Checker
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
public class KeyForSubchecker extends BaseTypeChecker {}
