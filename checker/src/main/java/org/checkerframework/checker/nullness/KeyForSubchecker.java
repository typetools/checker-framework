package org.checkerframework.checker.nullness;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.StubFiles;

/**
 * A type-checker for determining which values are keys for which maps. Typically used as part of
 * the compound checker for the nullness type system.
 *
 * @checker_framework.manual #map-key-checker Map Key Checker
 * @checker_framework.manual #nullness-checker Nullness Checker
 */
@StubFiles("/Users/smillst/jsr308/checker-framework/checker/jdk/nullness/src")
public class KeyForSubchecker extends BaseTypeChecker {}
