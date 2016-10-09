package tests.supportedquals.qual;

import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;

@SubtypeOf({})
@Target(TYPE_USE)
@DefaultQualifierInHierarchy
public @interface Qualifier {
}
