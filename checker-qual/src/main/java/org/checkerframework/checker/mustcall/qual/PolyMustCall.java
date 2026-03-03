package org.checkerframework.checker.mustcall.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.PolymorphicQualifier;

/**
 * The polymorphic qualifier for the Must Call type system. The semantics of this qualifier differ
 * from that of a standard polymorphic qualifier; see {@link MustCallAlias} for documentation of its
 * semantics.
 *
 * @checker_framework.manual #must-call-checker Must Call Checker
 * @checker_framework.manual #qualifier-polymorphism Qualifier polymorphism
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@PolymorphicQualifier(MustCallUnknown.class)
public @interface PolyMustCall {}
