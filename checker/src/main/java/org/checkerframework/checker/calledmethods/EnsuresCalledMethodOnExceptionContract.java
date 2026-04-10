package org.checkerframework.checker.calledmethods;

import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethodsOnException;

/**
 * A postcondition contract that a method calls the given method on the given expression when that
 * method throws an exception.
 *
 * <p>Instances of this class are plain old immutable data with no interesting behavior.
 *
 * @param expression The expression described by this postcondition.
 * @param method The method this postcondition promises to call.
 * @see EnsuresCalledMethodsOnException
 */
public record EnsuresCalledMethodOnExceptionContract(String expression, String method) {}
