package testlib.supportedquals.qual;

import static java.lang.annotation.ElementType.TYPE_USE;

import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultQualifierInHierarchy;
import org.checkerframework.framework.qual.SubtypeOf;

@SubtypeOf({})
@Target(TYPE_USE)
@DefaultQualifierInHierarchy
public @interface Qualifier {}
