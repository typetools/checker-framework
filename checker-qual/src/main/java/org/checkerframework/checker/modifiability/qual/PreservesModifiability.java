package org.checkerframework.checker.modifiability.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotated method preserves modifiability capabilities from its first argument to its return
 * value.
 *
 * <p>For example, if the first argument has a positive modifiability capability, such as
 * {@code @Growable}, {@code @SeqGrowable}, {@code @Shrinkable}, or {@code @Replaceable}, then the
 * return type has that same capability. If the first argument is {@code @IteratorPolyMod}, then the
 * return type is also {@code @IteratorPolyMod}. If the first argument has any other qualifier in a
 * capability hierarchy, then the return type is the top qualifier in that hierarchy.
 *
 * <p>It is an error to write this annotation on a method that does not have exactly one formal
 * parameter and a non-void result, because the annotation relates the result to the first argument.
 * If such a method is nonetheless annotated -- in an annotation file, say, which is not checked --
 * then the annotation has no effect on it.
 *
 * @checker_framework.manual #modifiability-checker Modifiability Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
// TODO: This annotation is trusted, not verified:  no check ensures that the method body actually
// preserves the capabilities of its first argument, so an incorrect use is unsound.  It also lacks
// @InheritedAnnotation, so a call through an override that does not repeat the annotation is not
// refined; the type of a call therefore depends on the static type of the receiver.  Either verify
// the contract in ModifiabilityBaseVisitor, or make the annotation @InheritedAnnotation and
// document it as trusted.
public @interface PreservesModifiability {}
