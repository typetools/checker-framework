package org.checkerframework.checker.index.upperbound;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.index.IndexUtil;
import org.checkerframework.checker.index.qual.SameLen;
import org.checkerframework.checker.index.samelen.SameLenAnnotatedTypeFactory;
import org.checkerframework.checker.index.upperbound.UBQualifier.LessThanLengthOf;
import org.checkerframework.checker.index.upperbound.UpperBoundUtil.SideEffectError;
import org.checkerframework.checker.index.upperbound.UpperBoundUtil.SideEffectKind;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.dataflow.analysis.FlowExpressions;
import org.checkerframework.dataflow.analysis.FlowExpressions.Receiver;
import org.checkerframework.dataflow.util.PurityUtils;
import org.checkerframework.framework.source.Result;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.util.FlowExpressionParseUtil;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.CollectionUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.InternalUtils;
import org.checkerframework.javacutil.TreeUtils;

/** Warns about array accesses that could be too high. */
public class UpperBoundVisitor extends BaseTypeVisitor<UpperBoundAnnotatedTypeFactory> {

    private static final String UPPER_BOUND = "array.access.unsafe.high";
    private static final String LOCAL_VAR_ANNO = "local.variable.unsafe.dependent.annotation";
    private static final String DEPENDENT_NOT_PERMITTED = "dependent.not.permitted";
    private static final String UPPER_BOUND_CONST = "array.access.unsafe.high.constant";
    private static final String UPPER_BOUND_RANGE = "array.access.unsafe.high.range";

    private final Map<Element, List<? extends Element>> enclosingElementsCache;
    private final int ENCLOSING_ELEMENTS_CACHE_SIZE =
            500; // arbitrary, but based on the size of the annotation cache in AnnotationUtils

    public UpperBoundVisitor(BaseTypeChecker checker) {
        super(checker);
        enclosingElementsCache = CollectionUtils.createLRUCache(ENCLOSING_ELEMENTS_CACHE_SIZE);
    }

    /**
     * This visits all local variable declarations and issues a warning if a dependent annotation is
     * written on a local variable. This is necessary for the soundness of the reassignment code,
     * which must unrefine all qualifiers, but which cannot collect all in-scope local variables. So
     * that there are no local variables with qualifiers that are not in the store, this method
     * forbids programmers from writing such qualifiers. This may introduce false positives if the
     * programmer would like to write a non-primary annotation on a local variable. Note that final
     * or effectively final are always permitted in dependent annotations, even in local variables,
     * and will not trigger this warning (as no side-effect can effect them).
     *
     * <p>Also issues warnings if the expression in a dependent annotation is not permitted.
     * Permitted expressions in dependent annotations must be at least one of the following:
     *
     * <ul>
     *   <li>final or effectively final variables
     *   <li>local variables
     *   <li>private fields
     *   <li>pure method calls all of whose arguments (including the receiver expression) are
     *       composed only of expressions in this list
     *   <li>accesses of a public final field whose access expression (sometimes called the
     *       receiver) is composed only of expressions in this list
     * </ul>
     *
     * <p>Any other expression results in a warning.
     */
    @Override
    public Void visitVariable(VariableTree node, Void p) {
        AnnotatedTypeMirror atm = atypeFactory.getAnnotatedTypeLhs(node);
        AnnotationMirror anm = atm.getAnnotationInHierarchy(atypeFactory.UNKNOWN);
        if (anm != null && AnnotationUtils.hasElementValue(anm, "value")) {
            // This is a dependent annotation. If this is a local variable,
            // issue a warning; dependent annotations should not be written on local variables.
            Element elt = TreeUtils.elementFromDeclaration(node);
            boolean allVariablesEffectivelyFinal = true;

            UBQualifier qual = UBQualifier.createUBQualifier(anm);
            if (qual.isLessThanLengthQualifier()) {
                LessThanLengthOf ltl = (LessThanLengthOf) qual;
                for (String array : ltl.getSequences()) {
                    Receiver rec = getReceiverForCheckingDependentTypes(array, node);
                    // covers final and effectively final variables
                    boolean isEffectivelyFinal =
                            rec == null
                                    || rec.isUnmodifiableByOtherCode()
                                    || (rec instanceof FlowExpressions.LocalVariable
                                            && ElementUtils.isEffectivelyFinal(
                                                    ((FlowExpressions.LocalVariable) rec)
                                                            .getElement()));
                    if (!isEffectivelyFinal) {
                        allVariablesEffectivelyFinal = false;
                        checkIfPermittedInDependentTypeAnno(rec, array, node);
                    }
                }
            }

            if (elt.getKind() == ElementKind.LOCAL_VARIABLE && !allVariablesEffectivelyFinal) {
                checker.report(Result.warning(LOCAL_VAR_ANNO), node);
            }
        }

        return super.visitVariable(node, p);
    }

    /**
     * Find the Receiver associated with the given String, on the current path. Issues a checker
     * warning if {@code expr} is unparseable.
     *
     * @param expr a String containing a Java expression that is currently in scope
     * @param tree the current tree, for error reporting
     * @return the Receiver for the Java expression named by {@code expr} in the current scope
     */
    private Receiver getReceiverForCheckingDependentTypes(String expr, Tree tree) {
        try {
            return atypeFactory.getReceiverFromJavaExpressionString(expr, getCurrentPath());
        } catch (FlowExpressionParseUtil.FlowExpressionParseException e) {
            // issue warning
            checker.report(
                    Result.warning(DEPENDENT_NOT_PERMITTED, expr, "the expression was unparseable"),
                    tree);
            return null;
        }
    }

    /**
     * Determines whether the Receiver {@code rec} is allowed in a dependent annotation, following
     * the rules defined by the JavaDoc on {@link #visitVariable(VariableTree, Void)}. Issues an
     * error if the Receiver does not meet the conditions listed there.
     *
     * @param rec the Receiver to check
     * @param expr the String that Receiver was derived from. Used only for error reporting.
     * @param tree the current scope. Used for error reporting.
     * @return true iff the Receiver is permitted in a dependent type
     */
    private boolean checkIfPermittedInDependentTypeAnno(Receiver rec, String expr, Tree tree) {

        if (rec == null) {
            return false;
        }

        if (rec.isUnmodifiableByOtherCode() || rec instanceof FlowExpressions.LocalVariable) {
            return true;
        }

        if (rec instanceof FlowExpressions.FieldAccess) {
            FlowExpressions.FieldAccess faRec = (FlowExpressions.FieldAccess) rec;
            if (faRec.getField().getModifiers().contains(Modifier.PRIVATE)
                    || (faRec.isFinal()
                            && checkIfPermittedInDependentTypeAnno(
                                    faRec.getReceiver(), faRec.getReceiver().toString(), tree))) {
                return true;
            } else {
                // issue warning
                checker.report(
                        Result.warning(
                                DEPENDENT_NOT_PERMITTED,
                                expr,
                                "fields in a dependent type must either be private or both public and final. The receiver "
                                        + "object must be one of: a local variable; a private field; or a public, final field"),
                        tree);
                return false;
            }
        }
        if (rec instanceof FlowExpressions.MethodCall) {
            FlowExpressions.MethodCall mcRec = (FlowExpressions.MethodCall) rec;
            boolean parametersArePermittedInDependentTypeAnno = true;
            for (FlowExpressions.Receiver r : mcRec.getParameters()) {
                parametersArePermittedInDependentTypeAnno =
                        parametersArePermittedInDependentTypeAnno
                                && checkIfPermittedInDependentTypeAnno(r, r.toString(), tree);
            }
            if (!PurityUtils.isSideEffectFree(atypeFactory, mcRec.getElement())) {
                // issue warning
                checker.report(
                        Result.warning(
                                DEPENDENT_NOT_PERMITTED,
                                expr,
                                "all method calls in dependent types must be pure"),
                        tree);
                return false;
            }
            if (parametersArePermittedInDependentTypeAnno
                    && checkIfPermittedInDependentTypeAnno(
                            mcRec.getReceiver(), mcRec.getReceiver().toString(), tree)) {
                return true;
            } else {
                // warning will already have been issued in this case.
                return false;
            }
        }
        checker.report(
                Result.warning(
                        DEPENDENT_NOT_PERMITTED,
                        expr,
                        "the expression did not fit one of the categories of permitted expression in dependent types. Those categories are: \n           1. final or effectively final variables\n"
                                + "           2. local variables\n"
                                + "           3. private fields\n"
                                + "           4. pure method calls all of whose arguments (including the receiver expression)\n"
                                + "           are composed only of expressions in this list\n"
                                + "           5. accesses of a public final field whose access expression (sometimes called the\n"
                                + "           receiver) is composed only of expressions in this list"),
                tree);
        return false;
    }

    /**
     * When the visitor reaches an array access, it needs to check a couple of things. First, it
     * checks if the index has been assigned a reasonable UpperBound type: only an index with type
     * LTLengthOf(arr) is safe to access arr. If that fails, it checks if the access is still safe.
     * To do so, it checks if the Value Checker knows the minimum length of arr by querying the
     * Value Annotated Type Factory. If the minimum length of the array is known, the visitor can
     * check if the index is less than that minimum length. If so, then the access is still safe.
     * Otherwise, report a potential unsafe access.
     */
    @Override
    public Void visitArrayAccess(ArrayAccessTree tree, Void type) {
        ExpressionTree indexTree = tree.getIndex();
        ExpressionTree arrTree = tree.getExpression();
        visitAccess(indexTree, arrTree);
        return super.visitArrayAccess(tree, type);
    }

    /**
     * Checks if this array access is either using a variable that is less than the length of the
     * array, or using a constant less than the array's minlen. Issues an error if neither is true.
     */
    private void visitAccess(ExpressionTree indexTree, ExpressionTree arrTree) {
        AnnotatedTypeMirror indexType = atypeFactory.getAnnotatedType(indexTree);
        String arrName = FlowExpressions.internalReprOf(this.atypeFactory, arrTree).toString();

        UBQualifier qualifier = UBQualifier.createUBQualifier(indexType, atypeFactory.UNKNOWN);
        if (qualifier.isLessThanLengthOf(arrName)) {
            return;
        }

        // Find max because it's important to determine whether the index is less than the
        // minimum length of the array. If it could be any of several values, only the max is of
        // interest.
        Long valMax = IndexUtil.getMaxValue(indexTree, atypeFactory.getValueAnnotatedTypeFactory());

        AnnotationMirror sameLenAnno = atypeFactory.sameLenAnnotationFromTree(arrTree);
        // Produce the full list of relevant names by checking the SameLen type.
        if (sameLenAnno != null && AnnotationUtils.areSameByClass(sameLenAnno, SameLen.class)) {
            if (AnnotationUtils.hasElementValue(sameLenAnno, "value")) {
                List<String> slNames =
                        AnnotationUtils.getElementValueArray(
                                sameLenAnno, "value", String.class, true);
                if (qualifier.isLessThanLengthOfAny(slNames)) {
                    return;
                }
                for (String slName : slNames) {
                    // Check if any of the arrays have a minlen that is greater than the
                    // known constant value.
                    int minlenSL =
                            atypeFactory
                                    .getValueAnnotatedTypeFactory()
                                    .getMinLenFromString(slName, arrTree, getCurrentPath());
                    if (valMax != null && valMax < minlenSL) {
                        return;
                    }
                }
            }
        }

        // Check against the minlen of the array itself.
        Integer minLen = IndexUtil.getMinLen(arrTree, atypeFactory.getValueAnnotatedTypeFactory());
        if (valMax != null && minLen != null && valMax < minLen) {
            return;
        }

        // We can issue three different errors:
        // 1. If the index is a compile-time constant, issue an error that describes the array type.
        // 2. If the index is a compile-time range and has no upperbound qualifier,
        //    issue an error that names the upperbound of the range and the array's type.
        // 3. If neither of the above, issue an error that names the upper bound type.

        if (IndexUtil.getExactValue(indexTree, atypeFactory.getValueAnnotatedTypeFactory())
                != null) {
            // Note that valMax is equal to the exact value in this case.
            checker.report(
                    Result.failure(
                            UPPER_BOUND_CONST,
                            valMax,
                            atypeFactory
                                    .getValueAnnotatedTypeFactory()
                                    .getAnnotatedType(arrTree)
                                    .toString(),
                            valMax + 1,
                            valMax + 1),
                    indexTree);
        } else if (valMax != null && qualifier.isUnknown()) {

            checker.report(
                    Result.failure(
                            UPPER_BOUND_RANGE,
                            atypeFactory
                                    .getValueAnnotatedTypeFactory()
                                    .getAnnotatedType(indexTree)
                                    .toString(),
                            atypeFactory
                                    .getValueAnnotatedTypeFactory()
                                    .getAnnotatedType(arrTree)
                                    .toString(),
                            arrName,
                            arrName,
                            valMax + 1),
                    indexTree);
        } else {
            checker.report(
                    Result.failure(UPPER_BOUND, indexType.toString(), arrName, arrName, arrName),
                    indexTree);
        }
    }

    /**
     * When non-side-effect-free methods are called, they may invalidate some dependent types. This
     * method checks if the method being called is side-effecting, and if so, it dispatches to the
     * code that finds in-scope dependent annotations and then checks whether they might be
     * invalidated.
     */
    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        ExecutableElement elt = TreeUtils.elementFromUse(node);
        if (!PurityUtils.isSideEffectFree(atypeFactory, elt)) {
            checkAnnotationsInClass(
                    elt,
                    null,
                    node,
                    getCurrentPath(),
                    UpperBoundUtil.SideEffectKind.SIDE_EFFECTING_METHOD_CALL);
        }
        return super.visitMethodInvocation(node, p);
    }

    /**
     * The standard rules for the commonAssignmentCheck continue to apply; this method first calls
     * {@code super.commonAssignmentCheck} before doing anything else. Afterwards, it checks to see
     * if the reassignment might invalidate any dependent types in scope.
     */
    @Override
    protected void commonAssignmentCheck(
            Tree varTree, ExpressionTree valueExp, @CompilerMessageKey String errorKey) {
        super.commonAssignmentCheck(varTree, valueExp, errorKey);
        if (varTree.getKind() == Kind.VARIABLE) {
            // Not a reassignment, so nothing to check
            return;
        }
        Element elt = InternalUtils.symbol(varTree);

        if (elt != null
                && (elt.getKind() == ElementKind.FIELD
                        || elt.getKind() == ElementKind.PARAMETER
                        || elt.getKind() == ElementKind.LOCAL_VARIABLE)
                && !ElementUtils.isEffectivelyFinal(elt)) {
            TypeKind typeKind = InternalUtils.typeOf(varTree).getKind();
            Receiver field = FlowExpressions.internalReprOf(atypeFactory, (ExpressionTree) varTree);
            SideEffectKind kind;
            if (elt.getKind() == ElementKind.FIELD) {
                if (typeKind == TypeKind.ARRAY) {
                    kind = UpperBoundUtil.SideEffectKind.ARRAY_FIELD_REASSIGNMENT;
                } else if (!typeKind.isPrimitive()) {
                    kind = UpperBoundUtil.SideEffectKind.NON_ARRAY_FIELD_REASSIGNMENT;
                } else {
                    return;
                }
            } else if (typeKind == TypeKind.ARRAY) {
                kind = SideEffectKind.LOCAL_VAR_REASSIGNMENT;
            } else {
                return;
            }
            checkAnnotationsInClass(elt, field, varTree, getCurrentPath(), kind);
        }
    }

    /**
     * Checks whether any annotations in scope are dependent and need to be invalidated by the given
     * side effect.
     *
     * @param elt An element corresponding to the side effect that caused the invalidation.
     * @param possiblyInvalidatedRec A receiver representing the possibly invalidated expression.
     * @param tree The tree on which the side effect occurs. Used in error reporting.
     * @param path The path on which the side effect occurs. Used for parsing.
     * @param sideEffectKind The type of side effect. See {@link SideEffectKind}.
     */
    private void checkAnnotationsInClass(
            Element elt,
            Receiver possiblyInvalidatedRec,
            Tree tree,
            TreePath path,
            SideEffectKind sideEffectKind) {
        List<? extends Element> enclosedElts =
                enclosingElementsCache.containsKey(elt) ? enclosingElementsCache.get(elt) : null;
        if (enclosedElts == null) {
            enclosedElts = ElementUtils.enclosingClass(elt).getEnclosedElements();
            enclosingElementsCache.put(elt, enclosedElts);
        }
        List<AnnotatedTypeMirror> enclosedTypes = findEnclosedTypes(enclosedElts);
        for (AnnotatedTypeMirror atm : enclosedTypes) {
            List<Receiver> rs =
                    UpperBoundUtil.getDependentReceivers(atm.getAnnotations(), path, atypeFactory);
            for (Receiver r : rs) {
                SideEffectError result =
                        UpperBoundUtil.isSideEffected(r, possiblyInvalidatedRec, sideEffectKind);
                if (result != UpperBoundUtil.SideEffectError.NO_ERROR) {
                    checker.report(Result.failure(result.errorKey, atm), tree);
                }
            }
        }
    }

    /**
     * Finds all the annotated types associated with a list of elements.
     *
     * @param enclosedElts the list of elements to find annotated types for
     * @return a list of annotated types
     */
    private List<AnnotatedTypeMirror> findEnclosedTypes(List<? extends Element> enclosedElts) {
        List<AnnotatedTypeMirror> enclosedTypes = new ArrayList<>();
        for (Element e : enclosedElts) {
            AnnotatedTypeMirror atm = atypeFactory.getAnnotatedType(e);
            enclosedTypes.add(atm);
            if (e.getKind() == ElementKind.METHOD) {
                ExecutableElement ee = (ExecutableElement) e;
                List<? extends Element> rgparam = ee.getParameters();
                for (Element param : rgparam) {
                    AnnotatedTypeMirror atmP = atypeFactory.getAnnotatedType(param);
                    enclosedTypes.add(atmP);
                }
            } else if (e.getKind() == ElementKind.CLASS) {
                enclosedTypes.addAll(findEnclosedTypes(e.getEnclosedElements()));
            }
        }
        return enclosedTypes;
    }

    @Override
    protected void commonAssignmentCheck(
            AnnotatedTypeMirror varType,
            ExpressionTree valueExp,
            @CompilerMessageKey String errorKey) {
        if (!relaxedCommonAssignment(varType, valueExp)) {
            super.commonAssignmentCheck(varType, valueExp, errorKey);
        }
    }

    /**
     * Returns whether the assignment is legal based on the relaxed assignment rules.
     *
     * <p>The relaxed assignment rules is the following: Assuming the varType (left-hand side) is
     * less than the length of some array given some offset
     *
     * <p>1. If both the offset and the value expression (rhs) are ints known at compile time, and
     * if the min length of the array is greater than offset + value, then the assignment is legal.
     * (This method returns true.)
     *
     * <p>2. If the value expression (rhs) is less than the length of an array that is the same
     * length as the array in the varType, and if the offsets are equal, then the assignment is
     * legal. (This method returns true.)
     *
     * <p>3. Otherwise the assignment is only legal if the usual assignment rules are true, so this
     * method returns false.
     *
     * <p>If the varType is less than the length of multiple arrays, then the this method only
     * returns true if the relaxed rules above apply for each array.
     *
     * <p>If the varType is an array type and the value express is an array initializer, then the
     * above rules are applied for expression in the initializer where the varType is the component
     * type of the array.
     */
    private boolean relaxedCommonAssignment(AnnotatedTypeMirror varType, ExpressionTree valueExp) {
        List<? extends ExpressionTree> expressions;
        if (valueExp.getKind() == Kind.NEW_ARRAY && varType.getKind() == TypeKind.ARRAY) {
            expressions = ((NewArrayTree) valueExp).getInitializers();
            if (expressions == null || expressions.isEmpty()) {
                return false;
            }
            // The qualifier we need for an array is in the component type, not varType.
            AnnotatedTypeMirror componentType = ((AnnotatedArrayType) varType).getComponentType();
            UBQualifier qualifier =
                    UBQualifier.createUBQualifier(componentType, atypeFactory.UNKNOWN);
            if (!qualifier.isLessThanLengthQualifier()) {
                return false;
            }
            for (ExpressionTree expressionTree : expressions) {
                if (!relaxedCommonAssignmentCheck((LessThanLengthOf) qualifier, expressionTree)) {
                    return false;
                }
            }
            return true;
        }

        UBQualifier qualifier = UBQualifier.createUBQualifier(varType, atypeFactory.UNKNOWN);
        return qualifier.isLessThanLengthQualifier()
                && relaxedCommonAssignmentCheck((LessThanLengthOf) qualifier, valueExp);
    }

    /**
     * Implements the actual check for the relaxed common assignment check. For what is permitted,
     * see {@link #relaxedCommonAssignment}.
     */
    private boolean relaxedCommonAssignmentCheck(
            LessThanLengthOf varLtlQual, ExpressionTree valueExp) {

        AnnotatedTypeMirror expType = atypeFactory.getAnnotatedType(valueExp);
        UBQualifier expQual = UBQualifier.createUBQualifier(expType, atypeFactory.UNKNOWN);

        Long value = IndexUtil.getMaxValue(valueExp, atypeFactory.getValueAnnotatedTypeFactory());

        if (value == null && !expQual.isLessThanLengthQualifier()) {
            return false;
        }

        SameLenAnnotatedTypeFactory sameLenFactory = atypeFactory.getSameLenAnnotatedTypeFactory();
        ValueAnnotatedTypeFactory valueAnnotatedTypeFactory =
                atypeFactory.getValueAnnotatedTypeFactory();
        checkloop:
        for (String sequenceName : varLtlQual.getSequences()) {

            List<String> sameLenSequences =
                    sameLenFactory.getSameLensFromString(sequenceName, valueExp, getCurrentPath());
            if (testSameLen(expQual, varLtlQual, sameLenSequences, sequenceName)) {
                continue;
            }

            int minlen =
                    valueAnnotatedTypeFactory.getMinLenFromString(
                            sequenceName, valueExp, getCurrentPath());
            if (testMinLen(value, minlen, sequenceName, varLtlQual)) {
                continue;
            }
            for (String sequence : sameLenSequences) {
                int minlenSL =
                        valueAnnotatedTypeFactory.getMinLenFromString(
                                sequence, valueExp, getCurrentPath());
                if (testMinLen(value, minlenSL, sequenceName, varLtlQual)) {
                    continue checkloop;
                }
            }

            return false;
        }

        return true;
    }

    /**
     * Tests whether replacing any of the arrays in sameLenArrays with arrayName makes expQual
     * equivalent to varQual.
     */
    private boolean testSameLen(
            UBQualifier expQual,
            LessThanLengthOf varQual,
            List<String> sameLenArrays,
            String arrayName) {

        if (!expQual.isLessThanLengthQualifier()) {
            return false;
        }

        for (String sameLenArrayName : sameLenArrays) {
            // Check whether replacing the value for any of the current type's offset results
            // in the type we're trying to match.
            if (varQual.isValidReplacement(
                    arrayName, sameLenArrayName, (LessThanLengthOf) expQual)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tests a constant value (value) against the minlen (minlens) of an array (arrayName) with a
     * qualifier (varQual).
     */
    private boolean testMinLen(Long value, int minLen, String arrayName, LessThanLengthOf varQual) {
        if (value == null) {
            return false;
        }
        return varQual.isValuePlusOffsetLessThanMinLen(arrayName, value, minLen);
    }
}
