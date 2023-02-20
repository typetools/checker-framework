package org.checkerframework.checker.testchecker.ainfer.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A declaration annotation used to test that the API for inferring declaration annotations on
 * parameters works properly. The presence of this annotation indicates that the checker should
 * treat the annotated element as if it were annotated as {@link AinferSibling1}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface AinferTreatAsSibling1 {}
