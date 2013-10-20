package checkers.source;

/*>>>
import checkers.nullness.quals.*;
*/

import javacutils.ErrorReporter;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;

/**
 * An AST visitor that provides a variety of compiler utilities and interfaces
 * to facilitate type-checking.
 */
public abstract class SourceVisitor<R, P>
        extends TreePathScanner<R, P> {

    /** The {@link Trees} instance to use for scanning. */
    protected final Trees trees;

    /** The {@link Elements} helper to use when scanning. */
    protected final Elements elements;

    /** The {@link Types} helper to use when scanning. */
    protected final Types types;

    /** The root of the AST that this {@link SourceVisitor} will scan. */
    protected CompilationUnitTree root;


    /**
     * Creates a {@link SourceVisitor} to use for scanning a source tree.
     *
     * @param checker the checker to invoke on the input source tree
     */
    public SourceVisitor(SourceChecker checker) {
        // Use the checker's processing environment to get the helpers we need.
        ProcessingEnvironment env = checker.getProcessingEnvironment();

        this.trees = Trees.instance(env);
        this.elements = env.getElementUtils();
        this.types = env.getTypeUtils();

        // Install the SourceChecker as the error handler
        // TODO: having this static state is ugly. Use the context to instantiate.
        ErrorReporter.setHandler(checker);
    }

    // Entry point for visitors, called once per
    // CompilationUnitTree.
    public R visit(CompilationUnitTree root, TreePath path, P p) {
        this.root = root;
        return this.scan(path, p);
    }
}
