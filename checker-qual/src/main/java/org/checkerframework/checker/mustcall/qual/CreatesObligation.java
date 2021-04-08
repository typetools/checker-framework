package org.checkerframework.checker.mustcall.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.InheritedAnnotation;
import org.checkerframework.framework.qual.JavaExpression;

/**
 * Indicates that the method resets the must-call type on the target (the value argument) to the
 * greatest lower bound of its current must-call type and its declared must-call type. This
 * effectively undoes flow-sensitive type refinement.
 *
 * <p>It is an error to call a method annotated by this annotation if the target's declared
 * CalledMethods type is non-empty (i.e. its type is not top in the CalledMethods hierarchy).
 *
 * <p>It is an error to fail to write this annotation on any method that (re-)assigns a non-final,
 * owning field whose declared type has a must-call obligation. It is an error if the target of the
 * {@literal @}CreatesObligation annotation on the re-assigning method is not the string "this".
 *
 * <p>This annotation is trusted, not checked. (Because this annotation can only add obligations,
 * the analysis remains sound.)
 *
 * <p>For example, consider the following code, which uses an {@literal @}CreatesObligation
 * annotation to indicate that the {@code reset()} method re-assigns the {@code socket} field:
 *
 * <pre>
 * @MustCall("stop")
 * class SocketContainer {
 *     private @Owning @MustCall("close") Socket socket = ...;
 *
 *     @EnsuresCalledMethods(value="this.socket", methods="close")
 *     public void stop() throws IOException {
 *       socket.close();
 *     }
 *
 *    @CreatesObligation("this")
 *    public void reset() {
 *      if (socket.isClosed()) {
 *        socket = new Socket(...);
 *      }
 *    }
 * }
 * </pre>
 *
 * A client of {@code SocketContainer} is permitted to call {@code reset()} arbitrarily-many times.
 * Each time it does so, a new {@code Socket} might be created, so only after the last call to
 * reset() may a {@code SocketContainer}'s must-call obligation of "stop" be fulfilled. The
 * {@literal @}CreatesObligation annotation on reset's declaration enforces this requirement: at any
 * call to reset(), all called-methods information about the receiver is removed from the stores of
 * the Must Call and Called Methods Checkers, so the client has to "start over" as if a fresh {@code
 * SocketContainer} object had been created.
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
