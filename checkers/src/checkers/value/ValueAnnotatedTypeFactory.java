package checkers.value;

import checkers.basetype.BaseAnnotatedTypeFactory;
import checkers.basetype.BaseTypeChecker;
import checkers.quals.TypeQualifiers;
import checkers.source.Result;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedArrayType;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.QualifierHierarchy;
import checkers.types.TreeAnnotator;
import checkers.types.TypeAnnotator;
import checkers.util.AnnotationBuilder;
import checkers.util.MultiGraphQualifierHierarchy;
import checkers.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import checkers.value.quals.Analyzable;
import checkers.value.quals.ArrayLen;
import checkers.value.quals.BoolVal;
import checkers.value.quals.BottomVal;
import checkers.value.quals.ByteVal;
import checkers.value.quals.CharVal;
import checkers.value.quals.DoubleVal;
import checkers.value.quals.FloatVal;
import checkers.value.quals.IntVal;
import checkers.value.quals.LongVal;
import checkers.value.quals.ShortVal;
import checkers.value.quals.StringVal;
import checkers.value.quals.UnknownVal;

import javacutils.AnnotationUtils;
import javacutils.TreeUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeMirror;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.TreePath;

/**
 * @author plvines <plvines@cs.washington.edu>
 * 
 *         AnnotatedTypeFactory for the Value type system.
 * 
 */
@TypeQualifiers({ ArrayLen.class, BoolVal.class, CharVal.class,
        DoubleVal.class, IntVal.class, StringVal.class, BottomVal.class,
        UnknownVal.class, FloatVal.class, ShortVal.class, ByteVal.class,
        LongVal.class })
public class ValueAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /** Annotation constants */
    protected final AnnotationMirror INTVAL, DOUBLEVAL, BOOLVAL, CHARVAL,
            ARRAYLEN, STRINGVAL, BOTTOMVAL, UNKNOWNVAL, ANALYZABLE, SHORTVAL,
            BYTEVAL, LONGVAL, FLOATVAL;

    private long t = 0;

    protected static final int MAX_VALUES = 10; // The maximum number of values
                                                // allowed in an annotation's
                                                // array
    protected final List<AnnotationMirror> constantAnnotations;
    protected final List<AnnotationMirror> orderedNumberAnnotations;
    protected Set<String> coveredClassStrings;

    /**
     * Constructor. Initializes all the AnnotationMirror constants.
     * 
     * @param checker
     * @param root
     * 
     */
    public ValueAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        INTVAL = AnnotationUtils.fromClass(elements, IntVal.class);
        CHARVAL = AnnotationUtils.fromClass(elements, CharVal.class);
        BOOLVAL = AnnotationUtils.fromClass(elements, BoolVal.class);
        ARRAYLEN = AnnotationUtils.fromClass(elements, ArrayLen.class);
        DOUBLEVAL = AnnotationUtils.fromClass(elements, DoubleVal.class);
        SHORTVAL = AnnotationUtils.fromClass(elements, ShortVal.class);
        LONGVAL = AnnotationUtils.fromClass(elements, LongVal.class);
        BYTEVAL = AnnotationUtils.fromClass(elements, ByteVal.class);
        STRINGVAL = AnnotationUtils.fromClass(elements, StringVal.class);
        FLOATVAL = AnnotationUtils.fromClass(elements, FloatVal.class);
        BOTTOMVAL = AnnotationUtils.fromClass(elements, BottomVal.class);
        ANALYZABLE = AnnotationUtils.fromClass(elements, Analyzable.class);
        UNKNOWNVAL = AnnotationUtils.fromClass(elements, UnknownVal.class);
        constantAnnotations = new ArrayList<AnnotationMirror>(9);
        constantAnnotations.add(DOUBLEVAL);
        constantAnnotations.add(INTVAL);
        constantAnnotations.add(CHARVAL);
        constantAnnotations.add(BOOLVAL);
        constantAnnotations.add(STRINGVAL);
        constantAnnotations.add(BOTTOMVAL);
        constantAnnotations.add(ARRAYLEN);
        constantAnnotations.add(ANALYZABLE);
        constantAnnotations.add(FLOATVAL);
        constantAnnotations.add(LONGVAL);
        constantAnnotations.add(SHORTVAL);
        constantAnnotations.add(BYTEVAL);
        constantAnnotations.add(UNKNOWNVAL);

        orderedNumberAnnotations = new ArrayList<AnnotationMirror>();
        orderedNumberAnnotations.add(DOUBLEVAL);
        orderedNumberAnnotations.add(FLOATVAL);
        orderedNumberAnnotations.add(LONGVAL);
        orderedNumberAnnotations.add(INTVAL);
        orderedNumberAnnotations.add(SHORTVAL);
        orderedNumberAnnotations.add(BYTEVAL);
        orderedNumberAnnotations.add(CHARVAL);

        coveredClassStrings = new HashSet<String>(19);
        coveredClassStrings.add("int");
        coveredClassStrings.add("java.lang.Integer");
        coveredClassStrings.add("double");
        coveredClassStrings.add("java.lang.Double");
        coveredClassStrings.add("byte");
        coveredClassStrings.add("java.lang.Byte");
        coveredClassStrings.add("java.lang.String");
        coveredClassStrings.add("char");
        coveredClassStrings.add("java.lang.Character");
        coveredClassStrings.add("float");
        coveredClassStrings.add("java.lang.Float");
        coveredClassStrings.add("boolean");
        coveredClassStrings.add("java.lang.Boolean");
        coveredClassStrings.add("long");
        coveredClassStrings.add("java.lang.Long");
        coveredClassStrings.add("short");
        coveredClassStrings.add("java.lang.Short");
        coveredClassStrings.add("byte[]");

        if (this.getClass().equals(ValueAnnotatedTypeFactory.class)) {
            this.postInit();
        }
    }

    public AnnotationMirror createAnnotation(String name, Set<?> values) {
        if (values.size() > 0 && values.size() < MAX_VALUES) {
            AnnotationBuilder builder = new AnnotationBuilder(processingEnv,
                    name);
            List<Object> valuesList = new ArrayList<Object>(values);
            builder.setValue("value", valuesList);
            return builder.build();
        } else {
            return UNKNOWNVAL;
        }
    }

    @Override
    protected MultiGraphQualifierHierarchy.MultiGraphFactory createQualifierHierarchyFactory() {
        return new MultiGraphQualifierHierarchy.MultiGraphFactory(this);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new ValueQualifierHierarchy(factory);
    }

    /**
     * The qualifier hierarchy for the Value type system
     */
    private final class ValueQualifierHierarchy extends
            MultiGraphQualifierHierarchy {

        /**
         * @param factory
         *            MultiGraphFactory to use to construct this
         * 
         * @return
         */
        public ValueQualifierHierarchy(
                MultiGraphQualifierHierarchy.MultiGraphFactory factory) {
            super(factory);
        }

        /**
         * Determines the least upper bound of a1 and a2. If a1 and a2 are both
         * the same type of Value annotation, then the LUB is the result of
         * taking all values from both a1 and a2 and removing duplicates. If a1
         * and a2 are not the same type of Value annotation they may still be
         * mergeable because some values can be implicitly cast as others. If a1
         * and a2 are both in {DoubleVal, IntVal, CharVal} then they will be
         * converted upwards: CharVal -> IntVal -> DoubleVal to arrive at a
         * common annotation type.
         * 
         * @param a1
         * @param a2
         * 
         * @return the least upper bound of a1 and a2
         */
        @Override
        public AnnotationMirror leastUpperBound(AnnotationMirror a1,
                AnnotationMirror a2) {
            if (!AnnotationUtils.areSameIgnoringValues(getTopAnnotation(a1),
                    getTopAnnotation(a2))) {
                return null;
            } else if (isSubtype(a1, a2)) {
                return a2;
            } else if (isSubtype(a2, a1)) {
                return a1;
            }
            // If both are the same type, determine the type and merge:
            else if (AnnotationUtils.areSameIgnoringValues(a1, a2)) {
                List<Object> a1Values = AnnotationUtils.getElementValueArray(
                        a1, "value", Object.class, true);
                List<Object> a2Values = AnnotationUtils.getElementValueArray(
                        a2, "value", Object.class, true);
                HashSet<Object> newValues = new HashSet<Object>(a1Values.size()
                        + a2Values.size());

                newValues.addAll(a1Values);
                newValues.addAll(a2Values);

                return createAnnotation(a1.getAnnotationType().toString(),
                        newValues);
            }
            // Annotations are in this hierarchy, but they are not the same
            else {
                // If either is UNKNOWNVAL, ARRAYLEN, or BOOLEAN then the LUB is
                // UnknownVal
                if (!isNumberAnnotation(a1) || !isNumberAnnotation(a2)) {
                    return UNKNOWNVAL;
                } else {
                    // At this point they must both be in the set
                    // {CharVal, IntVal, DoubleVal, ShortVal, ByteVal, LongVal,
                    // FloatVal} which means they
                    // can be LUB'd by casting upwards

                    AnnotationMirror[] sorted = getHighestAnnotation(a1, a2);
                    AnnotationMirror higher = sorted[0];
                    AnnotationMirror lower = sorted[1];
                    String anno = "checkers.value.quals.";
                    List<Number> valuesToCast;

                    // lower is CharVal
                    if (AnnotationUtils.areSameIgnoringValues(lower, CHARVAL)) {
                        List<Character> charVals = AnnotationUtils
                                .getElementValueArray(lower, "value",
                                        Character.class, true);

                        valuesToCast = new ArrayList<Number>(charVals.size());
                        for (Character c : charVals) {
                            valuesToCast.add(new Integer(c.charValue()));
                        }
                    } else {
                        valuesToCast = AnnotationUtils.getElementValueArray(
                                lower, "value", Number.class, true);
                    }

                    HashSet<Object> newValues = new HashSet<Object>(
                            AnnotationUtils.getElementValueArray(higher,
                                    "value", Object.class, true));

                    if (AnnotationUtils
                            .areSameIgnoringValues(higher, DOUBLEVAL)) {
                        for (Number n : valuesToCast) {
                            newValues.add(new Double(n.doubleValue()));
                        }
                        anno += "DoubleVal";
                    } else if (AnnotationUtils.areSameIgnoringValues(higher,
                            INTVAL)) {
                        for (Number n : valuesToCast) {
                            newValues.add(new Integer(n.intValue()));
                        }
                        anno += "IntVal";
                    } else if (AnnotationUtils.areSameIgnoringValues(higher,
                            LONGVAL)) {
                        for (Number n : valuesToCast) {
                            newValues.add(new Long(n.longValue()));
                        }
                        anno += "LongVal";
                    } else if (AnnotationUtils.areSameIgnoringValues(higher,
                            FLOATVAL)) {
                        for (Number n : valuesToCast) {
                            newValues.add(new Float(n.floatValue()));
                        }
                        anno += "FloatVal";
                    } else if (AnnotationUtils.areSameIgnoringValues(higher,
                            SHORTVAL)) {
                        for (Number n : valuesToCast) {
                            newValues.add(new Short(n.shortValue()));
                        }
                        anno += "ShortVal";
                    } else if (AnnotationUtils.areSameIgnoringValues(higher,
                            BYTEVAL)) {
                        for (Number n : valuesToCast) {
                            newValues.add(new Byte(n.byteValue()));
                        }
                        anno += "ByteVal";
                    }

                    return createAnnotation(anno, newValues);
                }
            }
        }

        private AnnotationMirror[] getHighestAnnotation(AnnotationMirror a1,
                AnnotationMirror a2) {
            AnnotationMirror[] higherFirst = new AnnotationMirror[2];
            for (AnnotationMirror m : orderedNumberAnnotations) {
                if (AnnotationUtils.areSameIgnoringValues(a1, m)) {
                    higherFirst[0] = a1;
                    higherFirst[1] = a2;
                    return higherFirst;
                } else if (AnnotationUtils.areSameIgnoringValues(a2, m)) {
                    higherFirst[0] = a2;
                    higherFirst[1] = a1;
                    return higherFirst;
                }
            }

            // No number-type annotation was found
            return null;
        }

        /**
         * Computes subtyping as per the subtyping in the qualifier hierarchy
         * structure unless both annotations are Value. In this case, rhs is a
         * subtype of lhs iff lhs contains at least every element of rhs
         * 
         * @param rhs
         * @param lhs
         * 
         * @return true if rhs is a subtype of lhs, false otherwise
         */
        @Override
        public boolean isSubtype(AnnotationMirror rhs, AnnotationMirror lhs) {
            if (System.currentTimeMillis() > t + 1000) {
                t = System.currentTimeMillis();
            }

            // Same types and value so subtype
            if (AnnotationUtils.areSame(rhs, lhs)) {
                return true;
            }
            // Same type, so might be subtype
            else if (AnnotationUtils.areSameIgnoringValues(lhs, rhs)) {
                List<Object> lhsValues = AnnotationUtils.getElementValueArray(
                        lhs, "value", Object.class, true);
                List<Object> rhsValues = AnnotationUtils.getElementValueArray(
                        rhs, "value", Object.class, true);
                return lhsValues.containsAll(rhsValues);
            }
            // Not the same type but if they are chars, doubles, or
            // ints they might be compatible due to implicit casting

            AnnotationMirror[] sorted = getHighestAnnotation(lhs, rhs);
            // We may be able to implicitly cast up, if both of the annotations
            // are number subtypes or chars, and if lhs is the higher of the two
            if (sorted != null && sorted[0] == lhs
                    && isNumberAnnotation(sorted[1])) {
                List<Number> rhsValues;
                if (AnnotationUtils.areSameIgnoringValues(rhs, CHARVAL)) {
                    List<Character> charVals = AnnotationUtils
                            .getElementValueArray(rhs, "value",
                                    Character.class, true);

                    rhsValues = new ArrayList<Number>(charVals.size());
                    for (Character c : charVals) {
                        rhsValues.add(new Integer(c.charValue()));
                    }
                } else {
                    rhsValues = AnnotationUtils.getElementValueArray(rhs,
                            "value", Number.class, true);
                }

                List<Number> lhsValues = AnnotationUtils.getElementValueArray(
                        lhs, "value", Number.class, true);

                boolean same = false;
                for (Number rhsN : rhsValues) {
                    for (Number lhsN : lhsValues) {
                        if (lhsN.doubleValue() == rhsN.doubleValue()) {
                            same = true;
                            break;
                        }
                    }
                    if (!same) {
                        return false;
                    }
                }
                return same;
            }

            // fallback to type-heirarchy since
            // values don't matter
            for (AnnotationMirror anno : constantAnnotations) {
                if (AnnotationUtils.areSameIgnoringValues(lhs, anno)) {
                    lhs = anno;
                }
                if (AnnotationUtils.areSameIgnoringValues(rhs, anno)) {
                    rhs = anno;
                }
            }

            return super.isSubtype(rhs, lhs);
        }

    }

    /**
     * Create the type annotator for this AnnotatedTypeFactory
     * 
     * @param checker
     * 
     * @return
     */
    @Override
    protected TypeAnnotator createTypeAnnotator() {
        return new ValueTypeAnnotator();
    }

    /**
     * Create the tree annotator for this AnnotatedTypeFactory, which contains
     * the customized visit methods
     * 
     * @param checker
     * 
     * @return
     */
    @Override
    protected TreeAnnotator createTreeAnnotator() {
        return new ValueTreeAnnotator(this);
    }

    /**
     * Just a BaseTypeChecker
     * 
     * @param checker
     * 
     */
    protected class ValueTypeAnnotator extends TypeAnnotator {
        public ValueTypeAnnotator() {
            super(ValueAnnotatedTypeFactory.this);
        }
    }

    /**
     * 
     * @param checker
     * @param factory
     * 
     * @return
     */
    protected class ValueTreeAnnotator extends TreeAnnotator {

        public ValueTreeAnnotator(ValueAnnotatedTypeFactory factory) {
            super(factory);
        }

        @Override
        public Void visitNewArray(NewArrayTree tree, AnnotatedTypeMirror type) {

            TreePath path = getPath(tree);

            if (path.getLeaf().getKind() != Tree.Kind.CLASS) {
                List<? extends ExpressionTree> dimensions = tree
                        .getDimensions();
                List<? extends ExpressionTree> initializers = tree
                        .getInitializers();

                // Dimensions provided
                if (!dimensions.isEmpty()) {
                    handleDimensions(dimensions, (AnnotatedArrayType) type);
                } else {
                    // Initializer used

                    int length = initializers.size();
                    HashSet<Integer> value = new HashSet<Integer>();
                    // If length is 0, so there is no initializer, and the
                    // kind of getType is not an ARRAY_TYPE, so the array is
                    // single-dimensional
                    if (length == 0)
                    // && tree.getType().getKind() != Tree.Kind.ARRAY_TYPE) -
                    // this caused an error on annotation with empty arrays
                    {
                        value.add(length);
                    }

                    // Check to ensure single-dimensionality by checking if
                    // the first initializer element is a list; either all
                    // elements must be lists or none so we only need to check
                    // the first
                    else if (length > 0
                            && !initializers.get(0).getClass()
                                    .equals(List.class)) {
                        value.add(length);
                    }

                    AnnotationMirror newQual;
                    String typeString = type.getUnderlyingType().toString();
                    if (typeString.equals("byte[]")
                            || typeString.equals("char[]")) {

                        boolean allLiterals = true;
                        char[] chars = new char[initializers.size()];
                        for (int i = 0; i < chars.length && allLiterals; i++) {
                            ExpressionTree e = initializers.get(i);
                            if (e.getKind() == Tree.Kind.INT_LITERAL) {
                                chars[i] = (char) (((Integer) ((LiteralTree) initializers
                                        .get(i)).getValue()).intValue());
                            } else {
                                allLiterals = false;
                            }
                        }

                        if (allLiterals) {
                            HashSet<String> stringFromChars = new HashSet<String>(
                                    1);
                            stringFromChars.add(new String(chars));
                            newQual = createAnnotation(
                                    "checkers.value.quals.StringVal",
                                    stringFromChars);
                            type.replaceAnnotation(newQual);
                            return null;
                        }
                    }

                    newQual = createAnnotation("checkers.value.quals.ArrayLen",
                            value);
                    type.replaceAnnotation(newQual);

                }
            }
            return super.visitNewArray(tree, type);
        }

        /**
         * Do one level, pop from list, recurse if not done
         * 
         * @param dimensions
         * @param type
         */
        /**
         * Recursive method to handle array initializations. Recursively
         * descends the initializer to find each dimension's size and create the
         * appropriate annotation for it.
         * 
         * @param dimensions
         *            a list of ExpressionTrees where each ExpressionTree is a
         *            specifier of the size of that dimension (should be an
         *            IntVal).
         * @param type
         *            the AnnotatedTypeMirror of the array
         */
        private void handleDimensions(
                List<? extends ExpressionTree> dimensions,
                AnnotatedArrayType type) {
            if (dimensions.size() > 1) {
                handleDimensions(dimensions.subList(1, dimensions.size()),
                        (AnnotatedArrayType) type.getComponentType());
            }

            AnnotationMirror dimType = getAnnotatedType(dimensions.get(0))
                    .getAnnotationInHierarchy(INTVAL);
            if (AnnotationUtils.areSameIgnoringValues(dimType, INTVAL)) {
                HashSet<Integer> lengths = new HashSet<Integer>(
                        AnnotationUtils.getElementValueArray(dimType, "value",
                                Integer.class, true));

                AnnotationMirror newQual = createAnnotation(
                        "checkers.value.quals.ArrayLen", lengths);
                type.replaceAnnotation(newQual);
            } else {
                type.replaceAnnotation(UNKNOWNVAL);
            }
        }

        @Override
        public Void visitTypeCast(TypeCastTree tree, AnnotatedTypeMirror type) {
            if (isClassCovered(type)) {
                String castedToString = type.toString();
                handleCast(tree.getExpression(), castedToString, type);
            }
            return super.visitTypeCast(tree, type);
        }

        @Override
        public Void visitAssignment(AssignmentTree tree,
                AnnotatedTypeMirror type) {
            super.visitAssignment(tree, type);
            return null;

        }

        @Override
        public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
            if (isClassCovered(type)) {
                // Handle Boolean Literal
                if (tree.getKind() == Tree.Kind.BOOLEAN_LITERAL) {
                    HashSet<Boolean> values = new HashSet<Boolean>();
                    values.add((Boolean) tree.getValue());
                    AnnotationMirror newQual = createAnnotation(
                            "checkers.value.quals.BoolVal", values);
                    type.replaceAnnotation(newQual);

                    return null;
                }

                // Handle Char Literal
                else if (tree.getKind() == Tree.Kind.CHAR_LITERAL) {
                    HashSet<Character> values = new HashSet<Character>();
                    values.add((Character) tree.getValue());
                    AnnotationMirror newQual = createAnnotation(
                            "checkers.value.quals.CharVal", values);
                    type.replaceAnnotation(newQual);

                    return null;
                }

                // Handle Double Literal
                else if (tree.getKind() == Tree.Kind.DOUBLE_LITERAL) {
                    HashSet<Double> values = new HashSet<Double>();
                    values.add((Double) tree.getValue());
                    AnnotationMirror newQual = createAnnotation(
                            "checkers.value.quals.DoubleVal", values);
                    type.replaceAnnotation(newQual);

                    return null;
                }
                // Handle Float Literal
                else if (tree.getKind() == Tree.Kind.FLOAT_LITERAL) {
                    HashSet<Float> values = new HashSet<Float>();
                    values.add((Float) tree.getValue());
                    AnnotationMirror newQual = createAnnotation(
                            "checkers.value.quals.FloatVal", values);
                    type.replaceAnnotation(newQual);

                    return null;
                }

                // Handle Integer Literal
                else if (tree.getKind() == Tree.Kind.INT_LITERAL) {
                    AnnotationMirror newQual;
                    // if (((Integer)tree.getValue()) >= 0 &&
                    // ((Integer)tree.getValue() <= Character.MAX_VALUE)){
                    // HashSet<Character> values = new HashSet<Character>();
                    // values.add((char)((Integer) tree.getValue()).intValue());
                    // newQual =
                    // createAnnotation("checkers.value.quals.CharVal", values);
                    // }else{
                    HashSet<Integer> values = new HashSet<Integer>();
                    values.add((Integer) tree.getValue());
                    newQual = createAnnotation("checkers.value.quals.IntVal",
                            values);
                    // }
                    type.replaceAnnotation(newQual);
                    return null;
                }
                // Handle Long Literal
                else if (tree.getKind() == Tree.Kind.LONG_LITERAL) {
                    HashSet<Long> values = new HashSet<Long>();
                    values.add((Long) tree.getValue());
                    AnnotationMirror newQual = createAnnotation(
                            "checkers.value.quals.LongVal", values);
                    type.replaceAnnotation(newQual);

                    return null;
                }

                // Handle a String Literal
                else if (tree.getKind() == Tree.Kind.STRING_LITERAL) {
                    HashSet<String> values = new HashSet<String>();
                    values.add((String) tree.getValue());
                    AnnotationMirror newQual = createAnnotation(
                            "checkers.value.quals.StringVal", values);
                    type.replaceAnnotation(newQual);

                    return null;
                }
            }
            // super.visitLiteral(tree, type);
            return null;
        }

        /**
         * NOTE: Because of the way CFGBuilder handles increment and decrement,
         * the value of any variable with being incremented or decrement will be
         * at least @IntVal (or higher if original type was higher). Thus, there
         * will be an error if you try to assign an incremented value to a
         * CharVal, ByteVal, ShortVal, or FloatVal, even if that is what the
         * incremented value originally was.
         * 
         * @param tree
         * @param type
         */
        @Override
        public Void visitUnary(UnaryTree tree, AnnotatedTypeMirror type) {
            super.visitUnary(tree, type);

            if (isClassCovered(type)) {
                Tree.Kind operation = tree.getKind();
                String finalTypeString = type.getUnderlyingType().toString();
                AnnotatedTypeMirror argType = getAnnotatedType(tree
                        .getExpression());

                if (!nonValueAnno(argType)) {
                    Class<?> argClass = getTypeValueClass(finalTypeString, tree);
                    handleCast(tree.getExpression(), finalTypeString, argType);

                    AnnotationMirror argAnno = argType
                            .getAnnotationInHierarchy(UNKNOWNVAL);
                    AnnotationMirror newAnno = evaluateUnaryOperator(argAnno,
                            operation.toString(), argClass, tree);
                    if (newAnno != null) {
                        type.replaceAnnotation(newAnno);
                        return null;
                    }
                }

                type.replaceAnnotation(UNKNOWNVAL);
            }
            return null;
        }

        private AnnotationMirror evaluateUnaryOperator(
                AnnotationMirror argAnno, String operation, Class<?> argClass,
                UnaryTree tree) {
            try {
                Class<?>[] argClasses = new Class<?>[] { argClass };
                Method m = Operators.class.getMethod(operation, argClasses);

                List<?> annoValues = AnnotationUtils.getElementValueArray(
                        argAnno, "value", argClass, true);
                ArrayList<Object> results = new ArrayList<Object>(
                        annoValues.size());

                for (Object val : annoValues) {
                    results.add(m.invoke(null, new Object[] { val }));
                }
                return resultAnnotationHandler(m.getReturnType(), results);
            } catch (ReflectiveOperationException e) {
                checker.report(Result
                        .warning("operator.unary.evaluation.failed", operation,
                                argClass), tree);
                return null;
            }
        }

        @Override
        public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
            super.visitBinary(tree, type);
            if (isClassCovered(type)) {
                Tree.Kind operation = tree.getKind();
                String finalTypeString = type.getUnderlyingType().toString();

                AnnotatedTypeMirror lhsType = getAnnotatedType(tree
                        .getLeftOperand());
                AnnotatedTypeMirror rhsType = getAnnotatedType(tree
                        .getRightOperand());
                if (!nonValueAnno(lhsType) && !nonValueAnno(rhsType)) {

                    Class<?> argClass = null;

                    // Non-Comparison Binary Operation
                    if (operation != Tree.Kind.EQUAL_TO
                            && operation != Tree.Kind.NOT_EQUAL_TO
                            && operation != Tree.Kind.GREATER_THAN
                            && operation != Tree.Kind.GREATER_THAN_EQUAL
                            && operation != Tree.Kind.LESS_THAN
                            && operation != Tree.Kind.LESS_THAN_EQUAL) {
                        argClass = getTypeValueClass(finalTypeString, tree);
                        handleBinaryCast(tree.getLeftOperand(), lhsType,
                                tree.getRightOperand(), rhsType,
                                finalTypeString);
                    }
                    // Comparison Binary Operation We're okay to cast
                    // everything to DoubleVal *UNLESS* we're
                    // comparing StringsVals, so we do This
                    // potentially means we could remove the
                    // non-double versions of comparisons in
                    // Operators.java
                    else {

                        if (AnnotationUtils.areSameIgnoringValues(
                                lhsType.getAnnotationInHierarchy(UNKNOWNVAL),
                                STRINGVAL)) {
                            argClass = getAnnotationValueClass(lhsType
                                    .getAnnotationInHierarchy(UNKNOWNVAL));
                        } else {
                            argClass = getTypeValueClass("double", tree);

                            handleBinaryCast(tree.getLeftOperand(), lhsType,
                                    tree.getRightOperand(), rhsType, "double");
                        }
                    }
                    AnnotationMirror lhsAnno = lhsType
                            .getAnnotationInHierarchy(UNKNOWNVAL);
                    AnnotationMirror rhsAnno = rhsType
                            .getAnnotationInHierarchy(UNKNOWNVAL);

                    AnnotationMirror newAnno = evaluateBinaryOperator(lhsAnno,
                            rhsAnno, operation.toString(), argClass, tree);

                    if (newAnno != null) {
                        type.replaceAnnotation(newAnno);

                        return null;
                    }
                }

                type.replaceAnnotation(UNKNOWNVAL);
            }
            return null;
        }

        /**
         * Casts the two arguments of a binary operator to the final type of
         * that operator. i.e. double + int -> double so DoubleVal + IntVal ->
         * DoubleVal
         * 
         * @param lhs
         * @param lhsType
         * @param rhs
         * @param rhsType
         * @param finalTypeString
         */
        private void handleBinaryCast(ExpressionTree lhs,
                AnnotatedTypeMirror lhsType, ExpressionTree rhs,
                AnnotatedTypeMirror rhsType, String finalTypeString) {
            handleCast(lhs, finalTypeString, lhsType);
            handleCast(rhs, finalTypeString, rhsType);
        }

        /**
         * This method resolves a binary operator by converting it to a
         * reflective call to one of the operators defined in
         * BinaryOperators.java. This method's arguments need to be correct
         * (such as annotations not being UnknownVal or being of different value
         * annotation types) so be careful you are going to call this.
         * 
         * @param lhsAnno
         *            the value annotation of the LHS argument (Not UnknownVal)
         * @param rhsAnno
         *            the value annotation of the RHS argument (Not UnknownVal)
         * @param operation
         *            the String name of the operation
         * @param argClass
         *            the Class of the operations arguments (used for reflective
         *            code)
         * 
         * @return
         */
        private AnnotationMirror evaluateBinaryOperator(
                AnnotationMirror lhsAnno, AnnotationMirror rhsAnno,
                String operation, Class<?> argClass, BinaryTree tree) {
            try {
                Class<?>[] argClasses = new Class<?>[] { argClass, argClass };
                Method m = Operators.class.getMethod(operation, argClasses);

                List<?> lhsAnnoValues = AnnotationUtils.getElementValueArray(
                        lhsAnno, "value", argClass, true);
                List<?> rhsAnnoValues = AnnotationUtils.getElementValueArray(
                        rhsAnno, "value", argClass, true);
                ArrayList<Object> results = new ArrayList<Object>(
                        lhsAnnoValues.size() * rhsAnnoValues.size());

                for (Object lhsO : lhsAnnoValues) {
                    for (Object rhsO : rhsAnnoValues) {
                        results.add(m.invoke(null, new Object[] { lhsO, rhsO }));
                    }
                }
                return resultAnnotationHandler(m.getReturnType(), results);
            } catch (ReflectiveOperationException e) {
                checker.report(Result.warning(
                        "operator.binary.evaluation.failed", operation,
                        argClass), tree);
                return null;
            }
        }

        /**
         * Simple method to take a MemberSelectTree representing a method call
         * and determine if the method's return is annotated with @Analyzable.
         * 
         * @param method
         * 
         * @return
         */
        private boolean methodIsAnalyzable(Element method) {
            return getDeclAnnotation(method, Analyzable.class) != null;
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree,
                AnnotatedTypeMirror type) {
            super.visitMethodInvocation(tree, type);

            if (isClassCovered(type)
                    && methodIsAnalyzable(TreeUtils.elementFromUse(tree))) {
                ExpressionTree methodTree = tree.getMethodSelect();

                // First, check that all argument values are known
                List<? extends Tree> argTrees = tree.getArguments();
                List<AnnotatedTypeMirror> argTypes = new ArrayList<AnnotatedTypeMirror>(
                        argTrees.size());
                for (Tree t : argTrees) {
                    argTypes.add(getAnnotatedType(t));
                }

                boolean known = true;
                for (AnnotatedTypeMirror t : argTypes) {
                    if (nonValueAnno(t)) {
                        known = false;
                    }
                }

                if (known) {

                    Class<?>[] argClasses = getClassList(argTypes, tree);

                    boolean isStatic = false;
                    AnnotatedTypeMirror recType = null;
                    Method method = null;
                    Class<?> recClass;

                    try {
                        if (tree.getMethodSelect().getKind() == Tree.Kind.IDENTIFIER) {
                            // Method is defined within the same class
                            recType = getSelfType(tree);

                            do {
                                recClass = Class.forName(recType
                                        .getUnderlyingType().toString());
                                try {
                                    method = recClass.getMethod(
                                            ((IdentifierTree) methodTree)
                                                    .getName().toString(),
                                            argClasses);
                                } catch (NoSuchMethodException e) {
                                    checker.report(Result.warning(
                                            "method.find.failed",
                                            ((IdentifierTree) methodTree)
                                                    .getName(), argTypes,
                                            recClass), tree);
                                }
                                recType = ((AnnotatedDeclaredType) recType)
                                        .getEnclosingType();
                            } while (method == null && recType != null);

                            if (method != null) {
                                isStatic = Modifier.isStatic(method
                                        .getModifiers());
                            } else {
                                type.replaceAnnotation(UNKNOWNVAL);
                                return null;
                            }
                        } else if (tree.getMethodSelect().getKind() == Tree.Kind.MEMBER_SELECT) {
                            // Method is defined in another class
                            recType = getAnnotatedType(((MemberSelectTree) methodTree)
                                    .getExpression());
                            recClass = Class.forName(recType
                                    .getUnderlyingType().toString());
                            method = recClass.getMethod(
                                    ((MemberSelectTree) methodTree)
                                            .getIdentifier().toString(),
                                    argClasses);
                            isStatic = Modifier.isStatic(method.getModifiers());
                        }

                        // Check if this is a method that can be evaluated

                        // Second, check that the receiver class and method can
                        // be reflectively instantiated, and that the method is
                        // static or the receiver is not UnknownVal

                        // Method is evaluatable because all arguments are known
                        // and the method is static or the receiver is known
                        if (isStatic || !nonValueAnno(recType)) {

                            if (!method.isAccessible()) {
                                method.setAccessible(true);
                            }
                            AnnotationMirror newAnno = evaluateMethod(recType,
                                    method, argTypes, type, tree);
                            if (newAnno != null) {
                                type.replaceAnnotation(newAnno);
                                return null;
                            }
                        }
                    } catch (ClassNotFoundException e) {
                        checker.report(
                                Result.warning("class.find.failed", recType),
                                tree);
                    } catch (NoSuchMethodException e) {
                        checker.report(Result
                                .warning("method.find.failed",
                                        ((MemberSelectTree) methodTree)
                                                .getIdentifier(), argTypes,
                                        recType), tree);
                    }
                }
            }
            // Method was not able to be analyzed
            type.replaceAnnotation(UNKNOWNVAL);

            return null;
        }

        /**
         * Evaluates the possible results of a method and returns an annotation
         * containing those results.
         * 
         * @param recType
         *            the AnnotatedTypeMirror of the receiver
         * @param method
         *            the method to evaluate
         * @param argTypes
         *            the List of AnnotatedTypeMirror for the arguments from
         *            which possible argument values are extracted
         * @param retType
         *            the AnnotatedTypeMirror of the tree being evaluated, used
         *            to determine the type of AnnotationMirr to return
         * 
         * @return an AnnotationMirror of the type specified by retType's
         *         underlyingType and with its value array populated by all the
         *         possible evaluations of method. Or UnknownVal
         */
        private AnnotationMirror evaluateMethod(AnnotatedTypeMirror recType,
                Method method, List<AnnotatedTypeMirror> argTypes,
                AnnotatedTypeMirror retType, MethodInvocationTree tree) {
            List<Object> recValues = null;
            // If we are going to need the values of the receiver, get them.
            // Otherwise they can be null because the method is static
            if (!Modifier.isStatic(method.getModifiers())) {
                recValues = getCastedValues(recType, tree);
            }

            // Get the values for all the arguments
            ArrayDeque<List<Object>> allArgValues = getAllArgumentAnnotationValues(
                    argTypes, tree);
            // Evaluate the method by recursively navigating through all the
            // argument value lists, adding all results to the results list.
            ArrayDeque<Object> specificArgValues = new ArrayDeque<Object>();
            ArrayList<Object> results = new ArrayList<Object>();

            evaluateMethodHelper(allArgValues, specificArgValues, recValues,
                    method, results, tree);
            return resultAnnotationHandler(retType, results, tree);
        }

        /**
         * Recursive helper function for evaluateMethod. Recurses through each
         * List of Object representing possible argument values. At each
         * recursion, all possible values for an argument are incremented
         * through and the method is invoked on each one and the result is added
         * to the results list.
         * 
         * @param argArrayDeque
         *            ArrayDeque of Lists of Objects representing possible
         *            values for each argument
         * @param values
         *            ArrayDeque of Objects containing the argument values for a
         *            specific invocation of method. This is the structure that
         *            is modified at each recursion to add a different argument
         *            value.
         * @param receiverValues
         *            a List of Object representing all the possible values of
         *            the receiver
         * @param method
         *            the method to invoke
         * @param results
         *            a List of all values returned. Once all calls are finished
         *            this will contain the results for all possible
         *            combinations of argument and receiver values invoking the
         *            method
         */
        private void evaluateMethodHelper(
                ArrayDeque<List<Object>> argArrayDeque,
                ArrayDeque<Object> values, List<Object> receiverValues,
                Method method, List<Object> results, MethodInvocationTree tree) {
            // If we have descended through all the argument value lists
            if (argArrayDeque.size() == 0) {
                try {
                    // If the receiver has values (the method is not static)
                    if (receiverValues != null) {

                        // Iterate through all the receiver's values
                        for (Object o : receiverValues) {

                            // If there were argument values, invoke with them
                            if (values.size() > 0) {
                                results.add(method.invoke(o, values.toArray()));
                            }
                            // Otherwise, invoke without them (the method took
                            // no arguments)
                            else {
                                results.add(method.invoke(o));
                            }
                        }
                    }
                    // If this is a static method, the receiver values do not
                    // exist/do not matter
                    else {
                        // If there were arguments, invoke with them
                        if (values.size() > 0) {
                            results.add(method.invoke(null, values.toArray()));
                        }
                        // Otherwise, invoke without them
                        else {
                            results.add(method.invoke(null));
                        }
                    }
                } catch (InvocationTargetException e) {
                    checker.report(Result.warning("method.evaluated.exception",
                            method, e.getTargetException().toString()), tree);
                    results = new ArrayList<Object>();
                } catch (ReflectiveOperationException e) {
                    checker.report(
                            Result.warning("method.evaluation.failed", method),
                            tree);
                    results = new ArrayList<Object>(); // fail by setting the
                    // results list to
                    // empty. Since we
                    // failed on the invoke,
                    // all calls of this
                    // method will fail, so
                    // the final results
                    // list will be an empty
                    // list. That will cause
                    // an UnknownVal
                    // annotation to be
                    // created, which seems
                    // appropriate here
                }
            }

            // If there are still lists of argument values left in the deque
            else {

                // Pop an argument off and iterate through all its values
                List<Object> argValues = argArrayDeque.pop();
                for (Object o : argValues) {

                    // Push one of the arg's values on and evaluate, then pop
                    // and do the next
                    values.push(o);
                    evaluateMethodHelper(argArrayDeque, values, receiverValues,
                            method, results, tree);
                    values.pop();
                }
            }
        }

        @Override
        public Void visitNewClass(NewClassTree tree, AnnotatedTypeMirror type) {

            super.visitNewClass(tree, type);

            if (isClassCovered(type)) {

                // First, make sure all the args are known
                List<? extends ExpressionTree> argTrees = tree.getArguments();
                ArrayList<AnnotatedTypeMirror> argTypes = new ArrayList<AnnotatedTypeMirror>(
                        argTrees.size());
                for (ExpressionTree e : argTrees) {
                    argTypes.add(getAnnotatedType(e));
                }
                boolean known = true;
                for (AnnotatedTypeMirror t : argTypes) {
                    if (nonValueAnno(t)) {
                        known = false;
                        break;
                    }
                }

                // If all the args are known we can evaluate
                if (known) {
                    try {
                        // get the constructor
                        Class<?>[] argClasses = getClassList(argTypes, tree);
                        Class<?> recClass = Class.forName(type
                                .getUnderlyingType().toString());
                        Constructor<?> constructor = recClass
                                .getConstructor(argClasses);

                        AnnotationMirror newAnno = evaluateNewClass(
                                constructor, argTypes, type, tree);
                        if (newAnno != null) {
                            type.replaceAnnotation(newAnno);
                            return null;
                        }
                    } catch (ReflectiveOperationException e) {
                        checker.report(Result.warning(
                                "constructor.evaluation.failed",
                                type.getUnderlyingType(), argTypes), tree);
                    }
                }
                type.replaceAnnotation(UNKNOWNVAL);

            }
            return null;
        }

        /**
         * Attempts to evaluate a New call by retrieving the constructor and
         * invoking it reflectively.
         * 
         * @param constructor
         *            the constructor to invoke
         * @param argTypes
         *            List of AnnnotatedTypeMirror of the arguments
         * @param retType
         *            AnnotatedTypeMirror of the tree being evaluate, used to
         *            determine what the return AnnotationMirror should be
         * 
         * @return an AnnotationMirror containing all the possible values of the
         *         New call based on combinations of argument values
         */
        private AnnotationMirror evaluateNewClass(Constructor<?> constructor,
                List<AnnotatedTypeMirror> argTypes,
                AnnotatedTypeMirror retType, NewClassTree tree) {
            ArrayDeque<List<Object>> allArgValues = getAllArgumentAnnotationValues(
                    argTypes, tree);

            ArrayDeque<Object> specificArgValues = new ArrayDeque<Object>();
            ArrayList<Object> results = new ArrayList<Object>();
            evaluateNewClassHelper(allArgValues, specificArgValues,
                    constructor, results, tree);

            return resultAnnotationHandler(retType, results, tree);
        }

        /**
         * Recurses through all the possible argument values and invokes the
         * constructor on each one, adding the result to the results list
         * 
         * @param argArrayDeque
         *            ArrayDeque of List of Object containing all the argument
         *            values
         * @param values
         *            ArrayDeque of Objects containing the argument values for a
         *            specific invocation of method. This is the structure that
         *            is modified at each recursion to add a different argument
         *            value.
         * @param constructor
         *            the constructor to invoke
         * @param results
         *            a List of all values returned. Once all calls are finished
         *            this will contain the results for all possible
         *            combinations of argument values invoking the constructor
         */
        private void evaluateNewClassHelper(
                ArrayDeque<List<Object>> argArrayDeque,
                ArrayDeque<Object> values, Constructor<?> constructor,
                List<Object> results, NewClassTree tree) {
            // If we have descended through all argument value lists
            if (argArrayDeque.size() == 0) {
                try {
                    // If there are argument values (not an empty constructor)
                    if (values.size() > 0) {
                        results.add(constructor.newInstance(values.toArray()));
                    }
                    // If there are no argument values (empty constructor)
                    else {
                        results.add(constructor.newInstance());
                    }
                } catch (ReflectiveOperationException e) {
                    checker.report(
                            Result.warning("constructor.invocation.failed"),
                            tree);
                    results = new ArrayList<Object>();// fail by setting the
                    // results list to
                    // empty. Since we
                    // failed on the
                    // newInstance, all
                    // calls of this
                    // constructor will
                    // fail, so the final
                    // results list will be
                    // an empty list. That
                    // will cause an
                    // UnknownVal annotation
                    // to be created, which
                    // seems appropriate
                    // here
                }
            }
            // If there are still lists of argument values left in the deque
            else {

                // Pop an argument off and iterate through all its values
                List<Object> argValues = argArrayDeque.pop();
                for (Object o : argValues) {

                    // Push one of the arg's values on and evaluate, then pop
                    // and do the next
                    values.push(o);
                    evaluateNewClassHelper(argArrayDeque, values, constructor,
                            results, tree);
                    values.pop();
                }
            }
        }

        @Override
        public Void visitMemberSelect(MemberSelectTree tree,
                AnnotatedTypeMirror type) {
            // NOTE: None of the objects, except arrays, being handled by this
            // system possess non-static fields, so I am assuming I can simply
            // reflectively call the fields on a reflectively generated object
            // representing the class
            super.visitMemberSelect(tree, type);
            AnnotatedTypeMirror receiverType = getAnnotatedType(tree
                    .getExpression());

            Element elem = TreeUtils.elementFromUse(tree);
            // KNOWN-LENGTH ARRAYS
            if (AnnotationUtils
                    .areSameIgnoringValues(
                            receiverType.getAnnotationInHierarchy(UNKNOWNVAL),
                            ARRAYLEN)) {
                if (tree.getIdentifier().contentEquals("length")) {
                    type.replaceAnnotation(handleArrayLength(receiverType));
                }
            } else if (methodIsAnalyzable(elem)
                    && elem.getKind() == javax.lang.model.element.ElementKind.FIELD) {
                TypeMirror retType = elem.asType();
                AnnotationMirror newAnno = evaluateStaticFieldAccess(
                        tree.getIdentifier(),
                        getAnnotatedType(tree.getExpression()), retType, tree);

                if (newAnno != null) {
                    type.replaceAnnotation(newAnno);
                } else {
                    type.replaceAnnotation(UNKNOWNVAL);
                }

            }

            return null;
        }

        /**
         * If the receiverType object has an ArrayLen annotation it returns an
         * IntVal with all the ArrayLen annotation's values as its possible
         * values.
         * 
         * @param receiverType
         */
        private AnnotationMirror handleArrayLength(
                AnnotatedTypeMirror receiverType) {
            AnnotationMirror recAnno = receiverType
                    .getAnnotationInHierarchy(UNKNOWNVAL);

            if (AnnotationUtils.areSameIgnoringValues(recAnno, ARRAYLEN)) {
                HashSet<Integer> lengthValues = new HashSet<Integer>(
                        AnnotationUtils.getElementValueArray(recAnno, "value",
                                Integer.class, true));

                return createAnnotation("checkers.value.quals.IntVal",
                        lengthValues);
            } else {
                return UNKNOWNVAL;
            }
        }

        /**
         * Evalautes a static field access by getting the field reflectively
         * from the field name and class name
         * 
         * @param fieldName
         *            the field to be access
         * @param recType
         *            the AnnotatedTypeMirror of the tree being evaluated, used
         *            to create the return annotation and to reflectively create
         *            the Class object to get the field from
         * 
         * @return
         */
        private AnnotationMirror evaluateStaticFieldAccess(Name fieldName,
                AnnotatedTypeMirror recType, TypeMirror retType,
                MemberSelectTree tree) {
            try {
                Class<?> recClass = Class.forName(recType.getUnderlyingType()
                        .toString());
                Field field = recClass.getField(fieldName.toString());

                ArrayList<Object> result = new ArrayList<Object>(1);
                result.add(field.get(recClass));

                return resultAnnotationHandler(retType, result, tree);
            } catch (ReflectiveOperationException e) {
                checker.report(Result.warning("field.access.failed", fieldName,
                        recType.getUnderlyingType()), tree);
                return null;
            }
        }

        private boolean isClassCovered(AnnotatedTypeMirror type) {
            return coveredClassStrings.contains(type.getUnderlyingType()
                    .toString());
        }

        /**
         * Gets a Class object corresponding to the String name stringType. If
         * stringType specifies a primitive or wrapper object, the primitive
         * version is returned ("int" or "java.lang.Integer" return int.class)
         * To get the Class corresponding to the value array a value annotation
         * has for a given type, use getTypeValueClass. (e.g. "int" return
         * Long.class)
         * 
         * @param stringType
         * 
         * @return
         */
        private Class<?> getClass(String stringType, Tree tree) {
            switch (stringType) {
            case "int":
            case "java.lang.Integer":
                return int.class;
            case "long":
            case "java.lang.Long":
                return long.class;
            case "short":
            case "java.lang.Short":
                return short.class;
            case "byte":
            case "java.lang.Byte":
                return byte.class;
            case "char":
            case "java.lang.Character":
                return char.class;
            case "double":
            case "java.lang.Double":
                return double.class;
            case "float":
            case "java.lang.Float":
                return float.class;
            case "boolean":
            case "java.lang.Boolean":
                return boolean.class;
            case "byte[]":
                return byte[].class;
            }

            try {
                return Class.forName(stringType);
            } catch (ClassNotFoundException e) {
                checker.report(Result.failure("class.find.failed", stringType),
                        tree);
                return Object.class;
            }

        }

        private Class<?> getAnnotationValueClass(AnnotationMirror anno) {
            if (AnnotationUtils.areSameIgnoringValues(anno, CHARVAL)) {
                return Character.class;
            } else if (AnnotationUtils.areSameIgnoringValues(anno, INTVAL)) {
                return Integer.class;
            } else if (AnnotationUtils.areSameIgnoringValues(anno, LONGVAL)) {
                return Long.class;
            } else if (AnnotationUtils.areSameIgnoringValues(anno, SHORTVAL)) {
                return Short.class;
            } else if (AnnotationUtils.areSameIgnoringValues(anno, BYTEVAL)) {
                return Byte.class;
            } else if (AnnotationUtils.areSameIgnoringValues(anno, FLOATVAL)) {
                return Float.class;
            } else if (AnnotationUtils.areSameIgnoringValues(anno, DOUBLEVAL)) {
                return Double.class;
            } else if (AnnotationUtils.areSameIgnoringValues(anno, BOOLVAL)) {
                return Boolean.class;
            } else if (AnnotationUtils.areSameIgnoringValues(anno, STRINGVAL)) {
                return String.class;
            }

            return null;

        }

        /**
         * Gets the Class corresponding to the objects stored in a value
         * annotations values array for an annotation on a variable of type
         * stringType. So "int" or "java.lang.Integer" return Long.class because
         * IntVal stores its values as a List<Long>. To get the primitive Class
         * corresponding to a string name, use getClass (e.g. "int" and
         * "java.lang.Integer" give int.class)
         * 
         * @param stringType
         * 
         * @return
         */
        private Class<?> getTypeValueClass(String stringType, Tree tree) {
            switch (stringType) {
            case "int":
            case "java.lang.Integer":
                return Integer.class;
            case "long":
            case "java.lang.Long":
                return Long.class;
            case "short":
            case "java.lang.Short":
                return Short.class;
            case "byte":
            case "java.lang.Byte":
                return Byte.class;
            case "char":
            case "java.lang.Character":
                return Character.class;
            case "double":
            case "java.lang.Double":
                return Double.class;
            case "float":
            case "java.lang.Float":
                return Float.class;
            case "boolean":
            case "java.lang.Boolean":
                return Boolean.class;
            case "byte[]":
                return String.class;
            }
            try {
                return Class.forName(stringType);
            } catch (ClassNotFoundException e) {
                checker.report(Result.failure("class.find.failed", stringType),
                        tree);
                return Object.class;
            }

        }

        /**
         * Gets a Class[] from a List of AnnotatedTypeMirror by calling getClass
         * on the underlying type of each element
         * 
         * @param typeList
         * 
         * @return
         */
        private Class<?>[] getClassList(List<AnnotatedTypeMirror> typeList,
                Tree tree) {
            // Get a Class array for the parameters
            Class<?>[] classList = new Class<?>[typeList.size()];
            for (int i = 0; i < typeList.size(); i++) {
                classList[i] = getClass(typeList.get(i).getUnderlyingType()
                        .toString(), tree);
            }

            return classList;
        }

        /**
         * Extracts the list of values on an annotation as a List of Object
         * while ensuring the actual type of each element is the same type as
         * the underlyingType of the typeMirror input (so an Integer returns a
         * list of Integer, not Long)
         * 
         * @param typeMirror
         *            the AnnotatedTypeMirror to pull values from and use to
         *            determine what type to cast the values to
         * 
         * @return List of Object where each element is the same type as the
         *         underlyingType of typeMirror
         */
        private List<Object> getCastedValues(AnnotatedTypeMirror typeMirror,
                Tree tree) {
            if (!nonValueAnno(typeMirror)) {
                // Class<?> annoValueClass =
                // getTypeValueClass(typeMirror.getUnderlyingType().toString());
                Class<?> annoValueClass = getAnnotationValueClass(typeMirror
                        .getAnnotationInHierarchy(UNKNOWNVAL));

                @SuppressWarnings("unchecked")
                // We know any type of value array
                // from an annotation is a
                // subtype of Object, so we are
                // casting it to that
                List<Object> tempValues = (List<Object>) AnnotationUtils
                        .getElementValueArray(
                                typeMirror.getAnnotationInHierarchy(UNKNOWNVAL),
                                "value", annoValueClass, true);

                // Since we will be reflectively invoking the method with these
                // values, it will matter that they are the proper type (Integer
                // and not Long for an int argument), so fix them if necessary

                fixAnnotationValueObjectType(
                        tempValues,
                        annoValueClass,
                        getClass(typeMirror.getUnderlyingType().toString(),
                                tree));
                return tempValues;
            } else {
                return null;
            }
        }

        /**
         * Extracts and correctly casts all the values of a List of
         * AnnotatedTypeMirror elements and stores them in an ArrayDeque. The
         * order in the ArrayDeque is the reverse of the order of the List
         * (List[0] -> ArrayDeque[size -1]). This ordering may not be intuitive
         * but this method is used in conjunction with the recursive descent to
         * evaluate method and constructor invocations, which will pop argument
         * values off and push them onto another deque, so the order actually
         * gets reversed twice and the original order is maintained.
         * 
         * @param argTypes
         *            a List of AnnotatedTypeMirror elements
         * 
         * @return an ArrayDeque containing List of Object where each list
         *         corresponds to the annotation values of an
         *         AnnotatedTypeMirror passed in.
         */
        private ArrayDeque<List<Object>> getAllArgumentAnnotationValues(
                List<AnnotatedTypeMirror> argTypes, Tree tree) {
            ArrayDeque<List<Object>> allArgValues = new ArrayDeque<List<Object>>();

            int count = 0;
            for (AnnotatedTypeMirror a : argTypes) {
                allArgValues.push(getCastedValues(a, tree));
                count++;
            }
            return allArgValues;
        }

        /**
         * Changes all elements in a List of Object from origClass to newClass.
         * 
         * @param listToFix
         * @param origClass
         *            is in {Double.class, Long.class}
         * @param newClass
         *            is in {int.class, short.class, byte.class, float.class} or
         *            their respective wrappers
         */
        private void fixAnnotationValueObjectType(List<Object> listToFix,
                Class<?> origClass, Class<?> newClass) {
            // Check if the types don't match because floats and ints get
            // promoted to Doubles and Longs in this annotation scheme

            // Only need to do this if the annotation values were Doubles or
            // Longs because only these annotations apply to multiple types

            // if (origClass == Long.class || origClass == Double.class){

            // if (newClass == Integer.class || newClass == int.class){
            // for (int i = 0; i < listToFix.size(); i++){
            // listToFix.set(i, new
            // Integer(((Long)listToFix.get(i)).intValue()));
            // }
            // }
            // else if (newClass == Short.class || newClass == short.class){
            // for (int i = 0; i < listToFix.size(); i++){
            // listToFix.set(i, new
            // Short(((Long)listToFix.get(i)).shortValue()));
            // }
            // }
            // else if (newClass == Byte.class || newClass == byte.class){
            // for (int i = 0; i < listToFix.size(); i++){
            // listToFix.set(i, new Byte(((Long)listToFix.get(i)).byteValue()));
            // }
            // }
            // else if (newClass == Float.class || newClass == float.class){
            // for (int i = 0; i < listToFix.size(); i++){
            // listToFix.set(i, new
            // Float(((Double)listToFix.get(i)).floatValue()));
            // }
            // }
            // }
            if (origClass == String.class && newClass == byte[].class) {
                for (int i = 0; i < listToFix.size(); i++) {
                    listToFix.set(i, ((String) listToFix.get(i)).getBytes());
                }
            }
        }

        /**
         * Overloaded version to accept an AnnotatedTypeMirror
         * 
         * @param resultType
         *            is evaluated using getClass to derived a Class object for
         *            passing to the other resultAnnotationHandler function
         * @param results
         * 
         * @return
         */
        private AnnotationMirror resultAnnotationHandler(
                AnnotatedTypeMirror resultType, List<Object> results, Tree tree) {
            return resultAnnotationHandler(
                    getClass(resultType.getUnderlyingType().toString(), tree),
                    results);
        }

        private AnnotationMirror resultAnnotationHandler(TypeMirror resultType,
                List<Object> results, Tree tree) {
            return resultAnnotationHandler(
                    getClass(resultType.toString(), tree), results);
        }

        /**
         * Returns an AnnotationMirror based on what Class it is supposed to
         * apply to, with the annotation containing results in its value field.
         * Annotations should never have empty value fields, so if |results| ==
         * 0 then UnknownVal is returned.
         * 
         * @param resultClass
         *            the Class to return an annotation
         * @param results
         *            the results to go in the annotation's value field
         * 
         * @return an AnnotationMirror containing results and corresponding to
         *         resultClass, if possible. UnknownVal otherwise
         */
        private AnnotationMirror resultAnnotationHandler(Class<?> resultClass,
                List<Object> results) {
            if (results.size() == 0) {
                return UNKNOWNVAL;
            } else if (resultClass == Boolean.class
                    || resultClass == boolean.class) {
                HashSet<Boolean> boolVals = new HashSet<Boolean>(results.size());
                for (Object o : results) {
                    boolVals.add((Boolean) o);
                }
                AnnotationMirror newAnno = createAnnotation(
                        "checkers.value.quals.BoolVal", boolVals);
                return newAnno;

            } else if (resultClass == Double.class
                    || resultClass == double.class) {
                HashSet<Double> doubleVals = new HashSet<Double>(results.size());
                for (Object o : results) {
                    doubleVals.add((Double) o);
                }
                return createAnnotation("checkers.value.quals.DoubleVal",
                        doubleVals);
            } else if (resultClass == Float.class || resultClass == float.class) {
                HashSet<Float> floatVals = new HashSet<Float>(results.size());
                for (Object o : results) {
                    floatVals.add((Float) o);
                }
                return createAnnotation("checkers.value.quals.FloatVal",
                        floatVals);
            } else if (resultClass == Integer.class || resultClass == int.class) {
                HashSet<Integer> intVals = new HashSet<Integer>(results.size());
                for (Object o : results) {
                    intVals.add((Integer) o);
                }
                return createAnnotation("checkers.value.quals.IntVal", intVals);
            } else if (resultClass == Short.class || resultClass == short.class) {
                HashSet<Short> shortVals = new HashSet<Short>(results.size());
                for (Object o : results) {
                    shortVals.add((Short) o);
                }
                return createAnnotation("checkers.value.quals.ShortVal",
                        shortVals);
            } else if (resultClass == Byte.class || resultClass == byte.class) {
                HashSet<Byte> byteVals = new HashSet<Byte>(results.size());
                for (Object o : results) {
                    byteVals.add((Byte) o);
                }
                return createAnnotation("checkers.value.quals.ByteVal",
                        byteVals);
            } else if (resultClass == Long.class || resultClass == long.class) {
                HashSet<Long> longVals = new HashSet<Long>(results.size());
                for (Object o : results) {
                    longVals.add((Long) o);
                }
                return createAnnotation("checkers.value.quals.LongVal",
                        longVals);
            } else if (resultClass == Character.class
                    || resultClass == char.class) {
                HashSet<Character> charVals = new HashSet<Character>(
                        results.size());
                for (Object o : results) {
                    charVals.add((Character) o);
                }
                return createAnnotation("checkers.value.quals.CharVal",
                        charVals);
            } else if (resultClass == String.class) {
                HashSet<String> stringVals = new HashSet<String>(results.size());
                for (Object o : results) {
                    stringVals.add((String) o);
                }
                return createAnnotation("checkers.value.quals.StringVal",
                        stringVals);
            } else if (resultClass == byte[].class) {
                HashSet<String> stringVals = new HashSet<String>(results.size());
                for (Object o : results) {
                    stringVals.add(new String((byte[]) o));
                }
                return createAnnotation("checkers.value.quals.StringVal",
                        stringVals);
            }
            return UNKNOWNVAL;
        }

        /**
         * Attempts to "cast" type from one value annotation to another as
         * specified by the value of castTypeString. If this conversion is not
         * possible, the AnnotatedTypeMirror remains unchanged. Otherwise, the
         * new annotation is created and replaces the old annotation on type
         * 
         * @param tree
         *            the ExpressionTree for the object being cast
         * @param castTypeString
         *            the String name of the type to cast the tree/type to
         * @param alteredType
         *            the AnnotatedTypeMirror that is being cast
         */
        private void handleCast(ExpressionTree tree, String castTypeString,
                AnnotatedTypeMirror alteredType) {

            AnnotatedTypeMirror treeType = getAnnotatedType(tree);
            if (!nonValueAnno(treeType)) {
                AnnotationMirror treeAnno = treeType
                        .getAnnotationInHierarchy(UNKNOWNVAL);
                AnnotationMirror newQual = treeAnno;

                String anno = "checkers.value.quals.";
                if (castTypeString.equals("boolean")) {
                    // do nothing
                } else if (castTypeString.equals("java.lang.String")) {
                    HashSet<String> newValues = new HashSet<String>();
                    List<Object> valuesToCast = AnnotationUtils
                            .getElementValueArray(treeAnno, "value",
                                    Object.class, true);
                    for (Object o : valuesToCast) {
                        newValues.add(o.toString());
                    }
                    anno += "StringVal";
                    alteredType.replaceAnnotation(createAnnotation(anno,
                            newValues));
                } else if (castTypeString.equals("char")
                        && isNumberAnnotation(treeAnno)) {
                    if (AnnotationUtils
                            .areSameIgnoringValues(treeAnno, CHARVAL)) {
                        alteredType.replaceAnnotation(treeAnno);
                    } else {
                        HashSet<Character> newValues = new HashSet<Character>();
                        List<Number> valuesToCast = AnnotationUtils
                                .getElementValueArray(treeAnno, "value",
                                        Number.class, true);
                        for (Number n : valuesToCast) {
                            newValues.add((char) n.intValue());
                        }
                        anno += "CharVal";
                        alteredType.replaceAnnotation(createAnnotation(anno,
                                newValues));
                    }
                } else {
                    List<Number> valuesToCast;
                    if (AnnotationUtils
                            .areSameIgnoringValues(treeAnno, CHARVAL)) {
                        List<Character> charVals = AnnotationUtils
                                .getElementValueArray(treeAnno, "value",
                                        Character.class, true);

                        valuesToCast = new ArrayList<Number>(charVals.size());
                        for (Character c : charVals) {
                            valuesToCast.add(new Integer(c.charValue()));
                        }
                    } else {
                        valuesToCast = AnnotationUtils.getElementValueArray(
                                treeAnno, "value", Number.class, true);
                    }

                    HashSet<Object> newValues = new HashSet<Object>();

                    if (castTypeString.equals("double")) {
                        for (Number n : valuesToCast) {
                            newValues.add(new Double(n.doubleValue()));
                        }
                        anno += "DoubleVal";
                    }

                    else if (castTypeString.equals("int")) {
                        for (Number n : valuesToCast) {
                            newValues.add(new Integer(n.intValue()));
                        }
                        anno += "IntVal";
                    }

                    else if (castTypeString.equals("long")) {
                        for (Number n : valuesToCast) {
                            newValues.add(new Long(n.longValue()));
                        }
                        anno += "LongVal";
                    }

                    else if (castTypeString.equals("float")) {
                        for (Number n : valuesToCast) {
                            newValues.add(new Float(n.floatValue()));
                        }
                        anno += "FloatVal";
                    }

                    else if (castTypeString.equals("short")) {
                        for (Number n : valuesToCast) {
                            newValues.add(new Short(n.shortValue()));
                        }
                        anno += "ShortVal";
                    }

                    else if (castTypeString.equals("byte")) {
                        for (Number n : valuesToCast) {
                            newValues.add(new Byte(n.byteValue()));
                        }
                        anno += "ByteVal";
                    }

                    alteredType.replaceAnnotation(createAnnotation(anno,
                            newValues));
                }
            }
        }

        /**
         * To make these numerous calls to check if an annotation is UnknownVal
         * or ArrayLen a little nicer looking
         * 
         * @param mirror
         *            the AnnotatedTypeMirror to check
         * 
         * @return true if the AnnotatedTypeMirror contains the UnknownVal or
         *         ArrayLen AnnotationMirror, false otherwise
         */
        private boolean nonValueAnno(AnnotatedTypeMirror mirror) {
            return AnnotationUtils.areSameIgnoringValues(
                    mirror.getAnnotationInHierarchy(UNKNOWNVAL), UNKNOWNVAL)
                    || AnnotationUtils
                            .areSameIgnoringValues(
                                    mirror.getAnnotationInHierarchy(ARRAYLEN),
                                    ARRAYLEN);
        }
    }

    private boolean isNumberAnnotation(AnnotationMirror anno) {
        for (AnnotationMirror m : orderedNumberAnnotations) {
            if (AnnotationUtils.areSameIgnoringValues(anno, m)) {
                return true;
            }
        }

        return false;
    }

}
