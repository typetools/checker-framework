package org.checkerframework.checker.minlen;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.minlen.qual.MinLen;
import org.checkerframework.checker.minlen.qual.MinLenBottom;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.StringVal;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * The MinLen checker is responsible for annotating arrays with their minimum lengths. It is meant
 * to be run by the upper bound checker.
 */
public class MinLenAnnotatedTypeFactory
        extends GenericAnnotatedTypeFactory<CFValue, MinLenStore, MinLenTransfer, MinLenAnalysis> {

    /** {@code @MinLen(0)}, which is the top qualifier. */
    final AnnotationMirror MIN_LEN_0;

    final AnnotationMirror MIN_LEN_BOTTOM;
    final List<ExecutableElement> listRemoveMethods;
    final List<ExecutableElement> listClearMethods;

    public MinLenAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, MinLen.class);
        builder.setValue("value", 0);
        listRemoveMethods = TreeUtils.getMethodList("java.util.List", "remove", 1, processingEnv);
        listClearMethods = TreeUtils.getMethodList("java.util.List", "clear", 0, processingEnv);
        listClearMethods.add(TreeUtils.getMethod("java.util.List", "removeAll", 1, processingEnv));
        listClearMethods.add(TreeUtils.getMethod("java.util.List", "retainAll", 1, processingEnv));

        MIN_LEN_0 = builder.build();
        MIN_LEN_BOTTOM = AnnotationUtils.fromClass(elements, MinLenBottom.class);
        this.postInit();
    }

    @Override
    protected void addCheckedCodeDefaults(QualifierDefaults defaults) {
        AnnotationMirror minLen0 = MIN_LEN_0;
        defaults.addCheckedCodeDefault(minLen0, TypeUseLocation.OTHERWISE);
    }
    /**
     * Provides a way to query the Constant Value Checker, which computes the values of expressions
     * known at compile time (constant prop + folding).
     */
    public ValueAnnotatedTypeFactory getValueAnnotatedTypeFactory() {
        return getTypeFactoryOfSubchecker(ValueChecker.class);
    }

    /** Returns the value type associated with the given ExpressionTree. */
    public AnnotatedTypeMirror valueTypeFromTree(Tree tree) {
        return getValueAnnotatedTypeFactory().getAnnotatedType(tree);
    }

    /**
     * Finds the minimum value in a value type. If there is no information (such as when the list is
     * empty or null), returns null. Otherwise, returns the smallest element in the list of possible
     * values.
     */
    public Integer getMinLenFromValueType(AnnotatedTypeMirror valueType) {
        List<Long> possibleValues = possibleValuesFromValueType(valueType);
        if (possibleValues == null || possibleValues.size() == 0) {
            return null;
        }
        // There must be at least one element in the list, because of the previous check.
        Integer min = Collections.min(possibleValues).intValue();
        return min;
    }

    /** Get the list of possible values from a value checker type. May return null. */
    private List<Long> possibleValuesFromValueType(AnnotatedTypeMirror valueType) {
        AnnotationMirror anm = valueType.getAnnotation(IntVal.class);
        if (anm == null) {
            return null;
        }
        return ValueAnnotatedTypeFactory.getIntValues(anm);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new MinLenQualifierHierarchy(factory);
    }

    public boolean isListRemove(ExecutableElement method) {
        for (ExecutableElement removeMethod : listRemoveMethods) {
            if (ElementUtils.isMethod(method, removeMethod, processingEnv)) {
                return true;
            }
        }
        return false;
    }

    public boolean isListClear(ExecutableElement method) {
        for (ExecutableElement removeMethod : listClearMethods) {
            if (ElementUtils.isMethod(method, removeMethod, processingEnv)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The qualifier hierarchy for the minlen type system. The qh is responsible for determining the
     * relationships between various qualifiers - especially subtyping relations. In particular,
     * it's used to determine the relationship between two identical qualifiers with differing
     * arguments (i.e. MinLen(2) vs MinLen(3).
     */
    private final class MinLenQualifierHierarchy extends MultiGraphQualifierHierarchy {

        Set<? extends AnnotationMirror> minLenTops = null;

        /** @param factory MultiGraphFactory to use to construct this */
        public MinLenQualifierHierarchy(MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
            super(factory);
        }

        @Override
        public Set<? extends AnnotationMirror> getTopAnnotations() {
            if (minLenTops == null) {
                Set<AnnotationMirror> tops = AnnotationUtils.createAnnotationSet();
                tops.add(MIN_LEN_0);
                minLenTops = Collections.unmodifiableSet(tops);
            }
            return minLenTops;
        }

        @Override
        public AnnotationMirror getTopAnnotation(AnnotationMirror start) {
            return MIN_LEN_0;
        }

        @Override
        public AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2) {
            // GLB of anything and bottom is bottom.
            if (AnnotationUtils.areSameByClass(a1, MinLenBottom.class)) {
                return a1;
            } else if (AnnotationUtils.areSameByClass(a2, MinLenBottom.class)) {
                return a2;
            } else {
                Integer a1Val = AnnotationUtils.getElementValue(a1, "value", Integer.class, true);
                Integer a2Val = AnnotationUtils.getElementValue(a2, "value", Integer.class, true);
                if (a1Val >= a2Val) {
                    return a1;
                } else {
                    return a2;
                }
            }
        }

        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
            // One of these is bottom. LUB of anything and bottom is the anything.
            if (AnnotationUtils.areSameByClass(a1, MinLenBottom.class)) {
                return a2;
            } else if (AnnotationUtils.areSameByClass(a2, MinLenBottom.class)) {
                return a1;
            } else {
                Integer a1Val = AnnotationUtils.getElementValue(a1, "value", Integer.class, true);
                Integer a2Val = AnnotationUtils.getElementValue(a2, "value", Integer.class, true);
                if (a1Val <= a2Val) {
                    return a1;
                } else {
                    return a2;
                }
            }
        }

        /**
         * Computes subtyping as per the subtyping in the qualifier hierarchy structure unless both
         * annotations are the same. In this case, rhs is a subtype of lhs iff lhs contains at least
         * every element of rhs.
         *
         * @return true if rhs is a subtype of lhs, false otherwise
         */
        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
            if (AnnotationUtils.areSameByClass(rhs, MinLenBottom.class)) {
                return true;
            } else if (AnnotationUtils.areSameByClass(lhs, MinLenBottom.class)) {
                return false;
            } else if (AnnotationUtils.areSameIgnoringValues(rhs, lhs)) {
                // Implies both are MinLen since that's the only other type.
                // There is no need for a check to see if these values exist - they must.

                Integer rhsVal = AnnotationUtils.getElementValue(rhs, "value", Integer.class, true);
                Integer lhsVal = AnnotationUtils.getElementValue(lhs, "value", Integer.class, true);
                return rhsVal >= lhsVal;
            }
            return false;
        }
    }

    @Override
    protected TypeAnnotator createTypeAnnotator() {
        return new ListTypeAnnotator(new MinLenTypeAnnotator(this), super.createTypeAnnotator());
    }

    protected class MinLenTypeAnnotator extends TypeAnnotator {

        public MinLenTypeAnnotator(MinLenAnnotatedTypeFactory atf) {
            super(atf);
        }

        @Override
        public Void visitArray(AnnotatedArrayType type, Void aVoid) {
            if (!type.hasAnnotation(MinLen.class)) {
                type.replaceAnnotation(MIN_LEN_0);
            }
            return super.visitArray(type, aVoid);
        }
    }

    private void addArrayLenAnnotation(AnnotatedTypeMirror valueType, AnnotatedTypeMirror type) {
        if (valueType.hasAnnotation(ArrayLen.class)) {
            AnnotationMirror anm = valueType.getAnnotation(ArrayLen.class);
            Integer val = Collections.min(ValueAnnotatedTypeFactory.getArrayLength(anm));
            type.replaceAnnotation(createMinLen(val));
        }
    }

    private void addStringValAnnotation(AnnotatedTypeMirror valueType, AnnotatedTypeMirror type) {
        if (valueType.hasAnnotation(StringVal.class)) {
            AnnotationMirror anm = valueType.getAnnotation(StringVal.class);
            String[] values =
                    AnnotationUtils.getElementValueArray(anm, "value", String.class, true)
                            .toArray(new String[0]);
            ArrayList<Integer> lengths = new ArrayList<>();
            for (int i = 0; i < values.length; i++) {
                lengths.add(values[i].length());
            }
            int val = Collections.min(lengths);
            type.replaceAnnotation(createMinLen(val));
        }
    }

    @Override
    public void addComputedTypeAnnotations(Element element, AnnotatedTypeMirror type) {
        super.addComputedTypeAnnotations(element, type);
        if (element != null) {
            AnnotatedTypeMirror valueType =
                    getValueAnnotatedTypeFactory().getAnnotatedType(element);
            addArrayLenAnnotation(valueType, type);
            addStringValAnnotation(valueType, type);
        }
    }

    @Override
    public void addComputedTypeAnnotations(Tree tree, AnnotatedTypeMirror type, boolean iUseFlow) {
        super.addComputedTypeAnnotations(tree, type, iUseFlow);
        // TODO: Martin: Why did I use this here? Because this is the check that happens in AnnotatedTypeFactory#getAnnotatedType
        // and causes the program to fail if it fails. I'm unsure of why; I should ask Suzanne when she gets back 1/2/17
        if (tree != null && TreeUtils.isExpressionTree(tree)) {
            AnnotatedTypeMirror valueType = valueTypeFromTree(tree);
            addArrayLenAnnotation(valueType, type);
            addStringValAnnotation(valueType, type);
        }
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                new MinLenTreeAnnotator(this),
                new PropagationTreeAnnotator(this),
                new ImplicitsTreeAnnotator(this));
    }

    protected class MinLenTreeAnnotator extends TreeAnnotator {

        public MinLenTreeAnnotator(MinLenAnnotatedTypeFactory factory) {
            super(factory);
        }

        @Override
        public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {

            if (tree.getKind() == Tree.Kind.STRING_LITERAL) {
                String lit = (String) (tree.getValue());
                type.replaceAnnotation(createMinLen(lit.length()));
            }

            return super.visitLiteral(tree, type);
        }

        @Override
        public Void visitNewArray(NewArrayTree node, AnnotatedTypeMirror type) {
            if (node.getDimensions().size() > 0) {
                // If the dimension of the new array is the length of another array, then the
                // MinLen of the new array is the min len of the other array.  (Dimensions that
                // are a constant value have a known ArrayLen which is converted to a MinLen in
                // addComputedTypeAnnotations.)
                ExpressionTree dimExp = node.getDimensions().get(0);
                if (TreeUtils.isArrayLengthAccess(dimExp)) {
                    AnnotationMirror minLenAnno =
                            getAnnotationMirror(
                                    ((MemberSelectTree) dimExp).getExpression(), MinLen.class);
                    if (minLenAnno != null) {
                        type.addAnnotation(minLenAnno);
                    }
                }
            }
            return null;
        }
    }

    protected static Integer getMinLenValue(AnnotationMirror annotation) {
        if (annotation == null || AnnotationUtils.areSameByClass(annotation, MinLenBottom.class)) {
            return null;
        }
        return AnnotationUtils.getElementValue(annotation, "value", Integer.class, true);
    }

    public Integer getMinLenValue(Tree tree) {
        AnnotatedTypeMirror atm = getAnnotatedType(tree);
        AnnotationMirror anm = atm.getAnnotation(MinLen.class);
        return getMinLenValue(anm);
    }

    public AnnotationMirror createMinLen(int val) {
        if (val == 0) {
            return MIN_LEN_0;
        }
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, MinLen.class);
        builder.setValue("value", val);
        return builder.build();
    }
}
