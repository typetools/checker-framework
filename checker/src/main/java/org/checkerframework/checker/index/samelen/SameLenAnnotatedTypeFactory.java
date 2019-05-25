package org.checkerframework.checker.index.samelen;

import static org.checkerframework.checker.index.IndexUtil.getValueOfAnnotationWithStringArgument;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.index.IndexMethodIdentifier;
import org.checkerframework.checker.index.IndexUtil;
import org.checkerframework.checker.index.qual.PolyLength;
import org.checkerframework.checker.index.qual.PolySameLen;
import org.checkerframework.checker.index.qual.SameLen;
import org.checkerframework.checker.index.qual.SameLenBottom;
import org.checkerframework.checker.index.qual.SameLenUnknown;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.framework.qual.PolyAll;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.framework.util.FlowExpressionParseUtil.FlowExpressionParseException;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 * The SameLen Checker is used to determine whether there are multiple fixed-length sequences (such
 * as arrays or strings) in a program that share the same length. It is part of the Index Checker,
 * and is used as a subchecker by the Index Checker's components.
 *
 * <p>This type factory adds extra expressions to @SameLen annotations when necessary. For example,
 * if the full version of the type {@code @SameLen({"a","b"})} should include "a", "b", and whatever
 * is in the @SameLen types for "a" and for "b".
 *
 * <p>Also, every sequence s should have type @SameLen("s"). However, sometimes the sequence has
 * no @SameLen annotation, and users may write the annotation without the variable itself, as in
 *
 * <pre>{@code @SameLen("b") String a;}</pre>
 *
 * rather than the more pedantic
 *
 * <pre>{@code @SameLen({"a", "b"}) String a;}</pre>
 *
 * <p>Here are two specific examples where this annotated type factory refines types:
 *
 * <ul>
 *   <li>User-written SameLen: If a variable is declared with a user-written {@code @SameLen}
 *       annotation, then the type of the new variable is the union of the user-written arrays in
 *       the annotation and the arrays listed in the SameLen types of each of those arrays.
 *   <li>New array: The type of an expression of the form {@code new T[a.length]} is the union of
 *       the SameLen type of {@code a} and the arrays listed in {@code a}'s SameLen type.
 * </ul>
 */
public class SameLenAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /** The @{@link SameLenUnknown} annotation. */
    public final AnnotationMirror UNKNOWN =
            AnnotationBuilder.fromClass(elements, SameLenUnknown.class);
    /** The @{@link SameLenBottom} annotation. */
    private final AnnotationMirror BOTTOM =
            AnnotationBuilder.fromClass(elements, SameLenBottom.class);
    /** The @{@link PolySameLen} annotation. */
    private final AnnotationMirror POLY = AnnotationBuilder.fromClass(elements, PolySameLen.class);

    private final IndexMethodIdentifier imf = new IndexMethodIdentifier(this);

    /** Create a new SameLenAnnotatedTypeFactory. */
    public SameLenAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        addAliasedAnnotation(PolyAll.class, POLY);
        addAliasedAnnotation(PolyLength.class, POLY);

        this.postInit();
    }

    /** Gets a helper object that holds references to methods with special handling. */
    IndexMethodIdentifier getMethodIdentifier() {
        return imf;
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        // Because the Index Checker is a subclass, the qualifiers have to be explicitly defined.
        return new LinkedHashSet<>(
                Arrays.asList(
                        SameLen.class,
                        SameLenBottom.class,
                        SameLenUnknown.class,
                        PolySameLen.class));
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new SameLenQualifierHierarchy(factory);
    }

    // Handles case "user-written SameLen"
    @Override
    public AnnotatedTypeMirror getAnnotatedTypeLhs(Tree tree) {
        AnnotatedTypeMirror atm = super.getAnnotatedTypeLhs(tree);

        if (tree.getKind() == Tree.Kind.VARIABLE) {
            AnnotationMirror anm = atm.getAnnotation(SameLen.class);
            if (anm != null) {

                Receiver r;
                try {
                    r = FlowExpressionParseUtil.internalReprOfVariable(this, (VariableTree) tree);
                } catch (FlowExpressionParseException ex) {
                    r = null;
                }

                if (r != null) {
                    String varName = r.toString();

                    List<String> exprs = IndexUtil.getValueOfAnnotationWithStringArgument(anm);
                    if (exprs.contains(varName)) {
                        exprs.remove(varName);
                    }
                    if (exprs.isEmpty()) {
                        atm.replaceAnnotation(UNKNOWN);
                    } else {
                        atm.replaceAnnotation(createSameLen(exprs));
                    }
                }
            }
        }

        return atm;
    }

    /** Returns true if the given expression may appear in a @SameLen annotation. */
    public static boolean mayAppearInSameLen(Receiver receiver) {
        return !receiver.containsUnknown()
                && !(receiver instanceof FlowExpressions.ArrayCreation)
                && !(receiver instanceof FlowExpressions.ClassName)
                // Big expressions cause a stack overflow in FlowExpressionParseUtil.
                // So limit them to an arbitrary length of 999.
                && receiver.toString().length() < 1000;
    }

    /**
     * The qualifier hierarchy for the SameLen type system. Most types are distinct and at the same
     * level: for instance @SameLen("a") and @SameLen("b) have nothing in common. However, if one
     * type includes even one overlapping name, then the types have to be the same:
     * so @SameLen({"a","b","c"} and @SameLen({"c","f","g"} are actually the same type -- both
     * should usually be replaced by a SameLen with the union of the lists of names.
     */
    private final class SameLenQualifierHierarchy extends MultiGraphQualifierHierarchy {

        /** @param factory MultiGraphFactory to use to construct this */
        public SameLenQualifierHierarchy(MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
            super(factory);
        }

        @Override
        public AnnotationMirror getTopAnnotation(AnnotationMirror start) {
            return UNKNOWN;
        }

        /**
         * If the collections are disjoint, returns null. Otherwise, returns their union. The
         * collections must not contain duplicates.
         */
        private Set<String> unionIfNotDisjoint(Collection<String> c1, Collection<String> c2) {
            Set<String> result = new TreeSet<>(c1);
            for (String s : c2) {
                if (!result.add(s)) {
                    return null;
                }
            }
            return result;
        }

        // The GLB of two SameLen annotations is the union of the two sets of arrays, or is bottom
        // if the sets do not intersect.
        @Override
        public AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2) {
            if (AnnotationUtils.hasElementValue(a1, "value")
                    && AnnotationUtils.hasElementValue(a2, "value")) {
                List<String> a1Val = getValueOfAnnotationWithStringArgument(a1);
                List<String> a2Val = getValueOfAnnotationWithStringArgument(a2);

                Set<String> exprs = unionIfNotDisjoint(a1Val, a2Val);
                if (exprs == null) {
                    return BOTTOM;
                } else {
                    return createSameLen(exprs);
                }
            } else {
                // the glb is either one of the annotations (if the other is top), or bottom.
                if (AnnotationUtils.areSameByClass(a1, SameLenUnknown.class)) {
                    return a2;
                } else if (AnnotationUtils.areSameByClass(a2, SameLenUnknown.class)) {
                    return a1;
                } else {
                    return BOTTOM;
                }
            }
        }

        // The LUB of two SameLen annotations is the intersection of the two sets of arrays, or is
        // top if they do not intersect.
        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
            if (AnnotationUtils.hasElementValue(a1, "value")
                    && AnnotationUtils.hasElementValue(a2, "value")) {
                List<String> a1Val = getValueOfAnnotationWithStringArgument(a1);
                List<String> a2Val = getValueOfAnnotationWithStringArgument(a2);

                if (!Collections.disjoint(a1Val, a2Val)) {
                    a1Val.retainAll(a2Val);
                    return createSameLen(a1Val);
                } else {
                    return UNKNOWN;
                }

            } else {
                // the lub is either one of the annotations (if the other is bottom), or top.
                if (AnnotationUtils.areSameByClass(a1, SameLenBottom.class)) {
                    return a2;
                } else if (AnnotationUtils.areSameByClass(a2, SameLenBottom.class)) {
                    return a1;
                } else if (AnnotationUtils.areSameByClass(a1, PolySameLen.class)
                        && AnnotationUtils.areSameByClass(a2, PolySameLen.class)) {
                    return a1;
                } else {
                    return UNKNOWN;
                }
            }
        }

        @Override
        public boolean isSubtype(AnnotationMirror subAnno, AnnotationMirror superAnno) {
            if (AnnotationUtils.areSameByClass(subAnno, SameLenBottom.class)) {
                return true;
            } else if (AnnotationUtils.areSameByClass(superAnno, SameLenUnknown.class)) {
                return true;
            } else if (AnnotationUtils.areSameByClass(subAnno, PolySameLen.class)) {
                return AnnotationUtils.areSameByClass(superAnno, PolySameLen.class);
            } else if (AnnotationUtils.hasElementValue(subAnno, "value")
                    && AnnotationUtils.hasElementValue(superAnno, "value")) {
                List<String> subArrays = getValueOfAnnotationWithStringArgument(subAnno);
                List<String> superArrays = getValueOfAnnotationWithStringArgument(superAnno);

                if (subArrays.containsAll(superArrays)) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(super.createTreeAnnotator(), new SameLenTreeAnnotator(this));
    }

    /**
     * SameLen needs a tree annotator in order to properly type the right side of assignments of new
     * arrays that are initialized with the length of another array.
     */
    protected class SameLenTreeAnnotator extends TreeAnnotator {

        public SameLenTreeAnnotator(SameLenAnnotatedTypeFactory factory) {
            super(factory);
        }

        // Case "new array" for "new T[a.length]"
        @Override
        public Void visitNewArray(NewArrayTree node, AnnotatedTypeMirror type) {
            if (node.getDimensions().size() == 1) {
                Tree dimensionTree = node.getDimensions().get(0);
                ExpressionTree sequenceTree =
                        IndexUtil.getLengthSequenceTree(dimensionTree, imf, processingEnv);
                if (sequenceTree != null) {
                    AnnotationMirror sequenceAnno =
                            getAnnotatedType(sequenceTree).getAnnotationInHierarchy(UNKNOWN);

                    Receiver rec = FlowExpressions.internalReprOf(this.atypeFactory, sequenceTree);
                    if (mayAppearInSameLen(rec)) {
                        String recString = rec.toString();
                        if (AnnotationUtils.areSameByClass(sequenceAnno, SameLenUnknown.class)) {
                            sequenceAnno = createSameLen(Collections.singletonList(recString));
                        } else if (AnnotationUtils.areSameByClass(sequenceAnno, SameLen.class)) {
                            // Add the sequence whose length is being used to the annotation.
                            List<String> exprs =
                                    getValueOfAnnotationWithStringArgument(sequenceAnno);
                            int index = Collections.binarySearch(exprs, recString);
                            if (index < 0) {
                                exprs.add(-index - 1, recString);
                                sequenceAnno = createSameLen(exprs);
                            }
                        }
                    }
                    type.addAnnotation(sequenceAnno);
                }
            }
            return null;
        }
    }

    /**
     * Find all the sequences that are members of the SameLen annotation associated with the
     * sequence named in sequenceExpression along the current path.
     */
    public List<String> getSameLensFromString(
            String sequenceExpression, Tree tree, TreePath currentPath) {
        AnnotationMirror sameLenAnno;
        try {
            sameLenAnno =
                    getAnnotationFromJavaExpressionString(
                            sequenceExpression, tree, currentPath, SameLen.class);
        } catch (FlowExpressionParseUtil.FlowExpressionParseException e) {
            // ignore parse errors
            sameLenAnno = null;
        }
        if (sameLenAnno == null) {
            return new ArrayList<>();
        }
        return getValueOfAnnotationWithStringArgument(sameLenAnno);
    }

    ///
    /// Creating @SameLen annotations
    ///

    /**
     * Creates a @SameLen annotation whose values are the given strings, from an <em>ordered</em>
     * collection such as a list or TreeSet in which the strings are in alphabetical order.
     */
    public AnnotationMirror createSameLen(Collection<String> exprs) {
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, SameLen.class);
        String[] exprArray = exprs.toArray(new String[0]);
        builder.setValue("value", exprArray);
        return builder.build();
    }

    // In Java 9, this method can be eliminated:  it is simple enough for clients to inline, using
    // List.of.
    /**
     * Combines the given arrays and annotations into a single SameLen annotation. See {@link
     * #createCombinedSameLen(List, List)}.
     */
    public AnnotationMirror createCombinedSameLen(
            Receiver rec1, Receiver rec2, AnnotationMirror a1, AnnotationMirror a2) {
        List<Receiver> receivers = new ArrayList<>();
        receivers.add(rec1);
        receivers.add(rec2);
        List<AnnotationMirror> annos = new ArrayList<>();
        annos.add(a1);
        annos.add(a2);
        return createCombinedSameLen(receivers, annos);
    }

    /**
     * Generates a SameLen that includes each receiver, as well as everything in the annotations2,
     * if they are SameLen annotations.
     *
     * @param receivers a list of receivers representing arrays to be included in the combined
     *     annotation
     * @param annos a list of annotations
     * @return a combined SameLen annotation
     */
    public AnnotationMirror createCombinedSameLen(
            List<FlowExpressions.Receiver> receivers, List<AnnotationMirror> annos) {

        Set<String> exprs = new TreeSet<>();
        for (Receiver rec : receivers) {
            if (mayAppearInSameLen(rec)) {
                exprs.add(rec.toString());
            }
        }
        for (AnnotationMirror anno : annos) {
            if (AnnotationUtils.areSameByClass(anno, SameLen.class)) {
                exprs.addAll(getValueOfAnnotationWithStringArgument(anno));
            }
        }
        return createSameLen(exprs);
    }
}
