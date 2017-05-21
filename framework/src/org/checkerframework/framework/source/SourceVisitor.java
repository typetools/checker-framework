package org.checkerframework.framework.source;

/*>>>
import org.checkerframework.checker.nullness.qual.*;
*/

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.checkerframework.javacutil.ErrorReporter;

/**
 * An AST visitor that provides a variety of compiler utilities and interfaces to facilitate
 * type-checking.
 */
public abstract class SourceVisitor<R, P> extends TreePathScanner<R, P> {

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

    /*
     * Set the CompilationUnitTree to be used during any visits.
     * For any later calls of {@see com.sun.source.util.TreePathScanner.scan(TreePath, P)},
     * the CompilationUnitTree of the TreePath has to be equal to {@code root}.
     */
    public void setRoot(CompilationUnitTree root) {
        this.root = root;
    }

    /*
     * Entry point for a type processor: the TreePath leaf is
     * a top-level type tree within root.
     */
    public void visit(TreePath path) {
        this.scan(path, null);
    }
}
