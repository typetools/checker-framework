package org.checkerframework.framework.source;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;

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

    /** A set of trees that are annotated with {@code @SuppressWarnings}. */
    public final List<Tree> treesWithSuppressWarnings;

    /** Whether or not a warning should be issued for unneeded warning suppressions. * */
    private final boolean warnUnneededSuppressions;

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

        this.treesWithSuppressWarnings = new ArrayList<>();
        this.warnUnneededSuppressions = checker.hasOption("warnUnneededSuppressions");
    }

    /**
     * Set the CompilationUnitTree to be used during any visits. For any later calls of {@code
     * com.sun.source.util.TreePathScanner.scan(TreePath, P)}, the CompilationUnitTree of the
     * TreePath has to be equal to {@code root}.
     */
    public void setRoot(CompilationUnitTree root) {
        this.root = root;
    }

    /**
     * Store the last Tree visited by the SourceVisitor. This is necessary because the finally
     * blocks in {@link com.sun.source.util.TreePathScanner#scan(TreePath, Object)} and {@link
     * com.sun.source.util.TreePathScanner#scan(Tree, Object)} set the visited Path to null. This
     * field is used to report a rough location for the error in {@link
     * org.checkerframework.framework.source.SourceChecker#logBugInCF(BugInCF)}.
     */
    /*package-private*/ Tree lastVisited;

    /** Entry point for a type processor: the TreePath leaf is a top-level type tree within root. */
    public void visit(TreePath path) {
        lastVisited = path.getLeaf();
        this.scan(path, null);
    }

    @Override
    public R scan(Tree tree, P p) {
        lastVisited = tree;
        return super.scan(tree, p);
    }

    @Override
    public R visitClass(ClassTree classTree, P p) {
        storeSuppressWarningsAnno(classTree);
        return super.visitClass(classTree, p);
    }

    @Override
    public R visitVariable(VariableTree variableTree, P p) {
        storeSuppressWarningsAnno(variableTree);
        return super.visitVariable(variableTree, p);
    }

    @Override
    public R visitMethod(MethodTree node, P p) {
        storeSuppressWarningsAnno(node);
        return super.visitMethod(node, p);
    }

    /**
     * If {@code tree} has a {@code @SuppressWarnings} add it to treesWithSuppressWarnings.
     *
     * @param tree a declaration on which a {@code @SuppressWarnings} annotation may be placed.
     */
    private void storeSuppressWarningsAnno(Tree tree) {
        if (!warnUnneededSuppressions) {
            return;
        }
        Element elt = TreeUtils.elementFromTree(tree);
        if (elt.getAnnotation(SuppressWarnings.class) != null) {
            treesWithSuppressWarnings.add(tree);
        }
    }
}
