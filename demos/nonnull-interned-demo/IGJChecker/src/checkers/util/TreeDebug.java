package checkers.util;

import checkers.source.*; 

import com.sun.source.tree.*;
import com.sun.tools.javac.tree.JCTree.JCNewArray;

import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;

/**
 * A utility class for displaying the structure of the AST of a program.
 * 
 * <p>
 * 
 * The class is actually an annotation processor; in order to use it, invoke the
 * compiler on the source file(s) for which you wish to view the structure of
 * the program.
 * 
 * <p>
 * 
 * The utility will display the {@link com.sun.source.tree.Tree.Kind} of each
 * node it encounters while scanning the AST, indented according to its depth in
 * the tree. Additionally, the names of identifiers and member selection trees
 * are displayed (since these names are not tree nodes and therefore not
 * directly visited during AST traversal).
 * 
 */
@SupportedAnnotationTypes({"*"})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class TreeDebug extends SourceChecker {

    @Override
    protected Properties getMessages() {
        return new Properties();
    }

    @Override
    protected SourceVisitor<?, ?> getSourceVisitor(CompilationUnitTree root) {
        return new Visitor(this, root);
    }

    public class Visitor extends SourceVisitor<Void, Void> {

        private StringBuffer buf; 

        public Visitor(TreeDebug td, CompilationUnitTree root) {
            super(td, root);
            buf = new StringBuffer();
        }

        @Override
        public Void scan(Tree node, Void p) {

            // Indent according to subtrees.
            if (getCurrentPath() != null) {
                for (Tree t : getCurrentPath()) 
                    buf.append("  ");
            }

            // Add node kind to the buffer.
            if (node == null)
                buf.append("null");
            else 
                buf.append(node.getKind());
            buf.append("\n");

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
            insert(((JCNewArray)node).dimAnnotations);
            return super.visitNewArray(node, p);
        }
    }
}
