package checkers.signature.quals;


import java.lang.annotation.Target;

import checkers.quals.DefaultQualifierInHierarchy;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

/**
 * Top qualifier in the type hierarchy.
 * This is also default for all Strings that are not known.
 * Unannotated string represents that a string can be anything with respect to signatures.
 * @author Kivanc Muslu
 */
@DefaultQualifierInHierarchy
@TypeQualifier
@SubtypeOf({})
public @interface UnannotatedString {}
