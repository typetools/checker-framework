package org.checkerframework.dataflow.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A method is called <em>side-effect-free</em> if it has no visible side effects, such as setting a
 * field of an object that existed before the method was called.
 *
 * <p>Only the visible side effects are important. The method is allowed to cache the answer to a
 * computationally expensive query, for instance. It is also allowed to modify newly created
 * objects. A constructor is side-effect-free if it does not modify any objects that existed before
 * it was called in ways that are externally visible.
 *
 * <p>This annotation is important to pluggable type-checking because if some fact about an object
 * is known before a call to such a method, then the fact is still known afterwards, even if the
 * fact is about some non-final field. When any non-{@code @SideEffectFree} method is called, then a
 * pluggable type-checker must assume that any field of any accessible object might have been
 * modified, which annuls the effect of flow-sensitive type refinement and prevents the pluggable
 * type-checker from making conclusions that are obvious to a programmer.
 *
 * <p>Also see {@link Pure}, which means both side-effect-free and {@link Deterministic}.
 *
 * <p><b>Functional-interface parameters:</b> A side-effect-free method may run code that its caller
 * handed to it. If a parameter's type is a functional interface whose functional method (its single
 * abstract method, or SAM) is not itself annotated with a purity annotation, then
 * {@code @SideEffectFree} is also a requirement on the method's callers: the argument must denote a
 * value whose functional method is side-effect-free. In return, the body of the method may call
 * that parameter's functional method without thereby being impure.
 * <!-- "<code>" instead of "{@code ...}" because of at-sign at beginning of line -->
 *
 * <pre><code>@SideEffectFree
 * int apply(Function&lt;String, Integer&gt; f, String s) {
 *   return f.apply(s);  // permitted: the receiver is a formal parameter of this method
 * }
 * </code></pre>
 *
 * <p>and, at calls to {@code apply}:
 *
 * <pre>{@code
 * apply(s -> s.length(), "hi");  // OK: the lambda body is side-effect-free
 * apply(s -> count++, "hi");     // error: the lambda body has a side effect
 * }</pre>
 *
 * <p>The requirement is imposed on every parameter whose type is a functional interface, whether or
 * not the method's body ever calls it, so that a caller can read the requirement off the signature
 * alone. The requirement is imposed only when the functional method is unannotated; when the
 * functional method is itself annotated {@code @SideEffectFree} (or {@link SideEffectsOnly}), that
 * annotation governs both the call in the body and the obligation on the argument, exactly as for
 * any other method.
 *
 * <p><b>Analysis:</b> The Checker Framework performs a conservative analysis to verify a
 * {@code @SideEffectFree} annotation. The Checker Framework issues a warning if the method uses any
 * of the following Java constructs:
 *
 * <ol>
 *   <li>Assignment to any expression, except for local variables and method parameters.<br>
 *       (Note that storing into an array element, such as {@code a[i] = x}, is not an assignment to
 *       a variable and is therefore forbidden.)
 *   <li>A method invocation of a method that is not {@code @SideEffectFree}. As an exception, a
 *       call to the functional method of one of the method's own formal parameters is permitted, as
 *       described above. The receiver must be the formal parameter itself; a local variable that
 *       has been assigned the parameter, or a field that holds it, does not qualify. Nor does a
 *       call to a {@code default} or {@code static} method of the functional interface.
 *   <li>Construction of a new object where the constructor is not {@code @SideEffectFree}.
 * </ol>
 *
 * <p>Creating a lambda or an anonymous class is not itself a side effect, so a side-effect-free
 * method may create an impure one. The body of a lambda is checked against the purity annotations
 * on the functional method it implements, not against those on the method that creates it.
 *
 * <p>At each call to a {@code @SideEffectFree} method or constructor, the Checker Framework also
 * checks that every argument passed to a functional-interface parameter is side-effect-free. If the
 * argument is a lambda, its body is checked directly and no annotation on it is needed. If the
 * argument is a method reference, the referenced method must be {@code @SideEffectFree}. If the
 * argument is a functional-interface parameter of the enclosing method, and that method is itself
 * {@code @SideEffectFree}, then the enclosing method's own callers have already discharged the
 * requirement and the argument is permitted. Otherwise, the functional method of the argument's
 * declared type must be {@code @SideEffectFree}.
 *
 * <p>These rules are conservative: any code that passes the checks is side-effect-free, but the
 * Checker Framework may issue false positive warnings, for code that uses one of the forbidden
 * constructs but is side-effect-free nonetheless. In particular, a method that caches its result
 * will be rejected.
 *
 * <p>This annotation is inherited by subtypes, just as if it were meta-annotated with
 * {@code @InheritedAnnotation}.
 *
 * @checker_framework.manual #type-refinement-purity Side effects, determinism, purity, and
 *     flow-sensitive analysis
 */
// @InheritedAnnotation cannot be written here, because "dataflow" project cannot depend on
// "framework" project.
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface SideEffectFree {}
