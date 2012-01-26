package checkers.javari.quals;

import java.lang.annotation.*;

import checkers.javari.JavariChecker;
import checkers.quals.*;

/**
 * Specifies the allowed mutabilities of a method's return value or the arguments,
 * based on the mutability type of the arguments and the receiver at a method
 * invocation. {@link PolyRead} has the same behavior as creating two copies
 * of the method signature, one where all of its occurrences are substituted with
 * {@link ReadOnly}, and one where all of its occurrences are substituted with
 * {@link Mutable}; that is, if it were possible to have annotation overloading,
 * <pre>
 * &#064;PolyRead getA() &#064;PolyRead {return a;}
 * </pre>
 * would be equivalent to
 * <pre>
 * &#064;ReadOnly getA() &#064;ReadOnly {return a;}
 * getA() {return a;}
 * </pre>
 *
 * As a first example, if {@link PolyRead} appears in the return type of a
 * method, at the method invocation it will be interpreted as {@link
 * ReadOnly} if any the arguments passed to parameter annotated with
 * {@link ReadOnly} is a readonly instance, or if the receiver type
 * is readonly and the method is invoked from a readonly context. That is,
 * <pre>
 *  &#064;PolyRead aTestMethod(String a,
 *                            &#064;PolyRead Object b,
 *                            List<&#064;PolyRead Date> c) &#064;PolyRead
 * </pre>
 * has a readonly return type if the argument passed as b is readonly,
 * or if the argument passed as c is a list of readonly Dates, or if
 * the aTestMethod is invoked from a readonly receiver. Otherwise, it
 * has a mutable return type.
 *
 * As a second example, if the receiver type of a constructor is
 * annotated with {@link PolyRead}, the created instance will be
 * readonly if any of the arguments passed to parameters annotated
 * with {@link PolyRead} is readonly, and it will be mutable
 * otherwise. That is,
 * <pre>
 *  public Something(String a,
 *                   &#064;PolyRead Object b,
 *                   List<&#064;PolyRead Date> c) &#064;PolyRead
 * </pre>
 * instantiates a readonly Something if a readonly argument is passed
 * as b, or if the argument passed as c is a list of readonly
 * Dates. Otherwise, it instantiates a mutable Something.
 *
 * As a third example, if the return type of a method is not annotated
 * anywhere with {@link PolyRead}, but its receiver type and some of
 * its parameters are, then, at a mutable instance, only mutable
 * arguments are accepted; at a readonly instance, both types of
 * arguments are accepted. That is,
 * <pre>
 *  aTestMethod(String a,
 *              &#064;PolyRead Object b,
 *              List<&#064;PolyRead Date> c) &#064;PolyRead
 * </pre>
 * when invoked from a mutable reference will only accept mutable
 * arguments as b, and lists of mutable Dates as c. When aTestMethod
 * is invoked from a readonly reference, it will accept readonly
 * arguments as b, and lists of readonly arguments as c.
 *
 * Since the code must be legal at both "overloaded" cases, parameters
 * annotated with {@link PolyRead} suffer the same restrictions inside
 * the method body as parameters annotated with {@link ReadOnly}, and
 * methods with receiver type annotated as {@link PolyRead} suffer the
 * same restrictions as methods with receiver type annotated as {@link
 * ReadOnly}.
 *
 * <p>
 *
 * This annotation is part of the Javari language.
 *
 * @see JavariChecker
 * @checker.framework.manual #javari-checker Javari Checker
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@TypeQualifier
@PolymorphicQualifier
public @interface PolyRead {}
