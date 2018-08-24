package org.checkerframework.checker.determinism;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.DeclaredType;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.determinism.qual.OrderNonDet;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeValidator;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.basetype.TypeValidator;
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

    /** Error message key for use of {@code @OrderNonDet} on non-collections. */
    private static final @CompilerMessageKey String ORDERNONDET_ON_NONCOLLECTION =
            "ordernondet.on.noncollection";
    /** Error message key for collections whose type is a subtype of their element types. */
    private static final @CompilerMessageKey String INVALID_ELEMENT_TYPE = "invalid.element.type";
    /** Error message key for arrays whose type is a subtype of their component types. */
    private static final @CompilerMessageKey String INVALID_ARRAY_COMPONENT_TYPE =
            "invalid.array.component.type";
    // TODO: I don't see the problem; such array accesses are legal.  In particular, both
    // `detArray[nonDetInt]` and `nonDetArray[detInt]` are legal and both should have type `@NonDet
    // ElementType`.
    // TODO: Later, it became clear that this is about array *assignment*, not array *access*.
    // Change the text accordingly.
    /**
     * Error message key for array accesses where the type of the index is a supertype of the array
     * type.
     */
    // TODO: This error key is pretty general and might conflict with other type systems.
    private static final @CompilerMessageKey String INVALID_ARRAY_ACCESS = "invalid.array.access";

    /**
     * The lower bound for exception parameters is {@code @Det}.
     *
     * @return set of lower bound annotations for exception parameters
     */
    @Override
    protected Set<? extends AnnotationMirror> getExceptionParameterLowerBoundAnnotations() {
        return Collections.singleton(atypeFactory.DET);
    }

    // TODO: in "type parameter" below, do you mean "type argument"?  I think the latter.
    // Parameters and arguments are different things, and it's confusing to write imprecisely.
    // TODO: why can't declarationType be from some other library?  If you just mean  it's a
    // non-primitive, non-array (?) class, say that.
    // TODO: the documentation says that declarationType and useType are both "any" type.  Do they
    // have any relationship to one another?  If so, explain it.

    /**
     * Reports errors for the following conditions:
     *
     * <ol>
     *   <li>When a non-collection is annotated as {@code @OrderNonDet}.
     *   <li>When the annotation on type parameter of a Collection or Iterator is a supertype of the
     *       annotation on the Collection. Example: {@code @Det List<@OrderNonDet String>}.
     * </ol>
     *
     * @param declarationType the type of any user-defined or a JDK class (TypeElement)
     * @param useType the use of any user-defined or a JDK class (instance type)
     * @param tree the tree where the type is used
     * @return true if the annotation is valid and false otherwise
     */
    @Override
    public boolean isValidUse(
            AnnotatedDeclaredType declarationType, AnnotatedDeclaredType useType, Tree tree) {
        DeclaredType javaType = useType.getUnderlyingType();

        // Checks for @OrderNonDet on non-collections and raises an error if this check succeeds.
        if (useType.hasAnnotation(atypeFactory.ORDERNONDET)
                // TODO: This would be a place to use the allowsOrderNonDet method, if you create
                // it.  (There is at least one other opportunity as well.)
                && (!(atypeFactory.isCollection(TypesUtils.getTypeElement(javaType).asType())
                        || atypeFactory.isIterator(javaType.asElement().asType())))) {
            checker.report(Result.failure(ORDERNONDET_ON_NONCOLLECTION), tree);
            return false;
        }

        // Raises an error if the annotation on the type parameter of a collection (or iterator) is
        // a supertype of the annotation on the collection (or iterator).
        if ((atypeFactory.isCollection(TypesUtils.getTypeElement(javaType).asType())
                        // TODO: Abstract out the body, and then it's easy to call that method 1
                        // or 2 times.
                        // TODO-rashmi: This won't work for maps since they have 2 type arguments.
                        && javaType.getTypeArguments().size() == 1)
                || atypeFactory.isIterator(javaType.asElement().asType())) {
            AnnotationMirror baseAnnotation = useType.getAnnotations().iterator().next();
            AnnotatedTypeMirror paramType = useType.getTypeArguments().iterator().next();
            // The previous line assumes that there is exactly one type parameter.
            // This type parameter may not always be annotated. For example, in the code
            // @OrderNonDet TreeSet<@Det Integer> treeSet; Iterator it = treeSet.iterator();
            // the type parameter of Iterator does not have an annotation.
            Iterator<AnnotationMirror> paramAnnotationIt = paramType.getAnnotations().iterator();
            if (paramAnnotationIt.hasNext()) {
                AnnotationMirror paramAnnotation = paramAnnotationIt.next();
                if (!atypeFactory
                        .getQualifierHierarchy()
                        .isSubtype(paramAnnotation, baseAnnotation)) {
                    checker.report(
                            Result.failure(INVALID_ELEMENT_TYPE, paramAnnotation, baseAnnotation),
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
     * @param tree the tree where the type is used; used only for error reporting
     * @return true if the annotation is valid and false otherwise
     */
    @Override
    public boolean isValidUse(AnnotatedPrimitiveType type, Tree tree) {
        Set<AnnotationMirror> annos = type.getAnnotations();
        // TODO: The code repeatedly calls AnnotationBuilder.fromClass.  It should be able to use
        // fields declared in other classes, or at least cache the results here.  That will improve
        // performance somewhat, by reducing computation and allocation, and by creating less
        // garbage for the garbage collector.
        if (annos.contains(AnnotationBuilder.fromClass(elements, OrderNonDet.class))) {
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
    public boolean isValidUse(AnnotatedTypeMirror.AnnotatedArrayType type, Tree tree) {
        if (type.getAnnotations().size() > 0
                && type.getComponentType().getAnnotations().size() > 0) {
            AnnotationMirror arrayType = type.getAnnotations().iterator().next();
            AnnotationMirror elementType =
                    type.getComponentType().getAnnotations().iterator().next();
            if (!atypeFactory.getQualifierHierarchy().isSubtype(elementType, arrayType)) {
                checker.report(
                        Result.failure(INVALID_ARRAY_COMPONENT_TYPE, elementType, arrayType), tree);
                return false;
            }
        }
        return true;
    }

    /**
     * When an element of an array of type {@code @OrderNonDet} or {@code @NonDet} is accessed, this
     * method annotates the type of that element as {@code @NonDet}. Example:
     *
     * <pre><code>
     * &nbsp; @Det int @NonDet int[] arr;
     * &nbsp; i = arr[0];
     * </code></pre>
     *
     * In the code above, type of i gets annotated as @NonDet.
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
     * Checks for invalid access of an array element on the lhs of an assignment and reports an
     * error if this check succeeds. This is to prevent side-effects to arrays. Example:
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
                        Result.failure(INVALID_ARRAY_ACCESS, indexType, arrTopType), varTree);
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
