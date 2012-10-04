package checkers.i18n;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.CompoundAssignmentTree;

import checkers.basetype.BaseTypeChecker;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.BasicAnnotatedTypeFactory;
import checkers.types.TreeAnnotator;

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
        public I18nTreeAnnotator(BaseTypeChecker checker) {
            super(checker, I18nAnnotatedTypeFactory.this);
        }

        @Override
        public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
            return null;
        }

        @Override
        public Void visitCompoundAssignment(CompoundAssignmentTree node, AnnotatedTypeMirror type) {
            return null;
        }
    }
}