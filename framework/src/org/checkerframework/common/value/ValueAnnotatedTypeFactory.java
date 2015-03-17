package org.checkerframework.common.value;

import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.BoolVal;
import org.checkerframework.common.value.qual.BottomVal;
import org.checkerframework.common.value.qual.DoubleVal;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.StaticallyExecutable;
import org.checkerframework.common.value.qual.StringVal;
import org.checkerframework.common.value.qual.UnknownVal;
import org.checkerframework.common.value.util.NumberMath;
import org.checkerframework.common.value.util.NumberUtils;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.qual.DefaultLocation;
import org.checkerframework.framework.qual.TypeQualifiers;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.ImplicitsTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.framework.util.defaults.QualifierDefaults;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.TreePath;

/**
 * @author plvines
 * @author smillst
 *
 *         AnnotatedTypeFactory for the Value type system.
 *
 */
@TypeQualifiers({ ArrayLen.class, BoolVal.class, DoubleVal.class, IntVal.class,
        StringVal.class, BottomVal.class, UnknownVal.class })
public class ValueAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    /** Annotation constants */
    protected final AnnotationMirror INTVAL, DOUBLEVAL, BOOLVAL, ARRAYLEN,
            STRINGVAL, BOTTOMVAL, UNKNOWNVAL, STATICALLY_EXECUTABLE;

    protected static final Set<Modifier> PUBLIC_STATIC_FINAL_SET = new HashSet<Modifier>(
            3);

    private long t = 0;

    protected static final int MAX_VALUES = 10; // The maximum number of values
                                                // allowed in an annotation's
                                                // array
    protected final List<AnnotationMirror> constantAnnotations;
    protected Set<String> coveredClassStrings;

    /** should this type factory report warnings? **/
    private boolean reportWarnings = true;
    
    private ReflectiveEvalutator evalutator = new ReflectiveEvalutator(checker, this, reportWarnings);

    /**
     * Constructor. Initializes all the AnnotationMirror constants.
     *
     * @param checker
     *            The checker used with this AnnotatedTypeFactory
     *
     */
    public ValueAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
        PUBLIC_STATIC_FINAL_SET.add(Modifier.PUBLIC);
        PUBLIC_STATIC_FINAL_SET.add(Modifier.FINAL);
        PUBLIC_STATIC_FINAL_SET.add(Modifier.STATIC);
        INTVAL = AnnotationUtils.fromClass(elements, IntVal.class);
        BOOLVAL = AnnotationUtils.fromClass(elements, BoolVal.class);
        ARRAYLEN = AnnotationUtils.fromClass(elements, ArrayLen.class);
        DOUBLEVAL = AnnotationUtils.fromClass(elements, DoubleVal.class);
        STRINGVAL = AnnotationUtils.fromClass(elements, StringVal.class);
        BOTTOMVAL = AnnotationUtils.fromClass(elements, BottomVal.class);
        STATICALLY_EXECUTABLE = AnnotationUtils.fromClass(elements,
                StaticallyExecutable.class);
        UNKNOWNVAL = AnnotationUtils.fromClass(elements, UnknownVal.class);
        constantAnnotations = new ArrayList<AnnotationMirror>(9);
        constantAnnotations.add(DOUBLEVAL);
        constantAnnotations.add(INTVAL);
        constantAnnotations.add(BOOLVAL);
        constantAnnotations.add(STRINGVAL);
        constantAnnotations.add(BOTTOMVAL);
        constantAnnotations.add(ARRAYLEN);
        constantAnnotations.add(STATICALLY_EXECUTABLE);
        constantAnnotations.add(UNKNOWNVAL);

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

    public void disableWarnings() {
        reportWarnings = false;
    }

    public void enableWarnings() {
        reportWarnings = true;
    }

    @Override
    public CFTransfer createFlowTransferFunction(
            CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
        // The super implementation uses the name of the checker
        // to reflectively create a transfer with the checker name followed
        // by Transfer. Since this factory is intended to be used with
        // any checker, explicitly create the default CFTransfer
        return new ValueTransfer(analysis);
    }

    @Override
    public AnnotatedTypeMirror getAnnotatedType(Tree tree) {
        if (tree.getKind() == Tree.Kind.POSTFIX_DECREMENT
                || tree.getKind() == Tree.Kind.POSTFIX_INCREMENT) {

            return getPostFixAnno((UnaryTree) tree,
                    super.getAnnotatedType(tree));

        } else {
            return super.getAnnotatedType(tree);
        }
    }

    private AnnotatedTypeMirror getPostFixAnno(UnaryTree tree,
            AnnotatedTypeMirror anno) {
        if (anno.hasAnnotation(DoubleVal.class)) {
            return postFixDoubl(anno,
                    tree.getKind() == Tree.Kind.POSTFIX_INCREMENT);
        } else if (anno.hasAnnotation(IntVal.class)) {
            return postFixInt(anno,
                    tree.getKind() == Tree.Kind.POSTFIX_INCREMENT);
        }
        return anno;

    }

    private AnnotatedTypeMirror postFixInt(AnnotatedTypeMirror anno,
            boolean increment) {
        List<Long> values = AnnotationUtils.getElementValueArray(
                anno.getAnnotation(IntVal.class), "value", Long.class, true);
        List<? extends Number> castedValues = NumberUtils.castNumbers(
                anno.getUnderlyingType(), values);
        List<Long> results = new ArrayList<>();
        for (Number value : castedValues) {
            NumberMath<?> number = NumberMath.getNumberMath(value);
            if (increment) {
                results.add(number.minus(1).longValue());
            } else {
                results.add(number.plus(1).longValue());

            }
        }
        anno.replaceAnnotation(createIntValAnnotation(results));
        return anno;
    }

    private AnnotatedTypeMirror postFixDoubl(AnnotatedTypeMirror anno,
            boolean increment) {
        List<Double> values = AnnotationUtils.getElementValueArray(
                anno.getAnnotation(DoubleVal.class), "value", Double.class,
                true);
        List<? extends Number> castedValues = NumberUtils.castNumbers(
                anno.getUnderlyingType(), values);
        List<Double> results = new ArrayList<>();
        for (Number value : castedValues) {
            NumberMath<?> number = NumberMath.getNumberMath(value);
            if (increment) {
                results.add(number.minus(1).doubleValue());
            } else {
                results.add(number.plus(1).doubleValue());

            }
        }
        anno.replaceAnnotation(createDoubleValAnnotation(results));
        return anno;
    }

    /**
     * Creates an annotation of the name given with the set of values given.
     * Issues a checker warning and return UNKNOWNVAL if values.size &gt;
     * MAX_VALUES
     *
     * @return annotation given by name with values=values, or UNKNOWNVAL
     */
    public AnnotationMirror createAnnotation(String name, Set<?> values) {

        if (values.size() > 0 && values.size() <= MAX_VALUES) {
            AnnotationBuilder builder = new AnnotationBuilder(processingEnv,
                    name);
            List<Object> valuesList = new ArrayList<Object>(values);
            builder.setValue("value", valuesList);
            return builder.build();
        } else {
            return UNKNOWNVAL;
        }
    }

    public AnnotationMirror createAnnotation(Class<? extends Annotation> name,
            Set<?> values) {
        return createAnnotation(name.getCanonicalName(), values);
    }

    @Override
    protected MultiGraphQualifierHierarchy.MultiGraphFactory createQualifierHierarchyFactory() {
        return new MultiGraphQualifierHierarchy.MultiGraphFactory(this);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new ValueQualifierHierarchy(factory);
    }

    @Override
    protected TypeAnnotator createTypeAnnotator() {
        return new ListTypeAnnotator(new ValueTypeAnnotator(this),
                super.createTypeAnnotator());
    }

    private class ValueTypeAnnotator extends TypeAnnotator {

        public ValueTypeAnnotator(AnnotatedTypeFactory atypeFactory) {
            super(atypeFactory);
        }

        @Override
        public Void visitPrimitive(AnnotatedPrimitiveType type, Void p) {
            replaceWithUnknownValIfTooManyValues((AnnotatedTypeMirror) type);

            return super.visitPrimitive(type, p);
        }

        @Override
        public Void visitDeclared(AnnotatedDeclaredType type, Void p) {
            replaceWithUnknownValIfTooManyValues((AnnotatedTypeMirror) type);

            return super.visitDeclared(type, p);
        }

        /**
         * If any constant-value annotation has &gt; MAX_VALUES number of values
         * provided, treats the value as UnknownVal. Works together with
         * ValueVisitor.visitAnnotation, which issues a warning to the user in
         * this case.
         */
        private void replaceWithUnknownValIfTooManyValues(
                AnnotatedTypeMirror atm) {
            AnnotationMirror anno = atm.getAnnotationInHierarchy(UNKNOWNVAL);

            if (anno != null && anno.getElementValues().size() > 0) {
                List<Object> values = AnnotationUtils.getElementValueArray(
                        anno, "value", Object.class, false);
                if (values != null && values.size() > MAX_VALUES) {
                    atm.replaceAnnotation(UNKNOWNVAL);
                }
            }
        }

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

        @Override
        public AnnotationMirror greatestLowerBound(AnnotationMirror a1,
                AnnotationMirror a2) {
            if (isSubtype(a1, a2)) {
                return a1;
            } else if (isSubtype(a2, a1)) {
                return a2;
            } else {
                // If the two are unrelated, then bottom is the GLB.
                return BOTTOMVAL;
            }
        }

        /**
         * Determines the least upper bound of a1 and a2. If a1 and a2 are both
         * the same type of Value annotation, then the LUB is the result of
         * taking all values from both a1 and a2 and removing duplicates. If a1
         * and a2 are not the same type of Value annotation they may still be
         * mergeable because some values can be implicitly cast as others. If a1
         * and a2 are both in {DoubleVal, IntVal} then they will be converted
         * upwards: IntVal -> DoubleVal to arrive at a common annotation type.
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
                // If either is UNKNOWNVAL, ARRAYLEN, STRINGVAL, or BOOLEAN then
                // the LUB is
                // UnknownVal
                if (!isNumberAnnotation(a1) || !isNumberAnnotation(a2)) {
                    return UNKNOWNVAL;
                } else {
                    // At this point one of them must be a DoubleVal and one an
                    // IntVal
                    AnnotationMirror higher;
                    AnnotationMirror lower;

                    if (AnnotationUtils.areSameIgnoringValues(a2, DOUBLEVAL)) {
                        higher = a2;
                        lower = a1;
                    } else {
                        higher = a1;
                        lower = a2;
                    }

                    String anno = "org.checkerframework.common.value.qual.";
                    List<Number> valuesToCast;

                    valuesToCast = AnnotationUtils.getElementValueArray(lower,
                            "value", Number.class, true);

                    HashSet<Object> newValues = new HashSet<Object>(
                            AnnotationUtils.getElementValueArray(higher,
                                    "value", Object.class, true));

                    for (Number n : valuesToCast) {
                        newValues.add(new Double(n.doubleValue()));
                    }
                    anno += "DoubleVal";

                    return createAnnotation(anno, newValues);
                }
            }
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
            // not the same type, but if they are DOUBLEVAL and INTVAL they
            // might still be subtypes
            if (AnnotationUtils.areSameIgnoringValues(lhs, DOUBLEVAL)
                    && AnnotationUtils.areSameIgnoringValues(rhs, INTVAL)) {
                List<Long> rhsValues;
                rhsValues = AnnotationUtils.getElementValueArray(rhs, "value",
                        Long.class, true);
                List<Double> lhsValues = AnnotationUtils.getElementValueArray(
                        lhs, "value", Double.class, true);
                boolean same = false;
                for (Long rhsLong : rhsValues) {
                    for (Double lhsDbl : lhsValues) {
                        if (lhsDbl.doubleValue() == rhsLong.doubleValue()) {
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

    @Override
    protected TreeAnnotator createTreeAnnotator() {
        // The ValueTreeAnnotator handles propagation differently,
        // so it doesn't need PropgationTreeAnnotator.
        return new ListTreeAnnotator(new ValueTreeAnnotator(this),
                new ImplicitsTreeAnnotator(this));
    }

    @Override
    protected QualifierDefaults createQualifierDefaults() {
        QualifierDefaults defaults = super.createQualifierDefaults();
        defaults.addAbsoluteDefault(UNKNOWNVAL, DefaultLocation.OTHERWISE);
        defaults.addAbsoluteDefault(BOTTOMVAL, DefaultLocation.LOWER_BOUNDS);

        return defaults;
    }

    /**
     * The TreeAnnotator for this AnnotatedTypeFactory
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
                    Class<?> clazz = getClass(type, tree);
                    String stringVal = null;
                    if (clazz.equals(byte[].class)) {
                        stringVal = getByteArrayStringVal(initializers);
                    } else if (clazz.equals(char[].class)) {
                        stringVal = getCharArrayStringVal(initializers);
                    }

                    if (stringVal != null) {
                        HashSet<String> stringFromChars = new HashSet<String>(1);
                        stringFromChars.add(stringVal);
                        newQual = createAnnotation(
                                "org.checkerframework.common.value.qual.StringVal",
                                stringFromChars);
                        type.replaceAnnotation(newQual);
                        return null;
                    }

                    newQual = createAnnotation(
                            "org.checkerframework.common.value.qual.ArrayLen",
                            value);
                    type.replaceAnnotation(newQual);

                }
            }
            return super.visitNewArray(tree, type);
        }

        private String getByteArrayStringVal(
                List<? extends ExpressionTree> initializers) {
            boolean allLiterals = true;
            byte[] bytes = new byte[initializers.size()];
            int i = 0;
            for (ExpressionTree e : initializers) {
                if (e.getKind() == Tree.Kind.INT_LITERAL) {
                    bytes[i] = (byte) (((Integer) ((LiteralTree) e).getValue())
                            .intValue());
                } else if (e.getKind() == Tree.Kind.CHAR_LITERAL) {
                    bytes[i] = (byte) (((Character) ((LiteralTree) e)
                            .getValue()).charValue());
                } else {
                    allLiterals = false;
                }
                i++;
            }
            if (allLiterals)
                return new String(bytes);
            // If any part of the initialize isn't know,
            // the stringval isn't known.
            return null;
        }

        private String getCharArrayStringVal(
                List<? extends ExpressionTree> initializers) {
            boolean allLiterals = true;
            String stringVal = "";
            for (ExpressionTree e : initializers) {
                if (e.getKind() == Tree.Kind.INT_LITERAL) {
                    char charVal = (char) (((Integer) ((LiteralTree) e)
                            .getValue()).intValue());
                    stringVal += charVal;
                } else if (e.getKind() == Tree.Kind.CHAR_LITERAL) {
                    char charVal = (((Character) ((LiteralTree) e).getValue()));
                    stringVal += charVal;
                } else {
                    allLiterals = false;
                }
            }
            if (allLiterals)
                return stringVal;
            // If any part of the initialize isn't know,
            // the stringval isn't known.
            return null;
        }

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
                List<Long> longLengths = AnnotationUtils.getElementValueArray(
                        dimType, "value", Long.class, true);

                HashSet<Integer> lengths = new HashSet<Integer>(
                        longLengths.size());
                for (Long l : longLengths) {
                    lengths.add(l.intValue());
                }
                AnnotationMirror newQual = createAnnotation(
                        "org.checkerframework.common.value.qual.ArrayLen",
                        lengths);
                type.replaceAnnotation(newQual);
            } else {
                type.replaceAnnotation(UNKNOWNVAL);
            }
        }

        @Override
        public Void visitTypeCast(TypeCastTree tree, AnnotatedTypeMirror type) {
            if (isClassCovered(type)) {
                handleCast(tree.getExpression(),
                        getClass(type, tree.getType()), type);
            } else if (type.getKind() == TypeKind.ARRAY) {
                handleArrayCast(tree, type);
            }
            return super.visitTypeCast(tree, type);
        }

        private void handleArrayCast(TypeCastTree tree, AnnotatedTypeMirror type) {
            if (tree.getExpression().getKind() == Kind.NULL_LITERAL) {
                type.replaceAnnotation(BOTTOMVAL);
            }

        }

        @Override
        public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
            if (isClassCovered(type)) {
                switch(tree.getKind()){
                case BOOLEAN_LITERAL:
                    AnnotationMirror boolAnno = createBooleanAnnotationMirror(Collections
                            .singletonList((Boolean) tree.getValue()));
                    type.replaceAnnotation(boolAnno);
                    return null;
                    
                case CHAR_LITERAL:
                    AnnotationMirror charAnno = createCharAnnotation(Collections
                            .singletonList((Character) tree.getValue()));
                    type.replaceAnnotation(charAnno);
                    return null;
                    
                case DOUBLE_LITERAL:
                    AnnotationMirror doubleAnno = createNumberAnnotationMirror(Collections
                            .<Number>singletonList((Double) tree.getValue()));
                    type.replaceAnnotation(doubleAnno);
                    return null;
                    
                case FLOAT_LITERAL:
                    AnnotationMirror floatAnno = createNumberAnnotationMirror(Collections
                            .<Number>singletonList((Float) tree.getValue()));
                    type.replaceAnnotation(floatAnno);
                    return null;
                case INT_LITERAL:
                    AnnotationMirror intAnno = createNumberAnnotationMirror(Collections
                            .<Number>singletonList((Integer) tree.getValue()));
                    type.replaceAnnotation(intAnno);
                    return null;
                case LONG_LITERAL:
                    AnnotationMirror longAnno = createNumberAnnotationMirror(Collections
                            .<Number>singletonList((Long) tree.getValue()));
                    type.replaceAnnotation(longAnno);
                    return null;
                case STRING_LITERAL:
                    AnnotationMirror stringAnno = createStringValAnnotationMirror(Collections
                            .singletonList((String) tree.getValue()));
                    type.replaceAnnotation(stringAnno);
                    return null;
                default:
                    return null;
                }

            }
            return null;
        }

        /**
         * Simple method to take a MemberSelectTree representing a method call
         * and determine if the method's return is annotated with
         *
         * @StaticallyExecutable.
         *
         * @param method
         *
         * @return
         */
        private boolean methodIsStaticallyExecutable(Element method) {
            return getDeclAnnotation(method, StaticallyExecutable.class) != null;
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree tree,
                AnnotatedTypeMirror type) {
            if (isClassCovered(type)
                    && methodIsStaticallyExecutable(TreeUtils
                            .elementFromUse(tree))) {
                // Get argument values
                List<? extends ExpressionTree> arguments = tree.getArguments();
                ArrayList<List<?>> argValues;
                if (arguments.size() > 0) {
                    argValues = new ArrayList<List<?>>();
                    for (ExpressionTree argument : arguments) {
                        AnnotatedTypeMirror argType = getAnnotatedType(argument);
                        List<?> values = getCastedValues(argType, argument);
                        if (values.isEmpty()) {
                            // values aren't known, so don't try to evaluate the
                            // method
                            return null;
                        }
                        argValues.add(values);
                    }
                } else {
                    argValues = null;
                }
                
                // Get receiver values
                AnnotatedTypeMirror receiver = getReceiverType(tree);
                List<?> receiverValues;
                
                if (receiver != null && !ElementUtils.isStatic(TreeUtils.elementFromUse(tree))) {
                    receiverValues = getCastedValues(receiver,
                            TreeUtils.getReceiverTree(tree));
                    if (receiverValues.isEmpty()) {
                        // values aren't known, so don't try to evaluate the
                        // method
                        return null;
                    }
                } else {
                    receiverValues = null;
                }
                
                // Evaluate method
                List<?> returnValues = evalutator.evaluteMethodCall(argValues,
                        receiverValues, tree);
                AnnotationMirror returnType = resultAnnotationHandler(
                        type.getUnderlyingType(), returnValues, tree);
                type.replaceAnnotation(returnType);
            }

            return null;
        }

        @Override
        public Void visitNewClass(NewClassTree tree, AnnotatedTypeMirror type) {
            boolean wrapperClass = TypesUtils.isBoxedPrimitive(type
                    .getUnderlyingType())
                    || TypesUtils.isDeclaredOfName(type.getUnderlyingType(),
                            "java.lang.String");

            if (wrapperClass || (isClassCovered(type)
                    && methodIsStaticallyExecutable(TreeUtils
                            .elementFromUse(tree)))) {
                // get arugment values
                List<? extends ExpressionTree> arguments = tree.getArguments();
                ArrayList<List<?>> argValues;
                if (arguments.size() > 0) {
                    argValues = new ArrayList<List<?>>();
                    for (ExpressionTree argument : arguments) {
                        AnnotatedTypeMirror argType = getAnnotatedType(argument);
                        List<?> values = getCastedValues(argType, argument);
                        if (values.isEmpty()) {
                            // values aren't known, so don't try to evaluate the
                            // method
                            return null;
                        }
                        argValues.add(values);
                    }
                } else {
                    argValues = null;
                }
                // Evaluate method
                List<?> returnValues = evalutator.evaluteConstrutorCall(argValues, tree, type.getUnderlyingType());
                AnnotationMirror returnType = resultAnnotationHandler(
                        type.getUnderlyingType(), returnValues, tree);
                type.replaceAnnotation(returnType);
            }

            return null;
        }

        @Override
        public Void visitMemberSelect(MemberSelectTree tree,
                AnnotatedTypeMirror type) {
            if (TreeUtils.isFieldAccess(tree)
                    && isClassCovered(type.getUnderlyingType())) {
                VariableElement elem = (VariableElement) InternalUtils
                        .symbol(tree);
                Object value = elem.getConstantValue();
                if (value != null) {
                    // compile time constant
                    type.replaceAnnotation(
                    resultAnnotationHandler(type.getUnderlyingType(), Collections.singletonList(value), tree));
                    return null;
                }
                if (ElementUtils.isStatic(elem)
                        && ElementUtils.isFinal(elem)) {
                    Element e = InternalUtils.symbol(tree.getExpression());
                    if (e != null) {
                        String classname = ElementUtils
                                .getQualifiedClassName(e).toString();
                        String fieldName = tree.getIdentifier().toString();
                        value = evalutator.evaluateStaticFieldAccess(classname,
                                fieldName, tree);
                        if (value != null)
                            type.replaceAnnotation(resultAnnotationHandler(type.getUnderlyingType(), Collections.singletonList(value), tree));
                        return null;
                    }
                }

                if (tree.getIdentifier().toString().equals("length")) {
                    AnnotatedTypeMirror receiverType = getAnnotatedType(tree
                            .getExpression());
                    if (receiverType.getKind() == TypeKind.ARRAY) {
                        AnnotationMirror arrayAnno = receiverType
                                .getAnnotation(ArrayLen.class);
                        if (arrayAnno != null) {
                            // array.length, where array : @ArrayLen(x)
                            List<Integer> lengths = ValueAnnotatedTypeFactory
                                    .getArrayLength(arrayAnno);
                            type.replaceAnnotation(createNumberAnnotationMirror(new ArrayList<Number>(
                                    lengths)));
                            return null;
                        }
                    }
                }
            }
            return null;
        }

        /**
         * Overloaded method for convenience of dealing with
         * AnnotatedTypeMirrors. See isClassCovered(TypeMirror type) below
         *
         * @param type
         * @return
         */
        private boolean isClassCovered(AnnotatedTypeMirror type) {
            return isClassCovered(type.getUnderlyingType());
        }

        /**
         *
         * @param type
         * @return true if the type name is in coveredClassStrings
         */
        private boolean isClassCovered(TypeMirror type) {
            return coveredClassStrings.contains(type.toString());
        }

        /**
         * 
         * @param typeMirror
         *            the underlying type is used
         * @param tree
         *            Tree for error reporting
         * @return class object corresponding to the typeMirror passed.
         */
        private Class<?> getClass(AnnotatedTypeMirror typeMirror, Tree tree) {
            TypeMirror type = typeMirror.getUnderlyingType();
            return ValueCheckerUtils.getClassFromType(type);
        }

        /**
         * Gets a list of values in the annotated casted to the type of the
         * tree.
         * 
         * @param typeMirror
         *            AnnotatedTypeMirror with values
         * @param tree
         *            Tree whose type the values are casted to and errors should
         *            be report
         * @return a list of values cast to the type of tree
         */
        private List<?> getCastedValues(AnnotatedTypeMirror typeMirror,
                Tree tree) {
            return getCastedValues(typeMirror, getClass(typeMirror, tree), tree);
        }

        /**
         * Returns a list of values from the typeMirror cast to castType
         * 
         * @param typeMirror
         *            AnnotatedTypeMirror with values
         * @param castType
         *            Class with type to cast to
         * @param tree
         *            Tree used for location of errors
         * @return a list of values casted to typeCast
         */
        private List<?> getCastedValues(AnnotatedTypeMirror typeMirror,
                Class<?> castType, Tree tree) {
            AnnotationMirror anno = getValueAnnotation(typeMirror);
            return getCastedValues(anno, castType, tree);
        }

        /**
         * Returns a list of values from the typeMirror cast to castType
         * 
         * @param typeMirror
         *            AnnotationMirror with values
         * @param castType
         *            Class with type to cast to
         * @param tree
         *            Tree used for location of errors
         * @return a list of values casted to typeCast
         */
        private List<?> getCastedValues(AnnotationMirror anno,
                Class<?> castType, Tree tree) {
            List<?> values = null;
            if (AnnotationUtils.areSameByClass(anno, DoubleVal.class)) {
                values = convertDoubleVal(anno, castType);
            } else if (AnnotationUtils.areSameByClass(anno, IntVal.class)) {
                values = convertIntVal(anno, castType);
            } else if (AnnotationUtils.areSameByClass(anno, StringVal.class)) {
                values = convertStringVal(anno, castType);
            } else if (AnnotationUtils.areSameByClass(anno, BoolVal.class)) {
                values = convertBoolVal(anno, castType);
            } else if (AnnotationUtils.areSameByClass(anno, BottomVal.class)) {
                values = convertBottomVal(anno, castType);
            } else if (AnnotationUtils.areSameByClass(anno, UnknownVal.class)){
                values = new ArrayList<>();
            }
            if (values == null) {
                if (reportWarnings)
                    checker.report(Result.warning("class.convert.failed", anno,
                            castType), tree);
                values = Collections.EMPTY_LIST;
            }

            return values;
        }

        private List<?> convertBottomVal(AnnotationMirror anno,
                Class<?> newClass) {
            if (newClass == String.class) {
                return Collections.singletonList("null");
            }
            return null;
        }

        private List<?> convertToStringVal(List<?> origValues) {
            List<String> strings = new ArrayList<>();
            for (Object value : origValues) {
                strings.add(value.toString());
            }
            return strings;
        }

        private List<?> convertBoolVal(AnnotationMirror anno, Class<?> newClass) {
            List<Boolean> bools = AnnotationUtils.getElementValueArray(anno,
                    "value", Boolean.class, true);
            if (newClass == Boolean.class || newClass == boolean.class) {
                return bools;
            } else if (newClass == String.class) {
                return convertToStringVal(bools);
            }
            return null;
        }

        private List<?> convertStringVal(AnnotationMirror anno,
                Class<?> newClass) {
            List<String> strings = AnnotationUtils.getElementValueArray(anno,
                    "value", String.class, true);
            if (newClass == String.class) {
                return strings;
            } else if (newClass == byte[].class) {
                List<byte[]> bytes = new ArrayList<>();
                for (String s : strings) {
                    bytes.add(s.getBytes());
                }
                return bytes;
            } else if (newClass == char[].class) {
                List<char[]> chars = new ArrayList<>();
                for (String s : strings) {
                    chars.add(s.toCharArray());
                }
                return chars;
            } else if (newClass == Object.class && strings.size() == 1) {
                if (strings.get(0).equals("null"))
                    return strings;
            }
            return null;
        }

        private List<?> convertIntVal(AnnotationMirror anno, Class<?> newClass) {
            List<Long> longs = AnnotationUtils.getElementValueArray(anno,
                    "value", Long.class, true);
            if (newClass == Long.class || newClass == long.class) {
                return longs;
            } else if (newClass == Integer.class || newClass == int.class) {
                List<Integer> ints = new ArrayList<Integer>();
                for (Long l : longs) {
                    ints.add(l.intValue());
                }
                return ints;
            } else if (newClass == Short.class || newClass == short.class) {
                List<Short> shorts = new ArrayList<>();
                for (Long l : longs) {
                    shorts.add(l.shortValue());
                }
                return shorts;
            } else if (newClass == Byte.class || newClass == byte.class) {
                List<Byte> bytes = new ArrayList<>();
                for (Long l : longs) {
                    bytes.add(l.byteValue());
                }
                return bytes;
            } else if (newClass == Character.class || newClass == char.class) {
                List<Character> chars = new ArrayList<>();
                for (Long l : longs) {
                    chars.add((char) l.intValue());
                }
                return chars;
            } else if (newClass == Double.class || newClass == double.class) {
                List<Double> doubles = new ArrayList<>();
                for (Long l : longs) {
                    doubles.add((double) l.intValue());
                }
                return doubles;
            } else if (newClass == Float.class || newClass == float.class) {
                List<Float> floats = new ArrayList<>();
                for (Long l : longs) {
                    floats.add((float) l.intValue());
                }
                return floats;
            } else if (newClass == String.class) {
                return convertToStringVal(longs);
            }
            return null;
        }

        private List<?> convertDoubleVal(AnnotationMirror anno,
                Class<?> newClass) {
            List<Double> doubles = AnnotationUtils.getElementValueArray(anno,
                    "value", Double.class, true);
            if (newClass == Double.class || newClass == double.class) {
                return doubles;
            } else if (newClass == Float.class || newClass == float.class) {
                List<Float> floats = new ArrayList<Float>();
                for (Double d : doubles) {
                    floats.add(d.floatValue());
                }
                return floats;
            } else if (newClass == String.class) {
                return convertToStringVal(doubles);
            }
            return null;
        }

        /**
         * Overloaded version to accept an AnnotatedTypeMirror
         *
         * @param resultType
         *            is evaluated using getClass to derived a Class object for
         *            passing to the other resultAnnotationHandler function
         * @param results
         * @param tree
         *            location for error reporting
         *
         * @return
         */
        private AnnotationMirror resultAnnotationHandler(
                TypeMirror resultType, List<?> results, Tree tree) {
        
            Class<?> resultClass = ValueCheckerUtils.getClassFromType(resultType);

            // For some reason null is included in the list of values,
            // so remove it so that it does not cause a NPE else where.
            results.remove(null);
            if (results.size() == 0) {
                return UNKNOWNVAL;
            } else if (resultClass == Boolean.class
                    || resultClass == boolean.class) {
                HashSet<Boolean> boolVals = new HashSet<Boolean>(results.size());
                for (Object o : results) {
                    boolVals.add((Boolean) o);
                }
                return createBooleanAnnotation(new ArrayList<Boolean>(boolVals));

            } else if (resultClass == Double.class
                    || resultClass == double.class
                    || resultClass == Float.class || resultClass == float.class
                    || resultClass == Integer.class || resultClass == int.class
                    || resultClass == Long.class || resultClass == long.class
                    || resultClass == Short.class || resultClass == short.class
                    || resultClass == Byte.class || resultClass == byte.class) {
                HashSet<Number> vals = new HashSet<>(results.size());
                for (Object o : results) {
                    vals.add((Number) o);
                }
                return createNumberAnnotationMirror(new ArrayList<Number>(vals));
            } else if (resultClass == char.class
                    || resultClass == Character.class) {
                HashSet<Character> intVals = new HashSet<>(results.size());
                for (Object o : results) {
                    intVals.add((Character) o);
                }
                return createCharAnnotation(new ArrayList<Character>(intVals));
            } else if (resultClass == String.class) {
                HashSet<String> stringVals = new HashSet<String>(results.size());
                for (Object o : results) {
                    stringVals.add((String) o);
                }
                return createStringValAnnotationMirror(new ArrayList<String>(
                        stringVals));
            } else if (resultClass == byte[].class) {
                HashSet<String> stringVals = new HashSet<String>(results.size());
                for (Object o : results) {
                    stringVals.add(new String((byte[]) o));
                }
                return createStringValAnnotationMirror(new ArrayList<String>(
                        stringVals));

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
         * @param castType
         *            the String name of the type to cast the tree/type to
         * @param alteredType
         *            the AnnotatedTypeMirror that is being cast
         */
        private void handleCast(ExpressionTree tree, Class<?> castType,
                AnnotatedTypeMirror alteredType) {

            AnnotatedTypeMirror treeType = getAnnotatedType(tree);
            if (!nonValueAnno(treeType)) {
                AnnotationMirror treeAnno = getValueAnnotation(treeType);

                String anno = "org.checkerframework.common.value.qual.";

                if (castType.equals(String.class)) {
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
                } else if (isNumberAnnotation(treeAnno)) {
                    List<Number> valuesToCast;
                    valuesToCast = AnnotationUtils.getElementValueArray(
                            treeAnno, "value", Number.class, true);

                    HashSet<Object> newValues = new HashSet<Object>();

                    if (castType.equals(double.class)) {
                        for (Number n : valuesToCast) {
                            newValues.add(new Double(n.doubleValue()));
                        }
                        anno += "DoubleVal";
                    }

                    else if (castType.equals(int.class)) {
                        for (Number n : valuesToCast) {
                            newValues.add(new Long(n.intValue()));
                        }
                        anno += "IntVal";
                    }

                    else if (castType.equals(long.class)) {
                        for (Number n : valuesToCast) {
                            newValues.add(new Long(n.longValue()));
                        }
                        anno += "IntVal";
                    }

                    else if (castType.equals(float.class)) {
                        for (Number n : valuesToCast) {
                            newValues.add(new Double(n.floatValue()));
                        }
                        anno += "DoubleVal";
                    }

                    else if (castType.equals(short.class)) {
                        for (Number n : valuesToCast) {
                            newValues.add(new Long(n.shortValue()));
                        }
                        anno += "IntVal";
                    }

                    else if (castType.equals(byte.class)) {
                        for (Number n : valuesToCast) {
                            newValues.add(new Long(n.byteValue()));
                        }
                        anno += "IntVal";
                    }

                    else if (castType.equals(char.class)) {
                        for (Number n : valuesToCast) {
                            newValues.add(new Long(n.intValue()));
                        }
                        anno += "IntVal";
                    }
                    alteredType.replaceAnnotation(createAnnotation(anno,
                            newValues));
                } else {
                    // If the expression to cast has a BoolVal anno or a
                    // StringVal anno,
                    // just copy the annotation of the expression.
                    alteredType.replaceAnnotation(treeAnno);
                }
            }
        }

        /**
         * Extract annotation in the Constant Value Checker's type hierarchy (if
         * one exists)
         *
         * @param atm
         *
         * @return
         */
        private AnnotationMirror getValueAnnotation(AnnotatedTypeMirror atm) {
            AnnotationMirror anno = atm.getAnnotationInHierarchy(UNKNOWNVAL);
            if (anno == null) {
                anno = atm.getEffectiveAnnotationInHierarchy(UNKNOWNVAL);
            }
            return anno;
        }

        /**
         * Check that the annotation in the Value Checker hierarchy has a value
         * of some kind.
         *
         * @param mirror
         *            the AnnotatedTypeMirror to check
         *
         * @return true if the AnnotatedTypeMirror contains the UnknownVal,
         *         ArrayLen, to BottomVal, false otherwise
         */
        private boolean nonValueAnno(AnnotatedTypeMirror mirror) {
            AnnotationMirror valueAnno = getValueAnnotation(mirror);
            return AnnotationUtils.areSameIgnoringValues(valueAnno, UNKNOWNVAL)
                    || AnnotationUtils.areSameByClass(valueAnno,
                            BottomVal.class)
                    || AnnotationUtils
                            .areSameIgnoringValues(
                                    mirror.getAnnotationInHierarchy(ARRAYLEN),
                                    ARRAYLEN);
        }
    }

    /**
     *
     * @param anno
     *
     * @return true if anno is an IntVal or DoubleVal, false otheriwse
     */
    private boolean isNumberAnnotation(AnnotationMirror anno) {
        return AnnotationUtils.areSameIgnoringValues(anno, INTVAL)
                || AnnotationUtils.areSameIgnoringValues(anno, DOUBLEVAL);
    }

    public AnnotationMirror createIntValAnnotation(List<Long> intValues) {
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv,
                IntVal.class);
        builder.setValue("value", intValues);
        return builder.build();
    }

    public AnnotationMirror createDoubleValAnnotation(List<Double> doubleValues) {
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv,
                DoubleVal.class);
        builder.setValue("value", doubleValues);
        return builder.build();
    }

    public AnnotationMirror createStringAnnotation(List<String> values) {
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv,
                StringVal.class);
        builder.setValue("value", values);
        return builder.build();
    }

    public AnnotationMirror createBooleanAnnotation(List<Boolean> values) {
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv,
                BoolVal.class);
        builder.setValue("value", values);
        return builder.build();
    }
    public AnnotationMirror createCharAnnotation(List<Character> values) {
        List<Long> longValues = new ArrayList<>();
        for(char value: values){
            longValues.add((long) value);
        }
        AnnotationBuilder builder = new AnnotationBuilder(processingEnv,
                IntVal.class);
        builder.setValue("value", longValues);
        return builder.build();
    }
    private AnnotationMirror createStringValAnnotationMirror(List<String> values) {
        if (values.isEmpty()) {
            return UNKNOWNVAL;
        }
        return createStringAnnotation(values);
    }

    private AnnotationMirror createNumberAnnotationMirror(List<Number> values) {
        if (values.isEmpty()) {
            return UNKNOWNVAL;
        }
        Number first = values.get(0);
        if (first instanceof Integer || first instanceof Short
                || first instanceof Long || first instanceof Byte) {
            List<Long> intValues = new ArrayList<>();
            for (Number number : values) {
                intValues.add(number.longValue());
            }
            return createIntValAnnotation(intValues);
        }
        if (first instanceof Double || first instanceof Float) {
            List<Double> intValues = new ArrayList<>();
            for (Number number : values) {
                intValues.add(number.doubleValue());
            }
            return createDoubleValAnnotation(intValues);
        }
        throw new UnsupportedOperationException("ValueAnnotatedTypeFactory: unexpected class: "+first.getClass());
    }

    private AnnotationMirror createBooleanAnnotationMirror(List<Boolean> values) {
        if (values.isEmpty()) {
            return UNKNOWNVAL;
        }
        return createBooleanAnnotation(values);

    }
    
    public static List<Integer> getArrayLength(AnnotationMirror arrayAnno) {
        return AnnotationUtils.getElementValueArray(
                arrayAnno, "value", Integer.class, true); 
    }
    
    public static List<Character> getCharValues(AnnotationMirror intAnno) {
        if (intAnno != null) {
            List<Long> intValues = AnnotationUtils.getElementValueArray(
                    intAnno, "value", Long.class, true);
            List<Character> charValues = new ArrayList<Character>();
            for (Long i : intValues) {
                charValues.add((char) i.intValue());
            }
            return charValues;
        }
        return new ArrayList<>();
    }
    
    public static List<Boolean> getBooleanValues(AnnotationMirror boolAnno) {
        if (boolAnno != null) {
            List<Boolean> boolValues = AnnotationUtils.getElementValueArray(
                    boolAnno, "value", Boolean.class, true);
            Set<Boolean> boolSet = new TreeSet<>(boolValues);
            if (boolSet.size() > 1) {
                // boolSet={true,false};
                return new ArrayList<>();
            }
            if (boolSet.size() == 0) {
                // boolSet={};
                return new ArrayList<>();
            }
            if (boolSet.size() == 1) {
                // boolSet={true} or boolSet={false}
                return new ArrayList<>(boolSet);
            }
        }
        return new ArrayList<>();
    }
}
