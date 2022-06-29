package org.checkerframework.checker.signedness.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.PolymorphicQualifier;

/**
 * A polymorphic qualifier for the signedness type system.
 *
 * <p>When two formal parameter types are annotated with {@code @PolySigned}, the two arguments must
 * have the same signedness type annotation. (This differs from the standard rule for polymorphic
 * qualifiers.)
 *
 * @checker_framework.manual #signedness-checker Signedness Checker
 * @checker_framework.manual #qualifier-polymorphism Qualifier polymorphism
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@PolymorphicQualifier(UnknownSignedness.class)
public @interface PolySigned {}
