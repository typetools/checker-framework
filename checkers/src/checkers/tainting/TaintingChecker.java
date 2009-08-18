package checkers.tainting;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.TypeQualifiers;
import checkers.quals.Unqualified;

@TypeQualifiers({Untainted.class, Unqualified.class})
public class TaintingChecker extends BaseTypeChecker {}
