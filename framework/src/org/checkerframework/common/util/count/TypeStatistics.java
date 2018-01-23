package org.checkerframework.common.util.count;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.TypeCastTree;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.source.SourceVisitor;
import org.checkerframework.javacutil.AnnotationProvider;
import org.checkerframework.javacutil.TreeUtils;

/**
 * An annotation processor for listing the facts about types and array accesses locations of
 * annotations. To invoke it, use
 *
 * <pre>
 * javac -proc:only -processor org.checkerframework.common.util.count.TypeStatistics <em>MyFile.java ...</em>
 * </pre>
 */
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class TypeStatistics extends SourceChecker {

    int generics = 0;
    int arrayAccesses = 0;
    int typecasts = 0;

    @Override
    public void typeProcessingOver() {
        System.out.printf("Found %d generic type uses.\n", generics);
        System.out.printf("Found %d array accesses and creations.\n", arrayAccesses);
        System.out.printf("Found %d typecasts.\n", typecasts);
    }

    @Override
    protected SourceVisitor<?, ?> createSourceVisitor() {
        return new Visitor(this);
    }

    class Visitor extends SourceVisitor<Void, Void> {

        public Visitor(TypeStatistics l) {
            super(l);
        }

        @Override
        public Void visitClass(ClassTree tree, Void p) {
            if (shouldSkipDefs(tree)) {
                // Not "return super.visitClass(classTree, p);" because that would
                // recursively call visitors on subtrees; we want to skip the
                // class entirely.
                return null;
            }
            generics += tree.getTypeParameters().size();
            return super.visitClass(tree, p);
        }

        @Override
        public Void visitNewArray(NewArrayTree node, Void aVoid) {
            arrayAccesses += node.getDimensions().size();

            return super.visitNewArray(node, aVoid);
        }

        @Override
        public Void visitNewClass(NewClassTree node, Void aVoid) {
            // TODO: diamonds
            if (TreeUtils.isDiamondTree(node)) {
                generics++;
            }
            generics += node.getTypeArguments().size();
            return super.visitNewClass(node, aVoid);
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree node, Void aVoid) {
            generics += node.getTypeArguments().size();
            return super.visitMethodInvocation(node, aVoid);
        }

        @Override
        public Void visitMethod(MethodTree node, Void aVoid) {
            generics += node.getTypeParameters().size();
            return super.visitMethod(node, aVoid);
        }

        @Override
        public Void visitParameterizedType(ParameterizedTypeTree tree, Void p) {
            generics += tree.getTypeArguments().size();
            return super.visitParameterizedType(tree, p);
        }

        @Override
        public Void visitArrayAccess(ArrayAccessTree node, Void aVoid) {
            arrayAccesses++;
            return super.visitArrayAccess(node, aVoid);
        }

        @Override
        public Void visitTypeCast(TypeCastTree node, Void aVoid) {
            typecasts++;
            return super.visitTypeCast(node, aVoid);
        }
    }

    @Override
    public AnnotationProvider getAnnotationProvider() {
        throw new UnsupportedOperationException(
                "getAnnotationProvider is not implemented for this class.");
    }
}
