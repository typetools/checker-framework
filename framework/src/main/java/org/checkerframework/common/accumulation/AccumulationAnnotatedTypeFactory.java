package org.checkerframework.common.accumulation;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.util.Elements;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.accumulation.AccumulationChecker.AliasAnalysis;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.returnsreceiver.ReturnsReceiverAnnotatedTypeFactory;
import org.checkerframework.common.returnsreceiver.ReturnsReceiverChecker;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.checkerframework.common.value.ValueCheckerUtils;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.ElementQualifierHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeSystemError;
import org.checkerframework.javacutil.UserError;

/**
 * An annotated type factory for an accumulation checker.
 *
 * <p>New accumulation checkers should extend this class and implement a constructor, which should
 * take a {@link BaseTypeChecker} and call both the constructor defined in this class and {@link
 * #postInit()}.
 */
public abstract class AccumulationAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /** The typechecker associated with this factory. */
    public final AccumulationChecker accumulationChecker;

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
     * The predicate annotation for this accumulation analysis, or null if predicates are not
     * supported. A predicate annotation must have a single element named "value" of type String.
     */
    private final @MonotonicNonNull Class<? extends Annotation> predicate;

    /**
     * Create an annotated type factory for an accumulation checker.
     *
     * @param checker the checker
     * @param accumulator the accumulator type in the hierarchy. Must be an annotation with a single
     *     argument named "value" whose type is a String array.
     * @param bottom the bottom type in the hierarchy, which must be a subtype of {@code
     *     accumulator}. The bottom type should be an annotation with no arguments.
     * @param predicate the predicate annotation. Either null (if predicates are not supported), or
     *     an annotation with a single element named "value" whose type is a String.
     */
    protected AccumulationAnnotatedTypeFactory(
            BaseTypeChecker checker,
            Class<? extends Annotation> accumulator,
            Class<? extends Annotation> bottom,
            @Nullable Class<? extends Annotation> predicate) {
        super(checker);
        if (!(checker instanceof AccumulationChecker)) {
            throw new TypeSystemError(
                    "AccumulationAnnotatedTypeFactory cannot be used with a checker "
                            + "class that is not a subtype of AccumulationChecker. Found class: "
                            + checker.getClass());
        }
        this.accumulationChecker = (AccumulationChecker) checker;

        this.accumulator = accumulator;
        // Check that the requirements of the accumulator are met.
        Method[] accDeclaredMethods = accumulator.getDeclaredMethods();
        if (accDeclaredMethods.length != 1) {
            rejectMalformedAccumulator("have exactly one element");
        }

        Method accValue = accDeclaredMethods[0];
        if (accValue.getName() != "value") { // interned
            rejectMalformedAccumulator("name its element \"value\"");
        }
        if (!accValue.getReturnType().isInstance(new String[0])) {
            rejectMalformedAccumulator("have an element of type String[]");
        }
        if (accValue.getDefaultValue() == null
                || ((String[]) accValue.getDefaultValue()).length != 0) {
            rejectMalformedAccumulator("have the empty String array {} as its default value");
        }

        this.predicate = predicate;
        // If there is a predicate annotation, check that its requirements are met.
        if (predicate != null) {
            Method[] predDeclaredMethods = predicate.getDeclaredMethods();
            if (predDeclaredMethods.length != 1) {
                rejectMalformedPredicate("have exactly one element");
            }
            Method predValue = predDeclaredMethods[0];
            if (predValue.getName() != "value") { // interned
                rejectMalformedPredicate("name its element \"value\"");
            }
            if (!predValue.getReturnType().isInstance("")) {
                rejectMalformedPredicate("have an element of type String");
            }
        }

        this.bottom = AnnotationBuilder.fromClass(elements, bottom);
        this.top = createAccumulatorAnnotation(Collections.emptyList());

        // Every subclass must call postInit!  This does not do so.
    }

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
        this(checker, accumulator, bottom, null);
    }

    /**
     * Common error message for malformed accumulator annotation.
     *
     * @param missing what is missing from the accumulator, suitable for use in this string to
     *     replace $MISSING$: "The accumulator annotation Foo must $MISSING$."
     */
    private void rejectMalformedAccumulator(String missing) {
        rejectMalformedAnno("accumulator", accumulator, missing);
    }

    /**
     * Common error message for malformed predicate annotation.
     *
     * @param missing what is missing from the predicate, suitable for use in this string to replace
     *     $MISSING$: "The predicate annotation Foo must $MISSING$."
     */
    private void rejectMalformedPredicate(String missing) {
        rejectMalformedAnno("predicate", predicate, missing);
    }

    /**
     * Common error message implementation. Call rejectMalformedAccumulator or
     * rejectMalformedPredicate as appropriate, rather than this method directly.
     *
     * @param annoTypeName the display name for the type of malformed annotation, such as
     *     "accumulator"
     * @param anno the malformed annotation
     * @param missing what is missing from the annotation, suitable for use in this string to
     *     replace $MISSING$: "The accumulator annotation Foo must $MISSING$."
     */
    private void rejectMalformedAnno(
            String annoTypeName, Class<? extends Annotation> anno, String missing) {
        throw new BugInCF("The " + annoTypeName + " annotation " + anno + " must " + missing + ".");
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
        if (!accumulationChecker.isEnabled(AliasAnalysis.RETURNS_RECEIVER)) {
            return false;
        }
        // Must call `getTypeFactoryOfSubchecker` each time, not store and reuse.
        ReturnsReceiverAnnotatedTypeFactory rrATF =
                getTypeFactoryOfSubchecker(ReturnsReceiverChecker.class);
        ExecutableElement methodEle = TreeUtils.elementFromUse(tree);
        AnnotatedExecutableType methodAtm = rrATF.getAnnotatedType(methodEle);
        AnnotatedTypeMirror rrType = methodAtm.getReturnType();
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
        return areSameByClass(anm, accumulator);
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
         * <p>This implementation propagates types from the receiver to the return value, without
         * change. Subclasses may override this method to also add additional properties, as
         * appropriate.
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
    protected QualifierHierarchy createQualifierHierarchy() {
        return new AccumulationQualifierHierarchy(this.getSupportedTypeQualifiers(), this.elements);
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
            throw new BugInCF(anno + " isn't an accumulator annotation");
        }
        List<String> values =
                AnnotationUtils.getElementValueArrayOrNull(anno, "value", String.class, false);
        if (values == null) {
            return Collections.emptyList();
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
     *
     * Predicate subtyping is defined as follows:
     *
     * <ul>
     *   <li>An accumulator is a subtype of a predicate if substitution from the accumulator to the
     *       predicate makes the predicate true. For example, {@code Acc(A)} is a subtype of {@code
     *       AccPred("A || B")}, because when A is replaced with {@code true} and B is replaced with
     *       {@code false}, the resulting boolean formula evaluates to true.
     *   <li>A predicate P is a subtype of an accumulator iff after converting the accumulator into
     *       a predicate representing the conjunction of its elements, P is a subtype of that
     *       predicate according to the rule for subtyping between two predicates defined below.
     *   <li>A predicate P is a subtype of another predicate Q iff P and Q are equal. An extension
     *       point ({@link #isPredicateSubtype(String, String)}) is provided to allow more complex
     *       subtyping behavior between predicates. (The "correct" subtyping rule is that P is a
     *       subtype of Q iff P implies Q. That rule would require an SMT solver in the general
     *       case, which is undesirable because it would require an external dependency. A user can
     *       override {@link #isPredicateSubtype(String, String)} if they require more precise
     *       subtyping; the check described here is overly conservative (and therefore sound), but
     *       not very precise.)
     * </ul>
     */
    protected class AccumulationQualifierHierarchy extends ElementQualifierHierarchy {

        /**
         * Creates a ElementQualifierHierarchy from the given classes.
         *
         * @param qualifierClasses classes of annotations that are the qualifiers for this hierarchy
         * @param elements element utils
         */
        protected AccumulationQualifierHierarchy(
                Collection<Class<? extends Annotation>> qualifierClasses, Elements elements) {
            super(qualifierClasses, elements);
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

            if (isPolymorphicQualifier(a1) && isPolymorphicQualifier(a2)) {
                return a1;
            } else if (isPolymorphicQualifier(a1) || isPolymorphicQualifier(a2)) {
                return bottom;
            }

            // If either is a predicate, then both should be converted to predicates and and-ed.
            if (isPredicate(a1) || isPredicate(a2)) {
                String a1Pred = convertToPredicate(a1);
                String a2Pred = convertToPredicate(a2);
                // check for top
                if (a1Pred.isEmpty()) {
                    return a2;
                } else if (a2Pred.isEmpty()) {
                    return a1;
                } else {
                    return createPredicateAnnotation("(" + a1Pred + ") && (" + a2Pred + ")");
                }
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

            if (isPolymorphicQualifier(a1) && isPolymorphicQualifier(a2)) {
                return a1;
            } else if (isPolymorphicQualifier(a1) || isPolymorphicQualifier(a2)) {
                return top;
            }

            // If either is a predicate, then both should be converted to predicates and or-ed.
            if (isPredicate(a1) || isPredicate(a2)) {
                String a1Pred = convertToPredicate(a1);
                String a2Pred = convertToPredicate(a2);
                // check for top
                if (a1Pred.isEmpty()) {
                    return a1;
                } else if (a2Pred.isEmpty()) {
                    return a2;
                } else {
                    return createPredicateAnnotation("(" + a1Pred + ") || (" + a2Pred + ")");
                }
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

            if (isPolymorphicQualifier(subAnno)) {
                if (isPolymorphicQualifier(superAnno)) {
                    return true;
                } else {
                    // Use this slightly more expensive conversion here because
                    // this is a rare code path and it's simpler to read than
                    // checking for both predicate and non-predicate forms of top.
                    return "".equals(convertToPredicate(superAnno));
                }
            } else if (isPolymorphicQualifier(superAnno)) {
                // Polymorphic annotations are only a supertype of other polymorphic annotations and
                // the bottom type, both of which have already been checked above.
                return false;
            }

            if (isPredicate(subAnno)) {
                return isPredicateSubtype(
                        convertToPredicate(subAnno), convertToPredicate(superAnno));
            } else if (isPredicate(superAnno)) {
                return evaluatePredicate(subAnno, convertToPredicate(superAnno));
            }

            List<String> subVal = getAccumulatedValues(subAnno);
            List<String> superVal = getAccumulatedValues(superAnno);
            return subVal.containsAll(superVal);
        }
    }

    /**
     * Extension point for subtyping behavior between predicates. This implementation conservatively
     * returns true only if the predicates are equal, or if the prospective supertype (q) is
     * equivalent to top (that is, the empty string).
     *
     * @param p a predicate
     * @param q another predicate
     * @return true if p is a subtype of q
     */
    protected boolean isPredicateSubtype(String p, String q) {
        return "".equals(q) || p.equals(q);
    }

    /**
     * Evaluates whether the accumulator annotation {@code subAnno} makes the predicate {@code pred}
     * true.
     *
     * @param subAnno an accumulator annotation
     * @param pred a predicate
     * @return whether the accumulator annotation satisfies the predicate
     */
    protected boolean evaluatePredicate(AnnotationMirror subAnno, String pred) {
        if (!isAccumulatorAnnotation(subAnno)) {
            throw new BugInCF(
                    "tried to evaluate a predicate using an annotation that wasn't an accumulator: "
                            + subAnno);
        }
        List<String> trueVariables = getAccumulatedValues(subAnno);
        return evaluatePredicate(trueVariables, pred);
    }

    /**
     * Checks that the given annotation either:
     *
     * <ul>
     *   <li>does not contain a predicate, or
     *   <li>contains a parse-able predicate
     * </ul>
     *
     * Used by the visitor to throw "predicate.invalid" errors; thus must be package-private.
     *
     * @param anm any annotation supported by this checker
     * @return null if there is nothing wrong with the annotation, or an error message indicating
     *     the problem if it has an invalid predicate
     */
    /* package-private */
    @Nullable String isValidPredicate(AnnotationMirror anm) {
        String pred = convertToPredicate(anm);
        try {
            evaluatePredicate(Collections.emptyList(), pred);
        } catch (UserError ue) {
            return ue.getLocalizedMessage();
        }
        return null;
    }

    /**
     * Evaluates whether treating the variables in {@code trueVariables} as {@code true} literals
     * (and all other names as {@code false} literals) makes the predicate {@code pred} evaluate to
     * true.
     *
     * @param trueVariables a list of names that should be replaced with {@code true}
     * @param pred a predicate
     * @return whether the true variables satisfy the predicate
     */
    protected boolean evaluatePredicate(List<String> trueVariables, String pred) {
        Expression expression;
        try {
            expression = StaticJavaParser.parseExpression(pred);
        } catch (ParseProblemException p) {
            throw new UserError("unparsable predicate: " + pred + ". Parse exception: " + p);
        }
        return evaluateBooleanExpression(expression, trueVariables);
    }

    /**
     * Evaluates a boolean expression, in JavaParser format, that contains only and, or,
     * parentheses, logical complement, and boolean literal nodes.
     *
     * @param expression a JavaParser boolean expression
     * @param trueVariables the names of the variables that should be considered "true"; all other
     *     literals are considered "false"
     * @return the result of evaluating the expression
     */
    private boolean evaluateBooleanExpression(Expression expression, List<String> trueVariables) {
        if (expression.isNameExpr()) {
            return trueVariables.contains(expression.asNameExpr().getNameAsString());
        } else if (expression.isBinaryExpr()) {
            if (expression.asBinaryExpr().getOperator() == BinaryExpr.Operator.OR) {
                return evaluateBooleanExpression(expression.asBinaryExpr().getLeft(), trueVariables)
                        || evaluateBooleanExpression(
                                expression.asBinaryExpr().getRight(), trueVariables);
            } else if (expression.asBinaryExpr().getOperator() == BinaryExpr.Operator.AND) {
                return evaluateBooleanExpression(expression.asBinaryExpr().getLeft(), trueVariables)
                        && evaluateBooleanExpression(
                                expression.asBinaryExpr().getRight(), trueVariables);
            }
        } else if (expression.isEnclosedExpr()) {
            return evaluateBooleanExpression(expression.asEnclosedExpr().getInner(), trueVariables);
        } else if (expression.isUnaryExpr()) {
            if (expression.asUnaryExpr().getOperator() == UnaryExpr.Operator.LOGICAL_COMPLEMENT) {
                return !evaluateBooleanExpression(
                        expression.asUnaryExpr().getExpression(), trueVariables);
            }
        }
        // This could be a BugInCF if there is a bug in the code above.
        throw new UserError(
                "encountered an unexpected type of expression in a "
                        + "predicate expression: "
                        + expression
                        + " was of type "
                        + expression.getClass());
    }

    /**
     * Creates a new predicate annotation from the given string.
     *
     * @param p a valid predicate
     * @return an annotation representing that predicate
     */
    protected AnnotationMirror createPredicateAnnotation(String p) {
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, predicate);
        builder.setValue("value", p);
        return builder.build();
    }

    /**
     * Converts the given annotation mirror to a predicate.
     *
     * @param anno an annotation
     * @return the predicate, as a String, that is equivalent to that annotation. May return the
     *     empty string.
     */
    protected String convertToPredicate(AnnotationMirror anno) {
        if (AnnotationUtils.areSame(anno, bottom)) {
            return "false";
        } else if (isPredicate(anno)) {
            if (AnnotationUtils.hasElementValue(anno, "value")) {
                return AnnotationUtils.getElementValue(anno, "value", String.class, false);
            } else {
                return "";
            }
        } else if (isAccumulatorAnnotation(anno)) {
            List<String> values = getAccumulatedValues(anno);
            StringJoiner sj = new StringJoiner(" && ");
            for (String value : values) {
                sj.add(value);
            }
            return sj.toString();
        } else {
            throw new BugInCF("annotation is not bottom, a predicate, or an accumulator: " + anno);
        }
    }

    /**
     * Returns true if anno is a predicate annotation.
     *
     * @param anno an annotation
     * @return true if anno is a predicate annotation
     */
    protected boolean isPredicate(AnnotationMirror anno) {
        return predicate != null && areSameByClass(anno, predicate);
    }
}
