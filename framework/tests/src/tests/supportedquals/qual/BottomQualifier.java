package tests.supportedquals.qual;

import org.checkerframework.framework.qual.SubtypeOf;

import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;

@SubtypeOf({Qualifier.class})
@Target(TYPE_USE)
public @interface BottomQualifier {
}
