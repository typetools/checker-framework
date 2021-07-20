package org.checkerframework.common.util.debug;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree.JCNewArray;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

/**
 * A utility class for displaying the structure of the AST of a program.
 *
 * <p>The class is actually an annotation processor; in order to use it, invoke the compiler on the
 * source file(s) for which you wish to view the structure of the program. You may also wish to use
 * the {@code -proc:only} javac option to stop compilation after annotation processing. (But, in
 * general {@code -proc:only} causes type annotation processors not to be run.)
 *
 * <p>The utility will display the {@link Kind} of each node it encounters while scanning the AST,
 * indented according to its depth in the tree. Additionally, the names of identifiers and member
 * selection trees are displayed (since these names are not tree nodes and therefore not directly
 * visited during AST traversal).
 *
 * @see org.checkerframework.common.util.debug.TreePrinter
 */
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class TreeDebug extends AbstractProcessor {

    protected Visitor createSourceVisitor(CompilationUnitTree root) {
        return new Visitor();
    }

    private static final String LINE_SEPARATOR = System.lineSeparator();

    public static class Visitor extends TreePathScanner<Void, Void> {

        private final StringBuilder buf;

        public Visitor() {
            buf = new StringBuilder();
        }

        @Override
        public Void scan(Tree node, Void p) {

            // Indent according to subtrees.
            if (getCurrentPath() != null) {
                for (TreePath tp = getCurrentPath(); tp != null; tp = tp.getParentPath()) {
                    buf.append("  ");
                }
            }

            // Add node kind to the buffer.
            if (node == null) {
                buf.append("null");
            } else {
                buf.append(node.getKind());
            }
            buf.append(LINE_SEPARATOR);

            // Visit subtrees.
            super.scan(node, p);

            // Display and clear the buffer.
            System.out.print(buf.toString());
            buf.setLength(0);

            return null;
        }

        /**
         * Splices additional information for a node into the buffer.
         *
         * @param text additional information for the node
         */
        private final void insert(Object text) {
            buf.insert(buf.length() - 1, " ");
            buf.insert(buf.length() - 1, text);
        }

        @Override
        public Void visitIdentifier(IdentifierTree node, Void p) {
            insert(node);
            return super.visitIdentifier(node, p);
        }

        @Override
        public Void visitMemberSelect(MemberSelectTree node, Void p) {
            insert(node.getExpression() + "." + node.getIdentifier());
            return super.visitMemberSelect(node, p);
        }

        @Override
        public Void visitNewArray(NewArrayTree node, Void p) {
            insert(((JCNewArray) node).annotations);
            insert("|");
            insert(((JCNewArray) node).dimAnnotations);
            return super.visitNewArray(node, p);
        }

        @Override
        public Void visitLiteral(LiteralTree node, Void p) {
            insert(node.getValue());
            return super.visitLiteral(node, p);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement element : ElementFilter.typesIn(roundEnv.getRootElements())) {
            TreePath path = Trees.instance(processingEnv).getPath(element);
            new Visitor().scan(path, null);
        }
        return false;
    }
}
