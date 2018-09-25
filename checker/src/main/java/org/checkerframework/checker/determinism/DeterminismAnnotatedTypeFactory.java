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
         *   <li>If the annotation on the type of method invocation resolves to {@code OrderNonDet}
         *       and if the return type of the invoked method isn't an array or a collection,
         *       replaces the annotation on {@code annotatedRetType} with {@code @NonDet}.
         *   <li>Return type of equals() gets the annotation {@code @Det}, when both the receiver
         *       and the argument satisfy these conditions (@see <a
         *       href="https://checkerframework.org/manual/#determinism-improved-precision-set-equals">Improves
         *       precision for Set.equals()</a>):
         *       <ol>
         *         <li>the type is {@code @OrderNonDet Set}, and
         *         <li>its type argument is not {@code @OrderNonDet List} or a subtype
         *       </ol>
         *   <li>Annotates the return types of System.getProperty("line.separator") and
         *       System.getProperty("line.separator") as {@code Det}. Usually, the return type of
         *       System.getProperty() is annotated as {@code NonDet}. We make an exception when the
         *       argument is either {@code line.separator} or {@code path.separator} because they
         *       will always produce the same result on the same machine.
         * </ol>
         *
         * @param node method invocation tree
         * @param annotatedRetType type of the method invocation
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

            // Annotates the return types of method calls "System.getProperty("line.separator")"
            // and "System.getProperty("path.separator")" as "@Det"
            ExecutableElement systemGetProperty =
                    TreeUtils.getMethod("java.lang.System", "getProperty", 1, getProcessingEnv());
            if (ElementUtils.isMethod(m, systemGetProperty, getProcessingEnv())) {
                String getPropoertyArgument = node.getArguments().get(0).toString();
                if (getPropoertyArgument.equals("\"" + "line.separator" + "\"")
                        || getPropoertyArgument.equals("\"" + "path.separator" + "\"")) {
                    annotatedRetType.replaceAnnotation(DET);
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
            if (AnnotationUtils.areSame(subAnno, POLYDET)
                    && AnnotationUtils.areSame(superAnno, POLYDET_UP)) {
                return true;
            }
            if (AnnotationUtils.areSame(subAnno, POLYDET_UP)
                    && AnnotationUtils.areSame(superAnno, POLYDET)) {
                return false;
            }
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
