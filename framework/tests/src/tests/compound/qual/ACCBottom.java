package tests.compound.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TypeQualifier;

@TypeQualifier
@SubtypeOf({ ACCTop.class })
@Target({ ElementType.TYPE_USE, ElementType.TYPE_PARAMETER })
public @interface ACCBottom {
}
