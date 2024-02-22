package org.checkerframework.checker.calledmethodsonelements;

import org.checkerframework.common.accumulation.AccumulationChecker;

/**
 * The Called Methods On Elements Checker tracks the methods that have definitely been called on all
 * elements within an array annotated with @OwningArry.
 */
public class CalledMethodsOnElementsChecker extends AccumulationChecker {}
