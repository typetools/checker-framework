package org.checkerframework.checker.testchecker.ainfer.qual;

import org.checkerframework.framework.qual.IgnoreInWholeProgramInference;

/**
 * Toy type system for testing field inference.
 *
 * @see AinferSibling1, AinferSibling2, AinferParent
 */
@IgnoreInWholeProgramInference
public @interface AinferToIgnore {}
