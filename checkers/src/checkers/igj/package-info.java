/**
 * Provides a typechecker plug-in for the IGJ {@link checkers.igj.quals}
 * qualifiers that finds (and verifies the absence of) immutability errors.
 *
 * The checker guarantees that no immutable object is modified and no object
 * is mutated through a read-only reference.
 *
 * @see checkers.igj.IGJChecker
 * @checker.framework.manual #igj-checker IGJ Checker
 */
package checkers.igj;
