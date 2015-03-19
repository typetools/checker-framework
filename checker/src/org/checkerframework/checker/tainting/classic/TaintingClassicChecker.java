package org.checkerframework.checker.tainting.classic;

import org.checkerframework.checker.tainting.classic.qual.Tainted;
import org.checkerframework.checker.tainting.classic.qual.PolyTainted;
import org.checkerframework.checker.tainting.classic.qual.Untainted;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.PolyAll;
import org.checkerframework.framework.qual.TypeQualifiers;
import org.checkerframework.framework.source.SuppressWarningsKeys;

/**
 * A type-checker plug-in for the Tainting type system qualifier that finds
 * (and verifies the absence of) trust bugs.
 * <p>
 *
 * It verifies that only verified values are trusted and that user-input
 * is sanitized before use.
 *
 * @checker_framework.manual #tainting-checker Tainting Checker
 */
@TypeQualifiers({Untainted.class, Tainted.class,
    PolyTainted.class, PolyAll.class})
@SuppressWarningsKeys("untainted")
public class TaintingClassicChecker extends BaseTypeChecker {}
