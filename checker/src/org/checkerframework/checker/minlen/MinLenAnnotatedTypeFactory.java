package org.checkerframework.checker.minlen;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import java.util.ArrayList;
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
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.javacutil.AnnotationUtils;

/**
 *  The MinLen checker is responsible for annotating arrays with their
 *  minimum lengths. It is meant to be run by the upper bound checker.
 */
public class MinLenAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /**
     * Provides a way to query the Constant Value Checker, which computes the
     * values of expressions known at compile time (constant prop + folding).
     */
    private final ValueAnnotatedTypeFactory valueAnnotatedTypeFactory;

    /**
     *  We need this to make an AnnotationBuilder for some reason.
     */
    protected static ProcessingEnvironment env;

    public MinLenAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        env = checker.getProcessingEnvironment();
        valueAnnotatedTypeFactory = getTypeFactoryOfSubchecker(ValueChecker.class);
        this.postInit();
    }

    /**
     * Finds the minimum value in a value type. If there is no information
     * (such as when the list is empty or null), returns null. Otherwise,
     * returns the smallest element in the list of possible values.
     */
    public Integer minLenFromValueType(AnnotatedTypeMirror valueType) {
        List<Long> possibleValues = possibleValuesFromValueType(valueType);
        if (possibleValues == null || possibleValues.size() == 0) {
            return null;
        }
        // We're sure there is at least one element in the list, because of the previous check.
        Integer min = Integer.MAX_VALUE;
        for (Long l : possibleValues) {
            if (l < min) {
                min = l.intValue();
            }
        }
        return min;
    }

    /**
     *  Get the list of possible values from a value checker type.
     *  May return null.
     */
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
     * The qualifier hierarchy for the minlen type system.
     * The qh is responsible for determining the relationships
     * between various qualifiers - especially subtyping relations.
     * In particular, it's used to determine the relationship between
     * two identical qualifiers with differing arguments (i.e.
     * MinLen(2) vs MinLen(3).
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
         * subtype of lhs iff lhs contains at least every element of rhs.
         *
         * @return true if rhs is a subtype of lhs, false otherwise.
         */
        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
            if (AnnotationUtils.areSameByClass(lhs, MinLenUnknown.class)) {
                return true;
            } else if (AnnotationUtils.areSameByClass(rhs, MinLenUnknown.class)) {
                return false;
            } else if (AnnotationUtils.areSameIgnoringValues(rhs, lhs)) {
                // Implies both are MinLen since that's the only other type.
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
        return new ListTypeAnnotator(new MinLenTypeAnnotator(this));
    }

    protected class MinLenTypeAnnotator extends TypeAnnotator {

        public MinLenTypeAnnotator(MinLenAnnotatedTypeFactory atf) {
            super(atf);
        }

        @Override
        public Void visitArray(AnnotatedArrayType type, Void aVoid) {
            //	    ArrayList<AnnotationMirror> rganm = new ArrayList<AnnotationMirror>();
            //rganm.add(createMinLen(0));
            //type.addMissingAnnotations(rganm);
            System.out.println(type);
            type.replaceAnnotation(createMinLen(0));
            System.out.println(type);
            return super.visitArray(type, aVoid);
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
            // When there is an explicit initialization, we know the length.
            if (tree.getDimensions().size() == 0) {
                type.replaceAnnotation(createMinLen(tree.getInitializers().size()));
                return super.visitNewArray(tree, type);
            }
            ExpressionTree dim = tree.getDimensions().get(0);
            AnnotatedTypeMirror valueType = valueAnnotatedTypeFactory.getAnnotatedType(dim);
            // Try to use value checker information.
            Integer val = minLenFromValueType(valueType);
            if (val != null) {
                type.replaceAnnotation(createMinLen(val));
            }
            /* For when the value checker doesn't know anything.
             * The old index checker had something about member select
             * trees here, but I don't know what those are so I'm just
             * going to not include them and see what happens.
             */
            AnnotationMirror ATM = getAnnotatedType(dim).getAnnotation(MinLen.class);
            if (dim.getKind().equals(Tree.Kind.MEMBER_SELECT)) {
                MemberSelectTree MST = (MemberSelectTree) dim;
                AnnotationMirror dimType =
                        getAnnotatedType(MST.getExpression()).getAnnotation(MinLen.class);
                // If it doesnt have this annotation it will be null.
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
        return builder.build();
    }
}
