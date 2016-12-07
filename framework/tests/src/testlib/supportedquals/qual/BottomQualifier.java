package testlib.supportedquals.qual;

import static java.lang.annotation.ElementType.TYPE_USE;

import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

@SubtypeOf({Qualifier.class})
@Target(TYPE_USE)
public @interface BottomQualifier {}
