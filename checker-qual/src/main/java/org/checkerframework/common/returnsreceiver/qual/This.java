package org.checkerframework.common.returnsreceiver.qual;

import org.checkerframework.framework.qual.PolymorphicQualifier;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Write {@code @This} on the return type of a method that always returns its receiver ({@code
 * this}). For example:
 *
 * <pre><code>
 * class MyBuilder {
 *   &#064;This MyBuilder setName(String name) {
 *     this.name = name;
 *     return this;
 *   }
 * }
 * </code></pre>
 *
 * <p>Strictly speaking, this is a polymorphic annotation, but when you write it on a return type,
 * the Returns Receiver Checker automatically adds it to the receiver, so the above method is
 * equivalent to:
 *
 * <pre><code>
 * &#064;This MyBuilder setName(@This MyBuilder this, String name) {
 *   this.name = name;
 *   return this;
 * }
 * </code></pre>
 *
 * <p>While it would be natural to make {@code @This} the default annotation for receivers, it leads
 * to false positives warnings due to <a
 * href="https://github.com/typetools/checker-framework/issues/2931">https://github.com/typetools/checker-framework/issues/2931</a>,
 * so this defaulting is currently elided.
 *
 * @checker_framework.manual #returns-receiver-checker Returns Receiver Checker
 * @checker_framework.manual #qualifier-polymorphism Qualifier polymorphism
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@PolymorphicQualifier
@TargetLocations({TypeUseLocation.RECEIVER, TypeUseLocation.RETURN})
public @interface This {}
