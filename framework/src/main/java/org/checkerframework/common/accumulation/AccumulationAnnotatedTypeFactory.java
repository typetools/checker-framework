package org.checkerframework.common.accumulation;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.returnsreceiver.ReturnsReceiverAnnotatedTypeFactory;
import org.checkerframework.common.returnsreceiver.ReturnsReceiverChecker;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.checkerframework.common.value.ValueCheckerUtils;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * An annotated type factory for an accumulation checker.
 *
 * <p>New accumulation checkers should extend this class and implement their own version of the
 * constructor, which should take a {@link BaseTypeChecker} and pass constants for the annotation
 * classes required by the constructor defined in this class.
 *
 * <p>New subclasses must also call {@link #postInit()} in their constructors.
 */
public abstract class AccumulationAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /** The canonical top and bottom annotations for this accumulation checker. */
    public final AnnotationMirror TOP, BOTTOM;

    /**
     * The annotation that accumulates things in this accumulation checker. Must be an annotation
     * with exactly one field named "value" whose type is a String array.
     */
    private final Class<? extends Annotation> ACC;

    /**
     * Create a new accumulation checker's annotated type factory.
     *
     * @param checker the checker
     * @param top the top type in the hierarchy
     * @param accumulator the accumulator type in the hierarchy. Must be an annotation with a single
     *     argument named "value" whose type is a String array.
     * @param bot the bottom type in the hierarchy
     */
    protected AccumulationAnnotatedTypeFactory(
            BaseTypeChecker checker,
            Class<? extends Annotation> top,
            Class<? extends Annotation> accumulator,
            Class<? extends Annotation> bot) {
        super(checker);

        TOP = AnnotationBuilder.fromClass(elements, top);
        BOTTOM = AnnotationBuilder.fromClass(elements, bot);
        ACC = accumulator;

        // Every subclass must call postInit!
        if (this.getClass() == AccumulationAnnotatedTypeFactory.class) {
            this.postInit();
        }
    }

    /**
     * Creates a new instance of the accumulator annotation that contains the elements of {@code
     * values} in sorted order.
     *
     * @param values the arguments to the annotation
     * @return an annotation mirror representing the accumulator annotation with values's arguments,
     *     or top is {@code values} is empty
     */
    public AnnotationMirror createAccumulatorAnnotation(final String... values) {
        if (values.length == 0) {
            return TOP;
        }
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, ACC);
        Arrays.sort(values);
        builder.setValue("value", values);
        return builder.build();
    }

    /**
     * Utility method that returns whether the return type of the given method invocation tree has
     * an @This annotation from the Returns Receiver Checker.
     *
     * @param tree the method invocation tree to check
     * @return whether the method being invoked returns its receiver
     */
    public boolean returnsThis(final MethodInvocationTree tree) {
        ReturnsReceiverAnnotatedTypeFactory rrATF =
                getTypeFactoryOfSubchecker(ReturnsReceiverChecker.class);
        ExecutableElement methodEle = TreeUtils.elementFromUse(tree);
        AnnotatedTypeMirror methodAtm = rrATF.getAnnotatedType(methodEle);
        AnnotatedTypeMirror rrType =
                ((AnnotatedTypeMirror.AnnotatedExecutableType) methodAtm).getReturnType();
        return rrType != null && rrType.hasAnnotation(This.class);
    }

    /**
     * Is the given annotation an accumulator annotation?
     *
     * @param anm an annotation mirror
     * @return whether the annotation mirror is an instance of this factory's accumulator annotation
     */
    public boolean isAccumulatorAnnotation(AnnotationMirror anm) {
        return AnnotationUtils.areSameByClass(anm, ACC);
    }

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                super.createTreeAnnotator(), new AccumulationTreeAnnotator(this));
    }

    /** Handles fluent APIs using the Returns Receiver Checker. */
    protected class AccumulationTreeAnnotator extends TreeAnnotator {

        /**
         * Mandatory constructor.
         *
         * @param factory the type factory
         */
        public AccumulationTreeAnnotator(AccumulationAnnotatedTypeFactory factory) {
            super(factory);
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
            // Check to see if the ReturnsReceiver Checker has a @This annotation
            // on the return type of the method.
            if (returnsThis(tree)) {

                // Fetch the current type of the receiver, or top if none exists.
                ExpressionTree receiverTree = TreeUtils.getReceiverTree(tree.getMethodSelect());
                AnnotatedTypeMirror receiverType;
                AnnotationMirror receiverAnno;

                if (receiverTree != null
                        && (receiverType = getAnnotatedType(receiverTree)) != null) {
                    receiverAnno = receiverType.getAnnotationInHierarchy(TOP);
                } else {
                    receiverAnno = TOP;
                }

                type.replaceAnnotation(receiverAnno);
            }
            return super.visitMethodInvocation(tree, type);
        }
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new AccumulationQualifierHierarchy(factory);
    }

    /**
     * All accumulation analyses share a similar type hierarchy. This hierarchy implements the
     * subtyping, LUB, and GLB for that hierarchy. The lattice looks like:
     *
     * <p>top / \ acc(x) acc(y) ... \ / acc(x,y) ... | bottom
     */
    protected class AccumulationQualifierHierarchy extends MultiGraphQualifierHierarchy {

        /**
         * Create the qualifier hierarchy
         *
         * @param factory the factory
         */
        public AccumulationQualifierHierarchy(MultiGraphFactory factory) {
            super(factory);
        }

        @Override
        public AnnotationMirror getTopAnnotation(final AnnotationMirror start) {
            return TOP;
        }

        /**
         * GLB in this type system is set union of the arguments of the two annotations, unless one
         * of them is bottom, in which case the result is also bottom.
         */
        @Override
        public AnnotationMirror greatestLowerBound(
                final AnnotationMirror a1, final AnnotationMirror a2) {
            if (AnnotationUtils.areSame(a1, BOTTOM) || AnnotationUtils.areSame(a2, BOTTOM)) {
                return BOTTOM;
            }

            if (!AnnotationUtils.hasElementValue(a1, "value")) {
                return a2;
            }

            if (!AnnotationUtils.hasElementValue(a2, "value")) {
                return a1;
            }

            if (isAccumulatorAnnotation(a1) && isAccumulatorAnnotation(a2)) {
                Set<String> a1Val =
                        new LinkedHashSet<>(
                                ValueCheckerUtils.getValueOfAnnotationWithStringArgument(a1));
                Set<String> a2Val =
                        new LinkedHashSet<>(
                                ValueCheckerUtils.getValueOfAnnotationWithStringArgument(a2));
                a1Val.addAll(a2Val);
                return createAccumulatorAnnotation(a1Val.toArray(new String[0]));
            } else {
                return BOTTOM;
            }
        }

        /**
         * LUB in this type system is set intersection of the arguments of the two annotations,
         * unless one of them is bottom, in which case the result is the other annotation.
         */
        @Override
        public AnnotationMirror leastUpperBound(
                final AnnotationMirror a1, final AnnotationMirror a2) {
            if (AnnotationUtils.areSame(a1, BOTTOM)) {
                return a2;
            } else if (AnnotationUtils.areSame(a2, BOTTOM)) {
                return a1;
            }

            if (!AnnotationUtils.hasElementValue(a1, "value")) {
                return a1;
            }

            if (!AnnotationUtils.hasElementValue(a2, "value")) {
                return a2;
            }

            if (isAccumulatorAnnotation(a1) && isAccumulatorAnnotation(a2)) {
                Set<String> a1Val =
                        new LinkedHashSet<>(
                                ValueCheckerUtils.getValueOfAnnotationWithStringArgument(a1));
                Set<String> a2Val =
                        new LinkedHashSet<>(
                                ValueCheckerUtils.getValueOfAnnotationWithStringArgument(a2));
                a1Val.retainAll(a2Val);
                return createAccumulatorAnnotation(a1Val.toArray(new String[0]));
            } else {
                return TOP;
            }
        }

        /** isSubtype in this type system is subset */
        @Override
        public boolean isSubtype(final AnnotationMirror subAnno, final AnnotationMirror superAnno) {
            if (AnnotationUtils.areSame(subAnno, BOTTOM)) {
                return true;
            } else if (AnnotationUtils.areSame(superAnno, BOTTOM)) {
                return false;
            }

            if (AnnotationUtils.areSame(superAnno, TOP)) {
                return true;
            }

            List<String> subVal =
                    AnnotationUtils.areSame(subAnno, TOP)
                            ? Collections.emptyList()
                            : ValueCheckerUtils.getValueOfAnnotationWithStringArgument(subAnno);

            // superAnno is a ACC annotation, so compare the sets
            return subVal.containsAll(
                    ValueCheckerUtils.getValueOfAnnotationWithStringArgument(superAnno));
        }
    }
}
