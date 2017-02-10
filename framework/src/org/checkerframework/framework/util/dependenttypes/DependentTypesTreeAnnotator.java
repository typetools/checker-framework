package org.checkerframework.framework.util.dependenttypes;

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Standardizes Java expressions in annotations and also view points adapts field accesses. (Other
 * viewpoint adaption is handled in {@link DependentTypesHelper}
 */
public class DependentTypesTreeAnnotator extends TreeAnnotator {
    private final DependentTypesHelper helper;

    public DependentTypesTreeAnnotator(
            AnnotatedTypeFactory atypeFactory, DependentTypesHelper helper) {
        super(atypeFactory);
        this.helper = helper;
    }

    @Override
    public Void visitNewArray(NewArrayTree node, AnnotatedTypeMirror annotatedType) {
        helper.standardizeExpression(node, annotatedType);
        return super.visitNewArray(node, annotatedType);
    }

    @Override
    public Void visitNewClass(NewClassTree node, AnnotatedTypeMirror annotatedType) {
        helper.standardizeExpression(node, annotatedType);
        return super.visitNewClass(node, annotatedType);
    }

    @Override
    public Void visitTypeCast(TypeCastTree node, AnnotatedTypeMirror annotatedType) {
        helper.standardizeExpression(node, annotatedType);
        return super.visitTypeCast(node, annotatedType);
    }

    @Override
    public Void visitVariable(VariableTree node, AnnotatedTypeMirror annotatedTypeMirror) {
        Element ele = TreeUtils.elementFromDeclaration(node);
        helper.standardizeVariable(node, annotatedTypeMirror, ele);
        return super.visitVariable(node, annotatedTypeMirror);
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, AnnotatedTypeMirror annotatedTypeMirror) {
        Element ele = TreeUtils.elementFromUse(node);
        if (ele.getKind() == ElementKind.FIELD) {
            helper.standardizeVariable(node, annotatedTypeMirror, ele);
        }
        return super.visitIdentifier(node, annotatedTypeMirror);
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree node, AnnotatedTypeMirror type) {
        helper.standardizeFieldAccess(node, type);
        return super.visitMemberSelect(node, type);
    }
}
