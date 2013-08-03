package checkers.tainting;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.TypeQualifiers;
import checkers.quals.PolyAll;
import checkers.source.SuppressWarningsKey;
import checkers.tainting.quals.*;

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
@SuppressWarningsKey("untainted")
public class TaintingChecker extends BaseTypeChecker {}
