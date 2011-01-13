package checkers.signature.quals;

import checkers.quals.SubtypeOf;
import checkers.quals.TypeQualifier;

@TypeQualifier
@SubtypeOf({UnannotatedString.class})
public @interface BinarySignature {}
