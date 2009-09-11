package checkers.source;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.util.*;

import checkers.quals.*;
import checkers.types.AnnotatedTypeFactory;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.*;

/**
 * An AST visitor that provides a variety of compiler utilities and interfaces
 * to facilitate typechecking.
 *
 * <p>
 *
 * The visitor does not return values or utilize the extra parameter provided
 * by {@link TreeScanner} (the class type parameters have type {@link Void});
 * instead, checking is performed within each method and errors are reported as
 * errors are found.
 */
@DefaultQualifier("checkers.nullness.quals.NonNull")
public abstract class SourceVisitor<R, P> extends TreePathScanner<R, P> {

    /** The {@link SourceChecker} to invoke on the input source tree. */
    protected final SourceChecker checker;

    /** The {@link Trees} instance to use for scanning. */
    protected final Trees trees;

    /** The {@link Elements} helper to use when scanning. */
    protected final Elements elements;

    /** The {@link Types} helper to use when scanning. */
    protected final Types types;

    /** The root of the AST that this {@link SourceVisitor} will scan. */
    protected final CompilationUnitTree root;

    /** The factory to use for obtaining "parsed" version of annotations. */
    protected final AnnotatedTypeFactory factory;

    /**
     * Creates a {@link SourceVisitor} to use for scanning a source tree.
     *
     * @param checker the checker to invoke on the input source tree
     * @param root the AST root that this scanner will check against
     */
    public SourceVisitor(SourceChecker checker, CompilationUnitTree root) {
        this.checker = checker;
        this.root = root;

        // Use the checker's processing environment to get the helpers we need.
        ProcessingEnvironment env = checker.getProcessingEnvironment();

        @Nullable Trees trees = Trees.instance(env);
        assert trees != null; /*nninvariant*/
        this.trees = trees;

        this.elements = env.getElementUtils();
        this.types = env.getTypeUtils();

        // Ask the checker for the AnnotatedTypeFactory.
        this.factory = checker.getFactory(env, root);
    }
}
