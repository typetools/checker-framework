package org.checkerframework.checker.determinism;

import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
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
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/** The annotated type factory for the determinism type-system. */
public class DeterminismAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
    /** The @PolyDet annotation. */
    public final AnnotationMirror POLYDET;
    /** The @PolyDet("up") annotation. */
    public final AnnotationMirror POLYDET_UP;
    /** The @PolyDet("down") annotation. */
    public final AnnotationMirror POLYDET_DOWN;
    /** The @PolyDet("use") annotation. */
    public final AnnotationMirror POLYDET_USE;
    /** The @NonDet annotation. */
    public final AnnotationMirror NONDET = AnnotationBuilder.fromClass(elements, NonDet.class);
    /** The @OrderNonDet annotation. */
    public final AnnotationMirror ORDERNONDET =
            AnnotationBuilder.fromClass(elements, OrderNonDet.class);
    /** The @Det annotation. */
    public final AnnotationMirror DET = AnnotationBuilder.fromClass(elements, Det.class);
    /** The Set interface. */
    private final TypeMirror SetInterfaceTypeMirror =
            TypesUtils.typeFromClass(Set.class, types, processingEnv.getElementUtils());
    /** The List interface. */
    private final TypeMirror ListInterfaceTypeMirror =
            TypesUtils.typeFromClass(List.class, types, processingEnv.getElementUtils());
    /** The Collection class. */
    private final TypeMirror CollectionInterfaceTypeMirror =
            TypesUtils.typeFromClass(Collection.class, types, processingEnv.getElementUtils());
    /** The Iterator class. */
    private final TypeMirror IteratorTypeMirror =
            TypesUtils.typeFromClass(Iterator.class, types, processingEnv.getElementUtils());
    /** The Arrays class. */
    private final TypeMirror ArraysTypeMirror =
            TypesUtils.typeFromClass(Arrays.class, types, processingEnv.getElementUtils());
    /** The Collections class. */
    private final TypeMirror CollectionsTypeMirror =
            TypesUtils.typeFromClass(Collections.class, types, processingEnv.getElementUtils());

    /**
     * Error message key for explicitly annotating main method parameter as anything other that
     * {@code @Det}.
     */
    private static final @CompilerMessageKey String INVALID_ANNOTATION_ON_PARAMETER =
            "invalid.annotation.on.parameter";

    public DeterminismAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        POLYDET = newPolyDet("");
        POLYDET_USE = newPolyDet("use");
        POLYDET_UP = newPolyDet("up");
        POLYDET_DOWN = newPolyDet("down");

        postInit();
    }

    /** Creates an AnnotationMirror for {@code @PolyDet} with the given argument. */
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
         *   <li>Return type of equals() called on a receiver of type {@code OrderNonDet Set} gets
         *       the {@code @Det} annotation under the following conditions:
         *       <ol>
         *         <li>The receiver does not have {@code List} or its subtype as a type parameter
         *         <li>The argument to equals() is also an {@code @OrderNonDet Set}
         *         <li>The argument to equals() also does not have {@code List} or its subtype as a
         *             type parameter
         *       </ol>
         * </ol>
         *
         * @param node method invocation tree
         * @param p annotated return type
         * @return visitMethodInvocation() of the super class
         */
        @Override
        public Void visitMethodInvocation(MethodInvocationTree node, AnnotatedTypeMirror p) {
            AnnotatedTypeMirror receiverType = atypeFactory.getReceiverType(node);

            // ReceiverType is null for abstract classes
            // (Example: Ordering.natural() in tests/all-systems/PolyCollectorTypeVars.java)
            if (receiverType == null) {
                return super.visitMethodInvocation(node, p);
            }

            AnnotatedTypeMirror.AnnotatedExecutableType invokedMethod =
                    atypeFactory.methodFromUse(node).methodType;
            ExecutableElement invokedMethodElement = invokedMethod.getElement();

            // Checks if return type (non-array, non-collection, and non-iterator) resolves to
            // @OrderNonDet.
            // If the check succeeds, the annotation on the return type is replaced with @NonDet.
            if (p.getAnnotations().contains(ORDERNONDET)
                    && !mayBeOrderNonDet(p.getUnderlyingType())) {
                p.replaceAnnotation(NONDET);
            }

            // TODO: This doesn't say where a reader of the code can find "the specification"?  You
            // should refer to the manual.  Is this behavior documented there?
            // Annotates the return type of "equals()" method called on a Set receiver
            // as described in the specification.
            // Example1: @OrderNonDet Set<@OrderNonDet List<@Det Integer>> s1;
            //           @OrderNonDet Set<@OrderNonDet List<@Det Integer>> s2;
            // s1.equals(s2) is @NonDet

            // Example 2: @OrderNonDet Set<@Det List<@Det Integer>> s1;
            //            @OrderNonDet Set<@Det List<@Det Integer>> s2;
            // s1.equals(s2) is @Det
            // TODO-rashmi: this can be more precise (@Det receiver and @OrderNonDet parameter)
            TypeElement receiverUnderlyingType =
                    TypesUtils.getTypeElement(receiverType.getUnderlyingType());

            // Without this check, NullPointerException in Collections class with buildJdk.
            // TODO-rashmi: check why?
            if (receiverUnderlyingType == null) {
                return super.visitMethodInvocation(node, p);
            }

            if (isEqualsMethod(invokedMethodElement)
                    && isSet(receiverUnderlyingType.asType())
                    && AnnotationUtils.areSame(
                            receiverType.getAnnotations().iterator().next(), ORDERNONDET)) {
                // Checks that the receiverType does not have "@OrderNonDet List" as a type
                // parameter
                if (!hasOrderNonDetListAsTypeParameter(receiverType)) {
                    AnnotatedTypeMirror parameter =
                            atypeFactory.getAnnotatedType(node.getArguments().get(0));
                    if (isSet(TypesUtils.getTypeElement(parameter.getUnderlyingType()).asType())
                            && parameter.hasAnnotation(ORDERNONDET)) {
                        // Checks that the parameter does not have "@OrderNonDet List" as a
                        // type parameter
                        if (!hasOrderNonDetListAsTypeParameter(parameter)) {
                            p.replaceAnnotation(DET);
                        }
                    }
                }
            }
            return super.visitMethodInvocation(node, p);
        }

        /** If array a is {@code @NonDet}, then a.length is {@code @NonDet}. */
        @Override
        public Void visitMemberSelect(
                MemberSelectTree node, AnnotatedTypeMirror annotatedTypeMirror) {
            if (TreeUtils.isArrayLengthAccess(node)) {
                AnnotatedTypeMirror.AnnotatedArrayType arrType =
                        (AnnotatedTypeMirror.AnnotatedArrayType)
                                atypeFactory.getAnnotatedType(node.getExpression());
                if (AnnotationUtils.areSame(arrType.getAnnotations().iterator().next(), NONDET)) {
                    annotatedTypeMirror.replaceAnnotation(NONDET);
                }
            }
            return super.visitMemberSelect(node, annotatedTypeMirror);
        }
    }

    /** Returns true if {@code @OrderNonDet List} appears as a type parameter in {@code atm}. */
    private boolean hasOrderNonDetListAsTypeParameter(AnnotatedTypeMirror atm) {
        AnnotatedTypeMirror.AnnotatedDeclaredType declaredType =
                (AnnotatedTypeMirror.AnnotatedDeclaredType) atm;
        for (AnnotatedTypeMirror argType : declaredType.getTypeArguments()) {
            if (isList(argType.getUnderlyingType()) && argType.hasAnnotation(ORDERNONDET)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if it is valid for {@code javaType} to have {@code @OrderNonDet} annotation.
     *
     * @param javaType the declared type to be checked
     * @return true if {@code javaType} is a Collection (or its subtype) or Iterator (or its
     *     subtype) or an array
     */
    public boolean mayBeOrderNonDet(TypeMirror javaType) {
        if (javaType.getKind() == TypeKind.ARRAY
                || isCollection(TypesUtils.getTypeElement(javaType).asType())
                || isIterator(TypesUtils.getTypeElement(javaType).asType())) {
            return true;
        }
        return false;
    }

    protected class DeterminismTypeAnnotator extends TypeAnnotator {
        public DeterminismTypeAnnotator(DeterminismAnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        /**
         * Places the following implicit annotation of {@code Det} on main method parameter. Places
         * the following default annotations:
         *
         * <ol>
         *   <li>Annotates array parameters and return types as {@code @PolyDet[@PolyDet]}.
         *   <li>Annotates the return type for static methods without any parameters as
         *       {@code @Det}.
         * </ol>
         */
        @Override
        public Void visitExecutable(
                final AnnotatedTypeMirror.AnnotatedExecutableType t, final Void p) {
            if (isMainMethod(t.getElement())) {
                AnnotatedTypeMirror paramType = t.getParameterTypes().get(0);
                if (paramType.getAnnotations().size() > 0 && !paramType.hasAnnotation(DET)) {
                    checker.report(Result.failure(INVALID_ANNOTATION_ON_PARAMETER), t);
                }
                paramType.replaceAnnotation(DET);
            } else {
                // Annotates array return types as @PolyDet[@PolyDet]
                AnnotatedTypeMirror retType = t.getReturnType();
                annotateArrayElementAsPolyDet(retType);

                // Annotates array parameter types as @PolyDet[@PolyDet]
                List<AnnotatedTypeMirror> paramTypes = t.getParameterTypes();
                for (AnnotatedTypeMirror paramType : paramTypes) {
                    annotateArrayElementAsPolyDet(paramType);
                }

                // If the invoked method is static and has no arguments,
                // its return type is annotated as @Det.
                if (ElementUtils.isStatic(t.getElement())) {
                    if (t.getElement().getParameters().size() == 0) {
                        if (t.getReturnType().getExplicitAnnotations().size() == 0) {
                            t.getReturnType().replaceAnnotation(DET);
                        }
                    }
                }
            }
            return super.visitExecutable(t, p);
        }
    }

    /**
     * Helper method that annotates component type of the array type {@code arrType} as @PolyDet.
     */
    private void annotateArrayElementAsPolyDet(AnnotatedTypeMirror arrType) {
        if (arrType.getKind() == TypeKind.ARRAY) {
            AnnotatedTypeMirror.AnnotatedArrayType arrParamType =
                    (AnnotatedTypeMirror.AnnotatedArrayType) arrType;
            if (arrParamType.getAnnotations().size() == 0) {
                if (arrParamType.getComponentType().getUnderlyingType().getKind()
                        != TypeKind.TYPEVAR) {
                    arrParamType.getComponentType().replaceAnnotation(POLYDET);
                }
            }
        }
    }

    /** @return true if {@code method} is equals */
    public static boolean isEqualsMethod(ExecutableElement method) {
        if (method.getReturnType().getKind() == TypeKind.BOOLEAN
                && method.getSimpleName().contentEquals("equals")
                && method.getParameters().size() == 1
                && TypesUtils.isObject(method.getParameters().get(0).asType())) {
            return true;
        }
        return false;
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
     * Adds default annotations for main method parameters ({@code @Det}) and other array parameters
     * ({@code @PolyDet[@PolyDet]}).
     */
    @Override
    public void addComputedTypeAnnotations(Element elt, AnnotatedTypeMirror type) {
        // TODO: This logic is very similar to logic elsewhere in this file.  Why is it duplicated?
        // When is each used?  Does the need for duplication indicate a problem in your design, or a
        // limitation in the Checker Framework?
        if (elt.getKind() == ElementKind.PARAMETER) {
            if (elt.getEnclosingElement().getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) elt.getEnclosingElement();
                if (isMainMethod(method)) {
                    type.addMissingAnnotations(Collections.singleton(DET));
                } else if (type.getKind() == TypeKind.ARRAY && type.getAnnotations().size() == 0) {
                    AnnotatedTypeMirror.AnnotatedArrayType arrType =
                            (AnnotatedTypeMirror.AnnotatedArrayType) type;
                    if (arrType.getComponentType().getKind() != TypeKind.TYPEVAR) {
                        arrType.getComponentType()
                                .addMissingAnnotations(Collections.singleton(POLYDET));
                    }
                }
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
