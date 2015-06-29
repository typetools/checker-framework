/**
 * Provides a type-checker plug-in for the IGJ {@link org.checkerframework.checker.igj.qual}
 * qualifiers that finds (and verifies the absence of) immutability errors.
 *
 * The checker guarantees that no immutable object is modified and no object
 * is mutated through a read-only reference.
 *
 * @see org.checkerframework.checker.igj.IGJChecker
 * @checker_framework.manual #igj-checker IGJ Checker
 */
package org.checkerframework.checker.igj;
