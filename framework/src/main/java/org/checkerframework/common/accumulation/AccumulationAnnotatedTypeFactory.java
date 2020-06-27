package org.checkerframework.common.accumulation;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;

/**
 * An annotated type factory for an accumulation checker.
 *
 * <p>New accumulation checkers should extend this class and implement a constructor, which should
 * take a {@link BaseTypeChecker} and call both the constructor defined in this class and {@link
 * #postInit()}.
 */
public abstract class AccumulationAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /**
     * The canonical top annotation for this accumulation checker: an instance of the accumulator
     * annotation with no arguments.
     */
    public final AnnotationMirror top;

    /** The canonical bottom annotation for this accumulation checker. */
    public final AnnotationMirror bottom;

    /**
     * The annotation that accumulates things in this accumulation checker. Must be an annotation
     * with exactly one field named "value" whose type is a String array.
     */
    private final Class<? extends Annotation> accumulator;

    /**
     * Create an annotated type factory for an accumulation checker.
     *
     * @param checker the checker
     * @param accumulator the accumulator type in the hierarchy. Must be an annotation with a single
     *     argument named "value" whose type is a String array.
     * @param bottom the bottom type in the hierarchy, which must be a subtype of {@code
     *     accumulator}. The bottom type should be an annotation with no arguments.
     */
    protected AccumulationAnnotatedTypeFactory(
            BaseTypeChecker checker,
            Class<? extends Annotation> accumulator,
            Class<? extends Annotation> bottom) {
        super(checker);

        this.accumulator = accumulator;

        // Check that the requirements of the accumulator are met.
        Method[] accDeclaredMethods = accumulator.getDeclaredMethods();
        if (accDeclaredMethods.length != 1) {
            rejectMalformedAccumulator("have exactly one element");
        }
        Method value = accDeclaredMethods[0];
        if (value.getName() != "value") { // interned
            rejectMalformedAccumulator("name its element \"value\"");
        }
        if (!value.getReturnType().isInstance(new String[0])) {
            rejectMalformedAccumulator("have an element of type String[]");
        }
        if (((String[]) value.getDefaultValue()).length != 0) {
            rejectMalformedAccumulator("have the empty String array {} as its default value");
        }

        this.bottom = AnnotationBuilder.fromClass(elements, bottom);
        this.top = createAccumulatorAnnotation(Collections.emptyList());

        // Every subclass must call postInit!  This does not do so for subclasses.
        if (this.getClass() == AccumulationAnnotatedTypeFactory.class) {
            this.postInit();
        }
    }

    /**
     * Common error message for malformed accumulator annotation.
     *
     * @param missing what is missing from the accumulator, suitable for use in this string to
     *     replace $MISSING$: "The accumulator annotation Foo must $MISSING$."
     */
    private void rejectMalformedAccumulator(String missing) {
        throw new BugInCF("The accumulator annotation " + accumulator + " must " + missing + ".");
    }

    /**
     * Creates a new instance of the accumulator annotation that contains the elements of {@code
     * values}.
     *
     * @param values the arguments to the annotation. The values can contain duplicates and can be
     *     in any order.
     * @return an annotation mirror representing the accumulator annotation with {@code values}'s
     *     arguments; this is top if {@code values} is empty
     */
    public AnnotationMirror createAccumulatorAnnotation(List<String> values) {
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, accumulator);
        builder.setValue("value", ValueCheckerUtils.removeDuplicates(values));
        return builder.build();
    }

    /**
     * Creates a new instance of the accumulator annotation that contains exactly one value.
     *
     * @param value the argument to the annotation
     * @return an annotation mirror representing the accumulator annotation with {@code value} as
     *     its argument
     */
    public AnnotationMirror createAccumulatorAnnotation(String value) {
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, accumulator);
        builder.setValue("value", Collections.singletonList(value));
        return builder.build();
    }

    /**
     * Returns true if the return type of the given method invocation tree has an @This annotation
     * from the Returns Receiver Checker.
     *
     * @param tree a method invocation tree
     * @return true if the method being invoked returns its receiver
     */
    public boolean returnsThis(final MethodInvocationTree tree) {
        // Must call `getTypeFactoryOfSubchecker` each time, not store and reuse.
        ReturnsReceiverAnnotatedTypeFactory rrATF =
                getTypeFactoryOfSubchecker(ReturnsReceiverChecker.class);
        ExecutableElement methodEle = TreeUtils.elementFromUse(tree);
        AnnotatedTypeMirror methodAtm = rrATF.getAnnotatedType(methodEle);
        AnnotatedTypeMirror rrType =
                ((AnnotatedTypeMirror.AnnotatedExecutableType) methodAtm).getReturnType();
        return rrType != null && rrType.hasAnnotation(This.class);
    }

    /**
     * Is the given annotation an accumulator annotation? Returns false if the argument is {@link
     * #bottom}.
     *
     * @param anm an annotation mirror
     * @return true if the annotation mirror is an instance of this factory's accumulator annotation
     */
    public boolean isAccumulatorAnnotation(AnnotationMirror anm) {
        return AnnotationUtils.areSameByClass(anm, accumulator);
    }

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                super.createTreeAnnotator(), new AccumulationTreeAnnotator(this));
    }

    /**
     * This tree annotator implements the following rule(s):
     *
     * <dl>
     *   <dt>RRA
     *   <dd>If a method returns its receiver, and the receiver has an accumulation type, then the
     *       default type of the method's return value is the type of the receiver.
     * </dl>
     */
    protected class AccumulationTreeAnnotator extends TreeAnnotator {

        /**
         * Creates an instance of this tree annotator for the given type factory.
         *
         * @param factory the type factory
         */
        public AccumulationTreeAnnotator(AccumulationAnnotatedTypeFactory factory) {
            super(factory);
        }

        /**
         * Implements rule RRA.
         *
         * @param tree a method invocation tree
         * @param type the type of {@code tree} (i.e. the return type of the invoked method). Is
         *     (possibly) side-effected by this method.
         * @return nothing, works by side-effect on {@code type}
         */
        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
            if (returnsThis(tree)) {
                // There is a @This annotation on the return type of the invoked method.
                ExpressionTree receiverTree = TreeUtils.getReceiverTree(tree.getMethodSelect());
                AnnotatedTypeMirror receiverType =
                        receiverTree == null ? null : getAnnotatedType(receiverTree);
                // The current type of the receiver, or top if none exists.
                AnnotationMirror receiverAnno =
                        receiverType == null ? top : receiverType.getAnnotationInHierarchy(top);

                AnnotationMirror returnAnno = type.getAnnotationInHierarchy(top);
                type.replaceAnnotation(qualHierarchy.greatestLowerBound(returnAnno, receiverAnno));
            }
            return super.visitMethodInvocation(tree, type);
        }
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new AccumulationQualifierHierarchy(factory);
    }

    /**
     * Returns all the values that anno has accumulated.
     *
     * @param anno an accumulator annotation; must not be bottom
     * @return the list of values the annotation has accumulated; it is a new list, so it is safe
     *     for clients to side-effect
     */
    public List<String> getAccumulatedValues(AnnotationMirror anno) {
        if (!isAccumulatorAnnotation(anno)) {
            throw new BugInCF(anno + "isn't an accumulator annotation");
        }
        List<String> values = ValueCheckerUtils.getValueOfAnnotationWithStringArgument(anno);
        if (values == null) {
            return new ArrayList<>(0);
        } else {
            return values;
        }
    }

    /**
     * All accumulation analyses share a similar type hierarchy. This class implements the
     * subtyping, LUB, and GLB for that hierarchy. The lattice looks like:
     *
     * <pre>
     *       acc()
     *      /   \
     * acc(x)   acc(y) ...
     *      \   /
     *     acc(x,y) ...
     *        |
     *      bottom
     * </pre>
     */
    protected class AccumulationQualifierHierarchy extends MultiGraphQualifierHierarchy {

        /**
         * Create the qualifier hierarchy.
         *
         * @param factory the factory
         */
        public AccumulationQualifierHierarchy(MultiGraphFactory factory) {
            super(factory);
        }

        @Override
        public AnnotationMirror getTopAnnotation(final AnnotationMirror start) {
            return top;
        }

        /**
         * GLB in this type system is set union of the arguments of the two annotations, unless one
         * of them is bottom, in which case the result is also bottom.
         */
        @Override
        public AnnotationMirror greatestLowerBound(
                final AnnotationMirror a1, final AnnotationMirror a2) {
            if (AnnotationUtils.areSame(a1, bottom) || AnnotationUtils.areSame(a2, bottom)) {
                return bottom;
            }

            List<String> a1Val = getAccumulatedValues(a1);
            List<String> a2Val = getAccumulatedValues(a2);
            // Avoid creating new annotation objects in the common case.
            if (a1Val.containsAll(a2Val)) {
                return a1;
            }
            if (a2Val.containsAll(a1Val)) {
                return a2;
            }
            a1Val.addAll(a2Val);
            return createAccumulatorAnnotation(a1Val);
        }

        /**
         * LUB in this type system is set intersection of the arguments of the two annotations,
         * unless one of them is bottom, in which case the result is the other annotation.
         */
        @Override
        public AnnotationMirror leastUpperBound(
                final AnnotationMirror a1, final AnnotationMirror a2) {
            if (AnnotationUtils.areSame(a1, bottom)) {
                return a2;
            } else if (AnnotationUtils.areSame(a2, bottom)) {
                return a1;
            }

            List<String> a1Val = getAccumulatedValues(a1);
            List<String> a2Val = getAccumulatedValues(a2);
            // Avoid creating new annotation objects in the common case.
            if (a1Val.containsAll(a2Val)) {
                return a2;
            }
            if (a2Val.containsAll(a1Val)) {
                return a1;
            }
            a1Val.retainAll(a2Val);
            return createAccumulatorAnnotation(a1Val);
        }

        /** isSubtype in this type system is subset. */
        @Override
        public boolean isSubtype(final AnnotationMirror subAnno, final AnnotationMirror superAnno) {
            if (AnnotationUtils.areSame(subAnno, bottom)) {
                return true;
            } else if (AnnotationUtils.areSame(superAnno, bottom)) {
                return false;
            }

            List<String> subVal = getAccumulatedValues(subAnno);
            List<String> superVal = getAccumulatedValues(superAnno);
            return subVal.containsAll(superVal);
        }
    }
}
