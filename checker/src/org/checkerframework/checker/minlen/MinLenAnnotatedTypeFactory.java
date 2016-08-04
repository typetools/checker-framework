package org.checkerframework.checker.minlen;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.minlen.qual.*;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationUtils;

public class MinLenAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {
    private final ValueAnnotatedTypeFactory valueAnnotatedTypeFactory;

    protected static ProcessingEnvironment env;

    public MinLenAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        env = checker.getProcessingEnvironment();
        valueAnnotatedTypeFactory = getTypeFactoryOfSubchecker(ValueChecker.class);
        this.postInit();
    }

    /** @throws IllegalArgumentException
     * find the minimum value in a value type. If there is no information,
     * throw an illegal argument exception (callers should check for this)
     */
    public int minLenFromValueType(AnnotatedTypeMirror valueType) {
        List<Long> possibleValues = possibleValuesFromValueType(valueType);
        if (possibleValues == null || possibleValues.size() == 0) {
            throw new IllegalArgumentException();
        }
        int min = Integer.MAX_VALUE;
        for (Long l : possibleValues) {
            if (l < min) {
                min = l.intValue();
            }
        }
        return min;
    }

    /** get the list of possible values from a value checker type */
    private List<Long> possibleValuesFromValueType(AnnotatedTypeMirror valueType) {
        List<Long> possibleValues = null;
        try {
            possibleValues =
                    ValueAnnotatedTypeFactory.getIntValues(valueType.getAnnotation(IntVal.class));
        } catch (NullPointerException npe) {
        }
        return possibleValues;
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new MinLenQualifierHierarchy(factory);
    }

    /**
     * The qualifier hierarchy for the upperbound type system
     * The qh is responsible for determining the relationships
     * between various qualifiers - especially subtyping relations.
     */
    private final class MinLenQualifierHierarchy extends MultiGraphQualifierHierarchy {
        /**
         * @param factory
         *            MultiGraphFactory to use to construct this
         */
        public MinLenQualifierHierarchy(MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
            super(factory);
        }

        /**
         * Computes subtyping as per the subtyping in the qualifier hierarchy
         * structure unless both annotations are the same. In this case, rhs is a
         * subtype of lhs iff lhs contains at least every element of rhs
         *
         * @return true if rhs is a subtype of lhs, false otherwise
         */
        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
            if (AnnotationUtils.areSameByClass(lhs, MinLenUnknown.class)) {
                return true;
            } else if (AnnotationUtils.areSameByClass(rhs, MinLenUnknown.class)) {
                return false;
            } else if (AnnotationUtils.areSameIgnoringValues(rhs, lhs)) {
                // implies both are MinLen since that's the only other type
                Integer rhsVal = AnnotationUtils.getElementValue(rhs, "value", Integer.class, true);
                Integer lhsVal = AnnotationUtils.getElementValue(lhs, "value", Integer.class, true);
                return rhsVal >= lhsVal;
            }
            return false;
        }
    }

    @Override
    public TreeAnnotator createTreeAnnotator() {
        return new ListTreeAnnotator(
                new MinLenTreeAnnotator(this), new PropagationTreeAnnotator(this));
    }

    protected class MinLenTreeAnnotator extends TreeAnnotator {

        public MinLenTreeAnnotator(MinLenAnnotatedTypeFactory factory) {
            super(factory);
        }

        /** When we encounter a new array, record how long it is.
         *  TODO write how this is done
         *
         */
        @Override
        public Void visitNewArray(NewArrayTree tree, AnnotatedTypeMirror type) {
            if (tree.getDimensions().size() == 0) {
                type.replaceAnnotation(createMinLen(tree.getInitializers().size()));
                return super.visitNewArray(tree, type);
            }
            ExpressionTree dim = tree.getDimensions().get(0);
            AnnotatedTypeMirror valueType = valueAnnotatedTypeFactory.getAnnotatedType(dim);
            try { // try to use value checker information
                int val = minLenFromValueType(valueType);
                type.replaceAnnotation(createMinLen(val));
            } catch (IllegalArgumentException iae) {
            } // if the value checker doesn't know anything
            // the index checker has something about member select
            // trees here, but I don't know what those are so I'm just
            // going to not include them and see what happens
            AnnotationMirror ATM = getAnnotatedType(dim).getAnnotation(MinLen.class);
            if (dim.getKind().equals(Tree.Kind.MEMBER_SELECT)) {
                MemberSelectTree MST = (MemberSelectTree) dim;
                AnnotationMirror dimType =
                        getAnnotatedType(MST.getExpression()).getAnnotation(MinLen.class);
                // if it doesnt have this annotation it will be null
                if (type != null) {
                    type.addAnnotation(dimType);
                }
            }

            return super.visitNewArray(tree, type);
        }
    }

    static AnnotationMirror createMinLen(int val) {
        AnnotationBuilder builder = new AnnotationBuilder(env, MinLen.class);
        builder.setValue("value", val);
        System.out.println("value: " + val);
        return builder.build();
    }
}
