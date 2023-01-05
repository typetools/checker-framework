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
 * Indicates that the method resets the expression's must-call type to its declared type. This
 * effectively undoes flow-sensitive type refinement. The expression is the {@code value}
 * argument/element. More precisely, a call to a method annotated by this annotation changes the
 * expression's must-call type to the least upper bound of its current must-call type and its
 * declared must-call type.
 *
 * <p>When calling a method annotated as {@code @CreatesMustCallFor("}<em>expression</em>{@code ")},
 * the {@code expression}'s static type in the Called Methods type system must be {@code @}{@link
 * CalledMethods}{@code ({})}. That is, {@code expression}'s CalledMethods type must be empty.
 *
 * <p>{@code @CreatesMustCallFor("obj")} must be written on any method that assigns a non-final,
 * owning field of {@code obj} whose declared type has a must-call obligation.
 *
 * <p>Because this annotation can only add obligations, it can be written safely on any method, even
 * one that does not actually create a new obligation. Writing this annotation on a method that does
 * not actually create any new obligations may lead to false alarms (warnings at correct code), but
 * never to missed alarms (lack of warnings at incorrect code).
 *
 * <p>As an example, consider the following code, which uses a {@code @CreatesMustCallFor}
 * annotation to indicate that the {@code reset()} method re-assigns the {@code socket} field:
 *
 * <pre>
 * &#64;MustCall("stop")
 * class SocketContainer {
 *     // Note that @MustCall("close") is the default type for java.net.Socket, but it
 *     // is included on the next line for illustrative purposes. This example would function
 *     // identically if that qualifier were omitted.
 *     private @Owning @MustCall("close") Socket socket = ...;
 *
 *     &#64;EnsuresCalledMethods(value="this.socket", methods="close")
 *     public void stop() throws IOException {
 *       socket.close();
 *     }
 *
 *    &#64;CreatesMustCallFor("this")
 *    public void reset() {
 *      if (socket.isClosed()) {
 *        socket = new Socket(...);
 *      }
 *    }
 * }
 * </pre>
 *
 * A client of {@code SocketContainer} is permitted to call {@code reset()} arbitrarily many times.
 * Each time it does so, a new {@code Socket} might be created. A {@code SocketContainer}'s
 * must-call obligation of "stop" is fulfilled only if {@code stop()} is called after the last call
 * to {@code reset()}. The {@code @CreatesMustCallFor} annotation on {@code reset()}'s declaration
 * enforces this requirement: at any call to {@code reset()}, all called-methods information about
 * the receiver is removed from the store of the Must Call Checker and the store of the Called
 * Methods Checker, so the client has to "start over" as if a fresh {@code SocketContainer} object
 * had been created.
 *
 * <p>When the {@code -AnoCreatesMustCallFor} command-line argument is passed to the checker, this
 * annotation is ignored and all fields are treated as non-owning.
 *
 * @checker_framework.manual #resource-leak-checker Resource Leak Checker
 */
@Target({ElementType.METHOD})
@InheritedAnnotation
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(CreatesMustCallFor.List.class)
public @interface CreatesMustCallFor {

  /**
   * Returns the expression whose must-call type is reset after a call to a method with this
   * annotation. The expression must be visible in the scope immediately before each call site, so
   * it can only refer to fields, the method's parameters (which should be referenced via the "#X"
   * syntax, where "#1" is the first argument, #2 is the second, etc.), or {@code "this"}. The
   * default is {@code "this"}. At call-sites, the viewpoint-adapted referent of expression must be
   * owning (an owning field, a local variable tracked in a resource alias set, etc.) or a {@code
   * reset.not.owning} error is issued.
   *
   * @return the expression to which must-call obligations are added when the annotated method is
   *     invoked
   */
  @JavaExpression
  String value() default "this";

  /**
   * A wrapper annotation that makes the {@link CreatesMustCallFor} annotation repeatable.
   *
   * <p>Programmers generally do not need to write this. It is created by Java when a programmer
   * writes more than one {@link CreatesMustCallFor} annotation at the same location.
   *
   * @checker_framework.manual #must-call-checker Must Call Checker
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD})
  @InheritedAnnotation
  public static @interface List {
    /**
     * Return the repeatable annotations.
     *
     * @return the repeatable annotations
     */
    CreatesMustCallFor[] value();
  }
}
