package org.checkerframework.checker.determinism;

import com.sun.source.tree.*;
import java.lang.annotation.Annotation;
import java.util.*;
import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.determinism.qual.*;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.*;
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
import org.checkerframework.javacutil.*;

/** The annotated type factory for the determinism type-system. */
public class DeterminismAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
    /** Annotation constants for @PolyDet. */
    public final AnnotationMirror POLYDET, POLYDET_USE, POLYDET_UP, POLYDET_DOWN;
    /** Annotation constant for @NonDet. */
    public final AnnotationMirror NONDET = AnnotationBuilder.fromClass(elements, NonDet.class);
    /** Annotation constant for @OrderNonDet. */
    public final AnnotationMirror ORDERNONDET =
            AnnotationBuilder.fromClass(elements, OrderNonDet.class);
    /** Annotation constant for @Det. */
    public final AnnotationMirror DET = AnnotationBuilder.fromClass(elements, Det.class);
    /** TypeMirror constant for the Set interface. */
    private final TypeMirror SetInterfaceTypeMirror =
            TypesUtils.typeFromClass(Set.class, types, processingEnv.getElementUtils());
    /** TypeMirror constant for the List interface. */
    private final TypeMirror ListInterfaceTypeMirror =
            TypesUtils.typeFromClass(List.class, types, processingEnv.getElementUtils());
    /** TypeMirror constant for the Collection class. */
    private final TypeMirror CollectionInterfaceTypeMirror =
            TypesUtils.typeFromClass(Collection.class, types, processingEnv.getElementUtils());
    /** TypeMirror constant for the Iterator class. */
    private final TypeMirror IteratorTypeMirror =
            TypesUtils.typeFromClass(Iterator.class, types, processingEnv.getElementUtils());
    /** TypeMirror constant for the Arrays class. */
    private final TypeMirror ArraysTypeMirror =
            TypesUtils.typeFromClass(Arrays.class, types, processingEnv.getElementUtils());
    /** TypeMirror constant for the Collections class. */
    private final TypeMirror CollectionsTypeMirror =
            TypesUtils.typeFromClass(Collections.class, types, processingEnv.getElementUtils());

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
         * Places the default annotation on the return type of a method invocation as follows:
         *
         * <ol>
         *   <li>The return type for static methods without any argument is {@code @Det}.
         *   <li>If {@code @PolyDet} resolves to {@code OrderNonDet} on a return type that isn't an
         *       array or a collection, it defaults to {@code @NonDet}.
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
            AnnotatedTypeMirror receiver = atypeFactory.getReceiverType(node);

            // Receiver is null for abstract classes
            // (Example: Ordering.natural() in tests/all-systems/PolyCollectorTypeVars.java)
            if (receiver == null) {
                return super.visitMethodInvocation(node, p);
            }

            AnnotatedTypeMirror.AnnotatedExecutableType invokedMethod =
                    atypeFactory.methodFromUse(node).methodType;
            ExecutableElement invokedMethodElement = invokedMethod.getElement();

            // Checks if return type (non-array and non-collection) resolves to @OrderNonDet.
            // If the check succeeds, the annotation on the return type is replaced with @NonDet.
            if (p.getAnnotations().contains(ORDERNONDET)
                    && !(p.getUnderlyingType().getKind() == TypeKind.ARRAY)
                    && !(isCollection(TypesUtils.getTypeElement(p.getUnderlyingType()).asType()))
                    && !(isIterator(TypesUtils.getTypeElement(p.getUnderlyingType()).asType()))) {
                p.replaceAnnotation(NONDET);
            }

            // For static methods with no arguments, this sets the default annotation to be @Det.
            if (ElementUtils.isStatic(invokedMethodElement)) {
                if (node.getArguments().size() == 0) {
                    if (p.getExplicitAnnotations().size() == 0) {
                        p.replaceAnnotation(DET);
                    }
                }
            }

            // Annotates the return type of "equals()" method called on a Set receiver
            // as described in the specification.
            // Example1: @OrderNonDet Set<@OrderNonDet List<@Det Integer>> s1;
            //           @OrderNonDet Set<@OrderNonDet List<@Det Integer>> s2;
            // s1.equals(s2) is @Det

            // Example 2: @OrderNonDet Set<@Det List<@Det Integer>> s1;
            //            @OrderNonDet Set<@Det List<@Det Integer>> s2;
            // s1.equals(s2) is @NonDet
            // TODO: this can be more precise (@Det receiver and @OrderNonDet parameter)
            TypeElement receiverUnderlyingType =
                    TypesUtils.getTypeElement(receiver.getUnderlyingType());

            // Without this check, NullPointerException in Collections class with buildJdk.
            // TODO: check why?
            if (receiverUnderlyingType == null) {
                return super.visitMethodInvocation(node, p);
            }

            if (isEqualsMethod(invokedMethodElement)
                    && isSet(receiverUnderlyingType.asType())
                    && AnnotationUtils.areSame(
                            receiver.getAnnotations().iterator().next(), ORDERNONDET)) {
                // Checks that the receiver does not have "@OrderNonDet List" as a type parameter
                if (!hasOrderNonDetListAsTypeParameter(receiver)) {
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

        /**
         * Annotates the length property of an array annotated as {@code @NonDet} to be
         * {@code @NonDet}.
         */
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

    /**
     * Checks if {@code @OrderNonDet List} appears as a type parameter in {@code atm} and returns
     * true if the checks succeeds.
     */
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

    protected class DeterminismTypeAnnotator extends TypeAnnotator {
        public DeterminismTypeAnnotator(DeterminismAnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        /**
         * Places the following default annotations:
         *
         * <ol>
         *   <li>Annotates the main method parameter as {@code Det}.
         *   <li>Annotates array parameters and return types as {@code @PolyDet[@PolyDet]}.
         * </ol>
         */
        @Override
        public Void visitExecutable(
                final AnnotatedTypeMirror.AnnotatedExecutableType t, final Void p) {
            if (isMainMethod(t.getElement())) {
                AnnotatedTypeMirror paramType = t.getParameterTypes().get(0);
                paramType.replaceAnnotation(DET);
            } else {
                // Annotates the array return types as @PolyDet[@PolyDet]
                AnnotatedTypeMirror retType = t.getReturnType();
                if (retType.getKind() == TypeKind.ARRAY) {
                    AnnotatedTypeMirror.AnnotatedArrayType arrRetType =
                            (AnnotatedTypeMirror.AnnotatedArrayType) retType;
                    if (arrRetType.getAnnotations().size() == 0
                            && arrRetType.getComponentType().getUnderlyingType().getKind()
                                    != TypeKind.TYPEVAR) {
                        arrRetType.replaceAnnotation(POLYDET);
                        arrRetType.getComponentType().replaceAnnotation(POLYDET);
                    }
                }

                // Annotates the array parameter types as @PolyDet[@PolyDet]
                List<AnnotatedTypeMirror> paramTypes = t.getParameterTypes();
                for (AnnotatedTypeMirror paramType : paramTypes) {
                    if (paramType.getKind() == TypeKind.ARRAY
                            && paramType.getUnderlyingType().getKind() != TypeKind.TYPEVAR) {
                        AnnotatedTypeMirror.AnnotatedArrayType arrParamType =
                                (AnnotatedTypeMirror.AnnotatedArrayType) paramType;
                        if (arrParamType.getAnnotations().size() == 0) {
                            arrParamType.replaceAnnotation(POLYDET);
                            arrParamType.getComponentType().replaceAnnotation(POLYDET);
                        }
                    }
                }
            }
            return super.visitExecutable(t, p);
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
     * Adds default annotations for main method parameters ({@code @Det}) and array parameters
     * ({@code @PolyDet[@PolyDet]}).
     */
    @Override
    public void addComputedTypeAnnotations(Element elt, AnnotatedTypeMirror type) {
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
                        type.addMissingAnnotations(Collections.singleton(POLYDET));
                    }
                }
            }
        }
        super.addComputedTypeAnnotations(elt, type);
    }

    /** @return true if {@code tm} is a Set or a subtype of Set */
    private boolean isSet(TypeMirror tm) {
        if (types.isSubtype(types.erasure(tm), types.erasure(SetInterfaceTypeMirror))) {
            return true;
        }
        return false;
    }

    /** @return true if {@code tm} is a List or a subtype of List */
    public boolean isList(TypeMirror tm) {
        // List and subclasses
        if (types.isSubtype(types.erasure(tm), types.erasure(ListInterfaceTypeMirror))) {
            return true;
        }
        return false;
    }

    /** @return true if {@code tm} is a Collection or a subtype of Collection */
    public boolean isCollection(TypeMirror tm) {
        javax.lang.model.util.Types types = processingEnv.getTypeUtils();
        if (types.isSubtype(types.erasure(tm), types.erasure(CollectionInterfaceTypeMirror))) {
            return true;
        }
        return false;
    }

    /** @return true if {@code tm} is an Iterator or a subtype of Iterator */
    public boolean isIterator(TypeMirror tm) {
        javax.lang.model.util.Types types = processingEnv.getTypeUtils();
        if (types.isSubtype(tm, IteratorTypeMirror)) {
            return true;
        }
        return false;
    }

    /** @return true if {@code tm} is the Arrays class */
    public boolean isArrays(TypeMirror tm) {
        if (types.isSameType(tm, ArraysTypeMirror)) {
            return true;
        }
        return false;
    }

    /** @return true if {@code tm} is a the Collections class */
    public boolean isCollections(TypeMirror tm) {
        if (types.isSameType(tm, CollectionsTypeMirror)) {
            return true;
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
         * Treat {@code @PolyDet} with values as {@code @PolyDet} without values in the qualifier
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
