package checkers.tainting;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.TypeQualifiers;
import checkers.quals.PolyAll;
import checkers.source.SuppressWarningsKeys;
import checkers.tainting.quals.*;
import checkers.types.BasicAnnotatedTypeFactory;

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
public class TaintingChecker extends BaseTypeChecker<BasicAnnotatedTypeFactory<?>> {}
