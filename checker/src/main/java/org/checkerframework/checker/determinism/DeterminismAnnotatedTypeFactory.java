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
    // TODO: What order are these annotations in?  PolyDet doesn't usually appear first in lists.
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

    public DeterminismAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        POLYDET = newPolyDet("");
        POLYDET_USE = newPolyDet("use");
        POLYDET_UP = newPolyDet("up");
        POLYDET_DOWN = newPolyDet("down");

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

        // TODO: p is a poor name for a parameter documented as "annotated return type".
        /**
         * Replaces the annotation on the return type of a method invocation as follows:
         *
         * <ol>
         *   <li>If {@code @PolyDet} resolves to {@code OrderNonDet} on a return type that isn't an
         *       array or a collection, it is replaced with {@code @NonDet}.
         *   <li>Return type of equals() gets the annotation {@code @Det}, when both the receiver
         *       and the argument satisfy these conditions::
         *       <ol>
         *         <li>the type is {@code OrderNonDet Set}, and
         *         <li>it does not have {@code List} or its subtype as a type argument
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

            ExecutableElement m = atypeFactory.methodFromUse(node).methodType.getElement();

            // If return type (non-array, non-collection, and non-iterator) resolves to
            // @OrderNonDet, replaces the annotation on the return type with @NonDet.
            if (p.getAnnotations().contains(ORDERNONDET)
                    && !mayBeOrderNonDet(p.getUnderlyingType())) {
                p.replaceAnnotation(NONDET);
            }

            // TODO: The section number will change due to edits such as insertion/deletion of other
            //   chapters.  Use a URL instead, such as
            //   https://checkerframework.org/manual/#handling-imprecision .
            // Annotates the return type of "equals()" method called on a Set receiver
            // as described in section 11.3.1 of the manual under the heading "Handling
            // Imprecision".
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
            // TODO: At the least, indicate when the value is null, similarly to the comment
            // about when receiverType can be null.
            // TODO-rashmi: check why?
            if (receiverUnderlyingType == null) {
                return super.visitMethodInvocation(node, p);
            }

            if (isEqualsMethod(m)) {
                AnnotatedTypeMirror argument =
                        atypeFactory.getAnnotatedType(node.getArguments().get(0));
                // TODO: I restructured this so the tests for the two arguments are easier to
                // compare.  Why are the tests for ORDERNONDET different for the two arguments?
                // More generally, remove all occurrences of idioms like ".iterator().next()" from
                // your code.  It is brittle and may break during future refactoring.
                if (isSet(receiverUnderlyingType.asType())
                        && receiverType.hasAnnotation(ORDERNONDET)
                        && !hasOrderNonDetListAsTypeArgument(receiverType)
                        && isSet(TypesUtils.getTypeElement(argument.getUnderlyingType()).asType())
                        && argument.hasAnnotation(ORDERNONDET)
                        && !hasOrderNonDetListAsTypeArgument(argument)) {
                    p.replaceAnnotation(DET);
                }
            }
            return super.visitMethodInvocation(node, p);
        }

        /** Annotates the length property of a {@code @NonDet} array as {@code @NonDet}. */
        @Override
        public Void visitMemberSelect(
                MemberSelectTree node, AnnotatedTypeMirror annotatedTypeMirror) {
            if (TreeUtils.isArrayLengthAccess(node)) {
                AnnotatedTypeMirror.AnnotatedArrayType arrType =
                        (AnnotatedTypeMirror.AnnotatedArrayType)
                                atypeFactory.getAnnotatedType(node.getExpression());
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
     * Returns true if {@code javaType} may be annotated as {@code @OrderNonDet}.
     *
     * @param javaType the declared type to be checked
     * @return true if {@code javaType} is Collection (or a subtype), Iterator (or a subtype), or an
     *     array
     */
    public boolean mayBeOrderNonDet(TypeMirror javaType) {
        return (javaType.getKind() == TypeKind.ARRAY
                || isCollection(TypesUtils.getTypeElement(javaType).asType())
                || isIterator(TypesUtils.getTypeElement(javaType).asType()));
    }

    protected class DeterminismTypeAnnotator extends TypeAnnotator {
        public DeterminismTypeAnnotator(DeterminismAnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        // TODO: I don't see where this handles the main method.  Is the documentation wrong?
        // TODO: What is the "incoming argument"?  If it is just `a`, say that (be specific).
        // TODO: Don't use passive voice, such as "is treated as".  What does the action?  Is this
        // related to the declarative mechanism for specifying defaults, which you are working
        // around here?
        /**
         * Places the implicit annotation {@code Det} on the type of the main method's parameter.
         * Places the following default annotations:
         *
         * <ol>
         *   <li>Annotates unannotated array arguments and return types as
         *       {@code @PolyDet[@PolyDet]}.
         *   <li>Annotates the return type for static methods without any parameters as
         *       {@code @Det}.
         * </ol>
         *
         * <p>Example: Consider the following code:
         *
         * <pre><code>
         * &nbsp; void testArr(int[] a) {
         * &nbsp; @Det int i = a[0];
         * &nbsp; }
         * </code></pre>
         *
         * Here, the line {@code @Det int i = a[0];} should be flagged as an error since {@code
         * a[0]} is {@code @PolyDet}. Without the method {@code visitExecutable}, the incoming
         * argument to {@code testArr} is treated as {@code @PolyDet[@Det]} and the line {@code @Det
         * int i = a[0];} is not flagged as an error by the checker.
         */
        @Override
        public Void visitExecutable(
                final AnnotatedTypeMirror.AnnotatedExecutableType t, final Void p) {
            annotateArrayElementAsPolyDet(t.getReturnType());
            for (AnnotatedTypeMirror paramType : t.getParameterTypes()) {
                annotateArrayElementAsPolyDet(paramType);
            }

            // Annotates the return type of a static method without parameters as @Det.
            if (ElementUtils.isStatic(t.getElement())) {
                if (t.getElement().getParameters().size() == 0) {
                    if (t.getReturnType().getExplicitAnnotations().size() == 0) {
                        t.getReturnType().replaceAnnotation(DET);
                    }
                }
            }
            return super.visitExecutable(t, p);
        }
    }

    // TODO: This documentation seems incorrect.  Does this do defaulting (only if the method is not
    // annotated) rather than unconditionally annotating?  In that case, the name of the method
    // should be changed too, to indicate that it does defaulting.
    // TODO: This does not handle multidimensional arrays.  Should it?  How should int[][][] get
    // defaulted?
    /**
     * Helper method that annotates component type of the array type {@code arrType} as @PolyDet.
     */
    private void annotateArrayElementAsPolyDet(AnnotatedTypeMirror arrType) {
        if (arrType.getKind() == TypeKind.ARRAY) {
            AnnotatedTypeMirror.AnnotatedArrayType arrParamType =
                    (AnnotatedTypeMirror.AnnotatedArrayType) arrType;
            // TODO: Should this use getExplicitAnnotations instead of getAnnotations?
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

    // TODO: This claims to do {@code @PolyDet[@PolyDet]} but it seems only to do the {@code
    // ...[@PolyDet]} part.  Document what it does.  I suspect that the {@code @PolyDet[...]} part
    // is handled by the declarative mechanism, but if so, please say that rather than leaving it
    // undocumented.
    /**
     * Adds implicit annotation for main method parameter ({@code @Det}) and default annotations for
     * other array parameters ({@code @PolyDet[@PolyDet]}).
     *
     * <p>Example: Consider the following code:
     *
     * <pre><code>
     * &nbsp; void testArr(int[] a) {
     * &nbsp; &nbsp; ...
     * &nbsp; }
     * </code></pre>
     *
     * This method {@code addComputedTypeAnnotations} annotates the parameter {@code int[] a} as
     * {@code @PolyDet int @PolyDet[] a}.
     */
    @Override
    public void addComputedTypeAnnotations(Element elt, AnnotatedTypeMirror type) {
        if (elt.getKind() == ElementKind.PARAMETER) {
            if (elt.getEnclosingElement().getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) elt.getEnclosingElement();
                if (isMainMethod(method)) {
                    if (type.getAnnotations().size() > 0 && !type.hasAnnotation(DET)) {
                        checker.report(Result.failure("invalid.annotation.on.parameter"), elt);
                    }
                    type.addMissingAnnotations(Collections.singleton(DET));
                    // TODO: Should this use getExplicitAnnotations instead of getAnnotations?
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
