package org.checkerframework.common.value;

import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;

import org.checkerframework.checker.signature.qual.BinaryName;
import org.checkerframework.checker.signature.qual.Identifier;
import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.ArrayLenRange;
import org.checkerframework.common.value.qual.StaticallyExecutable;
import org.checkerframework.common.value.util.NumberUtils;
import org.checkerframework.common.value.util.Range;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeSystemError;
import org.checkerframework.javacutil.TypesUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Name;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

/** The TreeAnnotator for this AnnotatedTypeFactory. It adds/replaces annotations. */
class ValueTreeAnnotator extends TreeAnnotator {

    /** The type factory to use. Shadows the field from the superclass with a more specific type. */
    @SuppressWarnings("HidingField")
    protected final ValueAnnotatedTypeFactory atypeFactory;

    /**
     * The domain of the Constant Value Checker: the types for which it estimates possible values.
     */
    protected static final Set<String> COVERED_CLASS_STRINGS =
            Collections.unmodifiableSet(
                    new HashSet<>(
                            Arrays.asList(
                                    "int",
                                    "java.lang.Integer",
                                    "double",
                                    "java.lang.Double",
                                    "byte",
                                    "java.lang.Byte",
                                    "java.lang.String",
                                    "char",
                                    "java.lang.Character",
                                    "float",
                                    "java.lang.Float",
                                    "boolean",
                                    "java.lang.Boolean",
                                    "long",
                                    "java.lang.Long",
                                    "short",
                                    "java.lang.Short",
                                    "char[]")));

    /**
     * Create a ValueTreeAnnotator.
     *
     * @param atypeFactory the ValueAnnotatedTypeFactory to use
     */
    public ValueTreeAnnotator(ValueAnnotatedTypeFactory atypeFactory) {
        super(atypeFactory);
        this.atypeFactory = atypeFactory;
    }

    @Override
    public Void visitNewArray(NewArrayTree tree, AnnotatedTypeMirror type) {

        List<? extends ExpressionTree> dimensions = tree.getDimensions();
        List<? extends ExpressionTree> initializers = tree.getInitializers();

        // Array construction can provide dimensions or use an initializer.

        // Dimensions provided
        if (!dimensions.isEmpty()) {
            handleDimensions(dimensions, (AnnotatedTypeMirror.AnnotatedArrayType) type);
        } else {
            // Initializer used
            handleInitializers(initializers, (AnnotatedTypeMirror.AnnotatedArrayType) type);

            AnnotationMirror newQual;
            Class<?> clazz = TypesUtils.getClassFromType(type.getUnderlyingType());
            String stringVal = null;
            if (clazz == char[].class) {
                stringVal = getCharArrayStringVal(initializers);
            }

            if (stringVal != null) {
                newQual = atypeFactory.createStringAnnotation(Collections.singletonList(stringVal));
                type.replaceAnnotation(newQual);
            }
        }

        return null;
    }

    /**
     * Recursive method to handle array initializations. Recursively descends the initializer to
     * find each dimension's size and create the appropriate annotation for it.
     *
     * <p>If the annotation of the dimension is {@code @IntVal}, create an {@code @ArrayLen} with
     * the same set of possible values. If the annotation is {@code @IntRange}, create an
     * {@code @ArrayLenRange}. If the annotation is {@code @BottomVal}, create an {@code @BottomVal}
     * instead. In other cases, no annotations are created.
     *
     * @param dimensions a list of ExpressionTrees where each ExpressionTree is a specifier of the
     *     size of that dimension
     * @param type the AnnotatedTypeMirror of the array
     */
    private void handleDimensions(
            List<? extends ExpressionTree> dimensions,
            AnnotatedTypeMirror.AnnotatedArrayType type) {
        if (dimensions.size() > 1) {
            handleDimensions(
                    dimensions.subList(1, dimensions.size()),
                    (AnnotatedTypeMirror.AnnotatedArrayType) type.getComponentType());
        }
        AnnotationMirror dimType =
                atypeFactory
                        .getAnnotatedType(dimensions.get(0))
                        .getAnnotationInHierarchy(atypeFactory.UNKNOWNVAL);

        if (AnnotationUtils.areSameByName(dimType, atypeFactory.BOTTOMVAL)) {
            type.replaceAnnotation(atypeFactory.BOTTOMVAL);
        } else {
            RangeOrListOfValues rolv = null;
            if (atypeFactory.isIntRange(dimType)) {
                rolv = new RangeOrListOfValues(atypeFactory.getRange(dimType));
            } else if (AnnotationUtils.areSameByName(
                    dimType, ValueAnnotatedTypeFactory.INTVAL_NAME)) {
                rolv =
                        new RangeOrListOfValues(
                                RangeOrListOfValues.convertLongsToInts(
                                        atypeFactory.getIntValues(dimType)));
            }
            if (rolv != null) {
                AnnotationMirror newQual = rolv.createAnnotation(atypeFactory);
                type.replaceAnnotation(newQual);
            }
        }
    }

    /**
     * Adds the ArrayLen/ArrayLenRange annotation from the array initializers to {@code type}.
     *
     * <p>If type is a multi-dimensional array, the initializers might also contain arrays, so this
     * method adds the annotations for those initializers, too.
     *
     * @param initializers initializer trees
     * @param type array type to which annotations are added
     */
    private void handleInitializers(
            List<? extends ExpressionTree> initializers,
            AnnotatedTypeMirror.AnnotatedArrayType type) {

        type.replaceAnnotation(
                atypeFactory.createArrayLenAnnotation(
                        Collections.singletonList(initializers.size())));

        if (type.getComponentType().getKind() != TypeKind.ARRAY) {
            return;
        }

        // A list of arrayLens.  arrayLenOfDimensions.get(i) is the array lengths for the ith
        // dimension.
        List<RangeOrListOfValues> arrayLenOfDimensions = new ArrayList<>();
        for (ExpressionTree init : initializers) {
            AnnotatedTypeMirror componentType = atypeFactory.getAnnotatedType(init);
            int dimension = 0;
            while (componentType.getKind() == TypeKind.ARRAY) {
                RangeOrListOfValues rolv = null;
                if (dimension < arrayLenOfDimensions.size()) {
                    rolv = arrayLenOfDimensions.get(dimension);
                }
                AnnotationMirror arrayLen = componentType.getAnnotation(ArrayLen.class);
                if (arrayLen != null) {
                    List<Integer> currentLengths = atypeFactory.getArrayLength(arrayLen);
                    if (rolv != null) {
                        rolv.addAll(currentLengths);
                    } else {
                        arrayLenOfDimensions.add(new RangeOrListOfValues(currentLengths));
                    }
                } else {
                    // Check for an arrayLenRange annotation
                    AnnotationMirror arrayLenRangeAnno =
                            componentType.getAnnotation(ArrayLenRange.class);
                    Range range;
                    if (arrayLenRangeAnno != null) {
                        range = atypeFactory.getRange(arrayLenRangeAnno);
                    } else {
                        range = Range.EVERYTHING;
                    }
                    if (rolv != null) {
                        rolv.add(range);
                    } else {
                        arrayLenOfDimensions.add(new RangeOrListOfValues(range));
                    }
                }

                dimension++;
                componentType =
                        ((AnnotatedTypeMirror.AnnotatedArrayType) componentType).getComponentType();
            }
        }

        AnnotatedTypeMirror componentType = type.getComponentType();
        int i = 0;
        while (componentType.getKind() == TypeKind.ARRAY && i < arrayLenOfDimensions.size()) {
            RangeOrListOfValues rolv = arrayLenOfDimensions.get(i);
            componentType.addAnnotation(rolv.createAnnotation(atypeFactory));
            componentType =
                    ((AnnotatedTypeMirror.AnnotatedArrayType) componentType).getComponentType();
            i++;
        }
    }

    /** Convert a char array to a String. Return null if unable to convert. */
    private String getCharArrayStringVal(List<? extends ExpressionTree> initializers) {
        boolean allLiterals = true;
        StringBuilder stringVal = new StringBuilder();
        for (ExpressionTree e : initializers) {
            Range range =
                    atypeFactory.getRange(
                            atypeFactory
                                    .getAnnotatedType(e)
                                    .getAnnotationInHierarchy(atypeFactory.UNKNOWNVAL));
            if (range != null && range.from == range.to) {
                char charVal = (char) range.from;
                stringVal.append(charVal);
            } else {
                allLiterals = false;
                break;
            }
        }
        if (allLiterals) {
            return stringVal.toString();
        }
        // If any part of the initializer isn't known,
        // the stringval isn't known.
        return null;
    }

    @Override
    public Void visitTypeCast(TypeCastTree tree, AnnotatedTypeMirror atm) {
        if (handledByValueChecker(atm)) {
            AnnotationMirror oldAnno =
                    atypeFactory
                            .getAnnotatedType(tree.getExpression())
                            .getAnnotationInHierarchy(atypeFactory.UNKNOWNVAL);
            if (oldAnno == null) {
                return null;
            }
            TypeMirror newType = atm.getUnderlyingType();
            AnnotationMirror newAnno;
            Range range;

            if (TypesUtils.isString(newType) || newType.getKind() == TypeKind.ARRAY) {
                // Strings and arrays do not allow conversions
                newAnno = oldAnno;
            } else if (atypeFactory.isIntRange(oldAnno)
                    && (range = atypeFactory.getRange(oldAnno))
                            .isWiderThan(ValueAnnotatedTypeFactory.MAX_VALUES)) {
                Class<?> newClass = TypesUtils.getClassFromType(newType);
                if (newClass == String.class) {
                    newAnno = atypeFactory.UNKNOWNVAL;
                } else if (newClass == Boolean.class || newClass == boolean.class) {
                    throw new UnsupportedOperationException(
                            "ValueAnnotatedTypeFactory: can't convert int to boolean");
                } else {
                    newAnno =
                            atypeFactory.createIntRangeAnnotation(
                                    NumberUtils.castRange(newType, range));
                }
            } else {
                List<?> values =
                        ValueCheckerUtils.getValuesCastedToType(oldAnno, newType, atypeFactory);
                newAnno = atypeFactory.createResultingAnnotation(atm.getUnderlyingType(), values);
            }
            atm.addMissingAnnotations(Collections.singleton(newAnno));
        } else if (atm.getKind() == TypeKind.ARRAY) {
            if (tree.getExpression().getKind() == Tree.Kind.NULL_LITERAL) {
                atm.addMissingAnnotations(Collections.singleton(atypeFactory.BOTTOMVAL));
            }
        }
        return null;
    }

    /**
     * Get the "value" field of the given annotation, casted to the given type. Empty list means no
     * value is possible (dead code). Null means no information is known -- any value is possible.
     */
    private List<?> getValues(AnnotatedTypeMirror type, TypeMirror castTo) {
        AnnotationMirror anno = type.getAnnotationInHierarchy(atypeFactory.UNKNOWNVAL);
        if (anno == null) {
            // If type is an AnnotatedTypeVariable (or other type without a primary annotation)
            // then anno will be null. It would be safe to use the annotation on the upper
            // bound; however, unless the upper bound was explicitly annotated, it will be
            // unknown.  AnnotatedTypes.findEffectiveAnnotationInHierarchy(, toSearch, top)
            return null;
        }
        return ValueCheckerUtils.getValuesCastedToType(anno, castTo, atypeFactory);
    }

    @Override
    public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
        if (!handledByValueChecker(type)) {
            return null;
        }
        Object value = tree.getValue();
        switch (tree.getKind()) {
            case BOOLEAN_LITERAL:
                AnnotationMirror boolAnno =
                        atypeFactory.createBooleanAnnotation(
                                Collections.singletonList((Boolean) value));
                type.replaceAnnotation(boolAnno);
                return null;

            case CHAR_LITERAL:
                AnnotationMirror charAnno =
                        atypeFactory.createCharAnnotation(
                                Collections.singletonList((Character) value));
                type.replaceAnnotation(charAnno);
                return null;

            case DOUBLE_LITERAL:
            case FLOAT_LITERAL:
            case INT_LITERAL:
            case LONG_LITERAL:
                AnnotationMirror numberAnno =
                        atypeFactory.createNumberAnnotationMirror(
                                Collections.singletonList((Number) value));
                type.replaceAnnotation(numberAnno);
                return null;
            case STRING_LITERAL:
                AnnotationMirror stringAnno =
                        atypeFactory.createStringAnnotation(
                                Collections.singletonList((String) value));
                type.replaceAnnotation(stringAnno);
                return null;
            default:
                return null;
        }
    }

    /**
     * Given a MemberSelectTree representing a method call, return true if the method's declaration
     * is annotated with {@code @StaticallyExecutable}.
     */
    private boolean methodIsStaticallyExecutable(Element method) {
        return atypeFactory.getDeclAnnotation(method, StaticallyExecutable.class) != null;
    }

    /**
     * Returns the Range of the Math.min or Math.max method, or null if the argument is none of
     * these methods or their arguments are not annotated in ValueChecker hierarchy.
     *
     * @return the Range of the Math.min or Math.max method, or null if the argument is none of
     *     these methods or their arguments are not annotated in ValueChecker hierarchy
     */
    private Range getRangeForMathMinMax(MethodInvocationTree tree) {
        if (atypeFactory.getMethodIdentifier().isMathMin(tree, atypeFactory.getProcessingEnv())) {
            AnnotatedTypeMirror arg1 = atypeFactory.getAnnotatedType(tree.getArguments().get(0));
            AnnotatedTypeMirror arg2 = atypeFactory.getAnnotatedType(tree.getArguments().get(1));
            Range rangeArg1 =
                    atypeFactory.getRange(arg1.getAnnotationInHierarchy(atypeFactory.UNKNOWNVAL));
            Range rangeArg2 =
                    atypeFactory.getRange(arg2.getAnnotationInHierarchy(atypeFactory.UNKNOWNVAL));
            if (rangeArg1 != null && rangeArg2 != null) {
                return rangeArg1.min(rangeArg2);
            }
        } else if (atypeFactory
                .getMethodIdentifier()
                .isMathMax(tree, atypeFactory.getProcessingEnv())) {
            AnnotatedTypeMirror arg1 = atypeFactory.getAnnotatedType(tree.getArguments().get(0));
            AnnotatedTypeMirror arg2 = atypeFactory.getAnnotatedType(tree.getArguments().get(1));
            Range rangeArg1 =
                    atypeFactory.getRange(arg1.getAnnotationInHierarchy(atypeFactory.UNKNOWNVAL));
            Range rangeArg2 =
                    atypeFactory.getRange(arg2.getAnnotationInHierarchy(atypeFactory.UNKNOWNVAL));
            if (rangeArg1 != null && rangeArg2 != null) {
                return rangeArg1.max(rangeArg2);
            }
        }
        return null;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree tree, AnnotatedTypeMirror type) {
        if (type.hasAnnotation(atypeFactory.UNKNOWNVAL)) {
            Range range = getRangeForMathMinMax(tree);
            if (range != null) {
                type.replaceAnnotation(atypeFactory.createIntRangeAnnotation(range));
            }
        }

        if (atypeFactory
                .getMethodIdentifier()
                .isArraysCopyOfInvocation(tree, atypeFactory.getProcessingEnv())) {
            List<? extends ExpressionTree> args = tree.getArguments();
            Range range =
                    ValueCheckerUtils.getPossibleValues(
                            atypeFactory.getAnnotatedType(args.get(1)), atypeFactory);
            if (range != null) {
                type.replaceAnnotation(atypeFactory.createArrayLenRangeAnnotation(range));
            }
        }

        if (!methodIsStaticallyExecutable(TreeUtils.elementFromUse(tree))
                || !handledByValueChecker(type)) {
            return null;
        }

        if (atypeFactory
                .getMethodIdentifier()
                .isStringLengthInvocation(tree, atypeFactory.getProcessingEnv())) {
            AnnotatedTypeMirror receiverType = atypeFactory.getReceiverType(tree);
            AnnotationMirror resultAnno =
                    atypeFactory.createArrayLengthResultAnnotation(receiverType);
            if (resultAnno != null) {
                type.replaceAnnotation(resultAnno);
            }
            return null;
        }

        if (atypeFactory
                .getMethodIdentifier()
                .isArrayGetLengthInvocation(tree, atypeFactory.getProcessingEnv())) {
            List<? extends ExpressionTree> args = tree.getArguments();
            AnnotatedTypeMirror argType = atypeFactory.getAnnotatedType(args.get(0));
            AnnotationMirror resultAnno = atypeFactory.createArrayLengthResultAnnotation(argType);
            if (resultAnno != null) {
                type.replaceAnnotation(resultAnno);
            }
            return null;
        }

        // Get argument values
        List<? extends ExpressionTree> arguments = tree.getArguments();
        ArrayList<List<?>> argValues;
        if (arguments.isEmpty()) {
            argValues = null;
        } else {
            argValues = new ArrayList<>(arguments.size());
            for (ExpressionTree argument : arguments) {
                AnnotatedTypeMirror argType = atypeFactory.getAnnotatedType(argument);
                List<?> values = getValues(argType, argType.getUnderlyingType());
                if (values == null || values.isEmpty()) {
                    // Values aren't known, so don't try to evaluate the method.
                    return null;
                }
                argValues.add(values);
            }
        }

        // Get receiver values
        AnnotatedTypeMirror receiver = atypeFactory.getReceiverType(tree);
        List<?> receiverValues;

        if (receiver != null && !ElementUtils.isStatic(TreeUtils.elementFromUse(tree))) {
            receiverValues = getValues(receiver, receiver.getUnderlyingType());
            if (receiverValues == null || receiverValues.isEmpty()) {
                // Values aren't known, so don't try to evaluate the method.
                return null;
            }
        } else {
            receiverValues = null;
        }

        // Evaluate method
        List<?> returnValues =
                atypeFactory.evaluator.evaluateMethodCall(argValues, receiverValues, tree);
        if (returnValues == null) {
            return null;
        }
        AnnotationMirror returnType =
                atypeFactory.createResultingAnnotation(type.getUnderlyingType(), returnValues);
        type.replaceAnnotation(returnType);

        return null;
    }

    @Override
    public Void visitNewClass(NewClassTree tree, AnnotatedTypeMirror type) {
        if (!methodIsStaticallyExecutable(TreeUtils.elementFromUse(tree))
                || !handledByValueChecker(type)) {
            return null;
        }

        // get argument values
        List<? extends ExpressionTree> arguments = tree.getArguments();
        ArrayList<List<?>> argValues;
        if (arguments.isEmpty()) {
            argValues = null;
        } else {
            argValues = new ArrayList<>(arguments.size());
            for (ExpressionTree argument : arguments) {
                AnnotatedTypeMirror argType = atypeFactory.getAnnotatedType(argument);
                List<?> values = getValues(argType, argType.getUnderlyingType());
                if (values == null || values.isEmpty()) {
                    // Values aren't known, so don't try to evaluate the method.
                    return null;
                }
                argValues.add(values);
            }
        }

        // Evaluate method
        List<?> returnValues =
                atypeFactory.evaluator.evaluteConstructorCall(
                        argValues, tree, type.getUnderlyingType());
        if (returnValues == null) {
            return null;
        }
        AnnotationMirror returnType =
                atypeFactory.createResultingAnnotation(type.getUnderlyingType(), returnValues);
        type.replaceAnnotation(returnType);

        return null;
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree tree, AnnotatedTypeMirror type) {
        visitFieldAccess(tree, type);
        visitEnumConstant(tree, type);

        if (TreeUtils.isArrayLengthAccess(tree)) {
            // The field access is to the length field, as in "someArrayExpression.length"
            AnnotatedTypeMirror receiverType = atypeFactory.getAnnotatedType(tree.getExpression());
            if (receiverType.getKind() == TypeKind.ARRAY) {
                AnnotationMirror resultAnno =
                        atypeFactory.createArrayLengthResultAnnotation(receiverType);
                if (resultAnno != null) {
                    type.replaceAnnotation(resultAnno);
                }
            }
        }
        return null;
    }

    /**
     * Visit a tree that might be a field access.
     *
     * @param tree a tree that might be a field access. It is either a MemberSelectTree or an
     *     IdentifierTree (if the programmer omitted the leading `this.`).
     * @param type its type
     */
    private void visitFieldAccess(ExpressionTree tree, AnnotatedTypeMirror type) {
        if (!TreeUtils.isFieldAccess(tree) || !handledByValueChecker(type)) {
            return;
        }

        VariableElement fieldElement = (VariableElement) TreeUtils.elementFromTree(tree);
        Object value = fieldElement.getConstantValue();
        if (value != null) {
            // The field is a compile-time constant.
            type.replaceAnnotation(
                    atypeFactory.createResultingAnnotation(type.getUnderlyingType(), value));
            return;
        }
        if (ElementUtils.isStatic(fieldElement) && ElementUtils.isFinal(fieldElement)) {
            // The field is static and final, but its declaration does not initialize it to a
            // compile-time constant.  Obtain its value reflectively.
            Element classElement = fieldElement.getEnclosingElement();
            if (classElement != null) {
                @SuppressWarnings("signature" // TODO: bug in ValueAnnotatedTypeFactory.
                // evaluateStaticFieldAccess requires a @ClassGetName but this passes a
                // @FullyQualifiedName.  They differ for inner classes.
                )
                @BinaryName String classname = ElementUtils.getQualifiedClassName(classElement).toString();
                @SuppressWarnings(
                        "signature") // https://tinyurl.com/cfissue/658 for Name.toString()
                @Identifier String fieldName = fieldElement.getSimpleName().toString();
                value =
                        atypeFactory.evaluator.evaluateStaticFieldAccess(
                                classname, fieldName, tree);
                if (value != null) {
                    type.replaceAnnotation(
                            atypeFactory.createResultingAnnotation(
                                    type.getUnderlyingType(), value));
                }
                return;
            }
        }

        return;
    }

    /** Returns true iff the given type is in the domain of the Constant Value Checker. */
    private boolean handledByValueChecker(AnnotatedTypeMirror type) {
        TypeMirror tm = type.getUnderlyingType();
        /* TODO: compare performance to the more readable.
        return TypesUtils.isPrimitive(tm)
                || TypesUtils.isBoxedPrimitive(tm)
                || TypesUtils.isString(tm)
                || tm.toString().equals("char[]"); // Why?
        */
        return COVERED_CLASS_STRINGS.contains(tm.toString());
    }

    @Override
    public Void visitConditionalExpression(
            ConditionalExpressionTree node, AnnotatedTypeMirror annotatedTypeMirror) {
        // Work around for https://github.com/typetools/checker-framework/issues/602.
        annotatedTypeMirror.replaceAnnotation(atypeFactory.UNKNOWNVAL);
        return null;
    }

    // An IdentifierTree can be a local variable (including formals, exception parameters, etc.) or
    // an implicit field access (where `this.` is omitted).
    // A field access is always an IdentifierTree or MemberSelectTree.
    @Override
    public Void visitIdentifier(IdentifierTree tree, AnnotatedTypeMirror type) {
        visitFieldAccess(tree, type);
        visitEnumConstant(tree, type);
        return null;
    }

    /**
     * Default the type of an enum constant {@code E.V} to {@code @StringVal("V")}. Does nothing if
     * the argument is not an enum constant.
     *
     * @param tree an Identifier or MemberSelect tree that might be an enum
     * @param type the type of that tree
     */
    private void visitEnumConstant(ExpressionTree tree, AnnotatedTypeMirror type) {
        Element decl = TreeUtils.elementFromTree(tree);
        if (decl.getKind() != ElementKind.ENUM_CONSTANT) {
            return;
        }

        Name id;
        switch (tree.getKind()) {
            case MEMBER_SELECT:
                id = ((MemberSelectTree) tree).getIdentifier();
                break;
            case IDENTIFIER:
                id = ((IdentifierTree) tree).getName();
                break;
            default:
                throw new TypeSystemError(
                        "unexpected kind of enum constant use tree: " + tree.getKind());
        }
        AnnotationMirror stringVal =
                atypeFactory.createStringAnnotation(Collections.singletonList(id.toString()));
        type.replaceAnnotation(stringVal);
    }
}
