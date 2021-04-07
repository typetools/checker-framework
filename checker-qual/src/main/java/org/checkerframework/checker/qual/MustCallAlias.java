package org.checkerframework.checker.mustcall.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A special polymorphic annotation that represents an either-or must-call obligation. This
 * annotation should always be used in pairs: one on a method receiver parameter or constructor
 * parameter, and the other on the method return type. When the annotated method is invoked, the
 * must-call obligation of the return type is the same as the parameter. Further, fulfilling the
 * must-call obligation of one is equivalent to fulfilling the must-call obligation of the other.
 *
 * <p>This annotation is useful for "wrapper" objects. For example, consider a Socket that must be
 * closed before it is de-allocated (i.e. its must-call type is {@code @MustCall("close")}). Calling
 * the {@code getOutputStream} on the Socket creates a new {@code OutputStream} object that wraps
 * the Socket. Calling its "close" method will close the underlying socket, but the Socket may also
 * be closed directly. Calling close on either object is permitted - thus, {@code @MustCallAlias}.
 *
 * <p>When a programmer writes {@code @MustCallAlias} on a parameter p and the return of a
 * constructor C, verification of the annotation necessitates the following additional checks:
 *
 * <ul>
 *   <li>The constructor must always write p into exactly one field f of the new C object.
 *   <li>Field f must be annotated @Owning (which necessitates that a must-call method of C
 *       resolves's f's must-call obligations).
 * </ul>
 *
 * When a programmer writes {@code @MustCallAlias} on the receiver parameter p and the return of a
 * method M, then all return sites in M must be calls to other methods with equivalent
 * {@code @MustCallAlias} annotations, or calls to constructors that satisfy the rules described in
 * the preceding paragraph and are also annotated as {@code @MustCallAlias}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
// In Java 11, this can be:
// @Target({ElementType.PARAMETER, ElementType.CONSTRUCTOR, ElementType.METHOD})
@Target({ElementType.PARAMETER, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.TYPE_USE})
public @interface MustCallAlias {}
