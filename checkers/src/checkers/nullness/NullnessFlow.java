package checkers.nullness;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.ElementFilter;

import checkers.flow.DefaultFlow;
import checkers.flow.Flow;
import checkers.flow.FlowState;
import checkers.nullness.quals.*;
import checkers.source.Result;
import checkers.source.SourceChecker;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.util.*;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;


/**
 * Implements Nullness-specific customizations of the flow-sensitive type
 * qualifier inference provided by {@link DefaultFlow}. In particular, if a
 * conditional null-check is performed, the checked value is treated as
 * {@link NonNull} or {@link Nullable} as appropriate in the subsequent
 * branches. For instance, for the check
 *
 * <pre>
 * if (x != null) {
 *     foo(x);
 * } else {
 *     bar(x);
 * }
 * </pre>
 *
 * {@code x} is treated as non-null in the argument for {@code foo} and as
 * possibly-null in the argument for {@code bar}.
 *
 * This class also handles the
 *   {@link AssertNonNullAfter},
 *   {@link AssertNonNullIfTrue},
 *   {@link AssertNonNullIfFalse},
 *   {@link AssertNonNullIfNonNull}, and
 *   {@link NonNullOnEntry}
 * annotations.
 *
 * @see Flow
 * @see DefaultFlow
 * @see NullnessSubchecker
 */
class NullnessFlow extends DefaultFlow<NullnessFlowState> {

    /**
     * A temporary flag to disable error messages from parsing the
     * AssertNonNullXXX and NonNullOnEntry annotations.
     * Once these annotations are parsed reliably, remove this flag.
     */
    private final boolean DO_ADVANCED_CHECKS;

    private final AnnotationMirror POLYNULL, RAW, NONNULL, PRIMITIVE;
    private boolean isNullPolyNull;
    private final AnnotatedTypeFactory rawFactory;
    private final Map<ExecutableElement, Set<VariableElement>> initializedFields;
    // TODO: use this field?
    // private final TreeParser treeParser;
    private final Resolver resolver;


    /**
     * Creates a NonNull-specific flow-sensitive inference.
     *
     * @param checker the current checker
     * @param root the compilation unit to scan
     * @param factory the type factory to use
     */
    public NullnessFlow(NullnessSubchecker checker, CompilationUnitTree root,
            Set<AnnotationMirror> annotations, NullnessAnnotatedTypeFactory factory) {
        super(checker, root, annotations, factory);
        POLYNULL = factory.POLYNULL;
        RAW = factory.RAW;
        NONNULL = factory.NONNULL;
        PRIMITIVE = factory.PRIMITIVE;
        isNullPolyNull = false;
        rawFactory = factory.rawnessFactory;
        initializedFields = new HashMap<ExecutableElement, Set<VariableElement>>();
        // treeParser = new TreeParser(checker.getProcessingEnvironment());
        resolver = new Resolver(checker.getProcessingEnvironment());

        DO_ADVANCED_CHECKS = checker.getLintOption("advancedchecks", NullnessSubchecker.ADVANCEDCHECKS_DEFAULT);
    }

    @Override
    protected NullnessFlowState createFlowState(Set<AnnotationMirror> annotations) {
        return new NullnessFlowState(annotations);
    }

    /**
     * Currently, flow can only find the complement of
     * a simple clause
     */
    private static boolean isFlippableLogic(ExpressionTree tree) {
        tree = TreeUtils.skipParens(tree);
        switch (tree.getKind()) {
        case EQUAL_TO:
        case NOT_EQUAL_TO:
        case INSTANCE_OF:
        case CONDITIONAL_OR:
            return true;
        case LOGICAL_COMPLEMENT:
            return isFlippableLogic(((UnaryTree)tree).getExpression());
        default:
            return false;
        }
    }

    @Override
    protected void scanCond(ExpressionTree tree) {
        if (debug != null) {
            debug.println("NullnessFlow::scanCond: " + tree);
        }

        super.scanCond(tree);
        if (tree == null)
            return;

        FlowState before = flowState_whenFalse.copy();

        NullnessFlowConditions conds = new NullnessFlowConditions((NullnessAnnotatedTypeFactory)factory, this, debug);
        conds.visit(tree, null);
        this.flowResults.putAll(conds.getTreeResults());

        boolean flippable = isFlippableLogic(tree);

        for (VariableElement elt : conds.getNonnullElements()) {
            int idx = flowState_whenFalse.vars.indexOf(elt);
            if (idx >= 0) {
                flowState_whenTrue.annos.set(NONNULL, idx);
                if (flippable && !conds.getExcludes().contains(elt))
                    flowState_whenFalse.annos.clear(NONNULL, idx);
            }
        }

        for (VariableElement elt : conds.getNullableElements()) {
            int idx = flowState_whenFalse.vars.indexOf(elt);
            if (idx >= 0) {
                // Mahmood on 01/28/2009:
                // I don't know why annosWhenTrue is cleared.  Its bits being
                // on indicate extra information not captured within the
                // analyzed condition
                // annosWhenTrue.clear(NONNULL, idx);
                if (flippable && !conds.getExcludes().contains(elt))
                    flowState_whenFalse.annos.set(NONNULL, idx);
            }
        }
        // annosWhenFalse.or(before);
        // GenKillBits.orlub(res.annosWhenFalse, before, annoRelations);
        flowState_whenFalse.or(before, annoRelations);

        isNullPolyNull = conds.isNullPolyNull();
        flowState_whenTrue.nnExprs.addAll(conds.getNonnullExpressions());
        flowState_whenFalse.nnExprs.addAll(conds.getNullableExpressions());

        // previously was in whenConditionFalse and/or visitIf
        tree = TreeUtils.skipParens(tree);
        // TODO: Add nnElems
        flowState_whenFalse.nnExprs.addAll(shouldInferNullnessIfFalseNullable(tree));
        flowState_whenFalse.nnExprs.addAll(shouldInferNullnessPureNegation(tree));
        if (tree.getKind() == Tree.Kind.LOGICAL_COMPLEMENT) {
            ExpressionTree unary = ((UnaryTree)tree).getExpression();
            flowState_whenFalse.nnExprs.addAll(shouldInferNullnessAfter(unary));
            flowState_whenFalse.nnExprs.addAll(shouldInferNullnessIfTrue(unary));
        }
    }

    @Override
    public Void visitBinary(BinaryTree node, Void p) {
        if (debug != null) {
            debug.println("NullnessFlow::visitBinary: " + node);
        }

        // TODO: Why is this not handled by NullnessFlowConditions???
        if (node.getKind() == Tree.Kind.CONDITIONAL_AND
                || node.getKind() == Tree.Kind.CONDITIONAL_OR) {
            scan(node.getLeftOperand(), p);
            NullnessFlowState before = flowState.copy();
            scan(node.getRightOperand(), p);
            flowState = before;
        } else {
            scan(node.getLeftOperand(), p);
            scan(node.getRightOperand(), p);
        }

        NullnessFlowConditions conds = new NullnessFlowConditions((NullnessAnnotatedTypeFactory)factory, this, debug);
        conds.visit(node, null);
        this.takeFromConds(conds);
        return null;
    }

    private void takeFromConds(NullnessFlowConditions conds) {
        this.flowResults.putAll(conds.getTreeResults());
        this.flowState.nnExprs.addAll(conds.getNonnullExpressions());
        this.flowState.nnExprs.removeAll(conds.getNullableExpressions());
    }


    private static String receiver(MethodInvocationTree node) {
        ExpressionTree sel = node.getMethodSelect();
        if (sel.getKind() == Tree.Kind.IDENTIFIER)
            return "";
        else if (sel.getKind() == Tree.Kind.MEMBER_SELECT)
            return ((MemberSelectTree)sel).getExpression().toString() + ".";
        SourceChecker.errorAbort("NullnessFlow.receiver: cannot be here");
        return null; // dead code
    }

    // TODO: move shouldInferNullness somewhere more appropriate. Maybe NullnessFlowConditions.
    public List<String> shouldInferNullness(ExpressionTree node) {
        List<String> result = new ArrayList<String>();
        result.addAll(shouldInferNullnessIfTrue(node));
        result.addAll(shouldInferNullnessIfFalse(node));
        result.addAll(shouldInferNullnessPureNegation(node));
        return result;
    }


    private static final Pattern parameterPtn = Pattern.compile("#(\\d+)");


    /**
     * Substitute patterns and ensure that the Strings are formatted according to our conventions.
     * Use the information from the given method declaration.
     *
     * @param method The method declaration to use.
     * @param annoValues The annotation values to substitute.
     * @return The substituted annotation values.
     */
    private List<String> substitutePatternsDecl(MethodTree method, List<String> annoValues) {
        List<? extends VariableTree> paramTrees = method.getParameters();
        List<String> params = new ArrayList<String>(paramTrees.size());
        for(VariableTree vt : paramTrees) {
            params.add(vt.getName().toString());
        }

        return substitutePatternsGeneric(method, null, params, annoValues);
    }

    /**
     * Substitute patterns and ensure that the Strings are formatted according to our conventions.
     * Use the information from the given method invocation.
     *
     * @param methodInvok The method invocation to use.
     * @param annoValues The annotation values to substitute.
     * @return The substituted annotation values.
     */
    private static List<String> substitutePatternsCall(MethodInvocationTree methodInvok, List<String> annoValues) {
        String receiver = receiver(methodInvok);

        List<? extends ExpressionTree> argExps = methodInvok.getArguments();
        List<String> args = new ArrayList<String>(argExps.size());
        for(ExpressionTree et : argExps) {
            args.add(et.toString());
        }

        return substitutePatternsGeneric(methodInvok, receiver, args, annoValues);
    }

    /**
     * Substitute patterns and ensure that the Strings are formatted according to our conventions.
     * This method is used by {@link substitutePatternsCall} and {@link substitutePatternsDecl}.
     *
     * @param node Tree used only for error messages.
     * @param receiver String representation of the receiver, if non-null.
     * @param argparams The values to substitute for parameter patterns.
     * @param annoValues The annotation values to substitute.
     * @return The substituted annotation values.
     */
    private static List<String> substitutePatternsGeneric(Tree node, String receiver,
            List<String> argparams, List<String> annoValues) {
        List<String> asserts = new ArrayList<String>();

        fields: for (String s : annoValues) {
            if (parameterPtn.matcher(s).matches()) {
                // exactly one parameter index, e.g. "#0"
                int param = Integer.valueOf(s.substring(1));
                if (param < argparams.size()) {
                    asserts.add(argparams.get(param).toString());
                } else {
                    //if (DO_ADVANCED_CHECKS) {
                        // checker.report(Result.failure("param.index.nullness.parse.error", s), node);
                        System.err.println(Result.failure("param.index.nullness.parse.error", s));
                    // }
                    continue;
                }
            } else if (parameterPtn.matcher(s).find()) {
                // parameter pattern(s) within the string
                Matcher matcher = parameterPtn.matcher(s);
                StringBuffer sb = new StringBuffer();
                while (matcher.find()) {
                    int param = Integer.valueOf(matcher.group(1));
                    if (param < argparams.size()) {
                        String rep = argparams.get(param).toString();
                        matcher.appendReplacement(sb, rep);
                    } else {
                        // if (DO_ADVANCED_CHECKS) {
                            // checker.report(Result.failure("param.index.nullness.parse.error", s), node);
                            System.err.println(Result.failure("param.index.nullness.parse.error", s));
                        // }
                        continue fields;
                    }
                }
                matcher.appendTail(sb);

                if (receiver!=null) {
                    asserts.add(receiver + sb.toString());
                } else {
                    asserts.add(sb.toString());
                }
            } else {
                if (receiver!=null) {
                    asserts.add(receiver + s);
                } else {
                    asserts.add(s);
                }
            }
        }
        return asserts;
    }

    private List<String> shouldInferNullnessIfTrue(ExpressionTree node) {
        node = TreeUtils.skipParens(node);

        if (node.getKind() == Tree.Kind.CONDITIONAL_AND) {
            BinaryTree bin = (BinaryTree)node;
            List<String> asserts = new ArrayList<String>();
            asserts.addAll(shouldInferNullnessIfTrue(bin.getLeftOperand()));
            asserts.addAll(shouldInferNullnessIfTrue(bin.getRightOperand()));
            return asserts;
        }
        if (node.getKind() != Tree.Kind.METHOD_INVOCATION)
            return Collections.emptyList();

        MethodInvocationTree methodInvok = (MethodInvocationTree)node;
        ExecutableElement method = TreeUtils.elementFromUse(methodInvok);

        List<String> asserts;
        AnnotationMirror anno = factory.getDeclAnnotation(method, AssertNonNullIfTrue.class);
        if (anno != null) {
            List<String> annitValue = AnnotationUtils.elementValueArray(anno, "value");
            asserts = substitutePatternsCall(methodInvok, annitValue);
        } else {
            asserts = Collections.emptyList();
        }

        return asserts;
    }

    private List<String> shouldInferNullnessAfter(ExpressionTree node) {
        node = TreeUtils.skipParens(node);

        if (node.getKind() == Tree.Kind.CONDITIONAL_AND) {
            BinaryTree bin = (BinaryTree)node;
            List<String> asserts = new ArrayList<String>();
            asserts.addAll(shouldInferNullnessAfter(bin.getLeftOperand()));
            asserts.addAll(shouldInferNullnessAfter(bin.getRightOperand()));
            return asserts;
        }
        if (node.getKind() != Tree.Kind.METHOD_INVOCATION)
            return Collections.emptyList();

        MethodInvocationTree methodInvok = (MethodInvocationTree)node;
        ExecutableElement method = TreeUtils.elementFromUse(methodInvok);

        List<String> asserts;
        AnnotationMirror anno = factory.getDeclAnnotation(method, AssertNonNullAfter.class);
        if (anno != null) {
            List<String> annaValue = AnnotationUtils.elementValueArray(anno, "value");
            asserts = substitutePatternsCall(methodInvok, annaValue);
        } else {
            asserts = Collections.emptyList();
        }

        return asserts;
    }

    private List<String> shouldInferNullnessIfFalse(ExpressionTree node) {
        if (node.getKind() != Tree.Kind.LOGICAL_COMPLEMENT
                || ((UnaryTree)node).getExpression().getKind() != Tree.Kind.METHOD_INVOCATION) {
            return Collections.emptyList();
        }

        MethodInvocationTree methodInvok = (MethodInvocationTree)((UnaryTree)node).getExpression();
        ExecutableElement method = TreeUtils.elementFromUse(methodInvok);

        List<String> asserts;
        AnnotationMirror anno = factory.getDeclAnnotation(method, AssertNonNullIfFalse.class);
        if (anno != null) {
            List<String> annifValue = AnnotationUtils.elementValueArray(anno, "value");
            asserts = substitutePatternsCall(methodInvok, annifValue);
        } else {
            asserts = Collections.emptyList();
        }

        return asserts;
    }

    private List<String> shouldInferNullnessIfFalseNullable(ExpressionTree node) {
        node = TreeUtils.skipParens(node);
        if (node.getKind() != Tree.Kind.METHOD_INVOCATION) {
            return Collections.emptyList();
        }

        MethodInvocationTree methodInvok = (MethodInvocationTree)node;
        ExecutableElement method = TreeUtils.elementFromUse(methodInvok);

        List<String> asserts;
        AnnotationMirror anno = factory.getDeclAnnotation(method, AssertNonNullIfFalse.class);
        if (anno != null) {
            List<String> annifValue = AnnotationUtils.elementValueArray(anno, "value");
            asserts = substitutePatternsCall(methodInvok, annifValue);
        } else {
            asserts = Collections.emptyList();
        }

        return asserts;
    }

    private List<String> shouldInferNullnessPureNegation(ExpressionTree node) {
        if (node.getKind() == Tree.Kind.EQUAL_TO) {
            BinaryTree binary = (BinaryTree)node;
            if (!isNull(binary.getLeftOperand()) && !isNull(binary.getRightOperand()))
                return Collections.emptyList();

            if (isNull(binary.getLeftOperand())
                    && isPure(binary.getRightOperand())) {
                return Collections.singletonList(binary.getRightOperand().toString());
            } else if (isNull(binary.getRightOperand())
                    && isPure(binary.getLeftOperand())) {
                return Collections.singletonList(binary.getLeftOperand().toString());
            } else
                return Collections.emptyList();
        } else if (node.getKind() == Tree.Kind.LOGICAL_COMPLEMENT
                && (TreeUtils.skipParens(((UnaryTree)node).getExpression()).getKind() == Tree.Kind.INSTANCE_OF)) {
            InstanceOfTree ioTree = (InstanceOfTree)TreeUtils.skipParens(((UnaryTree)node).getExpression());
            if (isPure(ioTree.getExpression()))
                return Collections.singletonList(ioTree.getExpression().toString());
            else
                return Collections.emptyList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public Void visitAssignment(AssignmentTree node, Void p) {
        if (debug != null) {
            debug.println("NullnessFlow::visitAssignment: " + node);
        }

        // clean nnExprs when they are reassigned
        // TODO: need to look deeper into the nnExprs, e.g. see test case in
        // AssertAfter2, where "get(parent)" is in nnExprs and "parent" is re-assigned.
        // The following is too simplistic:
        //   this.nnExprs.remove(node.getVariable().toString());
        // Just doing a "contains" on each string is probably too coarse, but
        // would ensure that we do not miss a case.
        // Instead look in more detail:
        String var = node.getVariable().toString();
        Iterator<String> iter = this.flowState.nnExprs.iterator();
        while (iter.hasNext()) {
            String nnExp = iter.next();
            if (var.equals(nnExp) ||
                    nnExp.contains("(" + var + ")") ||
                    nnExp.contains("(" + var + ", ") ||
                    nnExp.contains(", " + var + ")") ||
                    nnExp.contains(", " + var + ", ") ||
                    nnExp.contains("." + var) ||
                    nnExp.contains(var + ".")) {
                iter.remove();
            }
        }
        return super.visitAssignment(node, p);
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, Void p) {
        if (debug != null) {
            debug.println("NullnessFlow::visitCompoundAssignment: " + node);
        }

        super.visitCompoundAssignment(node, p);
        inferNullness(node.getVariable());
        return null;
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree node, Void p) {
        if (debug != null) {
            debug.println("NullnessFlow::visitMemberSelect: " + node);
        }

        super.visitMemberSelect(node, p);

        inferNullness(node.getExpression());
        if (this.flowState.nnExprs.contains(node.toString())) {
            markTree(node, NONNULL);
            // else is needed to avoid the double marking bug
//        } else if (this.flowState.nnElems.contains(TreeUtils.elementFromUse(node)) &&
//                !isNonNull(TreeUtils.elementFromUse(node))) {
//              markTree(node, NONNULL);
        }

        return null;
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, Void p) {
        if (debug != null) {
            debug.println("NullnessFlow::visitIdentifier: " + node);
        }

        super.visitIdentifier(node, p);
        if (this.flowState.nnExprs.contains(node.toString())) {
            markTree(node, NONNULL);
        }
        return null;
    }

    private boolean isNonNull(Element e) {
        int index = flowState.vars.indexOf(e);
        return flowState.annos.get(NONNULL, index);
    }

    @Override
    public Void visitArrayAccess(ArrayAccessTree node, Void p) {
        if (debug != null) {
            debug.println("NullnessFlow::visitArrayAccess: " + node);
        }

        super.visitArrayAccess(node, p);

        if (this.flowState.nnExprs.contains(node.toString()))
            markTree(node, NONNULL);

        return null;
    }

    @Override
    protected void clearOnCall(MethodTree enclMeth, ExecutableElement method) {
        if (debug != null) {
            debug.println("NullnessFlow::clearOnCall: " + method);
        }

        super.clearOnCall(enclMeth, method);

        boolean isPure = factory.getDeclAnnotation(method, Pure.class) != null;
        final String methodPackage = ElementUtils.enclosingPackage(method).getQualifiedName().toString();
        boolean isJDKMethod = methodPackage.startsWith("java") || methodPackage.startsWith("com.sun");

        if (!(isPure || isJDKMethod)) {
            // The first idea is this:
            // this.flowState.nnExprs.clear();
            // however, this clears out an assumption that might exist about the current method
            // call itself, i.e. something added by an earlier "AssertNonNullIfFalse".
            // As a first approximation, let's throw out everything that doesn't contain
            // the current method name, but is a method call.
            // This might leave too many fields in nnExprs, but otherwise we
            // would also clean out fields of LazyNonNull type, which will not become null again.
            // TODO: we need to be more fine grained and look at the receiver, etc.
            // Or maybe we can change the order of when things get cleared out?

            Iterator<String> it = this.flowState.nnExprs.iterator();
            while (it.hasNext()) {
                String s = it.next();
                if (!s.contains(method.getSimpleName() + "(") &&
                        s.contains("(")) {
                    it.remove();
                }
            }
        }
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        if (debug != null) {
            debug.println("NullnessFlow::visitMethodInvocation: " + node);
        }

        // GenKillBits<AnnotationMirror> prev = GenKillBits.copy(annos);
        NullnessFlowState prev = flowState.copy();

        /* Important: check the NonNullOnEntry annotation before calling the
         * super method, as the super method clears all knowledge of fields!
         */
        checkNonNullOnEntry(node);

        super.visitMethodInvocation(node, p);

        ExecutableElement method = TreeUtils.elementFromUse(node);
        if (factory.getDeclAnnotation(method, AssertParametersNonNull.class) != null) {
            for (ExpressionTree arg : node.getArguments())
                inferNullness(arg);
        }

        AnnotatedExecutableType methodType = factory.getAnnotatedType(method);
        List<AnnotatedTypeMirror> methodParams = methodType.getParameterTypes();
        List<? extends ExpressionTree> methodArgs = node.getArguments();
        for (int i = 0; i < methodParams.size() && i < methodArgs.size(); ++i) {
            if (methodParams.get(i).hasAnnotation(NONNULL))
                inferNullness(methodArgs.get(i));
        }

        for (int i = 0; i < this.flowState.vars.size(); ++i) {
            Element elem = this.flowState.vars.get(i);
            if (elem.getKind() == ElementKind.FIELD
                    && factory.getAnnotatedType(elem).hasAnnotation(LazyNonNull.class)
                    && prev.annos.get(NONNULL, i))
                this.flowState.annos.set(NONNULL, i);
        }

        this.flowState.nnExprs.addAll(shouldInferNullnessAfter(node));

        if (this.flowState.nnExprs.contains(node.toString())) {
            markTree(node, NONNULL);
        }

        return null;
    }

    private void markTree(Tree node, AnnotationMirror anno) {
        addFlowResult(flowResults, node, anno);
    }

    @Override
    public Void visitLiteral(LiteralTree node, Void p) {
        if (debug != null) {
            debug.println("NullnessFlow::visitLiteral: " + node);
        }

        super.visitLiteral(node, p);

        if (isNullPolyNull && node.getKind() == Tree.Kind.NULL_LITERAL) {
            markTree(node, POLYNULL);
        }
        return null;
    }

    private void inferNullness(ExpressionTree expr) {
        if (debug != null) {
            debug.println("NullnessFlow::inferNullness: " + expr);
        }

        Element elt = var(expr);
        if (expr instanceof IdentifierTree)
            elt = TreeUtils.elementFromUse((IdentifierTree) expr);
        else if (expr instanceof MemberSelectTree)
            elt = TreeUtils.elementFromUse((MemberSelectTree) expr);

        if (elt != null && this.flowState.vars.contains(elt)) {
            int idx = this.flowState.vars.indexOf(elt);
            this.flowState.annos.set(NONNULL, idx);
        }
    }

    @Override
    public Void visitMethod(MethodTree meth, Void p) {
        if (debug != null) {
            debug.println("NullnessFlow::visitMethod: " + meth);
        }

        TreePath path = new TreePath(getCurrentPath(), meth);

        // Cancel assumptions about fields (of this class) for a method with a
        // @Raw receiver.

        // GenKillBits<AnnotationMirror> prev = GenKillBits.copy(annos);
        NullnessFlowState prev = this.flowState.copy();

        if (hasRawReceiver(meth) || TreeUtils.isConstructor(meth)) {
            for (int i = 0; i < this.flowState.vars.size(); i++) {
                Element var = this.flowState.vars.get(i);
                if (var.getKind() == ElementKind.FIELD) {
                    this.flowState.annos.clear(NONNULL, i);
                }
            }
        }

        Element elem = TreeUtils.elementFromDeclaration(meth);
        if (factory.getDeclAnnotation(elem, NonNullOnEntry.class) != null
            || factory.getDeclAnnotation(elem, AssertNonNullIfTrue.class) != null
            || factory.getDeclAnnotation(elem, AssertNonNullIfFalse.class) != null
            || factory.getDeclAnnotation(elem, AssertNonNullAfter.class) != null) {

            List<? extends Element> myFieldElems;
            { // block to get all fields of the current class

                AnnotatedTypeMirror myType = factory.getAnnotatedType(TreeUtils.enclosingClass(factory.getPath(meth)));

                if (!(myType instanceof AnnotatedDeclaredType)) {
                    if (DO_ADVANCED_CHECKS) {
                        System.err.println("NullnessFlow::visitMethod: Bad myType: " + myType + ((myType == null) ? "" : ("  " + myType.getClass())));
                        System.err.println("  for method: " + meth);
                    }
                    return null;
                }

                Element myElem = ((AnnotatedDeclaredType) myType).getUnderlyingType().asElement();
                myFieldElems = allFields(myElem);
            }

            AnnotationMirror anno = factory.getDeclAnnotation(elem, NonNullOnEntry.class);
            if (anno != null) {
                List<String> fields = AnnotationUtils.elementValueArray(anno, "value");
                Pair<List<String>, List<VariableElement>> fieldsList =
                        validateNonNullOnEntry(path, myFieldElems, fields);
                this.flowState.nnExprs.addAll(fieldsList.first);

                for (VariableElement elt: fieldsList.second) {
                    if (!flowState.vars.contains(elt)) {
                        flowState.vars.add(elt);
                    }
                    flowState.annos.set(NONNULL, flowState.vars.indexOf(elt));
                }
            }

            // AssertNonNullIfXXX is checked in visitReturn
            // AssertNonNullAfter is checked in visitMethodEndCallback and visitReturn
        }

        try {
            return super.visitMethod(meth, p);
        } finally {
            this.flowState = prev;
        }
    }

    // Callback at the end of a method body. Use this for void methods that have
    // an AssertNonNullAfter annotation.
    @Override
    public void visitMethodEndCallback(MethodTree meth) {
        if (debug != null) {
            debug.println("NullnessFlow::visitMethodEndCallback: " + meth);
        }

        ExecutableElement methElem = TreeUtils.elementFromDeclaration(meth);
        TypeMirror retType = methElem.getReturnType();

        if (retType.getKind() == TypeKind.VOID &&
            factory.getDeclAnnotation(methElem, AssertNonNullAfter.class) != null) {
            checkAssertNonNullAfter(meth, methElem);
        }

        if (TreeUtils.isConstructor(meth)) {
            initializedFields.put(methElem, calcInitializedFields());
        }
    }

    /**
     * Returns the set of fields that are annotated as non-null in the
     * flowstate.
     */
    private Set<VariableElement> calcInitializedFields() {
        Set<VariableElement> initialized = new HashSet<VariableElement>();

        int i = 0;
        for (VariableElement f: flowState.vars) {
            if (f.getKind().isField() && flowState.annos.get(NONNULL, i)) {
                initialized.add(f);
            }
            i++;
        }

        return initialized;
    }

    public Set<VariableElement> initializedFieldsAfter(ExecutableElement method) {
        return initializedFields.get(method);
    }

    // Also see checkNonNullOnEntry for comparison
    private void checkAssertNonNullAfter(MethodTree meth, ExecutableElement methElem) {
        AnnotationMirror anno = factory.getDeclAnnotation(methElem, AssertNonNullAfter.class);
        List<String> annoValues = AnnotationUtils.elementValueArray(anno, "value");
        TreePath path = TreePath.getPath(TreeUtils.pathTillClass(getCurrentPath()), meth);
        ClassTree cls = TreeUtils.enclosingClass(factory.getPath(meth));

        for (String annoVal : annoValues) {

            if (parameterPtn.matcher(annoVal).find()) {
                if (DO_ADVANCED_CHECKS
                        && !checker.shouldSkipDefs(cls, meth)) {
                    checker.report(Result.warning("nullness.parse.error", annoVal), meth);
                }
                continue;
            }

            Element e = resolver.findVariable(annoVal, path);
            // TODO: Handles static fields only as they are
            // resolved in compile time
            if (ElementUtils.isError(e)) {
                if (false && DO_ADVANCED_CHECKS) { // TODO: Enable again
                    checker.report(Result.warning("field.not.found.nullness.parse.error", annoVal), meth);
                }
                // TODO: Check this
                e = findElement(annoVal, flowState.vars);
            }

            int index = this.flowState.vars.indexOf(e);
            if (DO_ADVANCED_CHECKS &&
                !checker.shouldSkipDefs(cls, meth) &&
                !(index != -1 && (this.flowState.annos.get(NONNULL, index) ||
                // TODO: a boxed primitive is sometimes still @Primitive.
                // Is this needed elsewhere? Fix it?
                        this.flowState.annos.get(PRIMITIVE, index)))) {
                checker.report(Result.failure("assert.postcondition.not.satisfied", annoVal), meth);
            }
        }
    }

    /**
     * Verify that the AssertNonNullIfTrue annotation is valid.
     *
     * @param meth The method declaration.
     * @param methElem The corresponding executable element.
     * @param ret The specific return statement within the method.
     */
    private void checkAssertNonNullIfTrue(MethodTree meth, ExecutableElement methElem, ReturnTree ret) {
        AnnotationMirror anno = factory.getDeclAnnotation(methElem, AssertNonNullIfTrue.class);
        List<String> annoValues = AnnotationUtils.elementValueArray(anno, "value");
        checkAssertNonNullIfXXX(meth, methElem, ret, annoValues, true);
    }

    /**
     * Verify that the AssertNonNullIfFalse annotation is valid.
     *
     * @param meth The method declaration.
     * @param methElem The corresponding executable element.
     * @param ret The specific return statement within the method.
     */
    private void checkAssertNonNullIfFalse(MethodTree meth, ExecutableElement methElem, ReturnTree ret) {
        AnnotationMirror anno = factory.getDeclAnnotation(methElem, AssertNonNullIfFalse.class);
        List<String> annoValues = AnnotationUtils.elementValueArray(anno, "value");
        checkAssertNonNullIfXXX(meth, methElem, ret, annoValues, false);
    }

    /**
     * Verify that the AssertNonNullIfTrue or AssertNonNullIfFalse annotation is valid.
     *
     * @param meth The method declaration.
     * @param methElem The corresponding executable element.
     * @param ret The specific return statement within the method.
     * @param annoValues The annotation values to check.
     * @param ifTrue True if the annotation values are from AssertNonNullIfTrue,
     *   false for annotation values from AssertNonNullIfFalse.
     */
    private void checkAssertNonNullIfXXX(MethodTree meth, ExecutableElement methElem, ReturnTree ret,
            List<String> annoValues, boolean ifTrue) {
        ExpressionTree retExp = ret.getExpression();
        if (factory.getAnnotatedType(retExp).getKind() != TypeKind.BOOLEAN) {
            return;
        }

        List<String> toCheck = substitutePatternsDecl(meth, annoValues);
        TreePath clspath = TreeUtils.pathTillClass(getCurrentPath());
        ClassTree cls = (ClassTree) clspath.getLeaf();
        TreePath path = TreePath.getPath(clspath, meth);

        NullnessFlowConditions conds = new NullnessFlowConditions((NullnessAnnotatedTypeFactory)factory, this, debug);
        conds.visit(retExp, null);
        this.takeFromConds(conds);

        // When would this help?
        // boolean flippable = isFlippableLogic(retExp);

        retExp = TreeUtils.skipParens(retExp);

        if (retExp.getKind() == Tree.Kind.BOOLEAN_LITERAL) {
            LiteralTree b = (LiteralTree) retExp;
            boolean val = (Boolean) b.getValue();

            if (ifTrue && val ||
                    !ifTrue && !val) {
                // Check annos?

                for (String check : toCheck) {
                    Element e = resolver.findVariable(check, path);
                    // TODO: Handles static fields only as they are
                    // resolved in compile time
                    if (ElementUtils.isError(e)) {
                        if (false && DO_ADVANCED_CHECKS) { // TODO: Enable again
                            checker.report(Result.warning("field.not.found.nullness.parse.error", check), meth);
                        }
                        // TODO: Check this
                        e = findElement(check, flowState.vars);
                    }

                    int index = this.flowState.vars.indexOf(e);
                    if (DO_ADVANCED_CHECKS &&
                        !checker.shouldSkipDefs(cls, meth) &&
                        !(index != -1 && this.flowState.annos.get(NONNULL, index)) &&
                        !this.flowState.nnExprs.contains(check)) {
                        checker.report(Result.failure(
                                                (ifTrue ? "assertiftrue" : "assertiffalse") + ".postcondition.not.satisfied",
                                                check), ret);
                        }

                }
            } else {
                // We have an IfTrue annotation and visit a "return false"
                // or we have an IfFalse annotation and visit a "return true"
                // -> nothing to do
            }
            return;
        }

        Stack<ExpressionTree> worklist = new Stack<ExpressionTree>();
        worklist.push(retExp);

        boolean checkedAll = true;
        for (String check : toCheck) {
            boolean checked = false;

            for (VariableElement ve : this.flowState.vars) {
                if (ve.getSimpleName().toString().equals(check)) {
                    if (this.flowState.annos.get(NONNULL, this.flowState.vars.indexOf(ve))
                            || this.flowState.nnExprs.contains(check)) {
                        checked = true;
                    }
                    break;
                }
            }

            if (!checked) {
                checkedAll = false;
            }
        }

        if (checkedAll) {
            // We successfully found all Strings to check in annos or nnExprs.
            // We do not have to look at the condition and can go home.
            return;
        }


        // make sure that only the right kind of boolean operation is used
        // TODO: this is a bit too coarse grained I think, subexpressions might be allowed to use
        // other operations.
        while (!worklist.isEmpty()) {
            // By not skipping the parens we keep on the top level.
            // ExpressionTree cond = TreeUtils.skipParens(worklist.pop());
            ExpressionTree cond = worklist.pop();

            if (cond.getKind() == Tree.Kind.CONDITIONAL_AND) {
                if (!ifTrue) {
                    checker.report(Result.failure("assertiffalse.nullness.condition.error"), ret);
                }

                BinaryTree bin = (BinaryTree) cond;
                worklist.push(bin.getLeftOperand());
                worklist.push(bin.getRightOperand());
            }

            if (cond.getKind() == Tree.Kind.CONDITIONAL_OR) {
                if (ifTrue) {
                    checker.report(Result.failure("assertiftrue.nullness.condition.error"), ret);
                }

                BinaryTree bin = (BinaryTree) cond;
                worklist.push(bin.getLeftOperand());
                worklist.push(bin.getRightOperand());
            }
        }

        for (String check : toCheck) {
            Element e = resolver.findVariable(check, path);
            // TODO: Handles static fields only as they are
            // resolved in compile time
            if (ElementUtils.isError(e)) {
                if (false && DO_ADVANCED_CHECKS) { // TODO: Enable again
                    checker.report(Result.warning("field.not.found.nullness.parse.error", check), meth);
                }
                // TODO: Check this
                e = findElement(check, flowState.vars);
            }
            if (ifTrue) {
                if (DO_ADVANCED_CHECKS &&
                    !checker.shouldSkipDefs(cls, meth) &&
                    !conds.getNonnullExpressions().contains(check) &&
                    !conds.getNonnullElements().contains(e) &&
                    !conds.getExplicitNonnullElements().contains(e)) {
                    checker.report(Result.failure(
                                                  "assertiftrue.postcondition.not.satisfied",
                                                  check), ret);
                }
            } else {
                if (DO_ADVANCED_CHECKS &&
                    !checker.shouldSkipDefs(cls, meth) &&
                    !conds.getNullableExpressions().contains(check) &&
                    !conds.getNullableElements().contains(e) &&
                    !conds.getExplicitNullableElements().contains(e)) {
                    checker.report(Result.failure(
                                                  "assertiffalse.postcondition.not.satisfied",
                                                  check), ret);
                }
            }
        }
    }

    /**
     * At call sites, verify that the NonNullOnEntry annotations hold.
     *
     * @param call The method call to check.
     */
    private void checkNonNullOnEntry(MethodInvocationTree call) {
        ExecutableElement method = TreeUtils.elementFromUse(call);

        if (factory.getDeclAnnotation(method, NonNullOnEntry.class) != null) {
            if (debug != null) {
                debug.println("NullnessFlow::checkNonNullOnEntry: Looking at call: " + call);
            }

            Element recvElem;
            List<? extends Element> recvFieldElems;
            List<VariableElement> recvImmediateFields;

            boolean isStatic = method.getModifiers().contains(Modifier.STATIC);
            {
                AnnotatedTypeMirror recvType;

                if (isStatic) {
                    ExecutableElement meth = TreeUtils.elementFromUse(call);
                    recvType = factory.getAnnotatedType(ElementUtils.enclosingClass(meth));
                } else {
                    recvType = this.factory.getReceiverType(call);
                }
                // System.err.printf("checkNonNullOnEntry(%s): recvType=%s, isStatic=%s%n", call, recvType, isStatic);

                if (!(recvType instanceof AnnotatedDeclaredType)) {
                    if (DO_ADVANCED_CHECKS) {
                        System.err.println("Bad recvType: " + recvType + ((recvType == null) ? "" : ("  " + recvType.getClass() + ")")));
                        System.err.println("  for call: " + call);
                    }
                    return;
                }

                recvElem = ((AnnotatedDeclaredType)recvType).getUnderlyingType().asElement();
                recvFieldElems = allFields(recvElem);
                recvImmediateFields = ElementFilter.fieldsIn(recvElem.getEnclosedElements());
            }
            AnnotationMirror nnoe = factory.getDeclAnnotation(method, NonNullOnEntry.class);
            List<String> nnoeExprs = AnnotationUtils.elementValueArray(nnoe, "value");

            for (String nnoeExpr : nnoeExprs) {
                // TODO: use this instead of the below.
                // Element el = Resolver.findVariable(nnoeExpr, PathTree.for(call));
                // if (el==null) {
                //     // output an error message
                //     continue;
                // }

                Element el = findElementInCall(recvElem, recvFieldElems, call, nnoeExpr);
                if (el==null) {
                    // we've already output an error message
                    continue;
                }
                if (factory.isMostEnclosingThisDeref(call) && !recvImmediateFields.contains(el)) {
                    // super class fields already initialized
                    // TODO: Handle nullable and raw fields
                    continue;
                }

                String elName = el.getSimpleName().toString();
                String elClass = el.getEnclosingElement().getSimpleName().toString();

                int index = this.flowState.vars.indexOf(el);
                if (index == -1 || !this.flowState.annos.get(NONNULL, index)) {
                    if (DO_ADVANCED_CHECKS &&
                            !checker.shouldSkipUses(method) &&
                            !this.flowState.nnExprs.contains(elName) &&
                            !this.flowState.nnExprs.contains(elClass + "." + elName)) {
                        checker.report(Result.failure("nonnullonentry.precondition.not.satisfied", nnoeExpr), call);
                    }
                } else {
                    // System.out.println("Success!");
                }
            }
        }
    }

    /**
     * Find the Element that corresponds to the expression (such as a field
     * access) from the point of view of the method declaration.
     *
     * @param recvElem The receiver element.
     * @param recvFieldElems All visible fields in the receiver element.
     * @param call The method call.
     * @param field The field or other expression to find.
     * @return The element corresponding to field within the receiver.
     */
    private Element findElementInCall(Element recvElem, List<? extends Element> recvFieldElems,
            MethodInvocationTree call, String field) {
        List<? extends Element> elemsToSearch;
        String fieldName;

        if (field.contains(".")) {
            // A static field access such as MyClass.myField

            // we only support single static field accesses: C.f but not C.f.g
            String[] parts = field.split("\\.");
            if (parts.length!=2) {
                // TODO: check for explicit "this"
                if (DO_ADVANCED_CHECKS) {
                    checker.report(Result.failure("nullness.parse.error", field), call);
                }
                return null;
            }
            String className = parts[0];
            fieldName = parts[1];

            // XXX It would be better to just resolve the name, in the
            // context of where the annotation that contains it occurs.
            // Try to find the class as an enclosing class.
            Element findClass = recvElem;
            while (findClass!=null &&
                    !findClass.getSimpleName().toString().equals(className)) {
                findClass=findClass.getEnclosingElement();
            }
            if (findClass==null) {
                // XXX The class name is not an enclosing class.
                // So, we should resolve it according to the imports.
                if (DO_ADVANCED_CHECKS) {
                    checker.report(Result.failure("class.not.found.nullness.parse.error", field), call);
                }
                return null;
            }

            elemsToSearch = allFields(findClass);
        } else {
            fieldName = field;
            elemsToSearch = recvFieldElems;
        }

        // whether a field with the name was already found
        boolean found = false;

        Element res = null;

        for (Element el : elemsToSearch) {
            String elName = el.getSimpleName().toString();

            if (fieldName.equals(elName)) {
                // ||   field.equals(elClass + "." + elName)) {
                // TODO: remove checks for hiding?
                if (found) {
                    // We already found a field with the same name
                    // before -> hiding.
                    if (DO_ADVANCED_CHECKS) {
                        checker.report(Result.failure("nonnull.hiding.violated", field), call);
                    }
                    return null;
                } else {
                    found = true;
                    res = el;
                }
            }
        }

        if (!found) {
            if (DO_ADVANCED_CHECKS) {
                checker.report(Result.failure("nullness.parse.error", field), call);
            }
        }
        return res;
    }


    /**
     * Determine all fields that we need to check.
     * Used for NonNullOnEntry and similar annotations.
     *
     * @param el The TypeElement to start with.
     * @return All fields declared in el and all superclasses.
     */
    private static List<VariableElement> allFields(Element el) {
        if (!(el instanceof TypeElement)) {
            System.err.println("NullnessFlow::allFields: the argument should be a TypeElement; it is a: " +
                    el!=null ? el.getClass() : null);
            return null;
        }
        TypeElement tyel = (TypeElement) el;

        List<VariableElement> res = new ArrayList<VariableElement>();

        boolean inSuper = false;

        while (tyel!=null && !ElementUtils.isObject(tyel)) {
            List<VariableElement> toAdd = ElementFilter.fieldsIn(tyel.getEnclosedElements());

            if (inSuper) {
                Iterator<VariableElement> it = toAdd.iterator();
                while (it.hasNext()) {
                    VariableElement newel = it.next();
                    if (newel.getModifiers().contains(Modifier.PRIVATE)) {
                        // do not add private fields from superclasses
                        it.remove();
                    }
                }
            }

            res.addAll(toAdd);

            TypeMirror suty = tyel.getSuperclass();
            inSuper = true;
            if (suty instanceof NoType) {
                break;
            } else if (suty instanceof DeclaredType) {
                DeclaredType dtsuty = (DeclaredType) suty;
                tyel = (TypeElement) dtsuty.asElement();
            } else {
                System.out.printf("unexpected supertype suty=%s (%s)%n  el=%s (%s)%n", suty, suty.getClass(), el, el.getClass());
                break;
            }
        }

        return res;
    }

    /**
     * Make sure that the Strings in the NNOE annotation are valid.
     * The returned list still contains all strings.
     * TODO: describe all changes:
     * The only change is that static fields might be expanded.
     *
     * @param meth The method declaration.
     * @param myFieldElems All fields visible at the declaration.
     * @param fields The NNOE annotation values.
     * @return The validated list of fields.
     */
    private Pair<List<String>, List<VariableElement>> validateNonNullOnEntry(TreePath path, List<? extends Element> myFieldElems, List<String> fields) {
        MethodTree meth = (MethodTree)path.getLeaf();
        List<String> nnExprs = new LinkedList<String>();
        List<VariableElement> nnElems = new LinkedList<VariableElement>();

        for (String field : fields) {
            // Always add the string as is as assumption
            nnExprs.add(field);

            // whether a field with the name was already found
            Element e = resolver.findVariable(field, path);
            // TODO: Handles static fields only as they are
            // resolved in compile time
            if (DO_ADVANCED_CHECKS && ElementUtils.isError(e)) {
                checker.report(Result.failure("field.not.found.nullness.parse.error", field), meth);
            } else if (ElementUtils.isStatic(e) && e instanceof VariableElement) {
                nnElems.add((VariableElement)e);
            }
        }

        return Pair.of(nnExprs, nnElems);
    }


    @Override
    public Void visitReturn(ReturnTree node, Void p) {
        if (debug != null) {
            debug.println("NullnessFlow::visitReturn: " + node);
        }

        super.visitReturn(node, p);

        checkAssertsOnReturn(node);

        return null;
    }

    private void checkAssertsOnReturn(ReturnTree ret) {
        MethodTree meth = TreeUtils.enclosingMethod(factory.getPath(ret));
        ExecutableElement methElem = TreeUtils.elementFromDeclaration(meth);

        if (factory.getDeclAnnotation(methElem, AssertNonNullAfter.class) != null) {
            checkAssertNonNullAfter(meth, methElem);
        }

        if (factory.getDeclAnnotation(methElem, AssertNonNullIfTrue.class) != null) {
            checkAssertNonNullIfTrue(meth, methElem, ret);
        }

        if (factory.getDeclAnnotation(methElem, AssertNonNullIfFalse.class) != null) {
            checkAssertNonNullIfFalse(meth, methElem, ret);
        }
    }


    /**
     * Determines whether a method has a receiver that is {@link Raw} given the
     * AST node for the method's declaration.
     *
     * @param node the method declaration
     * @return true if the method has a {@link Raw} receiver, false otherwise
     */
    private final boolean hasRawReceiver(MethodTree node) {
        return rawFactory.getAnnotatedType(node).getReceiverType().hasAnnotation(RAW);
    }

    /**
     * Convenience method: determine if the given tree is the null literal.
     *
     * @param tree the tree to check
     * @return true if the tree is the null literal, false otherwise
     */
    static final boolean isNull(final Tree tree) {
        return tree != null && tree.getKind() == Tree.Kind.NULL_LITERAL;
    }

    /**
     * Convenience method: determine if the given tree might have a variable
     * element.
     *
     * @param tree the tree to check
     * @return true if the tree may have a variable element, false otherwise
     */
    static final boolean hasVar(final ExpressionTree tree) {
        Tree tr = TreeUtils.skipParens(tree);
        if (tr.getKind() == Tree.Kind.ASSIGNMENT)
            tr = ((AssignmentTree)tr).getVariable();
        return (tr.getKind() == Tree.Kind.IDENTIFIER
                || tr.getKind() == Tree.Kind.MEMBER_SELECT);
    }

    /**
     * Convenience method: get the variable's element for the given tree.
     *
     * @param tree the tree to check
     * @return the element for the variable in the tree
     */
    // TODO: can we remove this method?
    static final Element var(ExpressionTree tree) {
        tree = TreeUtils.skipParens(tree);
        switch (tree.getKind()) {
        case IDENTIFIER:
            return TreeUtils.elementFromUse((IdentifierTree) tree);
        case MEMBER_SELECT:
            return TreeUtils.elementFromUse((MemberSelectTree) tree);
        case ASSIGNMENT:
            return var(((AssignmentTree)tree).getVariable());
        default:
            return null;
            //                throw new UnsupportedOperationException("var from "
            //                        + tree.getKind());
        }
    }

    /**
     * Returns true if it's a pure method invocation or array access.
     * TODO: what if the receiver or array index are not pure?
     */
    final boolean isPure(ExpressionTree tree) {
        tree = TreeUtils.skipParens(tree);
        if (tree.getKind() == Tree.Kind.METHOD_INVOCATION) {
            ExecutableElement method = (ExecutableElement) TreeUtils.elementFromUse(tree);
            boolean result = (factory.getDeclAnnotation(method, Pure.class)) != null;
            return result;
        }
        if (tree.getKind() == Tree.Kind.ARRAY_ACCESS ) {
            return true;
        }

        return false;
    }

    private VariableElement findElement(String elem, Collection<? extends VariableElement> vars) {
        for (VariableElement v: vars) {
            if (v.getSimpleName().toString().equals(elem))
                return v;
        }
        return null;
    }
}
