package org.checkerframework.framework.testchecker.singlepassdefault;

import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * A checker whose precedence lists exercise the case in which applying every default in one
 * traversal of a type would not give the same result as traversing the type once per default.
 */
public class SinglePassDefaultChecker extends BaseTypeChecker {}
