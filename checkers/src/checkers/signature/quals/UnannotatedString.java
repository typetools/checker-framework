package checkers.signature.quals;


import java.lang.annotation.Target;

import checkers.quals.DefaultQualifierInHierarchy;
import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

@DefaultQualifierInHierarchy
@TypeQualifier
@SubtypeOf({})
@Target({})
public @interface UnannotatedString {}
