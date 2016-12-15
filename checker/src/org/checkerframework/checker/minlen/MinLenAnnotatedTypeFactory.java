package org.checkerframework.checker.minlen;

import com.sun.source.tree.*;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.checker.minlen.qual.*;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.IntVal;
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
import org.checkerframework.javacutil.TreeUtils;

/**
 * The MinLen checker is responsible for annotating arrays with their minimum lengths. It is meant
 * to be run by the upper bound checker.
 */
public class MinLenAnnotatedTypeFactory
        extends GenericAnnotatedTypeFactory<
                MinLenValue, MinLenStore, MinLenTransfer, MinLenAnalysis> {

    protected static ProcessingEnvironment env;

    /**
     * Provides a way to query the Constant Value Checker, which computes the values of expressions
     * known at compile time (constant prop + folding).
     */
    private final ValueAnnotatedTypeFactory valueAnnotatedTypeFactory;

    public MinLenAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        valueAnnotatedTypeFactory = getTypeFactoryOfSubchecker(ValueChecker.class);
        env = checker.getProcessingEnvironment();
        this.postInit();
    }

    @Override
    protected void addCheckedCodeDefaults(QualifierDefaults defaults) {
        AnnotationMirror minLen0 = createMinLen(0);
        defaults.addCheckedCodeDefault(minLen0, TypeUseLocation.OTHERWISE);
    }

    /** Returns the value type associated with the given ExpressionTree. */
    public AnnotatedTypeMirror valueTypeFromTree(Tree tree) {
        return valueAnnotatedTypeFactory.getAnnotatedType(tree);
    }

    /**
     * Finds the minimum value in a value type. If there is no information (such as when the list is
     * empty or null), returns null. Otherwise, returns the smallest element in the list of possible
     * values.
     */
    public Integer minLenFromValueType(AnnotatedTypeMirror valueType) {
        List<Long> possibleValues = possibleValuesFromValueType(valueType);
        if (possibleValues == null || possibleValues.size() == 0) {
            return null;
        }
        // We're sure there is at least one element in the list, because of the previous check.
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
                tops.add(createMinLen(0));
                minLenTops = Collections.unmodifiableSet(tops);
            }
            return minLenTops;
        }

        @Override
        public AnnotationMirror getTopAnnotation(AnnotationMirror start) {
            return createMinLen(0);
        }

        @Override
        public AnnotationMirror greatestLowerBound(AnnotationMirror a1, AnnotationMirror a2) {
            if (AnnotationUtils.hasElementValue(a1, "value")
                    && AnnotationUtils.hasElementValue(a2, "value")) {
                Integer a1Val = AnnotationUtils.getElementValue(a1, "value", Integer.class, true);
                Integer a2Val = AnnotationUtils.getElementValue(a2, "value", Integer.class, true);
                if (a1Val >= a2Val) {
                    return a1;
                } else {
                    return a2;
                }
            } else {
                // One of these is bottom. GLB of anything and bottom is bottom.
                if (AnnotationUtils.areSameByClass(a1, MinLenBottom.class)) {
                    return a1;
                } else if (AnnotationUtils.areSameByClass(a2, MinLenBottom.class)) {
                    return a2;
                }
            }

            // This should be unreachable but we want the function to be complete, so return bottom.
            return createMinLenBottom();
        }

        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror a1, AnnotationMirror a2) {
            if (AnnotationUtils.hasElementValue(a1, "value")
                    && AnnotationUtils.hasElementValue(a2, "value")) {
                Integer a1Val = AnnotationUtils.getElementValue(a1, "value", Integer.class, true);
                Integer a2Val = AnnotationUtils.getElementValue(a2, "value", Integer.class, true);
                if (a1Val <= a2Val) {
                    return a1;
                } else {
                    return a2;
                }
            } else {
                // One of these is bottom. LUB of anything and bottom is the anything.
                if (AnnotationUtils.areSameByClass(a1, MinLenBottom.class)) {
                    return a2;
                } else if (AnnotationUtils.areSameByClass(a2, MinLenBottom.class)) {
                    return a1;
                }
            }

            // This should be unreachable but we want the function to be complete, so we return top.
            return createMinLen(0);
        }

        /**
         * Computes subtyping as per the subtyping in the qualifier hierarchy structure unless both
         * annotations are the same. In this case, rhs is a subtype of lhs iff lhs contains at least
         * every element of rhs.
         *
         * @return true if rhs is a subtype of lhs, false otherwise.
         */
        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
            if (AnnotationUtils.areSameByClass(rhs, MinLenBottom.class)) {
                return true;
            } else if (AnnotationUtils.areSameByClass(lhs, MinLenBottom.class)) {
                return false;
            } else if (AnnotationUtils.areSameIgnoringValues(rhs, lhs)) {
                // Implies both are MinLen since that's the only other type.
                // There used to be a check to see if these values existed.
                // It's been removed; if they don't exist, we should get the default (0).

                Integer rhsVal = AnnotationUtils.getElementValue(rhs, "value", Integer.class, true);
                Integer lhsVal = AnnotationUtils.getElementValue(lhs, "value", Integer.class, true);
                return rhsVal >= lhsVal;
            }
            return false;
        }
    }

    // This is based on a suggestion Suzanne had for making arrays always default
    // to MinLen(0).

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
                type.replaceAnnotation(createMinLen(0));
            }
            return super.visitArray(type, aVoid);
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

        /** When we encounter a new array, record how long it is. TODO write how this is done */
        @Override
        public Void visitNewArray(NewArrayTree tree, AnnotatedTypeMirror type) {

            AnnotatedTypeMirror valueType = valueAnnotatedTypeFactory.getAnnotatedType(tree);

            if (valueType.hasAnnotation(ArrayLen.class)) {
                AnnotationMirror anm = valueType.getAnnotation(ArrayLen.class);
                Integer val = Collections.min(ValueAnnotatedTypeFactory.getArrayLength(anm));
                type.replaceAnnotation(createMinLen(val));
            }

            return super.visitNewArray(tree, type);
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
        public Void visitIdentifier(IdentifierTree tree, AnnotatedTypeMirror type) {
            AnnotatedTypeMirror valueType = valueAnnotatedTypeFactory.getAnnotatedType(tree);

            if (valueType.hasAnnotation(ArrayLen.class)) {
                AnnotationMirror anm = valueType.getAnnotation(ArrayLen.class);
                Integer val = Collections.min(ValueAnnotatedTypeFactory.getArrayLength(anm));
                type.replaceAnnotation(createMinLen(val));
            }
            return super.visitIdentifier(tree, type);
        }
    }

    public ValueAnnotatedTypeFactory getValueAnnotatedTypeFactory() {
        return valueAnnotatedTypeFactory;
    }

    protected static int getMinLenValue(AnnotationMirror annotation) {
        if (annotation == null || AnnotationUtils.areSameByClass(annotation, MinLenBottom.class)) {
            return -1;
        }
        ExecutableElement valueMethod =
                TreeUtils.getMethod(
                        "org.checkerframework.checker.minlen.qual.MinLen", "value", 0, env);
        return (int)
                AnnotationUtils.getElementValuesWithDefaults(annotation)
                        .get(valueMethod)
                        .getValue();
    }

    public AnnotationMirror createMinLen(int val) {
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, MinLen.class);
        builder.setValue("value", val);
        return builder.build();
    }

    public AnnotationMirror createMinLenBottom() {
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv, MinLenBottom.class);
        return builder.build();
    }
}
