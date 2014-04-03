package org.checkerframework.checker.qualparam;

/**
 * An enum to represent the type of bound used in a wildcard.  This is not used
 * directly by the qualifier parameter framework, but it is expected to be
 * useful for annotations in checkers using qualifier parameters.
 */
public enum WildcardBound {
    UNBOUNDED,
    EXTENDS,
    SUPER
}
