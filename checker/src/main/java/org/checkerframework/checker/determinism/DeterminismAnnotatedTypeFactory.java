package org.checkerframework.checker.determinism;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import java.lang.annotation.Annotation;
import java.util.*;
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
    /** The java.util.AbstractList class. */
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

        // TODO: The below comment "is not {@code List}" seems wrong.  It should be "@OrderNonDet
        // List", right?
        /**
         * Replaces the annotation on the return type of a method invocation as follows:
         *
         * <ol>
         *   <li>If {@code @PolyDet} resolves to {@code OrderNonDet} on a return type that isn't an
         *       array or a collection, it is replaced with {@code @NonDet}.
         *   <li>Return type of equals() gets the annotation {@code @Det}, when both the receiver
         *       and the argument satisfy these conditions:
         *       <ol>
         *         <li>the type is {@code @OrderNonDet Set}, and
         *         <li>its type argument is not {@code List} or a subtype
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

            // TODO: Why is this referring to the manual rather than the documentation at the top of
            // the method?  This implies they are different.  Why doesn't the method docementation
            // reference the manual?

            // Annotates the return type of "equals()" method called on a Set receiver
            // as described in
            // https://checkerframework.org/manual/#determinism-improved-precision-set-equals.

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
        // TODO: The structure of isReceiverOrArgPoly is inconsistent with this method and with
        // isReceiverAndArgsDetOrOrderNonDet.
        // isReceiverOrArgPoly uses a short-cutting `return` but this does not.  Try to make code
        // use a consistent style.  When a reader sees a difference in coding paradigms, the reader
        // will assume there is a specific reason for it.  I would use the short-cutting version,
        // because it's no longer, but has less indentation; but either way, give the code similar
        // style.
        if (receiverType.hasAnnotation(DET)) {
            for (ExpressionTree arg : node.getArguments()) {
                AnnotatedTypeMirror argType = getAnnotatedType(arg);
                if (!argType.hasAnnotation(DET)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Returns true if all the arguments of the method invocation {@code node} and the {@code
     * receiverType} are annotated either as {@code @Det} or {@code @OrderNonDet}.
     */
    private boolean isReceiverAndArgsDetOrOrderNonDet(
            AnnotatedTypeMirror receiverType, MethodInvocationTree node) {
        if (receiverType.hasAnnotation(DET) || receiverType.hasAnnotation(ORDERNONDET)) {
            for (ExpressionTree arg : node.getArguments()) {
                AnnotatedTypeMirror argType = getAnnotatedType(arg);
                if (argType.hasAnnotation(NONDET)) {
                    return false;
                }
            }
            return true;
        }
        return false;
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

    protected class DeterminismTypeAnnotator extends TypeAnnotator {
        public DeterminismTypeAnnotator(DeterminismAnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        // TODO: in the comment, "arguments" should be "parameters".
        // TODO: its' inconsistent that the first bullet point says "annotates unannotated" but the
        // second bullet point says "annotates", which implies it is not defaulting.  You could
        // replace both by "Defaults" (using "default" as a verb).
        // TODO: The method has 3 features, but the example relates to the middle one.  There is no
        // indication of this in the comment, which makes it confusing to read.
        // TODO: The explanation is confusing.  It says that "a[0] is @PolyDet", but that does not
        // seem to be true, and the whole point of this method is that it isn't true.  (Maybe it
        // *should* be true, but that's different.)  I'm not sure a counterfactual is helpful here.
        // If you want to discuss problems, talk about types (what the type of an expression should
        // be) rather than talking about what "would be flagged as an error", which requires further
        // reasoning about all the behaviors of the checker, when the point here is to set the type
        // of an expression.  Likewise, the assignment "i = " is a distraction.  You could just as
        // well have written "... a[0] ..." to show that a[0] is being used as an rvalue, without
        // bringing in assignment which isn't relevant to the discussion and therefore is more
        // likely to confuse than to clarify.
        /**
         * Places the implicit annotation {@code Det} on the type of the main method's parameter.
         *
         * <p>Places the following default annotations:
         *
         * <ol>
         *   <li>Annotates unannotated component types of array arguments and return types as {@code
         *       ...[@PolyDet]}.
         *   <li>Annotates the return type for methods with no @PolyDet formal parameters (including
         *       the receiver) as {@code @Det}.
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
         * a[0]} is {@code @PolyDet}. Without the method {@code visitExecutable}, the argument
         * {@code a} defaults to {@code @PolyDet[@Det]} and the line {@code @Det int i = a[0];} is
         * not flagged as an error by the checker.
         */
        @Override
        public Void visitExecutable(final AnnotatedExecutableType t, final Void p) {
            if (isMainMethod(t.getElement())) {
                // TODO: There is repeated logic for this case, both in method
                // addComputedTypeAnnotations and in this method.  Please avoid duplication:  put
                // the logic in just one place.  Or explain why it is essential for it to be
                // duplicated (but I would not expect that to be necessary).
                AnnotatedTypeMirror paramType = t.getParameterTypes().get(0);
                paramType.replaceAnnotation(DET);
            } else {
                for (AnnotatedTypeMirror paramType : t.getParameterTypes()) {
                    defaultArrayElementAsPolyDet(paramType);
                }

                // t.getReceiverType() is null for both "Object <init>()"
                // and for static methods.
                // TODO: It's considered better style (and may be more efficient) to use
                // ".isempty()" rather than ".size() == 0".
                if (t.getReturnType().getAnnotations().size() == 0
                        && (t.getReceiverType() == null)) {
                    boolean unannotatedOrPolyDet = false;
                    for (AnnotatedTypeMirror paramType : t.getParameterTypes()) {
                        // The default is @PolyDet, so treat unannotated the same as @PolyDet
                        if (paramType.getAnnotations().size() == 0
                                || paramType.hasAnnotation(POLYDET)) {
                            unannotatedOrPolyDet = true;
                            break;
                        }
                    }
                    if (!unannotatedOrPolyDet) {
                        t.getReturnType().replaceAnnotation(DET);
                    }
                }
                defaultArrayElementAsPolyDet(t.getReturnType());
            }
            return super.visitExecutable(t, p);
        }
    }

    // TODO: It would be great to handle as many TODOs as possible.  Otherwise, once you address
    // them, the code has to be reviewed again.
    // TODO-rashmi: handle multidimensional arrays - here, addComputedTypes, DeterminismVisitor
    // and test.
    // TODO: You never need to write "Helper method that".  It is obvious from the `private`
    // modifier, and documentation should focus first on what the method does, and only secondarily
    // on how it is intended to be used.
    // TODO: It's inconsistent that the documentation says "component type" and the method name is
    // "element type".
    // TODO: The documentation "the array type" indicates that the argument must be an array.  That
    // is not true.  The parameter name `arrType` is also misleading, though clear documentatino
    // might be enough to fix that problem.
    /**
     * Helper method that places the default annotation on component type of the array type {@code
     * arrType} as @PolyDet.
     */
    private void defaultArrayElementAsPolyDet(AnnotatedTypeMirror arrType) {
        if (arrType.getKind() == TypeKind.ARRAY) {
            // TODO: do not capitalize variable names.
            AnnotatedArrayType AnnoArrType = (AnnotatedArrayType) arrType;
            // TODO: It's unclear how this example is related to the method.  What is it an example
            // of?  The example code is a method call, so it cannot be the argument to this method,
            // and the doucmentation gives no indication of how the method was called.  Examples
            // with no context appeared elsewhere in the code, too, and it does more harm than good.
            // Example: @Det int @Det[] returnArrExplicit(){}
            // Here, AnnoArrType is @Det int @Det[].
            // TODO: what is the point of the facts on the following two lines?  It's confusing to
            // state facts without indicating why they are relevant or what the reader should infer
            // from them.
            // arrParamType.getExplicitAnnotations().size() returns 0,
            // arrParamType.getAnnotations().size() returns 1.
            // getExplicitAnnotations works only with type use locations?
            if (AnnoArrType.getAnnotations().size() == 0) {
                if (AnnoArrType.getComponentType().getUnderlyingType().getKind()
                        != TypeKind.TYPEVAR) {
                    AnnoArrType.getComponentType().replaceAnnotation(POLYDET);
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
     */
    // TODO: How is this method related to visitExecutable?  The logic seems very similar.  Why does
    // it have to be duplicated?  If it's duplicated, why is the style/structure of the code
    // different in the two methods?
    @Override
    public void addComputedTypeAnnotations(Element elt, AnnotatedTypeMirror type) {
        if (elt.getKind() == ElementKind.PARAMETER) {
            if (elt.getEnclosingElement().getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) elt.getEnclosingElement();
                if (isMainMethod(method)) {
                    if (type.getAnnotations().size() > 0 && !type.hasAnnotation(DET)) {
                        checker.report(
                                Result.failure(
                                        "invalid.annotation.on.parameter",
                                        type.getAnnotationInHierarchy(NONDET)),
                                elt);
                    }
                    type.addMissingAnnotations(Collections.singleton(DET));
                    // TODO: Here is another comment that I cannot understand.  Why is the
                    // testArrParam method declaration important?
                    // Note: void testArrParam(@PolyDet int @PolyDet [] arr) {}
                    // getExplicitAnnotations().size() for arr is 0,
                    // getAnnotations().size() for arr is 1.
                } else if (type.getKind() == TypeKind.ARRAY && type.getAnnotations().size() == 0) {
                    AnnotatedArrayType arrType = (AnnotatedArrayType) type;
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

    // TODO: The term "its subtype" implies there is exactly one subtype (which is rare and not what
    // you mean).  "a subtype" doesn't make this implication.
    /** @return true if {@code tm} is AbstractList or its subtype */
    public boolean isAbstractList(TypeMirror tm) {
        return types.isSubtype(types.erasure(tm), types.erasure(AbstractListTypeMirror));
    }

    /** @return true if {@code tm} is AbstractSequentialList or its subtype */
    public boolean isAbstractSequentialList(TypeMirror tm) {
        return types.isSubtype(types.erasure(tm), types.erasure(AbstractSequentialListTypeMirror));
    }

    /** @return true if {@code tm} is ArrayList or its subtype */
    public boolean isArrayList(TypeMirror tm) {
        return types.isSubtype(types.erasure(tm), types.erasure(ArrayListTypeMirror));
    }

    /** @return true if {@code tm} is LinkedList or its subtype */
    public boolean isLinkedList(TypeMirror tm) {
        return types.isSubtype(types.erasure(tm), types.erasure(LinkedListTypeMirror));
    }

    /** @return true if {@code tm} is NavigableSet or its subtype */
    public boolean isNavigableSet(TypeMirror tm) {
        return types.isSubtype(types.erasure(tm), types.erasure(NavigableSetTypeMirror));
    }

    /** @return true if {@code tm} is SortedSet or its subtype */
    public boolean isSortedSet(TypeMirror tm) {
        return types.isSubtype(types.erasure(tm), types.erasure(SortedSetTypeMirror));
    }

    /** @return true if {@code tm} is TreeSet or its subtype */
    public boolean isTreeSet(TypeMirror tm) {
        return types.isSubtype(types.erasure(tm), types.erasure(TreeSetTypeMirror));
    }

    /** @return true if {@code tm} is Enumeration or its subtype */
    public boolean isEnumeration(TypeMirror tm) {
        return types.isSubtype(types.erasure(tm), types.erasure(EnumerationTypeMirror));
    }

    // TODO: The comment "returns a generic type" is at odds with the method name "isIteratorNext"
    /**
     * Returns true if {@code receiverUnderlyingType} is Iterator and if {@code
     * invokedMethodElement} returns a generic type.
     */
    private boolean isIteratorNext(
            TypeElement receiverUnderlyingType, ExecutableElement invokedMethodElement) {
        if (isIterator(receiverUnderlyingType.asType())) {
            if (invokedMethodElement.getSimpleName().contentEquals("next")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if {@code receiverUnderlyingType} is AbstractList, AbstractSequentialList, or
     * List and if {@code invokedMethodElement} returns a generic type.
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
     * Returns true if {@code receiverUnderlyingType} is NavigableSet and if {@code
     * invokedMethodElement} returns a generic type.
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
                    && invokedMethodElement.getParameters().size() == 0) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("higher")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 0) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("pollFirst")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 0) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("pollLast")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if {@code receiverUnderlyingType} is ArrayList and if {@code
     * invokedMethodElement} returns a generic type.
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
     * Returns true if {@code receiverUnderlyingType} is LinkedList and if {@code
     * invokedMethodElement} returns a generic type.
     */
    private boolean isLinkedListWithTypeVarReturn(
            TypeElement receiverUnderlyingType, ExecutableElement invokedMethodElement) {
        if (isLinkedList(receiverUnderlyingType.asType())) {
            if (invokedMethodElement.getSimpleName().contentEquals("unlink")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 1
                    && (types.isSameType(
                            invokedMethodElement.getParameters().get(0).asType(),
                            TypesUtils.typeFromClass(
                                    Node.class, types, processingEnv.getElementUtils())))) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("getFirst")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 0) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("getLast")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 0) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("removeFirst")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 0) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("removeLast")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 0) {
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
                    && invokedMethodElement.getParameters().size() == 0) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("element")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 0) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("poll")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 0) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("remove")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 0) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("peekFirst")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 0) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("peekLast")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 0) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("pollFirst")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 0) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("pollLast")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 0) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("pop")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if {@code receiverUnderlyingType} is SortedSet and if {@code
     * invokedMethodElement} returns a generic type.
     */
    private boolean isSortedSetWithTypeVarReturn(
            TypeElement receiverUnderlyingType, ExecutableElement invokedMethodElement) {
        if (isSortedSet(receiverUnderlyingType.asType())) {
            if (invokedMethodElement.getSimpleName().contentEquals("first")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 0) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("last")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if {@code receiverUnderlyingType} is TreeSet and if {@code invokedMethodElement}
     * returns a generic type.
     */
    private boolean isTreeSetWithTypeVarReturn(
            TypeElement receiverUnderlyingType, ExecutableElement invokedMethodElement) {
        if (isTreeSet(receiverUnderlyingType.asType())) {
            if (invokedMethodElement.getSimpleName().contentEquals("first")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 0) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("last")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 0) {
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
                    && invokedMethodElement.getParameters().size() == 0) {
                return true;
            }
            if (invokedMethodElement.getSimpleName().contentEquals("pollLast")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true if {@code receiverUnderlyingType} is Enumeration and if {@code
     * invokedMethodElement} returns a generic type.
     */
    private boolean isEnumerationWithTypeVarReturn(
            TypeElement receiverUnderlyingType, ExecutableElement invokedMethodElement) {
        if (isEnumeration(receiverUnderlyingType.asType())) {
            if (invokedMethodElement.getSimpleName().contentEquals("nextElement")
                    && invokedMethodElement.getReturnType().getKind() == TypeKind.TYPEVAR
                    && invokedMethodElement.getParameters().size() == 0) {
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
