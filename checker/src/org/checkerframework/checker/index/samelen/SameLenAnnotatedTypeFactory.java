package org.checkerframework.checker.index.samelen;

import static org.checkerframework.checker.index.IndexUtil.getValueOfAnnotationWithStringArgument;

import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreePath;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
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
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * The SameLen Checker is used to determine whether there are multiple arrays in a program that
 * share the same length. It is part of the Index Checker, and is used as a subchecker by the Index
 * Checker's components.
 */
public class SameLenAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    public final AnnotationMirror UNKNOWN;
    private final AnnotationMirror BOTTOM;
    private final AnnotationMirror POLY;

    public SameLenAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        UNKNOWN = AnnotationUtils.fromClass(elements, SameLenUnknown.class);
        BOTTOM = AnnotationUtils.fromClass(elements, SameLenBottom.class);
        POLY = AnnotationUtils.fromClass(elements, PolySameLen.class);
        addAliasedAnnotation(PolyAll.class, POLY);

        this.postInit();
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

    /**
     * Checks whether the two string lists contain at least one string that's the same. Not a smart
     * algorithm; meant to be run over small sets of data.
     *
     * @param listA the first string list
     * @param listB the second string list
     * @return true if the intersection is non-empty; false otherwise
     */
    private boolean overlap(List<String> listA, List<String> listB) {
        for (String a : listA) {
            for (String b : listB) {
                if (a.equals(b)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * This function finds the union of the values of two annotations. Both annotations must have a
     * value field; otherwise the function will fail.
     *
     * @return the set union of the two value fields
     */
    public AnnotationMirror getCombinedSameLen(List<String> a1Names, List<String> a2Names) {

        HashSet<String> newValues = new HashSet<String>(a1Names.size() + a2Names.size());

        newValues.addAll(a1Names);
        newValues.addAll(a2Names);
        String[] names = newValues.toArray(new String[newValues.size()]);
        Arrays.sort(names);
        return createSameLen(names);
    }

    /**
     * For the use of the transfer function; generates a SameLen that includes a and b, as well as
     * everything in sl1 and sl2, if they are SameLen annotations.
     *
     * @param aRec receiver representing the first array
     * @param bRec receiver representing the second array
     * @param sl1 the current annotation of the first array
     * @param sl2 the current annotation of the second array
     * @return a combined SameLen annotation
     */
    public AnnotationMirror createCombinedSameLen(
            FlowExpressions.Receiver aRec,
            FlowExpressions.Receiver bRec,
            AnnotationMirror sl1,
            AnnotationMirror sl2) {
        List<String> aValues = new ArrayList<String>();
        List<String> bValues = new ArrayList<String>();

        if (isReceiverToStringParsable(aRec)) {
            aValues.add(aRec.toString());
            if (AnnotationUtils.areSameByClass(sl1, SameLen.class)) {
                aValues.addAll(getValueOfAnnotationWithStringArgument(sl1));
            }
        }
        if (isReceiverToStringParsable(bRec)) {
            bValues.add(bRec.toString());
            if (AnnotationUtils.areSameByClass(sl2, SameLen.class)) {
                bValues.addAll(getValueOfAnnotationWithStringArgument(sl2));
            }
        }

        AnnotationMirror res = getCombinedSameLen(aValues, bValues);
        return res;
    }

    public static boolean isReceiverToStringParsable(Receiver receiver) {
        return !receiver.containsUnknown() && !(receiver instanceof FlowExpressions.ArrayCreation);
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

        @Override
        public AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2) {
            if (AnnotationUtils.hasElementValue(a1, "value")
                    && AnnotationUtils.hasElementValue(a2, "value")) {
                List<String> a1Val = getValueOfAnnotationWithStringArgument(a1);
                List<String> a2Val = getValueOfAnnotationWithStringArgument(a2);

                if (overlap(a1Val, a2Val)) {
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

        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
            if (AnnotationUtils.hasElementValue(a1, "value")
                    && AnnotationUtils.hasElementValue(a2, "value")) {
                List<String> a1Val = getValueOfAnnotationWithStringArgument(a1);
                List<String> a2Val = getValueOfAnnotationWithStringArgument(a2);

                if (overlap(a1Val, a2Val)) {
                    return getCombinedSameLen(a1Val, a2Val);
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
                List<String> a1Val = getValueOfAnnotationWithStringArgument(subAnno);
                List<String> a2Val = getValueOfAnnotationWithStringArgument(superAnno);

                if (overlap(a1Val, a2Val)) {
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

        @Override
        public Void visitNewArray(NewArrayTree node, AnnotatedTypeMirror type) {
            if (node.getDimensions().size() == 1
                    && TreeUtils.isArrayLengthAccess(node.getDimensions().get(0))) {
                MemberSelectTree arrayLength = (MemberSelectTree) (node.getDimensions().get(0));
                AnnotationMirror arrayAnno =
                        getAnnotatedType(arrayLength.getExpression())
                                .getAnnotationInHierarchy(UNKNOWN);

                if (AnnotationUtils.areSameByClass(arrayAnno, SameLenUnknown.class)) {
                    Receiver rec =
                            FlowExpressions.internalReprOf(
                                    this.atypeFactory, arrayLength.getExpression());
                    if (isReceiverToStringParsable(rec)) {
                        arrayAnno = createSameLen(rec.toString());
                    }
                }

                type.addAnnotation(arrayAnno);
            }
            return null;
        }
    }

    /** Creates a @SameLen annotation whose values are the given strings. */
    public AnnotationMirror createSameLen(String... val) {
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, SameLen.class);
        builder.setValue("value", val);
        return builder.build();
    }

    public List<String> getSameLensFromString(
            String arrayExpression, Tree tree, TreePath currentPath) {
        AnnotationMirror sameLenAnno = null;
        try {
            sameLenAnno =
                    getAnnotationFromJavaExpressionString(
                            arrayExpression, tree, currentPath, SameLen.class);
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
