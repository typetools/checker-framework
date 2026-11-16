package org.checkerframework.checker.testchecker.ainfer.qual;

import org.checkerframework.framework.qual.IgnoreInWholeProgramInference;

/**
 * Toy type system for testing field inference.
 *
 * @see AinferSibling1
 * @see AinferSibling2
 * @see AinferParent
 */
@IgnoreInWholeProgramInference
public @interface AinferToIgnore {}
