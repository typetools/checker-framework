package org.checkerframework.common.returnsrcvr;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import javax.lang.model.element.AnnotationMirror;
import javax.tools.Diagnostic;

import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.DiagMessage;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

/** A visitor that extends {@link BaseTypeVisitor} for the returns receiver checker */
public class ReturnsRcvrVisitor extends BaseTypeVisitor<ReturnsRcvrAnnotatedTypeFactory> {

    /**
     * Create a new {@code ReturnsRcvrVisitor}.
     *
     * @param checker the type-checker associated with this visitor
     */
    public ReturnsRcvrVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public Void visitAnnotation(AnnotationTree node, Void p) {
        AnnotationMirror annot = TreeUtils.annotationFromAnnotationTree(node);
        AnnotationMirror thisAnnot = getTypeFactory().THIS_ANNOT;
        if (AnnotationUtils.areSame(annot, thisAnnot)) {
            TreePath currentPath = getCurrentPath();
            TreePath parentPath = currentPath.getParentPath();
            Tree parent = parentPath.getLeaf();
            Tree grandparent = parentPath.getParentPath().getLeaf();
            boolean isReturnAnnot =
                    grandparent instanceof MethodTree
                            && (parent.equals(((MethodTree) grandparent).getReturnType())
                                    || parent instanceof ModifiersTree);
            boolean isCastAnnot =
                    grandparent instanceof TypeCastTree
                            && parent.equals(((TypeCastTree) grandparent).getType());
            if (!(isReturnAnnot || isCastAnnot)) {
                checker.report(
                        node, new DiagMessage(Diagnostic.Kind.ERROR, "invalid.this.location"));
            }
        }
        return super.visitAnnotation(node, p);
    }
}
