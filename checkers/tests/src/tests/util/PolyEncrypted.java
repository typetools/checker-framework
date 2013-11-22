package tests.util;

import checkers.quals.PolymorphicQualifier;
import checkers.quals.TypeQualifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@PolymorphicQualifier
@TypeQualifier
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
public @interface PolyEncrypted {}
