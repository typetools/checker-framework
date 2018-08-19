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
import org.checkerframework.javacutil.TypesUtils;

/** Visitor for the determinism type-system. */
public class DeterminismVisitor extends BaseTypeVisitor<DeterminismAnnotatedTypeFactory> {
    public DeterminismVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    /** Error message keys. */
    private static final @CompilerMessageKey String ORDERNONDET_ON_NONCOLLECTION =
            "ordernondet.on.noncollection";

    private static final @CompilerMessageKey String INVALID_ANNOTATION_SUBTYPE =
            "invalid.parameter.type";
    private static final @CompilerMessageKey String INVALID_ARRAY_ACCESS = "invalid.array.access";

    /**
     * Sets the lower bound for exception parameters to be {@code @Det}.
     *
     * @return Set of lower bound annotations for exception parameters.
     */
    @Override
    protected Set<? extends AnnotationMirror> getExceptionParameterLowerBoundAnnotations() {
        Set<AnnotationMirror> exceptionParam = AnnotationUtils.createAnnotationSet();
        exceptionParam.add(atypeFactory.DET);
        return exceptionParam;
    }

    /**
     * Reports errors for the following ordernondet.on.noncollections:
     *
     * <ol>
     *   <li>When a non-collection is annotated as {@code @OrderNonDet}.
     *   <li>When the annotation on type parameter of a Collection or Iterator is a supertype of the
     *       annotation on the Collection. Example: {@code @Det List<@OrderNonDet String>}.
     * </ol>
     *
     * @param declarationType the type of the class (TypeElement)
     * @param useType the use of the class (instance type)
     * @param tree the tree where the type is used
     * @return true if the annotation is valid and false otherwise.
     */
    @Override
    public boolean isValidUse(
            AnnotatedDeclaredType declarationType, AnnotatedDeclaredType useType, Tree tree) {
        DeclaredType javaType = useType.getUnderlyingType();

        // Check for @OrderNonDet on non-collections.
        if (useType.hasAnnotation(AnnotationBuilder.fromClass(elements, OrderNonDet.class))) {
            if (!(atypeFactory.isCollection(TypesUtils.getTypeElement(javaType).asType())
                    || atypeFactory.isIterator(javaType.asElement().asType()))) {
                checker.report(Result.failure(ORDERNONDET_ON_NONCOLLECTION), tree);
                return false;
            }
        }

        // For collections, check if annotation on the type parameter is a supertype of
        // the annotation on the collection.
        if ((atypeFactory.isCollection(TypesUtils.getTypeElement(javaType).asType())
                        && javaType.getTypeArguments().size() == 1)
                || atypeFactory.isIterator(javaType.asElement().asType())) {
            AnnotationMirror baseAnnotation = useType.getAnnotations().iterator().next();
            AnnotatedTypeMirror paramType = useType.getTypeArguments().iterator().next();
            Iterator<AnnotationMirror> paramAnnotationIt = paramType.getAnnotations().iterator();
            if (paramAnnotationIt.hasNext()) {
                AnnotationMirror paramAnnotation = paramAnnotationIt.next();
                if (!atypeFactory
                        .getQualifierHierarchy()
                        .isSubtype(paramAnnotation, baseAnnotation)) {
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

    /**
     * Reports an error if {@code @OrderNonDet} is used with a primitive type.
     *
     * @param type the use of the primitive type
     * @param tree the tree where the type is used
     * @return true if the annotation is valid and false otherwise.
     */
    @Override
    public boolean isValidUse(AnnotatedPrimitiveType type, Tree tree) {
        Set<AnnotationMirror> annos = type.getAnnotations();
        if (annos.contains(AnnotationBuilder.fromClass(elements, OrderNonDet.class))) {
            checker.report(Result.failure(ORDERNONDET_ON_NONCOLLECTION), tree);
            return false;
        }
        return true;
    }

    /**
     * Reports an error if the component type of an array has an annotation that is a supertype of
     * the array annotation Example: {@code @NonDet int @Det[]} is invalid.
     *
     * @param type the array type use
     * @param tree the tree where the type is used
     * @return true if the annotation is valid and false otherwise.
     */
    @Override
    public boolean isValidUse(AnnotatedTypeMirror.AnnotatedArrayType type, Tree tree) {
        AnnotationMirror arrayType;
        AnnotationMirror elementType;
        if (type.getAnnotations().size() > 0
                && type.getComponentType().getAnnotations().size() > 0) {
            arrayType = type.getAnnotations().iterator().next();
            elementType = type.getComponentType().getAnnotations().iterator().next();
            if (!atypeFactory.getQualifierHierarchy().isSubtype(elementType, arrayType)) {
                checker.report(
                        Result.failure(INVALID_ANNOTATION_SUBTYPE, elementType, arrayType), tree);
                return false;
            }
        }
        return true;
    }

    /**
     * When an element of {@code @OrderNonDet} or {@code @NonDet} array is accessed, this method
     * annotates the type of that element as {@code @Det}
     *
     * @param varType the annotated type of the variable
     * @param valueType the annotated type of the value
     * @param valueTree the location to use when reporting the error message
     * @param errorKey the error message to use if the check fails (must be a compiler message key)
     */
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

    /**
     * Checks for invalid assignment to an array element and reports an error. This is to prevent
     * side-effects to arrays. Example {@Code @Det int @Det [] x; @NonDet int i; x[i] = y} is
     * flagged as an error.
     *
     * @param varTree the AST node for the lvalue (usually a variable)
     * @param valueExp the AST node for the rvalue (the new value)
     * @param errorKey the error message to use if the check fails (must be a compiler message key)
     */
    @Override
    protected void commonAssignmentCheck(
            Tree varTree, ExpressionTree valueExp, @CompilerMessageKey String errorKey) {
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
            if (!atypeFactory.getQualifierHierarchy().isSubtype(indexType, arrTopType)) {
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
