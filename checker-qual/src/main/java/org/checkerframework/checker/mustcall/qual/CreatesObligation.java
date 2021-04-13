package org.checkerframework.checker.mustcall.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.checker.calledmethods.qual.CalledMethods;
import org.checkerframework.framework.qual.InheritedAnnotation;
import org.checkerframework.framework.qual.JavaExpression;

/**
 * Indicates that the method resets the must-call type on the target (the value argument) to the
 * least upper bound of its current must-call type and its declared must-call type. This effectively
 * undoes flow-sensitive type refinement.
 *
 * <p>When calling a method annotated as {@code @CreatesObligation("}<em>target</em>{@code ")}, the
 * expression {@code target} must not have type {@code @}{@link CalledMethods}{@code ({})}. That is,
 * {@code target}'s CalledMethods type must be non-empty.
 *
 * <p>{@code @CreatesObligation("this")} must be written on any method that assigns a non-final,
 * owning field whose declared type has a must-call obligation.
 *
 * <p>This annotation is trusted, not checked. (Because this annotation can only add obligations,
 * the analysis remains sound.)
 *
 * <p>For example, consider the following code, which uses a {@code @CreatesObligation} annotation
 * to indicate that the {@code reset()} method re-assigns the {@code socket} field:
 *
 * <pre>
 * &#64;MustCall("stop")
 * class SocketContainer {
 *     private @Owning @MustCall("close") Socket socket = ...;
 *
 *     &#64;EnsuresCalledMethods(value="this.socket", methods="close")
 *     public void stop() throws IOException {
 *       socket.close();
 *     }
 *
 *    &#64;CreatesObligation("this")
 *    public void reset() {
 *      if (socket.isClosed()) {
 *        socket = new Socket(...);
 *      }
 *    }
 * }
 * </pre>
 *
 * A client of {@code SocketContainer} is permitted to call {@code reset()} arbitrarily many times.
 * Each time it does so, a new {@code Socket} might be created, so only after the last call to
 * {@code reset()} is a client permitted to fulfill a {@code SocketContainer}'s must-call obligation
 * of "stop". The {@code @CreatesObligation} annotation on reset's declaration enforces this
 * requirement: at any call to {@code reset()}, all called-methods information about the receiver is
 * removed from the store of the Must Call Checker and the store of the Called Methods Checker, so
 * the client has to "start over" as if a fresh {@code SocketContainer} object had been created.
 *
 * @checker_framework.manual #must-call-checker Must Call Checker
 */
@Target({ElementType.METHOD})
@InheritedAnnotation
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(CreatesObligation.List.class)
public @interface CreatesObligation {

  /**
   * The target of this annotation is stored in this field. The target must be an expression which
   * can be refined in the store, such as a local variable or field.
   *
   * @return the expression to which must-call obligations are added when the annotated method is
   *     invoked
   */
  @JavaExpression
  String value() default "this";

  /**
   * A wrapper annotation that makes the {@link CreatesObligation} annotation repeatable.
   *
   * <p>Programmers generally do not need to write this. It is created by Java when a programmer
   * writes more than one {@link CreatesObligation} annotation at the same location.
   *
   * @checker_framework.manual #must-call-checker Must Call Checker
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD})
  @InheritedAnnotation
  @interface List {
    /**
     * Return the repeatable annotations.
     *
     * @return the repeatable annotations
     */
    CreatesObligation[] value();
  }
}
