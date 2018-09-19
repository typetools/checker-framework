package org.checkerframework.checker.determinism;

import com.sun.source.tree.*;
import java.lang.annotation.Annotation;
import java.util.*;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.determinism.qual.Det;
import org.checkerframework.checker.determinism.qual.NonDet;
import org.checkerframework.checker.determinism.qual.OrderNonDet;
import org.checkerframework.checker.determinism.qual.PolyDet;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.poly.QualifierPolymorphism;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.framework.util.GraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.javacutil.*;

/** The annotated type factory for the determinism type-system. */
public class DeterminismAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
    /** The @NonDet annotation. */
    public final AnnotationMirror NONDET = AnnotationBuilder.fromClass(elements, NonDet.class);
    /** The @OrderNonDet annotation. */
    public final AnnotationMirror ORDERNONDET =
            AnnotationBuilder.fromClass(elements, OrderNonDet.class);
    /** The @Det annotation. */
    public final AnnotationMirror DET = AnnotationBuilder.fromClass(elements, Det.class);
    /** The @PolyDet annotation. */
    public final AnnotationMirror POLYDET;
    /** The @PolyDet("up") annotation. */
    public final AnnotationMirror POLYDET_UP;
    /** The @PolyDet("down") annotation. */
    public final AnnotationMirror POLYDET_DOWN;
    /** The @PolyDet("use") annotation. */
    public final AnnotationMirror POLYDET_USE;

    /** The java.util.Set interface. */
    private final TypeMirror setInterfaceTypeMirror =
            TypesUtils.typeFromClass(Set.class, types, processingEnv.getElementUtils());
    /** The java.util.List interface. */
    private final TypeMirror listInterfaceTypeMirror =
            TypesUtils.typeFromClass(List.class, types, processingEnv.getElementUtils());
    /** The java.util.Collection class. */
    private final TypeMirror collectionInterfaceTypeMirror =
            TypesUtils.typeFromClass(Collection.class, types, processingEnv.getElementUtils());
    /** The java.util.Iterator class. */
    private final TypeMirror iteratorTypeMirror =
            TypesUtils.typeFromClass(Iterator.class, types, processingEnv.getElementUtils());
    /** The java.util.Arrays class. */
    private final TypeMirror arraysTypeMirror =
            TypesUtils.typeFromClass(Arrays.class, types, processingEnv.getElementUtils());
    /** The java.util.Collections class. */
    private final TypeMirror collectionsTypeMirror =
            TypesUtils.typeFromClass(Collections.class, types, processingEnv.getElementUtils());
    /** The java.util.AbstractList class. */
    private final TypeMirror abstractListTypeMirror =
            TypesUtils.typeFromClass(AbstractList.class, types, processingEnv.getElementUtils());
    /** The java.util.AbstractSequentialList class. */
    private final TypeMirror abstractSequentialListTypeMirror =
            TypesUtils.typeFromClass(
                    AbstractSequentialList.class, types, processingEnv.getElementUtils());
    /** The java.util.ArrayList class. */
    private final TypeMirror arrayListTypeMirror =
            TypesUtils.typeFromClass(ArrayList.class, types, processingEnv.getElementUtils());
    /** The java.util.LinkedList class. */
    private final TypeMirror linkedListTypeMirror =
            TypesUtils.typeFromClass(LinkedList.class, types, processingEnv.getElementUtils());
    /** The java.util.NavigableSet class. */
    private final TypeMirror navigableSetTypeMirror =
            TypesUtils.typeFromClass(NavigableSet.class, types, processingEnv.getElementUtils());
    /** The java.util.SortedSet class. */
    private final TypeMirror sortedSetTypeMirror =
            TypesUtils.typeFromClass(SortedSet.class, types, processingEnv.getElementUtils());
    /** The java.util.TreeSet class. */
    private final TypeMirror treeSetTypeMirror =
            TypesUtils.typeFromClass(TreeSet.class, types, processingEnv.getElementUtils());
    /** The java.util.Enumeration interface. */
    private final TypeMirror enumerationTypeMirror =
            TypesUtils.typeFromClass(Enumeration.class, types, processingEnv.getElementUtils());

    /** Creates {@code @PolyDet} annotation mirror constants. */
    public DeterminismAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        POLYDET = newPolyDet("");
        POLYDET_UP = newPolyDet("up");
        POLYDET_DOWN = newPolyDet("down");
        POLYDET_USE = newPolyDet("use");

        postInit();
    }

    /** Creates an AnnotationMirror for {@code @PolyDet} with {@code arg} as its value. */
    private AnnotationMirror newPolyDet(String arg) {
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, PolyDet.class);
        builder.setValue("value", arg);
        return builder.build();
    }

    @Override
    public QualifierPolymorphism createQualifierPolymorphism() {
        return new DeterminismQualifierPolymorphism(processingEnv, this);
    }

    @Override
    public CFTransfer createFlowTransferFunction(
            CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
        return new DeterminismTransfer((CFAnalysis) analysis);
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return new LinkedHashSet<>(
                Arrays.asList(Det.class, OrderNonDet.class, NonDet.class, PolyDet.class));
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                new DeterminismTreeAnnotator(this), super.createTreeAnnotator());
    }

    @Override
    protected TypeAnnotator createTypeAnnotator() {
        return new ListTypeAnnotator(
                super.createTypeAnnotator(),
                new DeterminismAnnotatedTypeFactory.DeterminismTypeAnnotator(this));
    }

    private class DeterminismTreeAnnotator extends TreeAnnotator {

        public DeterminismTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        /**
         * Replaces the annotation on the return type of a method invocation as follows:
         *
         * <ol>
         *   <li>If {@code @PolyDet} resolves to {@code OrderNonDet} on a return type that isn't an
         *       array or a collection, it is replaced with {@code @NonDet}.
         *   <li>Return type of equals() gets the annotation {@code @Det}, when both the receiver
         *       and the argument satisfy these conditions (@see <a
         *       href="https://checkerframework.org/manual/#determinism-improved-precision-set-equals">Improves
         *       precision for Set.equals()</a>):
         *       <ol>
         *         <li>the type is {@code @OrderNonDet Set}, and
         *         <li>its type argument is not {@code @OrderNonDet List} or a subtype
         *       </ol>
         * </ol>
         *
         * @param node method invocation tree
         * @param annotatedRetType annotated return type
         * @return visitMethodInvocation() of the super class
         */
        @Override
        public Void visitMethodInvocation(
                MethodInvocationTree node, AnnotatedTypeMirror annotatedRetType) {
            AnnotatedTypeMirror receiverType = getReceiverType(node);

            // ReceiverType is null for abstract classes
            // (Example: Ordering.natural() in tests/all-systems/PolyCollectorTypeVars.java)
            // For static methods, receiverType is the AnnotatedTypeMirror of the class in which the
            // invoked method "node" is declared.
            if (receiverType == null) {
                return super.visitMethodInvocation(node, annotatedRetType);
            }

            ExecutableElement m = TreeUtils.elementFromUse(node);

            // If return type (non-array, non-collection, and non-iterator) resolves to
            // @OrderNonDet, replaces the annotation on the return type with @NonDet.
            if (annotatedRetType.hasAnnotation(ORDERNONDET)
                    && !mayBeOrderNonDet(annotatedRetType)) {
                annotatedRetType.replaceAnnotation(NONDET);
            }

            // Annotates the return type of "equals()" method called on a Set receiver
            // as described in the specification of this method.

            // Example1: @OrderNonDet Set<@OrderNonDet List<@Det Integer>> s1;
            //           @OrderNonDet Set<@OrderNonDet List<@Det Integer>> s2;
            // s1.equals(s2) is @NonDet

            // Example 2: @OrderNonDet Set<@Det List<@Det Integer>> s1;
            //            @OrderNonDet Set<@Det List<@Det Integer>> s2;
            // s1.equals(s2) is @Det
            // TODO-rashmi: this can be more precise (@Det receiver and @OrderNonDet argument)
            TypeElement receiverUnderlyingType =
                    TypesUtils.getTypeElement(receiverType.getUnderlyingType());

            // Without this check, NullPointerException in Collections class with buildJdk.
            // Likely cause: Collections has a private constructor?
            // Error at line: public class Collections {
            // TODO-rashmi: check why?
            if (receiverUnderlyingType == null) {
                return super.visitMethodInvocation(node, annotatedRetType);
            }

            if (isEqualsMethod(m)) {
                AnnotatedTypeMirror argument = getAnnotatedType(node.getArguments().get(0));
                if (isSubClassOf(receiverType, setInterfaceTypeMirror)
                        && receiverType.hasAnnotation(ORDERNONDET)
                        && !hasOrderNonDetListAsTypeArgument(receiverType)
                        && isSubClassOf(argument, setInterfaceTypeMirror)
                        && argument.hasAnnotation(ORDERNONDET)
                        && !hasOrderNonDetListAsTypeArgument(argument)) {
                    annotatedRetType.replaceAnnotation(DET);
                }
            }

            // The following code is a workaround for Issue#14
            // (https://github.com/t-rasmud/checker-framework/issues/14).
            // Checks if the return type is not a TYPEVAR, and if the invoked method belongs
            // to a set of (hardcoded) Collection methods in the JDK that return a generic type.
            // If the check succeeds, annotates the return type depending on the
            // type of the receiver and the method invoked.
            // Note: Annotating a generic type with @PolyDet (or any annotation for that matter)
            // constrains both its upper and lower bounds which was the root cause for Issue#14.
            // Therefore, we do not annotate the return types of these methods in the JDK.
            // Instead, we annotate the return type at the method invocation.
            if (annotatedRetType.getUnderlyingType().getKind() != TypeKind.TYPEVAR) {
                if (isIteratorNext(m)
                        || isAbstractListWithTypeVarReturn(receiverType, m)
                        || isArrayListWithTypeVarReturn(receiverType, m)
                        || isLinkedListWithTypeVarReturn(receiverType, m)
                        || isEnumerationWithTypeVarReturn(receiverType, m)) {
                    // Annotates the return types of these methods as @PolyDet("up").
                    if (isReceiverOrArgPoly(receiverType, node)) {
                        annotatedRetType.replaceAnnotation(POLYDET_UP);
                        return super.visitMethodInvocation(node, annotatedRetType);
                    }
                    if (isReceiverAndArgsDet(receiverType, node)) {
                        annotatedRetType.replaceAnnotation(DET);
                    } else {
                        annotatedRetType.replaceAnnotation(NONDET);
                    }
                }
                if (isTreeSetWithTypeVarReturn(receiverType, m)
                        || isNavigableSetWithTypeVarReturn(receiverType, m)
                        || isSortedSetWithTypeVarReturn(receiverType, m)) {
                    // Annotates the return types of these methods as @PolyDet("down").
                    if (isReceiverOrArgPoly(receiverType, node)) {
                        annotatedRetType.replaceAnnotation(POLYDET_DOWN);
                        return super.visitMethodInvocation(node, annotatedRetType);
                    }
                    if (isReceiverAndArgsDetOrOrderNonDet(receiverType, node)) {
                        annotatedRetType.replaceAnnotation(DET);
                    } else {
                        annotatedRetType.replaceAnnotation(NONDET);
                    }
                }
            }

            return super.visitMethodInvocation(node, annotatedRetType);
        }

        /** Annotates the length property of a {@code @NonDet} array as {@code @NonDet}. */
        @Override
        public Void visitMemberSelect(
                MemberSelectTree node, AnnotatedTypeMirror annotatedTypeMirror) {
            if (TreeUtils.isArrayLengthAccess(node)) {
                AnnotatedArrayType arrType =
                        (AnnotatedArrayType) getAnnotatedType(node.getExpression());
                if (arrType.hasAnnotation(NONDET)) {
                    annotatedTypeMirror.replaceAnnotation(NONDET);
                }
            }
            return super.visitMemberSelect(node, annotatedTypeMirror);
        }
    }

    /**
     * Returns true if any of the arguments of the method invocation {@code node} or the {@code
     * receiverType} is annotated as {@code @PolyDet}.
     */
    private boolean isReceiverOrArgPoly(
            AnnotatedTypeMirror receiverType, MethodInvocationTree node) {
        if (receiverType.hasAnnotation(POLYDET)) {
            return true;
        }
        for (ExpressionTree arg : node.getArguments()) {
            AnnotatedTypeMirror argType = getAnnotatedType(arg);
            if (argType.hasAnnotation(POLYDET)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if all the arguments of the method invocation {@code node} and the {@code
     * receiverType} are annotated as {@code @Det}.
     */
    private boolean isReceiverAndArgsDet(
            AnnotatedTypeMirror receiverType, MethodInvocationTree node) {
        if (!receiverType.hasAnnotation(DET)) {
            return false;
        }
        for (ExpressionTree arg : node.getArguments()) {
            AnnotatedTypeMirror argType = getAnnotatedType(arg);
            if (!argType.hasAnnotation(DET)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if all the arguments of the method invocation {@code node} and the {@code
     * receiverType} are annotated either as {@code @Det} or {@code @OrderNonDet}.
     */
    private boolean isReceiverAndArgsDetOrOrderNonDet(
            AnnotatedTypeMirror receiverType, MethodInvocationTree node) {
        if (!(receiverType.hasAnnotation(DET) || receiverType.hasAnnotation(ORDERNONDET))) {
            return false;
        }
        for (ExpressionTree arg : node.getArguments()) {
            AnnotatedTypeMirror argType = getAnnotatedType(arg);
            if (argType.hasAnnotation(NONDET)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if {@code @OrderNonDet List} appears as a top-level type argument in {@code
     * atm}.
     */
    private boolean hasOrderNonDetListAsTypeArgument(AnnotatedTypeMirror atm) {
        AnnotatedDeclaredType declaredType = (AnnotatedDeclaredType) atm;
        for (AnnotatedTypeMirror argType : declaredType.getTypeArguments()) {
            if (isSubClassOf(argType, listInterfaceTypeMirror)
                    && argType.hasAnnotation(ORDERNONDET)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if {@code javaType} may be annotated as {@code @OrderNonDet}.
     *
     * @param javaType the type to be checked
     * @return true if {@code javaType} is Collection (or a subtype), Iterator (or a subtype), or an
     *     array
     */
    public boolean mayBeOrderNonDet(AnnotatedTypeMirror javaType) {
        return (javaType.getKind() == TypeKind.ARRAY
                || isCollection(javaType)
                || isIterator(javaType));
    }

    /**
     * Adds default annotations for:
     *
     * <ol>
     *   <li>component types of array parameters and returns.
     *   <li>return types of methods with no unannotated or @PolyDet formal parameters and receiver.
     * </ol>
     *
     * Adds implicit annotation for main method formal parameter.
     */
    protected class DeterminismTypeAnnotator extends TypeAnnotator {
        /** Calls the superclass constructor. */
        public DeterminismTypeAnnotator(DeterminismAnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        /**
         * Places the following default annotations:
         *
         * <ol>
         *   <li>Defaults the component types of array parameters and return types as {@code
         *       ...[@PolyDet]} in the body of the method represented by {@code executableType}.
         *   <li>Defaults the return type for methods with no @PolyDet formal parameters (including
         *       the receiver) as {@code @Det} in the method represented by {@code executableType}.
         * </ol>
         *
         * <p>NOTE: This method {@code visitExecutable} adds default types to parameter types inside
         * the method bodies, not in method signatures. The same defaults are added to method
         * signatures by {@code addComputedTypeAnnotations}.
         */
        @Override
        public Void visitExecutable(final AnnotatedExecutableType executableType, final Void p) {
            if (!isMainMethod(executableType.getElement())) {
                for (AnnotatedTypeMirror paramType : executableType.getParameterTypes()) {
                    defaultArrayComponentTypeAsPolyDet(paramType);
                }

                // t.getReceiverType() is null for both "Object <init>()"
                // and for static methods.
                if (executableType.getReturnType().getAnnotations().isEmpty()
                        && (executableType.getReceiverType() == null)) {
                    boolean unannotatedOrPolyDet = false;
                    for (AnnotatedTypeMirror paramType : executableType.getParameterTypes()) {
                        // The default is @PolyDet, so treat unannotated the same as @PolyDet
                        if (paramType.getAnnotations().isEmpty()
                                || paramType.hasAnnotation(POLYDET)) {
                            unannotatedOrPolyDet = true;
                            break;
                        }
                    }
                    if (!unannotatedOrPolyDet) {
                        executableType.getReturnType().replaceAnnotation(DET);
                    }
                }
                defaultArrayComponentTypeAsPolyDet(executableType.getReturnType());
            }
            return super.visitExecutable(executableType, p);
        }
    }

    /** If {@code type} is an array type, defaults all its nested component types as @PolyDet. */
    private void defaultArrayComponentTypeAsPolyDet(AnnotatedTypeMirror type) {
        if (type.getKind() == TypeKind.ARRAY) {
            AnnotatedArrayType annoArrType = (AnnotatedArrayType) type;
            // The following code uses "annoannoArrType.getAnnotations().isEmpty()"
            // to check if 'annoannoArrType' has explicit annotations.
            // It doesn't check for "annoannoArrType.getExplicitAnnotations().isEmpty()"
            // because "getExplicitAnnotations()" works only with type use locations?
            // For example: if 'annoannoArrType' is "@Det int @Det[]",
            // "arrParamType.getExplicitAnnotations().size()" returns 0,
            // "arrParamType.getAnnotations().size()" returns 1.
            if (annoArrType.getAnnotations().isEmpty()) {
                recursiveDefaultArrayComponentTypeAsPolyDet(annoArrType);
            }
        }
    }

    /**
     * Defaults all the nested component types of the array type {@code annoArrType} as
     * {@code @PolyDet}.
     *
     * <p>Example: If this method is called with {@code annoArrType} as {@code int[][]}, the
     * resulting {@code annoArrType} will be {@code @PolyDet int @PolyDet[][]}
     */
    void recursiveDefaultArrayComponentTypeAsPolyDet(AnnotatedArrayType annoArrType) {
        AnnotatedTypeMirror componentType = annoArrType.getComponentType();
        if (!componentType.getAnnotations().isEmpty()) {
            return;
        }
        if (componentType.getUnderlyingType().getKind() != TypeKind.TYPEVAR) {
            componentType.replaceAnnotation(POLYDET);
        }
        if (componentType.getKind() != TypeKind.ARRAY) {
            return;
        }
        recursiveDefaultArrayComponentTypeAsPolyDet((AnnotatedArrayType) componentType);
    }

    /** @return true if {@code method} is equals */
    public static boolean isEqualsMethod(ExecutableElement method) {
        return (method.getReturnType().getKind() == TypeKind.BOOLEAN
                && method.getSimpleName().contentEquals("equals")
                && method.getParameters().size() == 1
                && TypesUtils.isObject(method.getParameters().get(0).asType()));
    }

    /** @return true if {@code method} is a main method */
    public static boolean isMainMethod(ExecutableElement method) {
        if (method.getReturnType().getKind() == TypeKind.VOID
                && method.getSimpleName().contentEquals("main")
                && method.getParameters().size() == 1
                && method.getParameters().get(0).asType().getKind() == TypeKind.ARRAY) {
            ArrayType arrayType = (ArrayType) method.getParameters().get(0).asType();
            if (TypesUtils.isString(arrayType.getComponentType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds implicit annotation for main method formal parameter ({@code @Det}) and default
     * annotations for the component types of other array formal parameters ({@code ...[@PolyDet]}).
     *
     * <p>Note: The annotation on an array type defaults to {@code @PolyDet[]} and this defaulting
     * is handled by declarative mechanism.
     *
     * <p>Example: Consider the following code:
     *
     * <pre><code>
     * &nbsp; void testArr(int[] a) {
     * &nbsp;   ...
     * &nbsp; }
     * </code></pre>
     *
     * This method {@code addComputedTypeAnnotations} annotates the component type of parameter
     * {@code int[] a} as {@code @PolyDet int[] a}.
     *
     * <p>Note: Even though {@code visitExecutable} and {@code addComputedTypeAnnotations} have the
     * same logic for adding defaults to parameter types, the code structure is different. This is
     * because the argument to {@code visitExecutable} is an {@code AnnotatedExecutableType} which
     * represents the type of a method, constructor, or initializer, and the argument to {@code
     * addComputedTypeAnnotations} is any {@code Element}.
     */
    @Override
    public void addComputedTypeAnnotations(Element elt, AnnotatedTypeMirror type) {
        if (elt.getKind() == ElementKind.PARAMETER) {
            if (elt.getEnclosingElement().getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) elt.getEnclosingElement();
                if (isMainMethod(method)) {
                    if (!type.getAnnotations().isEmpty() && !type.hasAnnotation(DET)) {
                        checker.report(
                                Result.failure(
                                        "invalid.annotation.on.parameter",
                                        type.getAnnotationInHierarchy(NONDET)),
                                elt);
                    }
                    type.addMissingAnnotations(Collections.singleton(DET));
                } else {
                    defaultArrayComponentTypeAsPolyDet(type);
                }
            }
        }
        super.addComputedTypeAnnotations(elt, type);
    }

    /** @return true if {@code subClass} is a subtype of {@code superClass} */
    private boolean isSubClassOf(AnnotatedTypeMirror subClass, TypeMirror superClass) {
        return types.isSubtype(
                types.erasure(subClass.getUnderlyingType()), types.erasure(superClass));
    }

    /** @return true if {@code tm} is Collection or a subtype of Collection */
    public boolean isCollection(AnnotatedTypeMirror tm) {
        return types.isSubtype(
                types.erasure(tm.getUnderlyingType()),
                types.erasure(collectionInterfaceTypeMirror));
    }

    /** @return true if {@code tm} is Iterator or a subtype of Iterator */
    public boolean isIterator(AnnotatedTypeMirror tm) {
        return types.isSubtype(
                types.erasure(tm.getUnderlyingType()), types.erasure(iteratorTypeMirror));
    }

    /** @return true if {@code tm} is a List or a subtype of List */
    public boolean isList(TypeMirror tm) {
        return types.isSubtype(types.erasure(tm), types.erasure(listInterfaceTypeMirror));
    }

    /** @return true if {@code tm} is the Arrays class */
    public boolean isArrays(TypeMirror tm) {
        return types.isSameType(tm, arraysTypeMirror);
    }

    /** @return true if {@code tm} is the Collections class */
    public boolean isCollections(TypeMirror tm) {
        return types.isSameType(tm, collectionsTypeMirror);
    }

    /**
     * Returns true if {@code receiverUnderlyingType} is Iterator and if the return type of {@code
     * invokedMethodElement} is a type variable.
     */
    private boolean isIteratorNext(ExecutableElement invokedMethodElement) {
        ExecutableElement iteratorNext =
                TreeUtils.getMethod("java.util.Iterator", "next", 0, getProcessingEnv());
        return ElementUtils.isMethod(invokedMethodElement, iteratorNext, getProcessingEnv());
    }

    /**
     * Returns true if {@code receiverUnderlyingType} is AbstractList, AbstractSequentialList, or
     * List and if the return type of {@code invokedMethodElement} is a type variable.
     */
    private boolean isAbstractListWithTypeVarReturn(
            AnnotatedTypeMirror receiverType, ExecutableElement invokedMethodElement) {
        ProcessingEnvironment env = getProcessingEnv();
        ExecutableElement abstractListGet =
                TreeUtils.getMethod("java.util.AbstractList", "get", 1, env);
        ExecutableElement abstractSequentialListGet =
                TreeUtils.getMethod("java.util.AbstractSequentialList", "get", 1, env);
        ExecutableElement listGet = TreeUtils.getMethod("java.util.List", "get", 1, env);

        ExecutableElement abstractListSet =
                TreeUtils.getMethod("java.util.AbstractList", "set", 2, env);
        ExecutableElement abstractSequentialListSet =
                TreeUtils.getMethod("java.util.AbstractSequentialList", "set", 2, env);
        ExecutableElement listSet = TreeUtils.getMethod("java.util.List", "set", 2, env);

        ExecutableElement abstractListRemove =
                TreeUtils.getMethod("java.util.AbstractList", "remove", 1, env);
        ExecutableElement abstractSequentialListRemove =
                TreeUtils.getMethod("java.util.AbstractSequentialList", "remove", 1, env);
        ExecutableElement listRemove = TreeUtils.getMethod("java.util.List", "remove", env, "int");

        return (ElementUtils.isMethod(invokedMethodElement, abstractListGet, env)
                || ElementUtils.isMethod(invokedMethodElement, abstractSequentialListGet, env)
                || ElementUtils.isMethod(invokedMethodElement, listGet, env)
                || ElementUtils.isMethod(invokedMethodElement, abstractListSet, env)
                || ElementUtils.isMethod(invokedMethodElement, abstractSequentialListSet, env)
                || ElementUtils.isMethod(invokedMethodElement, listSet, env)
                || ElementUtils.isMethod(invokedMethodElement, abstractListRemove, env)
                || ElementUtils.isMethod(invokedMethodElement, abstractSequentialListRemove, env)
                || ElementUtils.isMethod(invokedMethodElement, listRemove, env));
    }

    /**
     * Returns true if {@code receiverUnderlyingType} is NavigableSet and if the return type of
     * {@code invokedMethodElement} is a type variable.
     */
    private boolean isNavigableSetWithTypeVarReturn(
            AnnotatedTypeMirror receiverType, ExecutableElement invokedMethodElement) {
        //        ProcessingEnvironment env = getProcessingEnv();
        //        ExecutableElement navigableSetLower =
        //                TreeUtils.getMethod("java.util.NavigableSet", "lower", 1, env);
        //        ExecutableElement navigableSetFloor =
        //                TreeUtils.getMethod("java.util.NavigableSet", "floor", 1, env);
        //        ExecutableElement navigableSetCeiling =
        //                TreeUtils.getMethod("java.util.NavigableSet", "ceiling", 0, env);
        //        ExecutableElement navigableSetHigher =
        //                TreeUtils.getMethod("java.util.NavigableSet", "higher", 0, env);
        //        ExecutableElement navigableSetPollFirst =
        //                TreeUtils.getMethod("java.util.NavigableSet", "pollFirst", 0, env);
        //        ExecutableElement navigableSetPollLast =
        //                TreeUtils.getMethod("java.util.NavigableSet", "pollLast", 0, env);
        //
        //        return (ElementUtils.isMethod(invokedMethodElement, navigableSetLower, env)
        //                || ElementUtils.isMethod(invokedMethodElement, navigableSetFloor, env)
        //                || ElementUtils.isMethod(invokedMethodElement, navigableSetCeiling, env)
        //                || ElementUtils.isMethod(invokedMethodElement, navigableSetHigher, env)
        //                || ElementUtils.isMethod(invokedMethodElement, navigableSetPollFirst, env)
        //                || ElementUtils.isMethod(invokedMethodElement, navigableSetPollLast,
        // env));
        if (isSubClassOf(receiverType, navigableSetTypeMirror)) {
            if (invokedMethodElement.getSimpleName().contentEquals("lower")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 1
                    && invokedMethodElement.getParameters().get(0).asType().getKind()
                            == TypeKind.TYPEVAR) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("floor")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 1
                    && invokedMethodElement.getParameters().get(0).asType().getKind()
                            == TypeKind.TYPEVAR) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("ceiling")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().isEmpty()) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("higher")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().isEmpty()) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("pollFirst")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().isEmpty()) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("pollLast")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if {@code receiverUnderlyingType} is ArrayList and if the return type of {@code
     * invokedMethodElement} is a type variable.
     */
    private boolean isArrayListWithTypeVarReturn(
            AnnotatedTypeMirror receiverType, ExecutableElement invokedMethodElement) {
        ProcessingEnvironment env = getProcessingEnv();
        ExecutableElement arrayListElementData =
                TreeUtils.getMethod("java.util.ArrayList", "elementData", 1, env);
        ExecutableElement arrayListGet = TreeUtils.getMethod("java.util.ArrayList", "get", 1, env);
        ExecutableElement arrayListSet = TreeUtils.getMethod("java.util.ArrayList", "set", 2, env);
        ExecutableElement arrayListRemove =
                TreeUtils.getMethod("java.util.ArrayList", "remove", env, "int");

        return (ElementUtils.isMethod(invokedMethodElement, arrayListElementData, env)
                || ElementUtils.isMethod(invokedMethodElement, arrayListGet, env)
                || ElementUtils.isMethod(invokedMethodElement, arrayListSet, env)
                || ElementUtils.isMethod(invokedMethodElement, arrayListRemove, env));
    }

    /**
     * Returns true if {@code receiverUnderlyingType} is LinkedList and if the return type of {@code
     * invokedMethodElement} is a type variable.
     */
    private boolean isLinkedListWithTypeVarReturn(
            AnnotatedTypeMirror receiverType, ExecutableElement invokedMethodElement) {
        ProcessingEnvironment env = getProcessingEnv();
        ExecutableElement linkedListUnlink =
                TreeUtils.getMethod("java.util.LinkedList", "unlink", 1, env);
        ExecutableElement linkedListGetFirst =
                TreeUtils.getMethod("java.util.LinkedList", "getFirst", 0, env);
        ExecutableElement linkedListGetLast =
                TreeUtils.getMethod("java.util.LinkedList", "getLast", 0, env);
        ExecutableElement linkedListRemoveFirst =
                TreeUtils.getMethod("java.util.LinkedList", "removeFirst", 0, env);
        ExecutableElement linkedListRemoveLast =
                TreeUtils.getMethod("java.util.LinkedList", "removeLast", 0, env);
        ExecutableElement linkedListGet =
                TreeUtils.getMethod("java.util.LinkedList", "get", 1, env);
        ExecutableElement linkedListSet =
                TreeUtils.getMethod("java.util.LinkedList", "set", 2, env);
        ExecutableElement linkedListRemove =
                TreeUtils.getMethod("java.util.LinkedList", "remove", env, "int");
        ExecutableElement linkedListPeek =
                TreeUtils.getMethod("java.util.LinkedList", "peek", 0, env);
        ExecutableElement linkedListElement =
                TreeUtils.getMethod("java.util.LinkedList", "element", 0, env);
        ExecutableElement linkedListPoll =
                TreeUtils.getMethod("java.util.LinkedList", "poll", 0, env);
        ExecutableElement linkedListRemove0 =
                TreeUtils.getMethod("java.util.LinkedList", "remove", 0, env);
        ExecutableElement linkedListPeekFirst =
                TreeUtils.getMethod("java.util.LinkedList", "peekFirst", 0, env);
        ExecutableElement linkedListPeekLast =
                TreeUtils.getMethod("java.util.LinkedList", "peekLast", 0, env);
        ExecutableElement linkedListPollFirst =
                TreeUtils.getMethod("java.util.LinkedList", "pollFirst", 0, env);
        ExecutableElement linkedListPollLast =
                TreeUtils.getMethod("java.util.LinkedList", "pollLast", 0, env);
        ExecutableElement linkedListPop =
                TreeUtils.getMethod("java.util.LinkedList", "pop", 0, env);

        return (ElementUtils.isMethod(invokedMethodElement, linkedListUnlink, env)
                || ElementUtils.isMethod(invokedMethodElement, linkedListGetFirst, env)
                || ElementUtils.isMethod(invokedMethodElement, linkedListGetLast, env)
                || ElementUtils.isMethod(invokedMethodElement, linkedListRemoveFirst, env)
                || ElementUtils.isMethod(invokedMethodElement, linkedListRemoveLast, env)
                || ElementUtils.isMethod(invokedMethodElement, linkedListGet, env)
                || ElementUtils.isMethod(invokedMethodElement, linkedListSet, env)
                || ElementUtils.isMethod(invokedMethodElement, linkedListRemove, env)
                || ElementUtils.isMethod(invokedMethodElement, linkedListPeek, env)
                || ElementUtils.isMethod(invokedMethodElement, linkedListElement, env)
                || ElementUtils.isMethod(invokedMethodElement, linkedListPoll, env)
                || ElementUtils.isMethod(invokedMethodElement, linkedListRemove0, env)
                || ElementUtils.isMethod(invokedMethodElement, linkedListPeekFirst, env)
                || ElementUtils.isMethod(invokedMethodElement, linkedListPeekLast, env)
                || ElementUtils.isMethod(invokedMethodElement, linkedListPollFirst, env)
                || ElementUtils.isMethod(invokedMethodElement, linkedListPollLast, env)
                || ElementUtils.isMethod(invokedMethodElement, linkedListPop, env));
    }

    /**
     * Returns true if {@code receiverUnderlyingType} is SortedSet and if the return type of {@code
     * invokedMethodElement} is a type variable.
     */
    private boolean isSortedSetWithTypeVarReturn(
            AnnotatedTypeMirror receiverType, ExecutableElement invokedMethodElement) {
        ProcessingEnvironment env = getProcessingEnv();
        ExecutableElement sortedSetFirst =
                TreeUtils.getMethod("java.util.SortedSet", "first", 0, env);
        ExecutableElement sortedSetLast =
                TreeUtils.getMethod("java.util.SortedSet", "last", 0, env);
        return (ElementUtils.isMethod(invokedMethodElement, sortedSetFirst, env)
                || ElementUtils.isMethod(invokedMethodElement, sortedSetLast, env));
    }

    /**
     * Returns true if {@code receiverUnderlyingType} is TreeSet and if the return type of {@code
     * invokedMethodElement} is a type variable.
     */
    private boolean isTreeSetWithTypeVarReturn(
            AnnotatedTypeMirror receiverType, ExecutableElement invokedMethodElement) {
        ProcessingEnvironment env = getProcessingEnv();
        ExecutableElement treeSetFirst = TreeUtils.getMethod("java.util.TreeSet", "first", 0, env);
        ExecutableElement treeSetLast = TreeUtils.getMethod("java.util.TreeSet", "last", 0, env);
        ExecutableElement treeSetLower = TreeUtils.getMethod("java.util.TreeSet", "lower", 1, env);
        ExecutableElement treeSetFloor = TreeUtils.getMethod("java.util.TreeSet", "floor", 1, env);
        ExecutableElement treeSetCeiling =
                TreeUtils.getMethod("java.util.TreeSet", "ceiling", 1, env);
        ExecutableElement treeSetHigher =
                TreeUtils.getMethod("java.util.TreeSet", "higher", 1, env);
        ExecutableElement treeSetPollFirst =
                TreeUtils.getMethod("java.util.TreeSet", "pollFirst", 0, env);
        ExecutableElement treeSetPollLast =
                TreeUtils.getMethod("java.util.TreeSet", "pollLast", 0, env);

        return (ElementUtils.isMethod(invokedMethodElement, treeSetFirst, env)
                || ElementUtils.isMethod(invokedMethodElement, treeSetLast, env)
                || ElementUtils.isMethod(invokedMethodElement, treeSetLower, env)
                || ElementUtils.isMethod(invokedMethodElement, treeSetFloor, env)
                || ElementUtils.isMethod(invokedMethodElement, treeSetCeiling, env)
                || ElementUtils.isMethod(invokedMethodElement, treeSetHigher, env)
                || ElementUtils.isMethod(invokedMethodElement, treeSetPollFirst, env)
                || ElementUtils.isMethod(invokedMethodElement, treeSetPollLast, env));
    }

    /**
     * Returns true if {@code receiverUnderlyingType} is Enumeration and if the return type of
     * {@code invokedMethodElement} is a type variable.
     */
    private boolean isEnumerationWithTypeVarReturn(
            AnnotatedTypeMirror receiverType, ExecutableElement invokedMethodElement) {
        ProcessingEnvironment env = getProcessingEnv();
        ExecutableElement enumerationNext =
                TreeUtils.getMethod("java.util.Enumeration", "nextElement", 0, env);
        return ElementUtils.isMethod(invokedMethodElement, enumerationNext, env);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(
            MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
        return new DeterminismQualifierHierarchy(factory, DET);
    }

    class DeterminismQualifierHierarchy extends GraphQualifierHierarchy {

        public DeterminismQualifierHierarchy(MultiGraphFactory f, AnnotationMirror bottom) {
            super(f, bottom);
        }

        /**
         * Treats {@code @PolyDet} with values as {@code @PolyDet} without values in the qualifier
         * hierarchy.
         */
        @Override
        public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
            if (AnnotationUtils.areSameIgnoringValues(subAnno, POLYDET)) {
                subAnno = POLYDET;
            }
            if (AnnotationUtils.areSameIgnoringValues(superAnno, POLYDET)) {
                superAnno = POLYDET;
            }
            return super.isSubtype(subAnno, superAnno);
        }
    }
}
