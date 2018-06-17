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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
import org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
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
 * <p>This annotated type factory refines types in X cases:
 *
 * <ul>
 *   <li>1. {@code @PolyAll} and {@code @PolyLength} are refined to {@code @PolySameLen }.
 *   <li>2. If a variable is declared with a user-written {@code @SameLen} annotation, then the type
 *       of the new variable is the union of the user-written arrays in the annotation and the
 *       arrays listed in the SameLen types of each of those arrays.
 *   <li>3. The greatest lower bound of two SameLen annotations is the union of the two sets of
 *       arrays.
 *   <li>4. The least upper bound of two SameLen annotations is the intersection of the two sets of
 *       arrays.
 *   <li>5. The type of an expression of the form {@code new T[a.length]} is the union of the
 *       SameLen type of {@code a} and the arrays listed in {@code a}'s SameLen type.
 * </ul>
 */
public class SameLenAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    public final AnnotationMirror UNKNOWN;
    private final AnnotationMirror BOTTOM;
    private final AnnotationMirror POLY;

    private final IndexMethodIdentifier imf;

    /** Handles case 1. */
    public SameLenAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        UNKNOWN = AnnotationBuilder.fromClass(elements, SameLenUnknown.class);
        BOTTOM = AnnotationBuilder.fromClass(elements, SameLenBottom.class);
        POLY = AnnotationBuilder.fromClass(elements, PolySameLen.class);
        addAliasedAnnotation(PolyAll.class, POLY);
        addAliasedAnnotation(PolyLength.class, POLY);

        imf = new IndexMethodIdentifier(this);

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

    /** Handles case 2. */
    @Override
    public AnnotatedTypeMirror getAnnotatedTypeLhs(Tree tree) {
        AnnotatedTypeMirror atm = super.getAnnotatedTypeLhs(tree);
        if (tree.getKind() == Tree.Kind.VARIABLE) {
            Receiver r;
            try {
                r = FlowExpressionParseUtil.internalReprOfVariable(this, (VariableTree) tree);
            } catch (FlowExpressionParseException ex) {
                r = null;
            }

            if (r != null) {
                String varName = r.toString();

                AnnotationMirror anm = atm.getAnnotation(SameLen.class);
                if (anm != null) {
                    List<String> slArrays = IndexUtil.getValueOfAnnotationWithStringArgument(anm);
                    if (slArrays.contains(varName)) {
                        slArrays.remove(varName);
                    }
                    if (slArrays.isEmpty()) {
                        atm.replaceAnnotation(UNKNOWN);
                    } else {
                        atm.replaceAnnotation(createSameLen(slArrays.toArray(new String[0])));
                    }
                }
            }
        }
        return atm;
    }

    /**
     * This function finds the union of the values of two annotations. Both annotations must have a
     * value field; otherwise the function will fail.
     *
     * @return the set union of the two value fields
     */
    public AnnotationMirror getCombinedSameLen(List<String> a1Names, List<String> a2Names) {

        HashSet<String> newValues = new HashSet<>(a1Names.size() + a2Names.size());

        newValues.addAll(a1Names);
        newValues.addAll(a2Names);
        String[] names = newValues.toArray(new String[newValues.size()]);
        return createSameLen(names);
    }

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
     * For the use of the transfer function; generates a SameLen that includes a and b, as well as
     * everything in sl1 and sl2, if they are SameLen annotations.
     *
     * @param receivers a list of receivers representing arrays to be included in the combined
     *     annotation
     * @param annos a list of the current annotations of the receivers. Must be the same length as
     *     receivers.
     * @return a combined SameLen annotation
     */
    public AnnotationMirror createCombinedSameLen(
            List<FlowExpressions.Receiver> receivers, List<AnnotationMirror> annos) {

        assert receivers.size() == annos.size();
        List<String> values = new ArrayList<>();
        for (int i = 0; i < receivers.size(); i++) {
            Receiver rec = receivers.get(i);
            AnnotationMirror anno = annos.get(i);
            if (shouldUseInAnnotation(rec)) {
                values.add(rec.toString());
            }
            if (AnnotationUtils.areSameByClass(anno, SameLen.class)) {
                values.addAll(getValueOfAnnotationWithStringArgument(anno));
            }
        }
        AnnotationMirror res = getCombinedSameLen(values, new ArrayList<>());
        return res;
    }

    public static boolean shouldUseInAnnotation(Receiver receiver) {
        return !receiver.containsUnknown()
                && !(receiver instanceof FlowExpressions.ArrayCreation)
                && !(receiver instanceof FlowExpressions.ClassName)
                // Big expressions cause a stack overflow in FlowExpressionParseUtil.
                // So limit them to an arbitrary length of 999.
                && receiver.toString().length() < 1000;
    }

    /**
     * The qualifier hierarchy for the sameLen type system. SameLen is strange, because most types
     * are distinct and at the same level: for instance @SameLen("a") and @SameLen("b) have nothing
     * in common. However, if one type includes even one overlapping name, then the types have to be
     * the same: so @SameLen({"a","b","c"} and @SameLen({"c","f","g"} are actually the same type,
     * and have to be treated as such - both should usually be replaced by a SameLen with the union
     * of the lists of names.
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

        /** Case 3. */
        @Override
        public AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2) {
            if (AnnotationUtils.hasElementValue(a1, "value")
                    && AnnotationUtils.hasElementValue(a2, "value")) {
                List<String> a1Val = getValueOfAnnotationWithStringArgument(a1);
                List<String> a2Val = getValueOfAnnotationWithStringArgument(a2);

                if (!Collections.disjoint(a1Val, a2Val)) {
                    return getCombinedSameLen(a1Val, a2Val);
                } else {
                    return BOTTOM;
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

        /** Case 4. */
        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
            if (AnnotationUtils.hasElementValue(a1, "value")
                    && AnnotationUtils.hasElementValue(a2, "value")) {
                List<String> a1Val = getValueOfAnnotationWithStringArgument(a1);
                List<String> a2Val = getValueOfAnnotationWithStringArgument(a2);

                if (!Collections.disjoint(a1Val, a2Val)) {
                    a1Val.retainAll(a2Val);
                    return createSameLen(a1Val.toArray(new String[0]));
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
        return new ListTreeAnnotator(
                super.createTreeAnnotator(),
                new SameLenTreeAnnotator(this),
                new PropagationTreeAnnotator(this),
                new ImplicitsTreeAnnotator(this));
    }

    /**
     * SameLen needs a tree annotator in order to properly type the right side of assignments of new
     * arrays that are initialized with the length of another array.
     */
    protected class SameLenTreeAnnotator extends TreeAnnotator {

        public SameLenTreeAnnotator(SameLenAnnotatedTypeFactory factory) {
            super(factory);
        }

        /** Case 5. */
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
                    if (shouldUseInAnnotation(rec)) {
                        if (AnnotationUtils.areSameByClass(sequenceAnno, SameLenUnknown.class)) {
                            sequenceAnno = createSameLen(rec.toString());
                        } else if (AnnotationUtils.areSameByClass(sequenceAnno, SameLen.class)) {
                            // Ensure that the sequence whose length is actually being used is part
                            // of the annotation. If not, add it.
                            List<String> sequenceAnnoSequences =
                                    getValueOfAnnotationWithStringArgument(sequenceAnno);
                            if (!sequenceAnnoSequences.contains(rec.toString())) {
                                sequenceAnnoSequences.add(rec.toString());
                                String[] newSequenceAnnoSequences =
                                        sequenceAnnoSequences.toArray(new String[0]);
                                sequenceAnno = createSameLen(newSequenceAnnoSequences);
                            }
                        }
                    }
                    type.addAnnotation(sequenceAnno);
                }
            }
            return null;
        }
    }

    /** Creates a @SameLen annotation whose values are the given strings. */
    public AnnotationMirror createSameLen(String... val) {
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, SameLen.class);
        Arrays.sort(val);
        builder.setValue("value", val);
        return builder.build();
    }

    /**
     * Find all the sequences that are members of the SameLen annotation associated with the
     * sequence named in sequenceExpression along the current path.
     */
    public List<String> getSameLensFromString(
            String sequenceExpression, Tree tree, TreePath currentPath) {
        AnnotationMirror sameLenAnno = null;
        try {
            sameLenAnno =
                    getAnnotationFromJavaExpressionString(
                            sequenceExpression, tree, currentPath, SameLen.class);
        } catch (FlowExpressionParseUtil.FlowExpressionParseException e) {
            // ignore parse errors
        }
        if (sameLenAnno == null) {
            // Could not find a more precise type, so return 0;
            return new ArrayList<>();
        }
        return getValueOfAnnotationWithStringArgument(sameLenAnno);
    }
}
