package org.checkerframework.common.value;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
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
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.framework.type.typeannotator.ImplicitsTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.ListTypeAnnotator;
import org.checkerframework.framework.type.typeannotator.TypeAnnotator;
import org.checkerframework.framework.util.AnnotationBuilder;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy;
import org.checkerframework.framework.util.MultiGraphQualifierHierarchy.MultiGraphFactory;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCUnary;

/**
 * @author plvines
 *
 *         AnnotatedTypeFactory for the Value type system.
 *
 */
@TypeQualifiers({ ArrayLen.class, BoolVal.class, DoubleVal.class, IntVal.class,
        StringVal.class, BottomVal.class, UnknownVal.class }) public class ValueAnnotatedTypeFactory
        extends BaseAnnotatedTypeFactory {

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
    /**
     * propTreeCache ensures that the ValueATF terminates on compound binary/unary trees.
     */
    private final Map<Tree, AnnotatedTypeMirror> propTreeCache = createLRUCache(200);
    
    /** should this type factory report warnings? **/
    private boolean reportWarnings = true;


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
    
    public void disableWarnings(){
        reportWarnings = false;
    }
    public void enableWarnings(){
        reportWarnings = true;
    }

    @Override
    public CFTransfer createFlowTransferFunction(
            CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
        // The super implementation uses the name of the checker
        // to reflectively create a transfer with the checker name followed
        // by Transfer. Since this factory is intended to be used with
        // any checker, explicitly create the default CFTransfer
        return new CFTransfer(analysis);
    }

    @Override
    public AnnotatedTypeMirror getAnnotatedType(Tree tree) {
        if(propTreeCache.containsKey(tree)){
            return propTreeCache.get(tree).deepCopy();
        }
        AnnotatedTypeMirror anno = super.getAnnotatedType(tree);
        if(tree instanceof JCBinary ||
                tree instanceof JCUnary){
            propTreeCache.put(tree, anno.deepCopy());
        }
        return anno;
    }

    /**
     * Creates an annotation of the name given with the set of values given.
     * Issues a checker warning and return UNKNOWNVAL if values.size &gt;
     * MAX_VALUES
     *
     * @param name
     * @param values
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

    @Override protected MultiGraphQualifierHierarchy.MultiGraphFactory createQualifierHierarchyFactory() {
        return new MultiGraphQualifierHierarchy.MultiGraphFactory(this);
    }

    @Override public QualifierHierarchy createQualifierHierarchy(
            MultiGraphFactory factory) {
        return new ValueQualifierHierarchy(factory);
    }

    @Override
    protected TypeAnnotator createTypeAnnotator() {
        return new ListTypeAnnotator(
                new ValueTypeAnnotator(this),
                super.createTypeAnnotator()
        );
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
        @Override
        public Void visitNull(AnnotatedNullType type, Void p) {
            HashSet<String> values = new HashSet<String>();
            values.add("null");
            AnnotationMirror newQual = createAnnotation(
                    "org.checkerframework.common.value.qual.StringVal",
                    values);
            type.replaceAnnotation(newQual);
            return super.visitNull(type, p);
        }

        /**
         * If any constant-value annotation has &gt; MAX_VALUES number of values provided, treats the value as UnknownVal.
         * Works together with ValueVisitor.visitAnnotation, which issues a warning to the user in this case.
         */
        private void replaceWithUnknownValIfTooManyValues(AnnotatedTypeMirror atm){
            AnnotationMirror anno = atm.getAnnotationInHierarchy(UNKNOWNVAL);

            if (anno != null && anno.getElementValues().size() > 0) {
                List<Object> values = AnnotationUtils.getElementValueArray(anno, "value", Object.class, false);
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
        @Override public AnnotationMirror leastUpperBound(AnnotationMirror a1,
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
                // If either is UNKNOWNVAL, ARRAYLEN, STRINGVAL, or BOOLEAN then the LUB is
                // UnknownVal
                if (!isNumberAnnotation(a1) || !isNumberAnnotation(a2)) {
                    return UNKNOWNVAL;
                } else {
                    // At this point one of them must be a DoubleVal and one an IntVal
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
        @Override public boolean isSubtype(AnnotationMirror rhs,
                AnnotationMirror lhs) {
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
            // not the same type, but if they are DOUBLEVAL and INTVAL they might still be subtypes
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
        // so it doesn't need PropgationTreeAnnotator. Also, the ValueChecker does not have
        // any implicit annotations, so there is no need to run the ImplicitTreeAnnotator.
        return new ValueTreeAnnotator(this);
    }

    @Override protected QualifierDefaults createQualifierDefaults() {
        QualifierDefaults defaults = super.createQualifierDefaults();
        defaults.addAbsoluteDefault(UNKNOWNVAL, DefaultLocation.OTHERWISE);

        return defaults;
    }

    /**
     * The TreeAnnotator for this AnnotatedTypeFactory
     */
    protected class ValueTreeAnnotator extends TreeAnnotator {

        public ValueTreeAnnotator(ValueAnnotatedTypeFactory factory) {
            super(factory);
        }

        @Override public Void visitNewArray(NewArrayTree tree,
                AnnotatedTypeMirror type) {

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
                    bytes[i] = (byte) (((Integer) ((LiteralTree) e)
                            .getValue()).intValue());
                } else if (e.getKind() == Tree.Kind.CHAR_LITERAL) {
                    bytes[i] = (byte) (((Character) ((LiteralTree) e).getValue())
                            .charValue());                    
                } else {
                    allLiterals = false;
                }
                i++;
            }
            if (allLiterals)
                return new String(bytes);
            //If any part of the initialize isn't know,
            //the stringval isn't known.
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
            //If any part of the initialize isn't know,
            //the stringval isn't known.
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

        @Override public Void visitTypeCast(TypeCastTree tree,
                AnnotatedTypeMirror type) {
            if (isClassCovered(type)) {
                handleCast(tree.getExpression(), getClass(type, tree.getType()), type);
            }
            return super.visitTypeCast(tree, type);
        }

        @Override public Void visitAssignment(AssignmentTree tree,
                AnnotatedTypeMirror type) {
            super.visitAssignment(tree, type);
            return null;

        }

        @Override public Void visitLiteral(LiteralTree tree,
                AnnotatedTypeMirror type) {
            if (isClassCovered(type)) {
                // Handle Boolean Literal
                if (tree.getKind() == Tree.Kind.BOOLEAN_LITERAL) {
                    HashSet<Boolean> values = new HashSet<Boolean>();
                    values.add((Boolean) tree.getValue());
                    AnnotationMirror newQual = createAnnotation(
                            "org.checkerframework.common.value.qual.BoolVal",
                            values);
                    type.replaceAnnotation(newQual);

                    return null;
                }

                // Handle Char Literal
                else if (tree.getKind() == Tree.Kind.CHAR_LITERAL) {
                    HashSet<Long> values = new HashSet<Long>();
                    values.add((long) ((Character) tree.getValue()).charValue());
                    AnnotationMirror newQual = createAnnotation(
                            "org.checkerframework.common.value.qual.IntVal",
                            values);
                    type.replaceAnnotation(newQual);

                    return null;
                }

                // Handle Double Literal
                else if (tree.getKind() == Tree.Kind.DOUBLE_LITERAL) {
                    HashSet<Double> values = new HashSet<Double>();
                    values.add((Double) tree.getValue());
                    AnnotationMirror newQual = createAnnotation(
                            "org.checkerframework.common.value.qual.DoubleVal",
                            values);
                    type.replaceAnnotation(newQual);

                    return null;
                }
                // Handle Float Literal
                else if (tree.getKind() == Tree.Kind.FLOAT_LITERAL) {
                    HashSet<Double> values = new HashSet<Double>();
                    values.add(new Double((Float) tree.getValue()));
                    AnnotationMirror newQual = createAnnotation(
                            "org.checkerframework.common.value.qual.DoubleVal",
                            values);
                    type.replaceAnnotation(newQual);

                    return null;
                }

                // Handle Integer Literal
                else if (tree.getKind() == Tree.Kind.INT_LITERAL) {
                    AnnotationMirror newQual;
                    HashSet<Long> values = new HashSet<Long>();
                    values.add(new Long((Integer) tree.getValue()));
                    newQual = createAnnotation(
                            "org.checkerframework.common.value.qual.IntVal",
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
                            "org.checkerframework.common.value.qual.IntVal",
                            values);
                    type.replaceAnnotation(newQual);

                    return null;
                }

                // Handle a String Literal
                else if (tree.getKind() == Tree.Kind.STRING_LITERAL) {
                    HashSet<String> values = new HashSet<String>();
                    values.add((String) tree.getValue());
                    AnnotationMirror newQual = createAnnotation(
                            "org.checkerframework.common.value.qual.StringVal",
                            values);
                    type.replaceAnnotation(newQual);

                    return null;
                }
                else if ( tree.getKind() == Tree.Kind.NULL_LITERAL){
                    HashSet<String> values = new HashSet<String>();
                    values.add("null");
                    AnnotationMirror newQual = createAnnotation(
                            "org.checkerframework.common.value.qual.StringVal",
                            values);
                    type.replaceAnnotation(newQual);
                }
            }
            // super.visitLiteral(tree, type);
            return null;
        }

        @Override public Void visitUnary(UnaryTree tree,
                AnnotatedTypeMirror type) {
            super.visitUnary(tree, type);

            if (isClassCovered(type)) {
                Tree.Kind operation = tree.getKind();

                AnnotatedTypeMirror argType = getAnnotatedType(tree
                        .getExpression());

                if (!nonValueAnno(argType)) {
                    Class<?> argClass = boxPrimatives(getClass(type, tree));
                    handleCast(tree.getExpression(), getClass(type, tree), argType);

                    AnnotationMirror argAnno = getValueAnnotation(argType);
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
        @Override
        public Void visitCompoundAssignment(CompoundAssignmentTree node,
                AnnotatedTypeMirror p) {
            // TODO Auto-generated method stub
            return super.visitCompoundAssignment(node, p);
        }


        /**
         * This method resolves a unary operator by converting it to a
         * reflective call to one of the operators defined in Operators.java.
         * The values in the annotations of the arguments will be converted to
         * the type of argClass. Thus argClass should always result in a
         * lossless casting (e.g. int to long).
         *
         * @param lhsType
         *            the annotated type mirror of the LHS argument
         * @param rhsType
         *            the annotated type mirror of the RHS argument
         * @param operation
         *            the String name of the operation
         * @param argClass
         *            the Class of the operations arguments (used for reflective
         *            code)
         * @param tree
         *            location to provide to error message
         *
         *
         * @return
         */
        private AnnotationMirror evaluateUnaryOperator(
                AnnotationMirror argAnno, String operation, Class<?> argClass,
                UnaryTree tree) {
            try {
                argClass = boxPrimatives(argClass);
                Class<?>[] argClasses = new Class<?>[] { argClass };
                Method m = Operators.class.getMethod(operation, argClasses);

                List<?> annoValues = getCastedValues(argAnno, argClass, tree);

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
        public Void visitBinary(BinaryTree tree,
                AnnotatedTypeMirror type) {
            if (!isClassCovered(type)) {
                return super.visitBinary(tree, type);
            }
            Tree.Kind operation = tree.getKind();

            AnnotatedTypeMirror lhsType = getAnnotatedType(tree
                    .getLeftOperand());
            AnnotatedTypeMirror rhsType = getAnnotatedType(tree
                    .getRightOperand());
            if (!nonValueAnno(lhsType) && !nonValueAnno(rhsType)) {

                Class<?> resultClass = null;
                AnnotationMirror newAnno = null;

                // Non-Comparison Binary Operation
                if (operation != Tree.Kind.EQUAL_TO
                        && operation != Tree.Kind.NOT_EQUAL_TO
                        && operation != Tree.Kind.GREATER_THAN
                        && operation != Tree.Kind.GREATER_THAN_EQUAL
                        && operation != Tree.Kind.LESS_THAN
                        && operation != Tree.Kind.LESS_THAN_EQUAL) {
                    resultClass = getClass(type, tree);
                    if (resultClass != String.class) {
                        // Convert to the final type
                        List<?> lhs = getCastedValues(lhsType, resultClass, tree);
                        List<?> rhs = getCastedValues(rhsType, resultClass, tree);

                        // Evaluate
                        newAnno = evaluateBinaryOperator(lhs, rhs,
                                operation.toString(), resultClass, tree);
                    } else {
                        // Get values based on underlying type of the left and
                        // right trees, then cast the values to strings.
                        List<?> lhs = convertToStringVal(getCastedValues(
                                lhsType, tree.getLeftOperand()));
                        List<?> rhs = convertToStringVal(getCastedValues(
                                rhsType, tree.getRightOperand()));
                        // Evaluate
                        newAnno = evaluateBinaryOperator(lhs, rhs,
                                operation.toString(), resultClass, tree);
                    }
                }
                // Comparison Binary Operation We're okay to cast
                // everything to DoubleVal *UNLESS* we're
                // comparing StringsVals, so we do This
                // potentially means we could remove the
                // non-double versions of comparisons in
                // Operators.java
                else {
                    if (AnnotationUtils.areSameIgnoringValues(
                            getValueAnnotation(lhsType), STRINGVAL)) {
                        resultClass = getAnnotationValueClass(getValueAnnotation(lhsType));
                    } else if (AnnotationUtils.areSameByClass(
                            getValueAnnotation(lhsType), BoolVal.class)) {
                        resultClass = getAnnotationValueClass(getValueAnnotation(lhsType));
                    }else {
                        resultClass = double.class;
                        handleBinaryCast(tree.getLeftOperand(), lhsType,
                                tree.getRightOperand(), rhsType, double.class);
                    }
                    newAnno = evaluateComparison(lhsType, rhsType,
                            operation.toString(), resultClass, tree);
                }
                if (newAnno != null) {
                    type.replaceAnnotation(newAnno);
                    return null;
                }
            }
            type.replaceAnnotation(UNKNOWNVAL);

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
         * @param castType
         */
        private void handleBinaryCast(ExpressionTree lhs,
                AnnotatedTypeMirror lhsType, ExpressionTree rhs,
                AnnotatedTypeMirror rhsType, Class<?> castType) {
            handleCast(lhs, castType, lhsType);
            handleCast(rhs, castType, rhsType);
        }

        private AnnotationMirror evaluateComparison(
                AnnotatedTypeMirror lhsType, AnnotatedTypeMirror rhsType,
                String operation, Class<?> argClass, BinaryTree tree) {
            try {
                argClass = boxPrimatives(argClass);
                Class<?>[] argClasses = new Class<?>[] { argClass, argClass };
                Method m = Operators.class.getMethod(operation, argClasses);

                List<?> lhsAnnoValues = getCastedValues(lhsType, argClass, tree);
                List<?> rhsAnnoValues = getCastedValues(rhsType, argClass, tree);

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
         * This method resolves a binary operator by converting it to a
         * reflective call to one of the operators defined in Operators.java.
         * The values in the annotations of the arguments will be converted to
         * the type of argClass. Thus argClass should always result in a
         * lossless casting (e.g. int to long).
         *
         * @param lhsAnnoValues
         *            the annotated type mirror of the LHS argument
         * @param rhsAnnoValues
         *            the annotated type mirror of the RHS argument
         * @param operation
         *            the String name of the operation
         * @param argClass
         *            the Class of the operations arguments (used for reflective
         *            code)
         * @param tree
         *            location for error reporting
         * @return
         */
        private AnnotationMirror evaluateBinaryOperator(
                List<?> lhsAnnoValues, List<?> rhsAnnoValues,
                String operation, Class<?> argClass, BinaryTree tree) {
            try {
                argClass = boxPrimatives(argClass);
                Class<?>[] argClasses = new Class<?>[] { argClass, argClass };
                Method m = Operators.class.getMethod(operation, argClasses);

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

        @Override public Void visitMethodInvocation(MethodInvocationTree tree,
                AnnotatedTypeMirror type) {
            super.visitMethodInvocation(tree, type);

            if (isClassCovered(type)
                    && methodIsStaticallyExecutable(TreeUtils
                            .elementFromUse(tree))) {
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

                    boolean isStatic = false;
                    AnnotatedTypeMirror recType = null;
                    Method method = null;

                    try {
                        method = getMethodObject(tree);
                        isStatic = java.lang.reflect.Modifier.isStatic(method
                                .getModifiers());

                        if (!isStatic) {
                            // Method is defined in another class
                            recType = getAnnotatedType(((MemberSelectTree) methodTree)
                                    .getExpression());
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
                    } catch (ClassNotFoundException | UnsupportedClassVersionError e) {
                        if(reportWarnings)
                        checker.report(Result.warning("class.find.failed",
                                (TreeUtils.elementFromUse(tree))
                                        .getEnclosingElement()), tree);

                    } catch (NoSuchMethodException e) {
                        // The class we attempted to getMethod from inside the call to getMethodObject.
                        Element classElem = TreeUtils.elementFromUse(tree).getEnclosingElement();

                        if (classElem == null) {
                            if(reportWarnings)
                            checker.report(Result
                                    .warning("method.find.failed",
                                            ((MemberSelectTree) methodTree)
                                                    .getIdentifier(), argTypes),tree);
                        }
                        else {
                            if(reportWarnings)
                            checker.report(Result
                                    .warning("method.find.failed.in.class",
                                            ((MemberSelectTree) methodTree)
                                                    .getIdentifier(), argTypes,
                                                    classElem), tree);
                        }
                    }
                }
            }
            // Method was not able to be analyzed
            type.replaceAnnotation(UNKNOWNVAL);

            return null;
        }

        /**
         * Method for reflectively obtaining a method object so it can
         * (potentially) be statically executed by the checker for constant
         * propagation
         *
         * @param tree
         * @return the Method object corresponding to the method being invoke in
         *         tree
         * @throws ClassNotFoundException
         * @throws NoSuchMethodException
         */
        private Method getMethodObject(MethodInvocationTree tree)
                throws ClassNotFoundException, NoSuchMethodException {
            Method method;
            ExecutableElement ele = TreeUtils.elementFromUse(tree);
            ele.getEnclosingElement();
            Name clazz = TypesUtils.getQualifiedName((DeclaredType) ele
                    .getEnclosingElement().asType());
            List<? extends VariableElement> paramEles = ele.getParameters();
            List<Class<?>> paramClzz = new ArrayList<>();
            for (Element e : paramEles) {
                TypeMirror pType = ElementUtils.getType(e);
                if (pType.getKind() == TypeKind.ARRAY) {
                    ArrayType pArrayType = (ArrayType) pType;
                    String par = TypesUtils.getQualifiedName(
                            (DeclaredType) pArrayType.getComponentType())
                            .toString();
                    if (par.equals("java.lang.Object")) {
                        paramClzz.add(java.lang.Object[].class);
                    } else if (par.equals("java.lang.String")) {
                        paramClzz.add(java.lang.String[].class);
                    }

                } else {
                    String paramClass = ElementUtils.getType(e).toString();
                    if (paramClass.contains("java")) {
                        paramClzz.add(Class.forName(paramClass));
                    } else {
                        paramClzz.add(getClass(ElementUtils.getType(e), tree));
                    }
                }
            }
            Class<?> clzz = Class.forName(clazz.toString());
            method = clzz.getMethod(ele.getSimpleName().toString(),
                    paramClzz.toArray(new Class<?>[0]));
            return method;
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
         * @param tree
         *            location for error reporting
         *
         * @return an AnnotationMirror of the type specified by retType's
         *         underlyingType and with its value array populated by all the
         *         possible evaluations of method. Or UnknownVal
         */
        private AnnotationMirror evaluateMethod(AnnotatedTypeMirror recType,
                Method method, List<AnnotatedTypeMirror> argTypes,
                AnnotatedTypeMirror retType, MethodInvocationTree tree) {
            List<?> recValues = null;
            // If we are going to need the values of the receiver, get them.
            // Otherwise they can be null because the method is static
            if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                recValues = getCastedValues(recType, tree);
            }

            // Get the values for all the arguments
            ArrayDeque<List<?>> allArgValues = getAllArgumentAnnotationValues(
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
         * @param tree
         *            location for error reporting
         */
        private void evaluateMethodHelper(
                ArrayDeque<List<?>> argArrayDeque,
                ArrayDeque<Object> values, List<?> receiverValues,
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
                    if(reportWarnings)
                    checker.report(Result.warning(
                            "method.evaluation.exception", method, e
                                    .getTargetException().toString()), tree);
                    results = new ArrayList<Object>();
                } catch (ReflectiveOperationException e) {
                    if(reportWarnings)
                    checker.report(
                            Result.warning("method.evaluation.failed", method),
                            tree);
                    results = new ArrayList<Object>();
                    /*
                     * fail by setting the results list to empty. Since we
                     * failed on the invoke, all calls of this method will fail,
                     * so the final results list will be an empty list. That
                     * will cause an UnknownVal annotation to be created, which
                     * seems appropriate here
                     */
                }
            }

            // If there are still lists of argument values left in the deque
            else {

                // Pop an argument off and iterate through all its values
                List<?> argValues = argArrayDeque.pop();
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

        @Override public Void visitNewClass(NewClassTree tree,
                AnnotatedTypeMirror type) {

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
                        Class<?>[] argClasses = getParameterTypes(tree);
                        Class<?> recClass = boxPrimatives(getClass(type, tree));
                        Constructor<?> constructor = recClass
                                .getConstructor(argClasses);

                        AnnotationMirror newAnno = evaluateNewClass(
                                constructor, argTypes, type, tree);
                        if (newAnno != null) {
                            type.replaceAnnotation(newAnno);
                            return null;
                        }
                    } catch (ReflectiveOperationException e) {
                        if(reportWarnings)
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
         * Returns the box primitive type if the passed type is an (unboxed)
         * primitive. Otherwise it returns the passed type
         * 
         * @param type
         * @return
         */
        private Class<?> boxPrimatives(Class<?> type) {
            if(type == byte.class){
                return Byte.class;
            } else if(type == short.class){
                return Short.class;
            } else if(type == int.class){
                return Integer.class;
            } else if(type == long.class){
                return Long.class;
            } else if(type == float.class){
                return Float.class;
            } else if(type == double.class){
                return Double.class;
            } else if(type == char.class){
                return Character.class;
            } else if(type == boolean.class){
                return Boolean.class;
            }  
            return type;
        }
        /**
         * Returns the unboxed primitive type if the passed type is a boxed
         * primitive. Otherwise it returns the passed type
         * 
         * @param type
         * @return
         */
        private Class<?> unboxPrimatives(Class<?> type) {
            if(type == Byte.class){
                return byte.class;
            } else if(type == Short.class){
                return short.class;
            } else if(type == Integer.class){
                return int.class;
            } else if(type == Long.class){
                return long.class;
            } else if(type == Float.class){
                return float.class;
            } else if(type == Double.class){
                return double.class;
            } else if(type == Character.class){
                return char.class;
            } else if(type == Boolean.class){
                return boolean.class;
            }  
            return type;
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
         * @param tree
         *            location for error reporting
         *
         * @return an AnnotationMirror containing all the possible values of the
         *         New call based on combinations of argument values
         */
        private AnnotationMirror evaluateNewClass(Constructor<?> constructor,
                List<AnnotatedTypeMirror> argTypes,
                AnnotatedTypeMirror retType, NewClassTree tree) {
            ArrayDeque<List<?>> allArgValues = getAllArgumentAnnotationValues(
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
         * @param tree
         *            location for error reporting
         */
        private void evaluateNewClassHelper(
                ArrayDeque<List<?>> argArrayDeque,
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
                    if(reportWarnings)
                    checker.report(
                            Result.warning("constructor.invocation.failed"),
                            tree);
                    results = new ArrayList<Object>();
                    /*
                     * fail by setting the results list to empty. Since we
                     * failed on the newInstance, all calls of this constructor
                     * will fail, so the final results list will be an empty
                     * list. That will cause an UnknownVal annotation to be
                     * created, which seems appropriate here
                     */
                }
            }
            // If there are still lists of argument values left in the deque
            else {

                // Pop an argument off and iterate through all its values
                List<?> argValues = argArrayDeque.pop();
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

        @Override public Void visitMemberSelect(MemberSelectTree tree,
                AnnotatedTypeMirror type) {
            /*
             * NOTE: None of the objects, except arrays, being handled by this
             * system possess non-static fields, so I am assuming I can simply
             * reflectively call the fields on a reflectively generated object
             * representing the class
             */
            super.visitMemberSelect(tree, type);
            AnnotatedTypeMirror receiverType = getAnnotatedType(tree
                    .getExpression());

            Element elem = TreeUtils.elementFromUse(tree);
            // KNOWN-LENGTH ARRAYS
            if (AnnotationUtils.areSameIgnoringValues(
                    getValueAnnotation(receiverType), ARRAYLEN)) {
                if (tree.getIdentifier().contentEquals("length")) {
                    type.replaceAnnotation(handleArrayLength(receiverType));
                }
            }

            if (isClassCovered(elem.asType())) {
                if (ElementUtils.isCompileTimeConstant(elem)) {

                    ArrayList<Object> value = new ArrayList<Object>(1);
                    value.add(((VariableElement) elem).getConstantValue());

                    AnnotationMirror newAnno = resultAnnotationHandler(
                            elem.asType(), value, tree);

                    if (newAnno != null) {
                        type.replaceAnnotation(newAnno);
                    } else {
                        type.replaceAnnotation(UNKNOWNVAL);
                    }

                } else if (elem.getKind() == javax.lang.model.element.ElementKind.FIELD
                        && ElementUtils.isStatic(elem)
                        && ElementUtils.isFinal(elem)
                        //If the identifier is class then this is a class literal, not a regular member select
                        && !tree.getIdentifier().toString().equals("class")) {
                    //if an element is not a compile-time constant, we still might be able to find the value
                    //if the type of the field is a wrapped primitive class.
                    //eg Boolean.FALSE
                    TypeMirror retType = elem.asType();
                    AnnotationMirror newAnno = evaluateStaticFieldAccess(
                            tree.getIdentifier(), retType, tree);
                    if (newAnno != null) {
                        type.replaceAnnotation(newAnno);
                    }
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
            AnnotationMirror recAnno = getValueAnnotation(receiverType);

            if (AnnotationUtils.areSameIgnoringValues(recAnno, ARRAYLEN)) {
                List<Integer>lengthInts =  AnnotationUtils.getElementValueArray(recAnno, "value",
                                Integer.class, true);
                HashSet<Long> lengthValues = new HashSet<>();
                for(int i: lengthInts){
                	lengthValues.add((long) i);
                }

                return createAnnotation(
                        "org.checkerframework.common.value.qual.IntVal",
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
         * @param tree
         *            location for error reporting
         *
         * @return
         */
        private AnnotationMirror evaluateStaticFieldAccess(Name fieldName,
                TypeMirror retType, MemberSelectTree tree) {
            String clzzname = "";
            try {
                Element e = InternalUtils.symbol(tree.getExpression());
                if (e == null)
                    return null;
                clzzname = ElementUtils.getQualifiedClassName(e).toString();
                Class<?> recClass = Class.forName(clzzname);
                Field field = recClass.getField(fieldName.toString());
                ArrayList<Object> result = new ArrayList<Object>(1);
                result.add(field.get(recClass));

                return resultAnnotationHandler(retType, result, tree);
            } catch (ClassNotFoundException | UnsupportedClassVersionError e) {
                if(reportWarnings)
                checker.report(Result.warning("class.find.failed", clzzname),
                        tree);
                return null;
            } catch (ReflectiveOperationException e) {
                if(reportWarnings)
                checker.report(Result.warning("field.access.failed", fieldName,
                        clzzname), tree);
                return null;
            }
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
         * Gets a Class object corresponding to the String name stringType. If
         * stringType specifies a primitive or wrapper object, the primitive
         * version is returned ("int" or "java.lang.Integer" return int.class)
         * To get the Class corresponding to the value array a value annotation
         * has for a given type, use getTypeValueClass. (e.g. "int" return
         * Long.class)
         *
         * @param stringType
         * @param tree
         *            location for error reporting
         *
         * @return
         */
        private Class<?> getClass(String stringType, Tree tree) {
            switch (stringType) {
            case "<nulltype>":
                return Object.class;
            }

            try {
                return Class.forName(stringType);
            } catch (ClassNotFoundException | UnsupportedClassVersionError e) {
                if(reportWarnings)
                checker.report(Result.failure("class.find.failed", stringType),
                        tree);
                return Object.class;
            }

        }

        /**
         * 
         * @param typeMirror the underlying type is used
         * @param tree Tree for error reporting
         * @return class object corresponding to the typeMirror passed.
         */
        private Class<?> getClass(AnnotatedTypeMirror typeMirror, Tree tree) {
            TypeMirror type = typeMirror.getUnderlyingType();
            return getClass(type, tree);
        }

        private Class<?> getClass(TypeMirror type, Tree tree) {

            switch (type.getKind()) {
            case INT:
                return int.class;
            case LONG:
                return long.class;
            case SHORT:
                return short.class;
            case BYTE:
                return byte.class;
            case CHAR:
                return char.class;
            case DOUBLE:
                return double.class;
            case FLOAT:
                return float.class;
            case BOOLEAN:
                return boolean.class;
            case ARRAY:
                return getArrayType(((ArrayType) type).getComponentType());
            case DECLARED:
                String stringType = TypesUtils.getQualifiedName(
                        (DeclaredType) type).toString();
                return getClass(stringType, tree);
            default:
                return Object.class;
            }
        }

        private Class<?> getArrayType(TypeMirror componentType) {
            switch (componentType.getKind()) {
            case INT:
                return int[].class;
            case LONG:
                return long[].class;
            case SHORT:
                return short[].class;
            case BYTE:
                return byte[].class;
            case CHAR:
                return char[].class;
            case DOUBLE:
                return double[].class;
            case FLOAT:
                return float[].class;
            case BOOLEAN:
                return boolean[].class;
            default:
                return Object[].class;
            }
        }

        /**
         *
         * @param anno
         * @return the Class object for the value array of an annotation or null
         *         if the annotation has no value array
         */
        private Class<?> getAnnotationValueClass(AnnotationMirror anno) {
            if (AnnotationUtils.areSameIgnoringValues(anno, INTVAL)) {
                return Long.class;
            } else if (AnnotationUtils.areSameIgnoringValues(anno, DOUBLEVAL)) {
                return Double.class;
            } else if (AnnotationUtils.areSameIgnoringValues(anno, BOOLVAL)) {
                return Boolean.class;
            } else if (AnnotationUtils.areSameIgnoringValues(anno, STRINGVAL)) {
                return String.class;
            }

            return null;

        }


        private Class<?>[] getParameterTypes(NewClassTree tree) {
            ExecutableElement e = TreeUtils.elementFromUse(tree);
            Class<?>[] classes = new Class<?>[e.getParameters().size()];
            int i=0;
            for(Element param: e.getParameters()){
                classes[i] = getClass(ElementUtils.getType(param), tree);
                i++;
            }
            return classes;
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
         * @param typeMirror  AnnotatedTypeMirror with values
         * @param castType Class with type to cast to
         * @param tree Tree used for location of errors
         * @return a list of values casted to typeCast
         */
        private List<?> getCastedValues(AnnotatedTypeMirror typeMirror,
                Class<?> castType, Tree tree) {
            AnnotationMirror anno = getValueAnnotation(typeMirror);
            return getCastedValues(anno, castType, tree);
        }
        /**
         * Returns a list of values from the typeMirror cast to castType
         * @param typeMirror  AnnotationMirror with values
         * @param castType Class with type to cast to
         * @param tree Tree used for location of errors
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
            }
            if (values == null) {
                if(reportWarnings)
                checker.report(Result.warning("class.convert.failed", anno,
                        castType), tree);
                values = Collections.EMPTY_LIST;
            }

            return values;
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
            } else if( newClass == Object.class && strings.size() == 1){
                if(strings.get(0).equals("null"))
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
            }else if (newClass == String.class) {
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
         * @param tree
         *            location for error reporting
         *
         * @return an ArrayDeque containing List of Object where each list
         *         corresponds to the annotation values of an
         *         AnnotatedTypeMirror passed in.
         */
        private ArrayDeque<List<?>> getAllArgumentAnnotationValues(
                List<AnnotatedTypeMirror> argTypes, Tree tree) {
            ArrayDeque<List<?>> allArgValues = new ArrayDeque<List<?>>();

            for (AnnotatedTypeMirror a : argTypes) {
                allArgValues.push(getCastedValues(a, tree));
            }
            return allArgValues;
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
                AnnotatedTypeMirror resultType, List<Object> results, Tree tree) {
            return resultAnnotationHandler(getClass(resultType, tree), results);
        }

        private AnnotationMirror resultAnnotationHandler(TypeMirror resultType,
                List<Object> results, Tree tree) {
            return resultAnnotationHandler(getClass(resultType, tree), results);
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
                AnnotationMirror newAnno = createAnnotation(
                        "org.checkerframework.common.value.qual.BoolVal",
                        boolVals);
                return newAnno;

            } else if (resultClass == Double.class
                    || resultClass == double.class) {
                HashSet<Double> doubleVals = new HashSet<Double>(results.size());
                for (Object o : results) {
                    doubleVals.add((Double) o);
                }
                return createAnnotation(
                        "org.checkerframework.common.value.qual.DoubleVal",
                        doubleVals);
            } else if (resultClass == Float.class || resultClass == float.class) {
                HashSet<Double> floatVals = new HashSet<Double>(results.size());
                for (Object o : results) {
                    floatVals.add(new Double((Float) o));
                }
                return createAnnotation(
                        "org.checkerframework.common.value.qual.DoubleVal",
                        floatVals);
            } else if (resultClass == Integer.class || resultClass == int.class
                    || resultClass == Long.class || resultClass == long.class
                    || resultClass == Short.class || resultClass == short.class
                    || resultClass == Byte.class || resultClass == byte.class) {
                HashSet<Long> intVals = new HashSet<Long>(results.size());
                for (Object o : results) {
                    intVals.add(((Number) o).longValue());
                }
                return createAnnotation(
                        "org.checkerframework.common.value.qual.IntVal",
                        intVals);
            } else if (resultClass == char.class
                    || resultClass == Character.class) {
                HashSet<Long> intVals = new HashSet<Long>(results.size());
                for (Object o : results) {
                    intVals.add(new Long(((Character) o).charValue()));
                }
                return createAnnotation(
                        "org.checkerframework.common.value.qual.IntVal",
                        intVals);
            } else if (resultClass == String.class) {
                HashSet<String> stringVals = new HashSet<String>(results.size());
                for (Object o : results) {
                    stringVals.add((String) o);
                }
                return createAnnotation(
                        "org.checkerframework.common.value.qual.StringVal",
                        stringVals);
            } else if (resultClass == byte[].class) {
                HashSet<String> stringVals = new HashSet<String>(results.size());
                for (Object o : results) {
                    stringVals.add(new String((byte[]) o));
                }
                return createAnnotation(
                        "org.checkerframework.common.value.qual.StringVal",
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
                } else if(isNumberAnnotation(treeAnno)) {
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
                    //If the expression to cast has a BoolVal anno or a StringVal anno,
                    //just copy the annotation of the expression.
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
         * Check that the annotation in the Value Checker hierarchy has
         * a value of some kind.
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
}
