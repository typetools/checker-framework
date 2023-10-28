package org.checkerframework.checker.mustcall.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This polymorphic annotation represents an either-or must-call obligation. This annotation should
 * always be used in pairs. On a method, it is written on some formal parameter type and on the
 * method return type. On a constructor, it is written on some formal parameter type and on the
 * result type. Fulfilling the must-call obligation of one is equivalent to fulfilling the must-call
 * obligation of the other. Beyond its impact as a polymorphic annotation on {@code MustCall} types,
 * the Resource Leak Checker uses {@link MustCallAlias} annotations to more precisely determine when
 * a must-call obligation has been satisfied.
 *
 * <p>This annotation is useful for wrapper objects. For example, consider the declaration of {@code
 * java.net.Socket#getOutputStream}:
 *
 * <pre>
 * &#64;MustCall("close")
 * class Socket {
 *   &#64;MustCallAlias OutputStream getOutputStream(&#64;MustCallAlias Socket this) { ... }
 * }</pre>
 *
 * Calling {@code close()} on the returned {@code OutputStream} will close the underlying socket,
 * but the Socket may also be closed directly, which has the same effect.
 *
 * <h2>Type system semantics</h2>
 *
 * Within the Must Call Checker's type system, {@code @MustCallAlias} annotations have a semantics
 * different from a standard polymorphic annotation, in that the relevant actual parameter type and
 * return type at a call site are not equated in all cases. Given an actual parameter {@code p}
 * passed in a {@code @MustCallAlias} position at a call site, the return type of the call is
 * defined as follows:
 *
 * <ul>
 *   <li>If the base return type has a non-empty {@code @InheritableMustCall("m")} annotation on its
 *       declaration, and {@code p} has a non-empty {@code @MustCall} type, then the return type is
 *       {@code @MustCall("m")}.
 *   <li>In all other cases, the return type has the same {@code @MustCall} type as {@code p}.
 * </ul>
 *
 * {@link PolyMustCall} has an identical type system semantics. This special treatment is required
 * to allow for a wrapper object to have a must-call method with a different name than the must-call
 * method name for the wrapped object.
 *
 * <h2>Verifying {@code @MustCallAlias} annotations</h2>
 *
 * Suppose that {@code @MustCallAlias} is written on the type of formal parameter {@code p}.
 *
 * <p>For a constructor:
 *
 * <ul>
 *   <li>The constructor must always write p into exactly one field {@code f} of the new object.
 *   <li>Field {@code f} must be annotated {@code @}{@link Owning}.
 * </ul>
 *
 * For a method:
 *
 * <ul>
 *   <li>All return sites must be calls to other methods or constructors with {@code @MustCallAlias}
 *       return types, and this method's {@code @MustCallAlias} parameter must be passed in the
 *       {@code MustCallAlias} position to that method or constructor (i.e., the calls must pass
 *       {@code @MustCallAlias} parameter through a chain of {@code @MustCallAlias}-annotated
 *       parameters and returns).
 * </ul>
 *
 * When the -AnoResourceAliases command-line argument is passed to the checker, this annotation is
 * treated identically to {@link PolyMustCall}. That is, the annotation still impacts {@link
 * MustCall} types as a polymorphic annotation (see "Type system semantics" above), but it is not
 * used by the Resource Leak Checker to more precisely reason about when obligations have been
 * satisfied.
 *
 * @checker_framework.manual #resource-leak-checker Resource Leak Checker
 * @checker_framework.manual #qualifier-polymorphism Qualifier polymorphism
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
// In Java 11, this can be:
// @Target({ElementType.PARAMETER, ElementType.CONSTRUCTOR, ElementType.METHOD})
@Target({ElementType.PARAMETER, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.TYPE_USE})
public @interface MustCallAlias {}
