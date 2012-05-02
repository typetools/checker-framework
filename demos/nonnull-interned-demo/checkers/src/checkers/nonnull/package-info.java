/**
 * Contains an implementation of a plugin for the {@code @NonNull}
 * type annotation, which indicates that a variable should never have a null 
 * value. The plugin issues a warning whenever a variable that has been declared
 * with a {@code @NonNull} annotation may become null.
 *
 * @see <a
 * href="http://pag.csail.mit.edu/jsr308/dist/README-checkers.html">README-checkers.html</a>
 * @see <a
 * href="http://pag.csail.mit.edu/jsr308/dist/nonnull-checker.html">nonnull-checker.html</a> in the checkers distribution
 */
package checkers.nonnull;
