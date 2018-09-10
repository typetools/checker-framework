package org.checkerframework.checker.determinism;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import java.util.Collections;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.basetype.TypeValidator;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.javacutil.AnnotationUtils;

/** Visitor for the determinism type-system. */
public class DeterminismVisitor extends BaseTypeVisitor<DeterminismAnnotatedTypeFactory> {
    public DeterminismVisitor(BaseTypeChecker checker) {
        super(checker);
    }

    /** Error message key for use of {@code @OrderNonDet} on non-collections. */
    private static final @CompilerMessageKey String ORDERNONDET_ON_NONCOLLECTION =
            "ordernondet.on.noncollection.and.nonarray";
    /** Error message key for collections whose type is a subtype of their element types. */
    private static final @CompilerMessageKey String INVALID_ELEMENT_TYPE = "invalid.element.type";
    /** Error message key for arrays whose type is a subtype of their component types. */
    private static final @CompilerMessageKey String INVALID_ARRAY_COMPONENT_TYPE =
            "invalid.array.component.type";
    /** Error message key for assignment to a deterministic array at a non-deterministic index. */
    public static final @CompilerMessageKey String INVALID_ARRAY_ASSIGNMENT =
            "invalid.array.assignment";

    /**
     * The lower bound for exception parameters is {@code @Det}.
     *
     * @return set of lower bound annotations for exception parameters
     */
    @Override
    protected Set<? extends AnnotationMirror> getExceptionParameterLowerBoundAnnotations() {
        return Collections.singleton(atypeFactory.DET);
    }

    /**
     * Reports errors for the following conditions:
     *
     * <ol>
     *   <li>When a non-collection is annotated as {@code @OrderNonDet}.
     *   <li>When the annotation on the type argument of a Collection or Iterator is a supertype of
     *       the annotation on the Collection. Example: {@code @Det List<@OrderNonDet String>}.
     * </ol>
     *
     * @param declarationType the type of any non-primitive, non-array class (TypeElement)
     * @param useType the use of the {@code declarationType} class (instance type)
     * @param tree the tree where the type is used
     * @return true if the annotation is valid and false otherwise (in which case an error is also
     *     reported)
     */
    @Override
    public boolean isValidUse(
            AnnotatedDeclaredType declarationType, AnnotatedDeclaredType useType, Tree tree) {
        TypeMirror javaType = useType.getUnderlyingType();

        // Raises an error if a non-collection type is annotated with @OrderNonDet.
        if (useType.hasAnnotation(atypeFactory.ORDERNONDET)
                && !atypeFactory.mayBeOrderNonDet(javaType)) {
            checker.report(Result.failure(ORDERNONDET_ON_NONCOLLECTION), tree);
            return false;
        }

        // Raises an error if the annotation on the type argument of a collection (or iterator) is
        // a supertype of the annotation on the collection (or iterator).
        AnnotationMirror baseAnnotation = useType.getAnnotationInHierarchy(atypeFactory.NONDET);
        if (atypeFactory.mayBeOrderNonDet(javaType)) {
            for (AnnotatedTypeMirror argType : useType.getTypeArguments()) {
                if (argType.getAnnotations().size() > 0) {
                    AnnotationMirror argAnnotation =
                            argType.getAnnotationInHierarchy(atypeFactory.NONDET);
                    if (!isSubtype(argAnnotation, baseAnnotation, tree, INVALID_ELEMENT_TYPE)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Reports an error if {@code @OrderNonDet} is used with a primitive type.
     *
     * @param type the use of the primitive type
     * @param tree the tree where the type is used; used only for error reporting
     * @return true if the annotation is valid and false otherwise
     */
    @Override
    public boolean isValidUse(AnnotatedPrimitiveType type, Tree tree) {
        if (type.hasAnnotation(atypeFactory.ORDERNONDET)) {
            checker.report(Result.failure(ORDERNONDET_ON_NONCOLLECTION), tree);
            return false;
        }
        return true;
    }

    /**
     * Reports an error if the component type of an array has an annotation that is a supertype of
     * the array annotation. Example: {@code @NonDet int @Det[]} is invalid.
     *
     * @param type the array type use
     * @param tree the tree where the type is used
     * @return true if the annotation is valid and false otherwise
     */
    @Override
    public boolean isValidUse(AnnotatedArrayType type, Tree tree) {
        if (type.getAnnotations().size() > 0
                && type.getComponentType().getAnnotations().size() > 0) {
            AnnotationMirror arrayType = type.getAnnotationInHierarchy(atypeFactory.NONDET);
            AnnotationMirror elementType =
                    type.getComponentType().getAnnotationInHierarchy(atypeFactory.NONDET);
            if (!isSubtype(elementType, arrayType, tree, INVALID_ARRAY_COMPONENT_TYPE)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Reports an error in case of invalid access of an array element on the lhs of an assignment.
     * This is to prevent side-effects to arrays. Example:
     *
     * <pre><code>
     * &nbsp; @Det int @Det [] x;
     * &nbsp; @NonDet int i;
     * &nbsp; x[i] = y;
     * </code></pre>
     *
     * The array access {@code x[i]} is flagged as an error.
     *
     * @param varTree the AST node for the lvalue
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
            AnnotatedArrayType arrType = (AnnotatedArrayType) arrExprType;
            AnnotationMirror arrTopType = arrType.getAnnotationInHierarchy(atypeFactory.NONDET);
            AnnotationMirror indexType =
                    atypeFactory
                            .getAnnotatedType(arrTree.getIndex())
                            .getAnnotationInHierarchy(atypeFactory.NONDET);
            isSubtype(indexType, arrTopType, varTree, INVALID_ARRAY_ASSIGNMENT);
        }
        super.commonAssignmentCheck(varTree, valueExp, errorKey);
    }

    // NOTE: Checker Framework treats x[i] as an lvalue like array access.
    // It is possible to distinguish whether a "[]" operator is in an lvalue or an rvalue position.
    // But, the "visitArrayAccess" method does not give access to valueType (the annotated type of rhs value)
    // like in "commonAssignmentCheck" below, making it difficult to replace the annotation on the rvalue.

    /**
     * When an array of type {@code @OrderNonDet} or {@code @NonDet} is accessed, this method
     * annotates the type of the array access expression (equivalently, the array element) as
     * {@code @NonDet}. Example:
     *
     * <pre><code>
     * &nbsp; @Det int @NonDet int[] arr;
     * &nbsp; int val = arr[0];
     * </code></pre>
     *
     * In the code above, type of val gets annotated as @NonDet.
     *
     * <p>Note: If we were to replace every array access with this rule, the checker would allow
     * invalid assignments to array elements. Example:
     *
     * <pre><code>
     * &nbsp; @Det int @OrderNonDet [] x;
     * &nbsp; @NonDet int i;
     * &nbsp; x[i] = y;
     * </code></pre>
     *
     * Here, we expect the checker to flag the assignment {@code x[i] = y} as an error. Had we
     * replaced the type of every {@code @OrderNonDet} and {@code @NonDet} array access with
     * {@code @NonDet}, the array access {@code x[i]} would have the type {@code @NonDet} and this
     * assignment would not be flagged as an error.
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
        if (valueTree.getKind() == Tree.Kind.ARRAY_ACCESS) {
            ArrayAccessTree arrTree = (ArrayAccessTree) valueTree;
            AnnotatedArrayType arrType =
                    (AnnotatedArrayType) atypeFactory.getAnnotatedType(arrTree.getExpression());
            AnnotationMirror arrTopType = arrType.getAnnotationInHierarchy(atypeFactory.NONDET);
            if (AnnotationUtils.areSame(arrTopType, atypeFactory.ORDERNONDET)
                    || AnnotationUtils.areSame(arrTopType, atypeFactory.NONDET)) {
                valueType.replaceAnnotation(atypeFactory.NONDET);
            }
        }
        super.commonAssignmentCheck(varType, valueType, valueTree, errorKey);
    }

    /**
     * Reports the given {@code errorMessage} if {@code subAnnotation} is not a subtype of {@code
     * superAnnotation}.
     *
     * @return true if {@code subAnnotation} is a subtype of {@code superAnnotation}, false
     *     otherwise
     */
    private boolean isSubtype(
            AnnotationMirror subAnnotation,
            AnnotationMirror superAnnotation,
            Tree tree,
            @CompilerMessageKey String errorMessage) {
        if (atypeFactory.getQualifierHierarchy().isSubtype(subAnnotation, superAnnotation)) {
            return true;
        }
        checker.report(Result.failure(errorMessage, superAnnotation, subAnnotation), tree);
        return false;
    }

    @Override
    protected TypeValidator createTypeValidator() {
        return new BaseTypeValidator(checker, this, atypeFactory) {
            @Override
            protected void reportInvalidAnnotationsOnUse(AnnotatedTypeMirror type, Tree p) {}
        };
    }
}
