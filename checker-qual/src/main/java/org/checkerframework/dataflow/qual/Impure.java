package org.checkerframework.dataflow.qual;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code Impure} is a method annotation that means neither {@link SideEffectFree} and {@link
 * Deterministic} (i.e., not {@link Pure}).
 *
 * <p>This annotation should not be written by a programmer (leaving a method unannotated is
 * equivalent to writing this annotation), but it may be inferred by tools. Conceptually, it
 * completes the "lattice" of purity annotations by serving as a top element.
 *
 * <p>That is, any {@code Pure} method can be treated as {@code SideEffectFree} or {@code
 * Determinsitic}, any {@code Deterministic} method can be treated as {@code Impure}, etc. (Liskov
 * substitutability for method implementations). The completeness of this lattice is necessary for
 * practical inference of the purity of chains of overridden methods.
 *
 * @checker_framework.manual #type-refinement-purity Side effects, determinism, purity, and
 *     flow-sensitive analysis
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface Impure {}
