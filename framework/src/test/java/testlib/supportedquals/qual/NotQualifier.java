package testlib.supportedquals.qual;

import static java.lang.annotation.ElementType.TYPE_USE;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({TYPE_USE, ElementType.FIELD})
public @interface NotQualifier {}
