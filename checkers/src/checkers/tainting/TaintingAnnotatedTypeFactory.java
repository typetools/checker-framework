/*
package checkers.tainting;

import com.sun.source.tree.*;

import checkers.basetype.BaseTypeChecker;
import checkers.tainting.quals.Untainted;
import checkers.types.BasicAnnotatedTypeFactory;
import checkers.types.TreeAnnotator;
*/

/**
 * Adds implicit and default {@code Untainted} annotation, only if the user
 * does not explicitly insert them.
 * <p/>
 *
 * This factory will add the {@link Untainted} annotation to a type if the
 * input is
 *
 * <ol>
 * <li value="1">a string literal (Note: Handled by Unqualified meta-annotation)
 * <li value="2">a string concatenation where both operands are untainted
 * </ol>
 *
 */
/*
 * This class no longer performs any special functionality, as
 * superclasses where changed.
 * Currently both binary/compound ops on reference types and primitive
 * types give the LUB of the arguments. If there needs to be a difference
 * we could add a TreeAnnotator again.

public class TaintingAnnotatedTypeFactory
  extends BasicAnnotatedTypeFactory<TaintingChecker> {

    public TaintingAnnotatedTypeFactory(TaintingChecker checker,
            CompilationUnitTree root) {
        super(checker, root);
        this.postInit();
    }
}
*/