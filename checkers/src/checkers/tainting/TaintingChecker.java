package checkers.tainting;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.PolyAll;
import checkers.quals.TypeQualifiers;
import checkers.source.SuppressWarningsKeys;
import checkers.tainting.quals.PolyTainted;
import checkers.tainting.quals.Tainted;
import checkers.tainting.quals.Untainted;

/**
 * A type-checker plug-in for the Tainting type system qualifier that finds
 * (and verifies the absence of) trust bugs.
 * <p>
 *
 * It verifies that only verified values are trusted and that user-input
 * is sanitized before use.
 *
 * @checker.framework.manual #tainting-checker Tainting Checker
 */
@TypeQualifiers({Untainted.class, Tainted.class,
    PolyTainted.class, PolyAll.class})
@SuppressWarningsKeys("untainted")
public class TaintingChecker extends BaseTypeChecker {}
