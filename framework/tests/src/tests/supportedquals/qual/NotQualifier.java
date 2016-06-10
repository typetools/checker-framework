package tests.supportedquals.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;

@Target({TYPE_USE, ElementType.FIELD})
public @interface NotQualifier {
}
