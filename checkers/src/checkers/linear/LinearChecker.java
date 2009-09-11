package checkers.linear;

import checkers.basetype.BaseTypeChecker;
import checkers.linear.quals.*;
import checkers.quals.TypeQualifiers;

@TypeQualifiers({Normal.class, Linear.class, Unusable.class})
public class LinearChecker extends BaseTypeChecker { }
