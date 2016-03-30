package org.checkerframework.checker.fenum;

import java.util.Collections;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.javacutil.TreeUtils;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.Tree;

public class FenumVisitor extends BaseTypeVisitor<FenumAnnotatedTypeFactory> {
    public FenumVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    public Void visitBinary(BinaryTree node, Void p) {
        if (!TreeUtils.isStringConcatenation(node)) {
            // TODO: ignore string concatenations

            AnnotatedTypeMirror lhs = atypeFactory.getAnnotatedType(node.getLeftOperand());
            AnnotatedTypeMirror rhs = atypeFactory.getAnnotatedType(node.getRightOperand());
            if (!(atypeFactory.getTypeHierarchy().isSubtype(lhs, rhs)
                  || atypeFactory.getTypeHierarchy().isSubtype(rhs, lhs))) {
                checker.report(Result.failure("binary.type.incompatible", lhs, rhs), node);
            }
        }
        return super.visitBinary(node, p);
    }

    @Override
    public Void visitSwitch(SwitchTree node, Void p) {
        ExpressionTree expr = node.getExpression();
        AnnotatedTypeMirror exprType = atypeFactory.getAnnotatedType(expr);

        for (CaseTree caseExpr : node.getCases()) {
            ExpressionTree realCaseExpr = caseExpr.getExpression();
            if (realCaseExpr != null) {
                AnnotatedTypeMirror caseType = atypeFactory.getAnnotatedType(realCaseExpr);

                this.commonAssignmentCheck(exprType, caseType, caseExpr,
                        "switch.type.incompatible");
            }
        }
        return super.visitSwitch(node, p);
    }

    @Override
    protected boolean checkConstructorInvocation(AnnotatedDeclaredType dt,
            AnnotatedExecutableType constructor, Tree src) {
        // Ignore the default annotation on the constructor
        return true;
    }

    @Override
    protected Set<? extends AnnotationMirror> getExceptionParameterLowerBoundAnnotations() {
        return Collections.singleton(atypeFactory.FENUM_UNQUALIFIED);
    }

    // TODO: should we require a match between switch expression and cases?

    @Override
    public boolean isValidUse(AnnotatedDeclaredType declarationType,
                             AnnotatedDeclaredType useType,
                             Tree tree) {
        // The checker calls this method to compare the annotation used in a
        // type to the modifier it adds to the class declaration. As our default
        // modifier is Unqualified, this results in an error when a non-subtype
        // is used. Can we use FenumTop as default instead?
        return true;
    }

}
