package org.checkerframework.checker.determinism;

import com.sun.source.tree.*;
import java.lang.annotation.Annotation;
import java.util.*;
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
import org.checkerframework.dataflow.cfg.node.Node;
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
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

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
    private final TypeMirror SetInterfaceTypeMirror =
            TypesUtils.typeFromClass(Set.class, types, processingEnv.getElementUtils());
    /** The java.util.List interface. */
    private final TypeMirror ListInterfaceTypeMirror =
            TypesUtils.typeFromClass(List.class, types, processingEnv.getElementUtils());
    /** The java.util.Collection class. */
    private final TypeMirror CollectionInterfaceTypeMirror =
            TypesUtils.typeFromClass(Collection.class, types, processingEnv.getElementUtils());
    /** The java.util.Iterator class. */
    private final TypeMirror IteratorTypeMirror =
            TypesUtils.typeFromClass(Iterator.class, types, processingEnv.getElementUtils());
    /** The java.util.Arrays class. */
    private final TypeMirror ArraysTypeMirror =
            TypesUtils.typeFromClass(Arrays.class, types, processingEnv.getElementUtils());
    /** The java.util.Collections class. */
    private final TypeMirror CollectionsTypeMirror =
            TypesUtils.typeFromClass(Collections.class, types, processingEnv.getElementUtils());
    /** The java.util.AbstractList class. */
    private final TypeMirror AbstractListTypeMirror =
            TypesUtils.typeFromClass(AbstractList.class, types, processingEnv.getElementUtils());
    /** The java.util.AbstractSequentialList class. */
    private final TypeMirror AbstractSequentialListTypeMirror =
            TypesUtils.typeFromClass(
                    AbstractSequentialList.class, types, processingEnv.getElementUtils());
    /** The java.util.ArrayList class. */
    private final TypeMirror ArrayListTypeMirror =
            TypesUtils.typeFromClass(ArrayList.class, types, processingEnv.getElementUtils());
    /** The java.util.LinkedList class. */
    private final TypeMirror LinkedListTypeMirror =
            TypesUtils.typeFromClass(LinkedList.class, types, processingEnv.getElementUtils());
    /** The java.util.NavigableSet class. */
    private final TypeMirror NavigableSetTypeMirror =
            TypesUtils.typeFromClass(NavigableSet.class, types, processingEnv.getElementUtils());
    /** The java.util.SortedSet class. */
    private final TypeMirror SortedSetTypeMirror =
            TypesUtils.typeFromClass(SortedSet.class, types, processingEnv.getElementUtils());
    /** The java.util.TreeSet class. */
    private final TypeMirror TreeSetTypeMirror =
            TypesUtils.typeFromClass(TreeSet.class, types, processingEnv.getElementUtils());
    /** The java.util.Enumeration interface. */
    private final TypeMirror EnumerationTypeMirror =
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
            if (receiverType == null) {
                return super.visitMethodInvocation(node, annotatedRetType);
            }

            ExecutableElement m = methodFromUse(node).methodType.getElement();

            // If return type (non-array, non-collection, and non-iterator) resolves to
            // @OrderNonDet, replaces the annotation on the return type with @NonDet.
            if (annotatedRetType.getAnnotations().contains(ORDERNONDET)
                    && !mayBeOrderNonDet(annotatedRetType.getUnderlyingType())) {
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
                if (isSet(receiverUnderlyingType.asType())
                        && receiverType.hasAnnotation(ORDERNONDET)
                        && !hasOrderNonDetListAsTypeArgument(receiverType)
                        && isSet(TypesUtils.getTypeElement(argument.getUnderlyingType()).asType())
                        && argument.hasAnnotation(ORDERNONDET)
                        && !hasOrderNonDetListAsTypeArgument(argument)) {
                    annotatedRetType.replaceAnnotation(DET);
                }
            }

            // The following code fixes Issue#14
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
                if (isIteratorNext(receiverUnderlyingType, m)
                        || isAbstractListWithTypeVarReturn(receiverUnderlyingType, m)
                        || isArrayListWithTypeVarReturn(receiverUnderlyingType, m)
                        || isLinkedListWithTypeVarReturn(receiverUnderlyingType, m)
                        || isEnumerationWithTypeVarReturn(receiverUnderlyingType, m)) {
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
                if (isTreeSetWithTypeVarReturn(receiverUnderlyingType, m)
                        || isNavigableSetWithTypeVarReturn(receiverUnderlyingType, m)
                        || isSortedSetWithTypeVarReturn(receiverUnderlyingType, m)) {
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
            if (isList(argType.getUnderlyingType()) && argType.hasAnnotation(ORDERNONDET)) {
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
    public boolean mayBeOrderNonDet(TypeMirror javaType) {
        return (javaType.getKind() == TypeKind.ARRAY
                || isCollection(TypesUtils.getTypeElement(javaType).asType())
                || isIterator(TypesUtils.getTypeElement(javaType).asType()));
    }

    /**
     * Adds default annotations for:
     *
     * <ol>
     *   <li>component types of array parameters and returns.
     *   <li>return types of methods with no unannotated or @PolyDet formal parameters and receiver.
     * </ol>
     *
     * Adds implicit annotation for main method parameter.
     */
    protected class DeterminismTypeAnnotator extends TypeAnnotator {
        /** Calls the superclass constructor. */
        public DeterminismTypeAnnotator(DeterminismAnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        /**
         * Places the implicit annotation {@code Det} on the type of the main method's parameter
         * inside the main method body.
         *
         * <p>Places the following default annotations:
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
            if (isMainMethod(executableType.getElement())) {
                AnnotatedTypeMirror paramType = executableType.getParameterTypes().get(0);
                paramType.replaceAnnotation(DET);
            } else {
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
            annoArrType.getComponentType().replaceAnnotation(POLYDET);
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
     * Adds implicit annotation for main method parameter ({@code @Det}) and default annotations for
     * the component types of other array parameters ({@code ...[@PolyDet]}).
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
     * represents the type of a method, constructor or an initializer and the argument to {@code
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
                }

                defaultArrayComponentTypeAsPolyDet(type);
            }
        }
        super.addComputedTypeAnnotations(elt, type);
    }

    /** @return true if {@code tm} is Set or a subtype of Set */
    private boolean isSet(TypeMirror tm) {
        return types.isSubtype(types.erasure(tm), types.erasure(SetInterfaceTypeMirror));
    }

    /** @return true if {@code tm} is a List or a subtype of List */
    public boolean isList(TypeMirror tm) {
        return types.isSubtype(types.erasure(tm), types.erasure(ListInterfaceTypeMirror));
    }

    /** @return true if {@code tm} is Collection or a subtype of Collection */
    public boolean isCollection(TypeMirror tm) {
        return types.isSubtype(types.erasure(tm), types.erasure(CollectionInterfaceTypeMirror));
    }

    /** @return true if {@code tm} is Iterator or a subtype of Iterator */
    public boolean isIterator(TypeMirror tm) {
        return types.isSubtype(types.erasure(tm), types.erasure(IteratorTypeMirror));
    }

    /** @return true if {@code tm} is the Arrays class */
    public boolean isArrays(TypeMirror tm) {
        return types.isSameType(tm, ArraysTypeMirror);
    }

    /** @return true if {@code tm} is the Collections class */
    public boolean isCollections(TypeMirror tm) {
        return types.isSameType(tm, CollectionsTypeMirror);
    }

    /** @return true if {@code tm} is AbstractList or a subtype of AbstractList */
    public boolean isAbstractList(TypeMirror tm) {
        return types.isSubtype(types.erasure(tm), types.erasure(AbstractListTypeMirror));
    }

    /**
     * @return true if {@code tm} is AbstractSequentialList or a subtype of AbstractSequentialList
     */
    public boolean isAbstractSequentialList(TypeMirror tm) {
        return types.isSubtype(types.erasure(tm), types.erasure(AbstractSequentialListTypeMirror));
    }

    /** @return true if {@code tm} is ArrayList or a subtype of ArrayList */
    public boolean isArrayList(TypeMirror tm) {
        return types.isSubtype(types.erasure(tm), types.erasure(ArrayListTypeMirror));
    }

    /** @return true if {@code tm} is LinkedList or a subtype of LinkedList */
    public boolean isLinkedList(TypeMirror tm) {
        return types.isSubtype(types.erasure(tm), types.erasure(LinkedListTypeMirror));
    }

    /** @return true if {@code tm} is NavigableSet or a subtype of NavigableSet */
    public boolean isNavigableSet(TypeMirror tm) {
        return types.isSubtype(types.erasure(tm), types.erasure(NavigableSetTypeMirror));
    }

    /** @return true if {@code tm} is SortedSet or a subtype of SortedSet */
    public boolean isSortedSet(TypeMirror tm) {
        return types.isSubtype(types.erasure(tm), types.erasure(SortedSetTypeMirror));
    }

    /** @return true if {@code tm} is TreeSet or a subtype of TreeSet */
    public boolean isTreeSet(TypeMirror tm) {
        return types.isSubtype(types.erasure(tm), types.erasure(TreeSetTypeMirror));
    }

    /** @return true if {@code tm} is Enumeration or a subtype of Enumeration */
    public boolean isEnumeration(TypeMirror tm) {
        return types.isSubtype(types.erasure(tm), types.erasure(EnumerationTypeMirror));
    }

    /**
     * Returns true if {@code receiverUnderlyingType} is Iterator and if the return type of {@code
     * invokedMethodElement} is a type variable.
     */
    private boolean isIteratorNext(
            TypeElement receiverUnderlyingType, ExecutableElement invokedMethodElement) {
        if (isIterator(receiverUnderlyingType.asType())) {
            if (invokedMethodElement.getSimpleName().contentEquals("next")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if {@code receiverUnderlyingType} is AbstractList, AbstractSequentialList, or
     * List and if the return type of {@code invokedMethodElement} is a type variable.
     */
    private boolean isAbstractListWithTypeVarReturn(
            TypeElement receiverUnderlyingType, ExecutableElement invokedMethodElement) {
        if (isAbstractList(receiverUnderlyingType.asType())
                || isAbstractSequentialList(receiverUnderlyingType.asType())
                || isList(receiverUnderlyingType.asType())) {
            if (invokedMethodElement.getSimpleName().contentEquals("get")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 1
                    && invokedMethodElement.getParameters().get(0).asType().getKind()
                            == TypeKind.INT) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("set")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 2
                    && invokedMethodElement.getParameters().get(0).asType().getKind()
                            == TypeKind.INT
                    && invokedMethodElement.getParameters().get(1).asType().getKind()
                            == TypeKind.TYPEVAR) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("remove")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 1
                    && invokedMethodElement.getParameters().get(0).asType().getKind()
                            == TypeKind.INT) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if {@code receiverUnderlyingType} is NavigableSet and if the return type of
     * {@code invokedMethodElement} is a type variable.
     */
    private boolean isNavigableSetWithTypeVarReturn(
            TypeElement receiverUnderlyingType, ExecutableElement invokedMethodElement) {
        if (isNavigableSet(receiverUnderlyingType.asType())) {
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
            TypeElement receiverUnderlyingType, ExecutableElement invokedMethodElement) {
        if (isArrayList(receiverUnderlyingType.asType())) {
            if (invokedMethodElement.getSimpleName().contentEquals("elementData")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 1
                    && invokedMethodElement.getParameters().get(0).asType().getKind()
                            == TypeKind.INT) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("get")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 1
                    && invokedMethodElement.getParameters().get(0).asType().getKind()
                            == TypeKind.INT) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("set")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 2
                    && invokedMethodElement.getParameters().get(0).asType().getKind()
                            == TypeKind.INT
                    && invokedMethodElement.getParameters().get(1).asType().getKind()
                            == TypeKind.TYPEVAR) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("remove")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 1
                    && invokedMethodElement.getParameters().get(0).asType().getKind()
                            == TypeKind.INT) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if {@code receiverUnderlyingType} is LinkedList and if the return type of {@code
     * invokedMethodElement} is a type variable.
     */
    private boolean isLinkedListWithTypeVarReturn(
            TypeElement receiverUnderlyingType, ExecutableElement invokedMethodElement) {
        if (isLinkedList(receiverUnderlyingType.asType())) {
            if (invokedMethodElement.getSimpleName().contentEquals("unlink")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 1
                    && (types.isSameType(
                            types.erasure(invokedMethodElement.getParameters().get(0).asType()),
                            types.erasure(
                                    TypesUtils.typeFromClass(
                                            Node.class,
                                            types,
                                            processingEnv.getElementUtils()))))) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("getFirst")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().isEmpty()) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("getLast")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().isEmpty()) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("removeFirst")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().isEmpty()) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("removeLast")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().isEmpty()) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("get")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 1
                    && invokedMethodElement.getParameters().get(0).asType().getKind()
                            == TypeKind.INT) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("set")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 2
                    && invokedMethodElement.getParameters().get(0).asType().getKind()
                            == TypeKind.INT
                    && invokedMethodElement.getParameters().get(1).asType().getKind()
                            == TypeKind.TYPEVAR) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("remove")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 1
                    && invokedMethodElement.getParameters().get(0).asType().getKind()
                            == TypeKind.INT) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("peek")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().isEmpty()) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("element")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().isEmpty()) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("poll")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().isEmpty()) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("remove")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().isEmpty()) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("peekFirst")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().isEmpty()) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("peekLast")
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
            if (invokedMethodElement.getSimpleName().contentEquals("pop")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if {@code receiverUnderlyingType} is SortedSet and if the return type of {@code
     * invokedMethodElement} is a type variable.
     */
    private boolean isSortedSetWithTypeVarReturn(
            TypeElement receiverUnderlyingType, ExecutableElement invokedMethodElement) {
        if (isSortedSet(receiverUnderlyingType.asType())) {
            if (invokedMethodElement.getSimpleName().contentEquals("first")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().isEmpty()) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("last")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if {@code receiverUnderlyingType} is TreeSet and if the return type of {@code
     * invokedMethodElement} is a type variable.
     */
    private boolean isTreeSetWithTypeVarReturn(
            TypeElement receiverUnderlyingType, ExecutableElement invokedMethodElement) {
        if (isTreeSet(receiverUnderlyingType.asType())) {
            if (invokedMethodElement.getSimpleName().contentEquals("first")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().isEmpty()) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("last")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().isEmpty()) {
                return true;
            }
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
                    && invokedMethodElement.getParameters().size() == 1
                    && invokedMethodElement.getParameters().get(0).asType().getKind()
                            == TypeKind.TYPEVAR) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("higher")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 1
                    && invokedMethodElement.getParameters().get(0).asType().getKind()
                            == TypeKind.TYPEVAR) {
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
     * Returns true if {@code receiverUnderlyingType} is Enumeration and if the return type of
     * {@code invokedMethodElement} is a type variable.
     */
    private boolean isEnumerationWithTypeVarReturn(
            TypeElement receiverUnderlyingType, ExecutableElement invokedMethodElement) {
        if (isEnumeration(receiverUnderlyingType.asType())) {
            if (invokedMethodElement.getSimpleName().contentEquals("nextElement")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().isEmpty()) {
                return true;
            }
        }
        return false;
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
