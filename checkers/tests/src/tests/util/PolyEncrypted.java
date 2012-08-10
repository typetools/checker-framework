package tests.util;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;

import checkers.quals.*;

@PolymorphicQualifier
@TypeQualifier
@Target(ElementType.TYPE_USE)
public @interface PolyEncrypted {}
