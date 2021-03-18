package org.checkerframework.framework.util.dependenttypes;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Standardizes Java expressions in annotations and also viewpoint-adapts field accesses. Other
 * viewpoint adaption is handled in {@link DependentTypesHelper}.
 */
public class DependentTypesTreeAnnotator extends TreeAnnotator {
    private final DependentTypesHelper helper;

    public DependentTypesTreeAnnotator(
            AnnotatedTypeFactory atypeFactory, DependentTypesHelper helper) {
        super(atypeFactory);
        this.helper = helper;
    }

    @Override
    public Void visitClass(ClassTree node, AnnotatedTypeMirror annotatedTypeMirror) {
        TypeElement ele = TreeUtils.elementFromDeclaration(node);
        helper.atClassDeclaration(annotatedTypeMirror, ele);
        return super.visitClass(node, annotatedTypeMirror);
    }

    @Override
    public Void visitNewArray(NewArrayTree node, AnnotatedTypeMirror annotatedType) {
        helper.atExpression(node, annotatedType);
        return super.visitNewArray(node, annotatedType);
    }

    @Override
    public Void visitNewClass(NewClassTree node, AnnotatedTypeMirror annotatedType) {
        helper.atExpression(node, annotatedType);
        return super.visitNewClass(node, annotatedType);
    }

    @Override
    public Void visitTypeCast(TypeCastTree node, AnnotatedTypeMirror annotatedType) {
        helper.atExpression(node, annotatedType);
        return super.visitTypeCast(node, annotatedType);
    }

    @Override
    public Void visitVariable(VariableTree node, AnnotatedTypeMirror annotatedTypeMirror) {
        Element ele = TreeUtils.elementFromDeclaration(node);
        helper.atVariableDeclaration(node, annotatedTypeMirror, ele);
        return super.visitVariable(node, annotatedTypeMirror);
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, AnnotatedTypeMirror annotatedTypeMirror) {
        Element ele = TreeUtils.elementFromUse(node);
        if (ele.getKind() == ElementKind.FIELD || ele.getKind() == ElementKind.ENUM_CONSTANT) {
            helper.atVariableDeclaration(node, annotatedTypeMirror, ele);
        }
        return super.visitIdentifier(node, annotatedTypeMirror);
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree node, AnnotatedTypeMirror type) {
        Element ele = TreeUtils.elementFromUse(node);
        if (ele.getKind() == ElementKind.FIELD || ele.getKind() == ElementKind.ENUM_CONSTANT) {
            helper.atFieldAccess(node, type);
        }
        return super.visitMemberSelect(node, type);
    }
}
