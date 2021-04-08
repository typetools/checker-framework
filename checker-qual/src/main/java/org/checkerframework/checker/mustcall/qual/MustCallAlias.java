package org.checkerframework.checker.mustcall.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This polymorphic annotation represents an either-or must-call obligation. This annotation should
 * always be used in pairs: one on a method receiver parameter or constructor parameter, and the
 * other on the method return type. Fulfilling the must-call obligation of one is equivalent to
 * fulfilling the must-call obligation of the other.
 *
 * <p>This annotation is useful for wrapper objects. For example, consider the declaration of {@code
 * java.net.Socket#getOutputStream}:
 *
 * <pre>
 * @MustCall("close")
 * class Socket {
 *   @MustCallAlias OutputStream getOutputStream(@MustCallAlias Socket this) { ... }
 * }
 * </pre>
 *
 * When a client calls {@code getOutputStream} on the Socket creates a new {@code OutputStream}
 * object that wraps the Socket. Calling its {@code close()} method will close the underlying
 * socket, but the Socket may also be closed directly. Calling close on either object is permitted
 * -- thus, both the receiver and return type above are annotated as {@code @MustCallAlias}.
 *
 * <h3>Verifying {@code @MustCallAlias} annotations</h3>
 *
 * Suppose that {@code @MustCallAlias} is written on the type of parameter {@code p}.
 *
 * <p>For a constructor:
 *
 * <ul>
 *   <li>The constructor must always write p into exactly one field {@code f} of the new object.
 *   <li>Field {@code f} must be annotated {@code @}{@link Owning} (which necessitates that a
 *       must-call method of the object being constructed resolves's {@code f}'s must-call
 *       obligations).
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
 * @checker_framework.manual #must-call-checker Must Call Checker
 * @checker_framework.manual #qualifier-polymorphism Qualifier polymorphism
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
// In Java 11, this can be:
// @Target({ElementType.PARAMETER, ElementType.CONSTRUCTOR, ElementType.METHOD})
@Target({ElementType.PARAMETER, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.TYPE_USE})
public @interface MustCallAlias {}
