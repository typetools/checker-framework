package org.checkerframework.checker.testchecker.wholeprograminference.qual;

import org.checkerframework.framework.qual.IgnoreInWholeProgramInference;

/**
 * Toy type system for testing field inference.
 *
 * @see Sibling1, Sibling2, Parent
 */
@IgnoreInWholeProgramInference
public @interface ToIgnore {}
