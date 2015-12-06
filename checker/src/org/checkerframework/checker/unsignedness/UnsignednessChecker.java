package org.checkerframework.checker.unsignedness;

import org.checkerframework.checker.unsignedness.qual.*;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.qual.TypeQualifiers;

@TypeQualifiers( { UnknownSignedness.class, Unsigned.class, 
    Signed.class, Constant.class, UnsignednessBottom.class } )
public final class UnsignednessChecker extends BaseTypeChecker {
}