/**
 * Contains the essential functionality for interfacing a compile-time (source)
 * typechecker plug-in to the Java compiler. This allows a checker to use
 * the compiler's error reporting mechanism and to access abstract syntax trees
 * and compiler utility classes.
 *
 * <p>
 *
 * Most classes won't want to extend the classes in this package directly; the
 * classes in the {@link checkers.basetype} package provide subtype checking
 * functionality.
 *
 * @see checkers.basetype
 * @checker.framework.manual #writing-a-checker Writing a Checker
 */
package checkers.source;
