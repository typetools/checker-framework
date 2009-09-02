package checkers.tainting;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;
import checkers.source.SuppressWarningsKey;
import checkers.tainting.quals.Untainted;

/**
 * A type-checker plug-in for the Tainting type system qualifier that finds
 * (and verifies the absence of) trust bugs.
 *
 * It verifies that only verified values are trusted and that user-input
 * is sanitized before use.
 */
@TypeQualifiers({Untainted.class, Unqualified.class})
@SuppressWarningsKey("untainted")
public class TaintingChecker extends BaseTypeChecker {}
