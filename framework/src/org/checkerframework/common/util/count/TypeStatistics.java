package org.checkerframework.common.util.count;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.ParameterizedTypeTree;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.source.SourceVisitor;
import org.checkerframework.javacutil.AnnotationProvider;

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

    @Override
    public void typeProcessingOver() {
        System.out.printf("Found %d generic type uses.\n", generics);
        System.out.printf("Found %d array accesses and creations.\n", arrayAccesses);
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
            return super.visitClass(tree, p);
        }

        @Override
        public Void visitNewArray(NewArrayTree node, Void aVoid) {
            arrayAccesses += node.getDimensions().size();
            return super.visitNewArray(node, aVoid);
        }

        @Override
        public Void visitParameterizedType(ParameterizedTypeTree tree, Void p) {
            generics++;
            return super.visitParameterizedType(tree, p);
        }

        @Override
        public Void visitArrayAccess(ArrayAccessTree node, Void aVoid) {
            arrayAccesses++;
            return super.visitArrayAccess(node, aVoid);
        }
    }

    @Override
    public AnnotationProvider getAnnotationProvider() {
        throw new UnsupportedOperationException(
                "getAnnotationProvider is not implemented for this class.");
    }
}
