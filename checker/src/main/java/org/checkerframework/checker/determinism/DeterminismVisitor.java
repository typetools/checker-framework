package org.checkerframework.checker.determinism;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import java.util.*;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.DeclaredType;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.determinism.qual.*;
import org.checkerframework.common.basetype.*;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

public class DeterminismVisitor extends BaseTypeVisitor<DeterminismAnnotatedTypeFactory> {
    public DeterminismVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    private static final @CompilerMessageKey String INVALID_ANNOTATION = "invalid.annotation";
    private static final @CompilerMessageKey String INVALID_ANNOTATION_SUBTYPE =
            "invalid.parameter.type";
    private static final @CompilerMessageKey String INVALID_ARRAY_ACCESS = "invalid.array.access";

    @Override
    protected Set<? extends AnnotationMirror> getExceptionParameterLowerBoundAnnotations() {
        Set<AnnotationMirror> exceptionParam = AnnotationUtils.createAnnotationSet();
        exceptionParam.add(atypeFactory.DET);
        return exceptionParam;
    }

    @Override
    public boolean isValidUse(
            AnnotatedDeclaredType declarationType, AnnotatedDeclaredType useType, Tree tree) {
        DeclaredType javaType = useType.getUnderlyingType();
        if (useType.hasAnnotation(AnnotationBuilder.fromClass(elements, OrderNonDet.class))) {
            if (!(atypeFactory.isCollection(javaType.asElement().asType())
                    || atypeFactory.isIterator(javaType.asElement().asType()))) {
                checker.report(Result.failure(INVALID_ANNOTATION), tree);
                return false;
            }
        }

        // Sets and lists
        if ((atypeFactory.isCollection(javaType.asElement().asType())
                        && javaType.getTypeArguments().size() == 1)
                || atypeFactory.isIterator(javaType.asElement().asType())) {
            AnnotationMirror baseAnnotation = useType.getAnnotations().iterator().next();
            AnnotatedTypeMirror paramType = useType.getTypeArguments().iterator().next();
            Iterator<AnnotationMirror> paramAnnotationIt = paramType.getAnnotations().iterator();
            if (paramAnnotationIt.hasNext()) {
                AnnotationMirror paramAnnotation = paramAnnotationIt.next();
                if (!isSubType(paramAnnotation, baseAnnotation)) {
                    checker.report(
                            Result.failure(
                                    INVALID_ANNOTATION_SUBTYPE,
                                    "parameter type(" + paramAnnotation + ")",
                                    "base type(" + baseAnnotation + ")"),
                            tree);
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isSubType(AnnotationMirror a1, AnnotationMirror a2) {
        boolean ret;
        ret =
                ((types.isSameType(a1.getAnnotationType(), atypeFactory.DET.getAnnotationType())
                                && types.isSameType(
                                        a2.getAnnotationType(),
                                        atypeFactory.DET.getAnnotationType()))
                        || (types.isSameType(
                                        a1.getAnnotationType(),
                                        atypeFactory.ORDERNONDET.getAnnotationType())
                                && types.isSameType(
                                        a2.getAnnotationType(),
                                        atypeFactory.ORDERNONDET.getAnnotationType()))
                        || (types.isSameType(
                                        a1.getAnnotationType(),
                                        atypeFactory.NONDET.getAnnotationType())
                                && types.isSameType(
                                        a2.getAnnotationType(),
                                        atypeFactory.NONDET.getAnnotationType())));
        return (ret || atypeFactory.getQualifierHierarchy().isSubtype(a1, a2));
    }

    @Override
    public boolean isValidUse(AnnotatedPrimitiveType type, Tree tree) {
        Set<AnnotationMirror> annos = type.getAnnotations();
        if (annos.contains(AnnotationBuilder.fromClass(elements, OrderNonDet.class))) {
            checker.report(Result.failure(INVALID_ANNOTATION), tree);
            return false;
        }
        return true;
    }

    @Override
    public boolean isValidUse(AnnotatedTypeMirror.AnnotatedArrayType type, Tree tree) {
        Set<AnnotationMirror> annos = type.getAnnotations();
        // TODO: Allow OrderNonDet array?
        //        if (annos.contains(AnnotationBuilder.fromClass(elements, OrderNonDet.class)))
        //            checker.report(Result.failure(INVALID_ANNOTATION), tree);
        // Do not allow arrays of type @NonDet Object @Det []
        AnnotationMirror arrayType;
        AnnotationMirror elementType;
        if (type.getAnnotations().size() > 0
                && type.getComponentType().getAnnotations().size() > 0) {
            arrayType = type.getAnnotations().iterator().next();
            elementType = type.getComponentType().getAnnotations().iterator().next();
            if (!isSubType(elementType, arrayType)) {
                checker.report(
                        Result.failure(INVALID_ANNOTATION_SUBTYPE, elementType, arrayType), tree);
                return false;
            }
        }
        return true;
    }

    @Override
    protected void commonAssignmentCheck(
            AnnotatedTypeMirror varType,
            AnnotatedTypeMirror valueType,
            Tree valueTree,
            @CompilerMessageKey String errorKey) {
        // Accessing elements of @OrderNonDet or @NonDet arrays returns @NonDet element
        // even if the component type is @Det
        if (valueTree.getKind() == Tree.Kind.ARRAY_ACCESS) {
            ArrayAccessTree arrTree = (ArrayAccessTree) valueTree;
            AnnotatedTypeMirror arrExprType =
                    atypeFactory.getAnnotatedType(arrTree.getExpression());
            AnnotatedTypeMirror.AnnotatedArrayType arrType =
                    (AnnotatedTypeMirror.AnnotatedArrayType) arrExprType;
            AnnotationMirror arrTopType = arrType.getAnnotations().iterator().next();
            if (AnnotationUtils.areSame(arrTopType, atypeFactory.ORDERNONDET)
                    || AnnotationUtils.areSame(arrTopType, atypeFactory.NONDET)) {
                valueType.replaceAnnotation(atypeFactory.NONDET);
            }
        }
        super.commonAssignmentCheck(varType, valueType, valueTree, errorKey);
    }

    @Override
    protected void commonAssignmentCheck(
            Tree varTree, ExpressionTree valueExp, @CompilerMessageKey String errorKey) {
        // This is to prevent side-effects to arrays
        // Example {@Code @Det int @Det [] x;}
        // {@Code x[@NonDet int] = y} is flagged as an error
        if (varTree.getKind() == Tree.Kind.ARRAY_ACCESS) {
            ArrayAccessTree arrTree = (ArrayAccessTree) varTree;
            AnnotatedTypeMirror arrExprType =
                    atypeFactory.getAnnotatedType(arrTree.getExpression());
            AnnotatedTypeMirror.AnnotatedArrayType arrType =
                    (AnnotatedTypeMirror.AnnotatedArrayType) arrExprType;
            AnnotationMirror arrTopType = arrType.getAnnotations().iterator().next();
            AnnotationMirror indexType =
                    atypeFactory
                            .getAnnotatedType(arrTree.getIndex())
                            .getAnnotations()
                            .iterator()
                            .next();
            if (!isSubType(indexType, arrTopType)) {
                checker.report(
                        Result.failure(
                                INVALID_ARRAY_ACCESS,
                                "index type(" + indexType + ")",
                                "array type(" + arrTopType + ")"),
                        varTree);
                return;
            }
        }
        super.commonAssignmentCheck(varTree, valueExp, errorKey);
    }

    @Override
    protected TypeValidator createTypeValidator() {
        return new BaseTypeValidator(checker, this, atypeFactory) {
            @Override
            protected void reportInvalidAnnotationsOnUse(AnnotatedTypeMirror type, Tree p) {}
        };
    }
}
