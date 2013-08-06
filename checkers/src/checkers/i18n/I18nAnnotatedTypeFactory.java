package checkers.i18n;

import checkers.basetype.BaseTypeChecker;
import checkers.i18n.quals.Localized;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.BasicAnnotatedTypeFactory;
import checkers.types.TreeAnnotator;

import javacutils.AnnotationUtils;

import javax.lang.model.element.AnnotationMirror;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;

public class I18nAnnotatedTypeFactory extends BasicAnnotatedTypeFactory<I18nSubchecker> {

    public I18nAnnotatedTypeFactory(I18nSubchecker checker,
            CompilationUnitTree root) {
        super(checker, root);
        this.postInit();
    }

    @Override
    public TreeAnnotator createTreeAnnotator(I18nSubchecker checker) {
        return new I18nTreeAnnotator(checker);
    }

    /** Do not propagate types through binary/compound operations.
     */
    private class I18nTreeAnnotator extends TreeAnnotator {
        private final AnnotationMirror LOCALIZED;

        public I18nTreeAnnotator(BaseTypeChecker<?> checker) {
            super(checker, I18nAnnotatedTypeFactory.this);
            LOCALIZED = AnnotationUtils.fromClass(elements, Localized.class);
        }

        @Override
        public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
            type.removeAnnotation(LOCALIZED);
            return null;
        }

        @Override
        public Void visitCompoundAssignment(CompoundAssignmentTree node, AnnotatedTypeMirror type) {
            type.removeAnnotation(LOCALIZED);
            return null;
        }

        @Override
        public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
            if (!type.isAnnotatedInHierarchy(LOCALIZED)) {
                if (tree.getKind() == Tree.Kind.STRING_LITERAL && tree.getValue().equals("")) {
                    type.addAnnotation(LOCALIZED);
                }
            }
            return super.visitLiteral(tree, type);
        }
    }
}