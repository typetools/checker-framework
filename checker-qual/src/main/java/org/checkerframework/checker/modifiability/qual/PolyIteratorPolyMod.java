package org.checkerframework.checker.modifiability.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.PolymorphicQualifier;

/**
 * A polymorphic qualifier for the Iterator hierarchy.
 *
 * <p>When used on a method, whether the return type has {@code @IteratorPolyMod} matches whether
 * the argument or receiver annotated with {@code @PolyIteratorPolyMod} has
 * {@code @IteratorPolyMod}.
 *
 * @checker_framework.manual #modifiability-checker Modifiability Checker
 * @checker_framework.manual #qualifier-polymorphism Qualifier polymorphism
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@PolymorphicQualifier(MaybeIteratorPolyMod.class)
public @interface PolyIteratorPolyMod {}
