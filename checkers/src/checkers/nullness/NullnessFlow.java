package checkers.nullness;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;

import checkers.flow.*;
import checkers.nullness.quals.*;
import checkers.source.Result;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.types.AnnotatedTypeMirror.AnnotatedExecutableType;
import checkers.util.ElementUtils;
import checkers.util.InternalUtils;
import checkers.util.TreeUtils;

import com.sun.source.tree.*;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.*;


/**
 * Implements Nullness-specific customizations of the flow-sensitive type
 * qualifier inference provided by {@link Flow}. In particular, if a
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
 * @see Flow
 * @see NullnessSubchecker
 */
class NullnessFlow extends Flow {

    private final AnnotationMirror POLYNULL, RAW, NONNULL;
    private boolean isNullPolyNull;
    private List<String> nnExprs, nnExprsWhenTrue, nnExprsWhenFalse;
    private final AnnotatedTypeFactory rawFactory;

    /**
     * Creates a NonNull-specific flow-sensitive inference.
     *
     * @param checker the current checker
     * @param root the compilation unit to scan
     * @param annotations the annotations to use
     * @param factory the type factory to use
     */
    public NullnessFlow(NullnessSubchecker checker, CompilationUnitTree root,
            NullnessAnnotatedTypeFactory factory) {
        super(checker, root, Collections.singleton(factory.NONNULL), factory);
        POLYNULL = factory.POLYNULL;
        RAW = factory.RAW;
        NONNULL = factory.NONNULL;
        isNullPolyNull = false;
        this.rawFactory = factory.rawnessFactory;
        nnExprs = new ArrayList<String>();
        nnExprsWhenTrue = nnExprsWhenFalse = null;
    }

    /**
     * Currently, flow can only find the complement of
     * a simple clause
     */
    private static boolean isFlippableLogic(Tree tree) {
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
    protected SplitTuple split() {
    	SplitTuple res = super.split();
        nnExprsWhenFalse = new ArrayList<String>(nnExprs);
        nnExprsWhenTrue = nnExprs;
//        nnExprs = null;
        return res;
    }

    /*
    @Override
    protected void merge() {
        super.merge();
        nnExprs = new ArrayList<String>(nnExprs);
        nnExprs.retainAll(nnExprsWhenFalse);
        nnExprsWhenTrue = nnExprsWhenFalse = null;
    }*/

    private Stack<List<String>> levelnnExprs = new Stack<List<String>>();

    @Override
    protected void pushNewLevel() {
        levelnnExprs.push(nnExprs);
        nnExprs = new ArrayList<String>(nnExprs);
    }

    @Override
    protected void popLastLevel() {
        nnExprs = levelnnExprs.pop();
    }
    @Override
    protected SplitTuple scanCond(Tree tree) {
    	SplitTuple res = super.scanCond(tree);
        if (tree == null)
            return res;

        GenKillBits<AnnotationMirror> before = GenKillBits.copy(res.annosWhenFalse);

        Conditions conds = new Conditions();
        conds.visit(tree, null);

        boolean flippable = isFlippableLogic(tree);

        for (VariableElement elt : conds.getNonnullElements()) {
            int idx = vars.indexOf(elt);
            if (idx >= 0) {
                res.annosWhenTrue.set(NONNULL, idx);
                if (flippable && !conds.excludes.contains(elt))
                    res.annosWhenFalse.clear(NONNULL, idx);
            }
        }

        for (VariableElement elt : conds.getNullableElements()) {
            int idx = vars.indexOf(elt);
            if (idx >= 0) {
                // Mahmood on 01/28/2009:
                // I don't know why annosWhenTrue is cleared.  Its bits being
                // on indicate extra information not captured within the
                // analyzed condition
                // annosWhenTrue.clear(NONNULL, idx);
                if (flippable && !conds.excludes.contains(elt))
                    res.annosWhenFalse.set(NONNULL, idx);
            }
        }
        // annosWhenFalse.or(before);
        GenKillBits.orlub(res.annosWhenFalse, before, annoRelations);

        isNullPolyNull = conds.isNullPolyNull;
        nnExprsWhenTrue.addAll(conds.nonnullExpressions);
        nnExprsWhenFalse.addAll(conds.nullableExpressions);
        
        return res;
    }

    @Override
    public Void visitBinary(BinaryTree node, Void p) {
        if (node.getKind() == Tree.Kind.CONDITIONAL_AND
            || node.getKind() == Tree.Kind.CONDITIONAL_OR) {
            scan(node.getLeftOperand(), p);
            GenKillBits<AnnotationMirror> before = GenKillBits.copy(annos);
            scan(node.getRightOperand(), p);
            annos = before;
        } else {
            scan(node.getLeftOperand(), p);
            scan(node.getRightOperand(), p);
        }
        Conditions conds = new Conditions();
        conds.visit(node, null);
        return null;
    }

    /**
     * A utility class used by a NonNull-specific flow-sensitive qualifier
     * inference to determine nullness in conditionally-executed scopes.
     *
     * <p>
     *
     * This class is implemented as a visitor; it should be only be initially
     * invoked on the conditions of e.g. if statements.
     */
    class Conditions extends SimpleTreeVisitor<Void, Void> {

        private BitSet nonnull = new BitSet(0);
        private BitSet nullable = new BitSet(0);
        public boolean isNullPolyNull = false;
        public List<String> nonnullExpressions = new LinkedList<String>();
        public List<String> nullableExpressions = new LinkedList<String>();

        private final List<VariableElement> vars = new LinkedList<VariableElement>();

        /** Variables that should be ignored when setting annoWhenFalse. */
        final Set<Element> excludes = new HashSet<Element>();

        /**
         * @return the elements that this analysis has determined to be NonNull when
         *         the condition being analyzed is true
         */
        public Set<VariableElement> getNonnullElements() {
            return getElements(true);
        }

        /**
         * @return the elements that this analysis has determined to be Nullable
         *         when the condition being analyzed is true
         */
        public Set<VariableElement> getNullableElements() {
            return getElements(false);
        }

        /**
         * @param isNN true to get NonNull elements, false to get Nullable
         *             elements
         * @return the elements that this analysis has determined to be NonNull (if
         *         isNN is true) or Nullable (if isNN is false) when the condition
         *         being analyzed is true
         */
        private Set<VariableElement> getElements(boolean isNN) {
            Set<VariableElement> result = new HashSet<VariableElement>();
            for (int i = 0; i < vars.size(); i++)
                if ((isNN && nonnull.get(i) && !nullable.get(i))
                     || (!isNN && nullable.get(i) && !nonnull.get(i)))
                    result.add(vars.get(i));
            return Collections.unmodifiableSet(result);
        }

        @Override
        public Void visitUnary(final UnaryTree node, final Void p) {

            visit(node.getExpression(), p);

            if (node.getKind() != Tree.Kind.LOGICAL_COMPLEMENT)
                return null;

            // only invert if cardinal is one
            if (nonnull.cardinality() + nullable.cardinality() == 1) {
                nonnull.xor(nullable);
                nullable.xor(nonnull);
                nonnull.xor(nullable);
            } else {
                // give up
                nonnull.clear();
                nullable.clear();
            }
            isNullPolyNull = false;

            // the false branch of a logic complement of instance is nonnull!
            if (TreeUtils.skipParens(node.getExpression()).getKind() == Tree.Kind.INSTANCE_OF) {
                ExpressionTree expr = ((InstanceOfTree)TreeUtils.skipParens(node.getExpression())).getExpression();
                this.excludes.remove(var(expr));
            }

            nnExprs.addAll(shouldInferNullness(node));
            return null;
        }

        @Override
        public Void visitInstanceOf(InstanceOfTree node, Void p) {

            Tree expr = node.getExpression();
            visit(expr, p);

            if (hasVar(expr)) {
                int idx = vars.indexOf(var(expr));
                nonnull.set(idx);
                nullable.clear(idx);
                excludes.add(var(expr));
            }

            return super.visitInstanceOf(node, p);
        }

        /**
         * Splits the gen-kill sets at a branch, descends into each branch (the
         * left and right children), and merges the gen-kill when the branches
         * rejoin in one of two ways ("and" or "or").
         *
         * @param left the left branch child
         * @param right the right branch child
         * @param mergeAnd true if merging should be done using boolean "and",
         *        false if it should be done using boolean "or"
         */
        private void splitAndMerge(Tree left, Tree right, final boolean mergeAnd) {

            BitSet nonnullOld = (BitSet)nonnull.clone();
            BitSet nullableOld = (BitSet)nullable.clone();

            visit(left, null);

            final BitSet nonnullSplit = (BitSet)nonnull.clone();
            final BitSet nullableSplit = (BitSet)nullable.clone();

            new TreeScanner<Void, Void>() {

                private void record(Element e, Tree node) {
                    int idx = vars.indexOf(e);
                    if (idx >= 0) {
                        if (mergeAnd ? nullableSplit.get(idx) : nonnullSplit.get(idx)) {
                            markTree(node, NONNULL);
                        }
                    }
                    if ((mergeAnd ? nullableExpressions : nonnullExpressions).contains(node.toString())) {
                        markTree(node, NONNULL);
                    }
                }

                @Override
                public Void visitIdentifier(IdentifierTree node, Void p) {
                    Element e = TreeUtils.elementFromUse(node);
                    record(e, node);
                    return super.visitIdentifier(node, p);
                }

                @Override
                public Void visitMemberSelect(MemberSelectTree node, Void p) {
                    Element e = TreeUtils.elementFromUse(node);
                    record(e, node);
                    return super.visitMemberSelect(node, p);
                }

                @Override
                public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
                    if ((mergeAnd ? nullableExpressions : nonnullExpressions).contains(node.toString())) {
                        markTree(node, NONNULL);
                    }
                    return super.visitMethodInvocation(node, p);
                }
            }.scan(right, null);

            nonnull = (BitSet)nonnullOld.clone();
            nullable = (BitSet)nullableOld.clone();

            visit(right, null);

            if (mergeAnd) {
                nonnullSplit.and(nonnull);
                nullableSplit.or(nullable);
            } else {
                nonnullSplit.or(nonnull);
                nullableSplit.or(nullable);
//                nnExprs.clear();
            }

            nonnull = nonnullOld;
            nullable = nullableOld;

            nonnull.or(nonnullSplit);
            nullable.or(nullableSplit);

            // when flow leads to a contradiction: a var is both nullable
            // and nonnull, treat it as a nonnull
            nullable.andNot(nonnull);
        }

        @Override
        public Void visitConditionalExpression(ConditionalExpressionTree node,
                Void p) {

            // (a ? b : c) --> (a && b) || c

            BitSet nonnullOld = (BitSet)nonnull.clone();
            BitSet nullableOld = (BitSet)nullable.clone();

            splitAndMerge(node.getCondition(), node.getTrueExpression(), false);

            BitSet nonnullSplit = (BitSet)nonnull.clone();
            BitSet nullableSplit = (BitSet)nullable.clone();

            visit(node.getFalseExpression(), p);

            nonnullSplit.and(nonnull);
            nullableSplit.and(nullable);

            nonnull = nonnullOld;
            nullable = nullableOld;

            nonnull.or(nonnullSplit);
            nullable.or(nullableSplit);

            return super.visitConditionalExpression(node, p);
        }

        private void mark(Element var, boolean isNonnull) {
            if (var == null)
                return;
            int idx = vars.indexOf(var);
            if (isNonnull) {
                nonnull.set(idx);
                nullable.clear(idx);
            } else {
                nullable.set(idx);
                nonnull.clear(idx);
            }
        }

        @Override
        public Void visitBinary(final BinaryTree node, final Void p) {

            final Tree left = node.getLeftOperand();
            final Tree right = node.getRightOperand();
            final Kind oper = node.getKind();

            if (oper == Tree.Kind.CONDITIONAL_AND)
                splitAndMerge(left, right, false);
            else if (oper == Tree.Kind.CONDITIONAL_OR)
                splitAndMerge(left, right, true);
            else if (oper == Tree.Kind.EQUAL_TO) {
                visit(left, p);
                visit(right, p);

                Element var = null;
                if (hasVar(left) && isNull(right))
                    var = var(left);
                else if (isNull(left) && hasVar(right))
                    var = var(right);

                mark(var, false);

                if (var != null) {
                    if (factory.getAnnotatedType(var).getAnnotation(PolyNull.class.getName()) != null)
                        isNullPolyNull = true;
                } else {
                    AnnotatedTypeMirror leftType = factory.getAnnotatedType(left);
                    AnnotatedTypeMirror rightType = factory.getAnnotatedType(right);
                    if (leftType.hasAnnotation(NONNULL) && !rightType.hasAnnotation(NONNULL))
                        mark(var(right), true);
                    if (rightType.hasAnnotation(NONNULL) && !leftType.hasAnnotation(NONNULL))
                        mark(var(left), true);
                }

                if (isNull(right) && isPure(left))
                    this.nullableExpressions.add(left.toString());
                else if (isNull(left) && isPure(right))
                    this.nullableExpressions.add(right.toString());

            } else if (oper == Tree.Kind.NOT_EQUAL_TO) {
                visit(left, p);
                visit(right, p);

                Element var = null;
                if (hasVar(left) && isNull(right))
                    var = var(left);
                else if (isNull(left) && hasVar(right))
                    var = var(right);

                mark(var, true);

                // Handle Pure methods
                if (isNull(right) && isPure(left))
                    this.nonnullExpressions.add(left.toString());
                else if (isNull(left) && isPure(right))
                    this.nonnullExpressions.add(right.toString());
            }

            return null;
        }

        @Override
        public Void visitIdentifier(final IdentifierTree node, final Void p) {
            final Element e = TreeUtils.elementFromUse(node);
            assert e instanceof VariableElement;
            if (!vars.contains(e))
                vars.add((VariableElement) e);
            return super.visitIdentifier(node, p);
        }

        @Override
        public Void visitMemberSelect(final MemberSelectTree node, final Void p) {
            final Element e = TreeUtils.elementFromUse(node);
            assert e instanceof VariableElement;
            if (!vars.contains(e))
                vars.add((VariableElement) e);
            if (nnExprs.contains(node.toString())) {
                markTree(node, NONNULL);
            }
            return super.visitMemberSelect(node, p);
        }

        @Override
        public Void visitParenthesized(final ParenthesizedTree node, final Void p) {
            // Skip parens.
            return visit(node.getExpression(), p);
        }

        @Override
        public Void visitAssignment(final AssignmentTree node, final Void p) {
            visit(node.getVariable(), p);
            visit(node.getExpression(), p);
            return null;
        }

        @Override
        public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
            super.visitMethodInvocation(node, p);

            this.nonnullExpressions.addAll(shouldInferNullness(node));

            return null;
        }
    }

    private static String receiver(MethodInvocationTree node) {
        ExpressionTree sel = node.getMethodSelect();
        if (sel.getKind() == Tree.Kind.IDENTIFIER)
            return "";
        else if (sel.getKind() == Tree.Kind.MEMBER_SELECT)
            return ((MemberSelectTree)sel).getExpression().toString() + ".";
        throw new AssertionError("Cannot be here");
    }

    private List<String> shouldInferNullness(ExpressionTree node) {
        List<String> result = new ArrayList<String>();
        result.addAll(shouldInferNullnessIfTrue(node));
        result.addAll(shouldInferNullnessIfFalse(node));
        result.addAll(shouldInferNullnessPureNegation(node));
        return result;
    }

    private static final Pattern parameterPtn = Pattern.compile("#(\\d+)");
    
    // substitute patterns and ensure that the Strings are formatted according to our conventions
    private List<String> substitutePatterns(MethodInvocationTree methodInvok, String[] annoValues) {
        List<String> asserts = new ArrayList<String>();
        String receiver = receiver(methodInvok);
        fields: for (String s : annoValues) {
            if (parameterPtn.matcher(s).matches()) {
            	// exactly one parameter index, e.g. "#0"
                int param = Integer.valueOf(s.substring(1));
                if (param < methodInvok.getArguments().size()) {
                    asserts.add(methodInvok.getArguments().get(param).toString());
                } else {
                	checker.report(Result.failure("param.index.nullness.parse.error", s), methodInvok);
                	continue;
                }
            } else if (parameterPtn.matcher(s).find()) {
            	// parameter pattern(s) within the string
            	Matcher matcher = parameterPtn.matcher(s);
				StringBuffer sb = new StringBuffer();
				while (matcher.find()) {
					int param = Integer.valueOf(matcher.group(1));
					if (param < methodInvok.getArguments().size()) {
						String rep = methodInvok.getArguments().get(param).toString();
						matcher.appendReplacement(sb, rep);
					} else {
	                	checker.report(Result.failure("param.index.nullness.parse.error", s), methodInvok);
	                	continue fields;
					}
				}
				matcher.appendTail(sb);
				
				asserts.add(receiver + sb.toString());
            } else {
                asserts.add(receiver + s);
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
        if (method.getAnnotation(AssertNonNullIfTrue.class) != null) {
            AssertNonNullIfTrue anno = method.getAnnotation(AssertNonNullIfTrue.class);
            asserts = substitutePatterns(methodInvok, anno.value());
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
        if (method.getAnnotation(AssertNonNullAfter.class) != null) {
            AssertNonNullAfter anno = method.getAnnotation(AssertNonNullAfter.class);
            asserts = substitutePatterns(methodInvok, anno.value());
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
        if (method.getAnnotation(AssertNonNullIfFalse.class) != null) {
            AssertNonNullIfFalse anno = method.getAnnotation(AssertNonNullIfFalse.class);
            asserts = substitutePatterns(methodInvok, anno.value());
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
        if (method.getAnnotation(AssertNonNullIfFalse.class) != null) {
            AssertNonNullIfFalse anno = method.getAnnotation(AssertNonNullIfFalse.class);
            asserts = substitutePatterns(methodInvok, anno.value());
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
    public Void visitAssert(AssertTree node, Void p) {

        ExpressionTree cond = TreeUtils.skipParens(node.getCondition());
        this.nnExprs.addAll(shouldInferNullness(cond));
        this.nnExprs.addAll(shouldInferNullnessIfFalseNullable(cond));

        if (containsKey(node.getDetail(), checker.getSuppressWarningsKey())
            && cond.getKind() == Tree.Kind.NOT_EQUAL_TO
            && ((BinaryTree)cond).getRightOperand().getKind() == Tree.Kind.NULL_LITERAL) {
            ExpressionTree expr = ((BinaryTree)cond).getLeftOperand();
            String s = TreeUtils.skipParens(expr).toString();
            if (!nnExprs.contains(s))
                nnExprs.add(s);
        }
        super.visitAssert(node, p);

        return null;
    }

    @Override
    public Void visitAssignment(AssignmentTree node, Void p) {
        // clean nnExprs when they are reassigned
        // TODO: need to look deeper into the nnExprs, e.g. see test case in 
        // AssertAfter2, where "get(parent)" is in nnExprs and "parent" is re-assigned.
    	// The following is too simplistic:
    	//   this.nnExprs.remove(node.getVariable().toString());
    	// Just doing a "contains" on each string is probably too coarse, but
    	// would ensure that we do not miss a case.
    	// Instead look in more detail:
    	String var = node.getVariable().toString();
    	Iterator<String> iter = nnExprs.iterator();
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
        super.visitCompoundAssignment(node, p);
    	inferNullness(node.getVariable());
        return null;
    }

    private boolean isTerminating(BlockTree stmt) {
        for (StatementTree tr : stmt.getStatements()) {
            if (isTerminating(tr))
                return true;
        }
        return false;
    }

    private boolean isTerminating(StatementTree stmt) {
        if (stmt instanceof BlockTree) {
            return isTerminating((BlockTree)stmt);
        }

        switch (stmt.getKind()) {
        case THROW:
        case RETURN:
        case BREAK:
        case CONTINUE:
            return true;
        default:
            return false;
        }
    }

    @Override
    protected void whenConditionFalse(ExpressionTree node, Void p) {
        node = TreeUtils.skipParens(node);
        this.nnExprs.addAll(shouldInferNullnessIfFalseNullable(node));
        if (node.getKind() == Tree.Kind.LOGICAL_COMPLEMENT) {
            ExpressionTree unary = ((UnaryTree)node).getExpression();
            this.nnExprs.addAll(shouldInferNullnessAfter(unary));
            this.nnExprs.addAll(shouldInferNullnessIfTrue(unary));
        }
    }

    @Override
    public Void visitIf(IfTree node, Void p) {
        super.visitIf(node, p);

        ExpressionTree cond = TreeUtils.skipParens(node.getCondition());
        if (isTerminating(node.getThenStatement())) {
            if (cond.getKind() == Tree.Kind.LOGICAL_COMPLEMENT)
                this.nnExprs.addAll(shouldInferNullness(((UnaryTree)cond).getExpression()));
            this.nnExprs.addAll(shouldInferNullnessIfFalseNullable(cond));
            this.nnExprs.addAll(shouldInferNullnessPureNegation(cond));
        }
        return null;
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree node, Void p) {

        super.visitMemberSelect(node, p);

        inferNullness(node.getExpression());
        if (nnExprs.contains(node.toString())) {
            markTree(node, NONNULL);
        }

        return null;
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, Void p) {
        super.visitIdentifier(node, p);
        if (nnExprs.contains(node.toString())) {
            markTree(node, NONNULL);
        }

        return null;
    }

    @Override
    public Void visitArrayAccess(ArrayAccessTree node, Void p) {
        super.visitArrayAccess(node, p);

        if (nnExprs.contains(node.toString()))
            markTree(node, NONNULL);

        return null;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        GenKillBits<AnnotationMirror> prev = GenKillBits.copy(annos);
        
        /* Important: check the NonNullOnEntry annotation before calling the
         * super method, as the super method clears all knowledge of fields!
         */
        checkNonNullOnEntry(node);
        
        super.visitMethodInvocation(node, p);
        
        ExecutableElement method = TreeUtils.elementFromUse(node);
        if (method.getAnnotation(AssertParametersNonNull.class) != null) {
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

        for (int i = 0; i < vars.size(); ++i) {
            Element elem = vars.get(i);
            if (elem.getKind() == ElementKind.FIELD
                && elem.getAnnotation(LazyNonNull.class) != null
                && prev.get(NONNULL, i))
                annos.set(NONNULL, i);
        }

        this.nnExprs.addAll(shouldInferNullnessAfter(node));

        if (nnExprs.contains(node.toString())) {
            markTree(node, NONNULL);
        }

        return null;
    }
    
    private void markTree(Tree node, AnnotationMirror anno) {
        flowResults.put(node, anno);
    }

    @Override
    public Void visitLiteral(LiteralTree node, Void p) {
        super.visitLiteral(node, p);

        if (isNullPolyNull && node.getKind() == Tree.Kind.NULL_LITERAL) {
            markTree(node, POLYNULL);
        }
        return null;
    }

    void inferNullness(ExpressionTree expr) {
        Element elt = var(expr);
        if (expr instanceof IdentifierTree)
            elt = TreeUtils.elementFromUse((IdentifierTree) expr);
        else if (expr instanceof MemberSelectTree)
            elt = TreeUtils.elementFromUse((MemberSelectTree) expr);

        if (elt != null && vars.contains(elt)) {
            int idx = vars.indexOf(elt);
            annos.set(NONNULL, idx);
        }
    }

    @Override
    public Void visitMethod(MethodTree meth, Void p) {

        // Cancel assumptions about fields (of this class) for a method with a
        // @Raw receiver.

        GenKillBits<AnnotationMirror> prev = GenKillBits.copy(annos);

        if (hasRawReceiver(meth)) {
            for (int i = 0; i < vars.size(); i++) {
                Element var = vars.get(i);
                if (var.getKind() == ElementKind.FIELD)
                    annos.clear(NONNULL, i);
            }
        }

        List<String> prevNnExprs = new ArrayList<String>(nnExprs);

        Element elem = TreeUtils.elementFromDeclaration(meth);
		if (elem.getAnnotation(NonNullOnEntry.class) != null
				|| elem.getAnnotation(AssertNonNullIfTrue.class) != null
				|| elem.getAnnotation(AssertNonNullIfFalse.class) != null
				|| elem.getAnnotation(AssertNonNullAfter.class) != null) {

			List<? extends Element> myFieldElems;
			{ // block to get all fields of the current class

				AnnotatedTypeMirror myType = factory.getAnnotatedType(TreeUtils.enclosingClass(factory.getPath(meth)));

				if (!(myType instanceof AnnotatedDeclaredType)) {
					System.err.println("NullnessFlow::visitMethod: What's wrong with: " + myType);
					return null;
				}

				Element myElem = ((AnnotatedDeclaredType) myType).getUnderlyingType().asElement();
				myFieldElems = allFields(myElem);
			}

			if (elem.getAnnotation(NonNullOnEntry.class) != null) {
				String[] fields = elem.getAnnotation(NonNullOnEntry.class).value();
				List<String> fieldsList = validateNonNullOnEntry(meth, myFieldElems, fields);
				this.nnExprs.addAll(fieldsList);
			}

			List<String> assertAnnos = new LinkedList<String>();
			if (elem.getAnnotation(AssertNonNullIfTrue.class) != null) {
				String[] patterns = elem.getAnnotation(AssertNonNullIfTrue.class).value();
				assertAnnos.addAll(Arrays.asList(patterns));
			}
			if (elem.getAnnotation(AssertNonNullIfFalse.class) != null) {
				String[] patterns = elem.getAnnotation(AssertNonNullIfFalse.class).value();
				assertAnnos.addAll(Arrays.asList(patterns));
			}
			
			validateAsserts(meth, myFieldElems, assertAnnos);
			
			// AssertNonNullAfter is checked in visitMethodEndCallback and visitReturn
		}
        
        try {
            return super.visitMethod(meth, p);
        } finally {
            annos = prev;
            this.nnExprs = prevNnExprs;
        }
    }
    
    // Callback at the end of a method body. Use this for void methods that have
    // an AssertNonNullAfter annotation.
    public void visitMethodEndCallback(MethodTree meth) {
    	ExecutableElement methElem = TreeUtils.elementFromDeclaration(meth);
    	TypeMirror retType = methElem.getReturnType();
    	
    	if (retType.getKind() == TypeKind.VOID &&
    			methElem.getAnnotation(AssertNonNullAfter.class) != null) {
    		checkAssertNonNullAfter(meth, methElem);
    	}
    	
    }
    
    // Also see checkNonNullOnEntry for comparison 
    private void checkAssertNonNullAfter(MethodTree meth, ExecutableElement methElem) {
		String[] annoValues = methElem.getAnnotation(AssertNonNullAfter.class).value();

		for (String annoVal : annoValues) {
			// whether a field with the name was already found
			boolean found = false;
			// whether a field without the NonNull annotation was found
			boolean error = false;

			List<? extends Element> elemsToSearch;
			String fieldName;
			
			if (parameterPtn.matcher(annoVal).find()) {
            	checker.report(Result.warning("nullness.parse.error", annoVal), meth);
            	continue;
			} else if (annoVal.contains(".")) {
				// we only support single static field accesses, i.e. C.f
				String[] parts = annoVal.split("\\.");
				if (parts.length!=2) {
					checker.report(Result.failure("dots.nullness.parse.error", annoVal), meth);
					continue;
				}
				String className = parts[0];
				fieldName = parts[1];
				
				Element findClass = methElem;
				while (findClass!=null &&
					!findClass.getSimpleName().toString().equals(className)) {
					findClass=findClass.getEnclosingElement();
				}
				if (findClass==null) {
					checker.report(Result.failure("class.not.found.nullness.parse.error", annoVal), meth);
					continue;
				}
				
				elemsToSearch = allFields(findClass);
			} else {
				// interpret as an instance field of "this".
				fieldName = annoVal;
				elemsToSearch = allFields(ElementUtils.enclosingClass(methElem));
			}
			
			for (Element el : elemsToSearch) {
				String elName = el.getSimpleName().toString();
				String elClass = el.getEnclosingElement().getSimpleName().toString();

				if (fieldName.equals(elName)) {
					if (found) {
						// We already found a field with the same name
						// before -> hiding.
						checker.report(Result.failure("nonnull.hiding.violated", annoVal), meth);
						continue;
					} else {
						found = true;
					}
					int index = vars.indexOf(el);
					if (index == -1 || !annos.get(NONNULL, index)) {
						if (!this.nnExprs.contains(elName)
								&& !this.nnExprs.contains(elClass + "."	+ elName)) {
							error = true;
						}
						// Instead of reporting the error here, just
						// record it.
						// Then, if there is hiding, we report hiding
						// first.
						// If there is an error, we report it after the
						// loop.
						// checker.report(Result.failure("nonnullonentry.precondition.not.satisfied",
						// node), node);
					} else {
						// System.out.println("Success!");
						// We want to go through all fields to ensure
						// that we have
						// no problem with hiding of fields.
						// Once hiding is handled in a nicer way, we can
						// directly jump to the outer loop.
						// continue fieldloop;
					}
				}
			}

			if(!found || error) {
				checker.report(Result.failure("assert.postcondition.not.satisfied", annoVal), meth);
			}
		}
	}

    	
    
    
    
    // at call sites, ensure that the NNOE entries hold
    private void checkNonNullOnEntry(MethodInvocationTree call) {
        ExecutableElement method = TreeUtils.elementFromUse(call);

		if (method.getAnnotation(NonNullOnEntry.class) != null) {
			if (debug != null) {
				debug.println("NullnessFlow::checkNonNullOnEntry: Looking at call: " + call);
			}

			Element recvElem;
			List<? extends Element> recvFieldElems;
			{ // block to get all fields of the receiver type
				// TODO: move to a separate method and only call when it's needed in
				// the loop. Now we create this list even for static fields where it is not used.
				ExpressionTree recv = TreeUtils.getReceiverTree(call);
				
				AnnotatedTypeMirror recvType;
				if (recv==null) {
					recvType = factory.getAnnotatedType(TreeUtils.enclosingClass(factory.getPath(call)));
				} else {
					recvType = factory.getAnnotatedType(recv);
				}
				
				if (!(recvType instanceof AnnotatedDeclaredType)) {
					System.err.println("What's wrong with: " + recvType);
			    	return;
				}
				
				recvElem = ((AnnotatedDeclaredType)recvType).getUnderlyingType().asElement();
				recvFieldElems = allFields(recvElem);
			}

			String[] fields = method.getAnnotation(NonNullOnEntry.class).value();

			// fieldloop:
			for (String field : fields) {
				// whether a field with the name was already found
				boolean found = false;
				// whether a field without the NonNull annotation was found
				boolean error = false;

				List<? extends Element> elemsToSearch;
				String fieldName;
				
				if (field.contains(".")) {
					// we only support single static field accesses, i.e. C.f
					String[] parts = field.split("\\.");
					if (parts.length!=2) {
						checker.report(Result.failure("dots.nullness.parse.error", field), call);
						continue;
					}
					String className = parts[0];
					fieldName = parts[1];
					
					Element findClass = recvElem;
					while (findClass!=null &&
						!findClass.getSimpleName().toString().equals(className)) {
						findClass=findClass.getEnclosingElement();
					}
					if (findClass==null) {
						checker.report(Result.failure("class.not.found.nullness.parse.error", field), call);
						continue;
					}
					
					elemsToSearch = allFields(findClass);
				} else {
					fieldName = field;
					elemsToSearch = recvFieldElems;
				}
				
				for (Element el : elemsToSearch) {
					String elName = el.getSimpleName().toString();
					String elClass = el.getEnclosingElement().getSimpleName().toString();

					if (fieldName.equals(elName)) {
							// ||	field.equals(elClass + "." + elName)) {
						// TODO: remove checks for hiding?
						if (found) {
							// We already found a field with the same name
							// before -> hiding.
							checker.report(Result.failure("nonnull.hiding.violated", field), call);
							continue;
						} else {
							found = true;
						}
						int index = vars.indexOf(el);
						if (index == -1 || !annos.get(NONNULL, index)) {
							if (!this.nnExprs.contains(elName)
									&& !this.nnExprs.contains(elClass + "."	+ elName)) {
								error = true;
							}
							// Instead of reporting the error here, just
							// record it.
							// Then, if there is hiding, we report hiding
							// first.
							// If there is an error, we report it after the
							// loop.
							// checker.report(Result.failure("nonnullonentry.precondition.not.satisfied",
							// node), node);
						} else {
							// System.out.println("Success!");
							// We want to go through all fields to ensure
							// that we have
							// no problem with hiding of fields.
							// Once hiding is handled in a nicer way, we can
							// directly jump to the outer loop.
							// continue fieldloop;
						}
					}
				}

				if(!found || error) {
					checker.report(Result.failure("nonnullonentry.precondition.not.satisfied", field), call);
				}
			}
		}
    }
    
    /**
     * Determine all fields that we need to check.
     * Used for NonNullOnEntry.
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
    	
    	while (tyel!=null && !ElementUtils.isObject(tyel)) {
    		res.addAll(ElementFilter.fieldsIn(tyel.getEnclosedElements()));
    		TypeMirror suty = tyel.getSuperclass();
    		if (suty instanceof DeclaredType) {
    			DeclaredType dtsuty = (DeclaredType) suty;
    			tyel = (TypeElement) dtsuty.asElement();
    		} else {
    			System.out.println("What's happening here?? " + el);
    			break;
    		}
    	}
    	
    	return res;
    }
    

    // Make sure that the Strings in the NNOE annotation are valid.
    // The returned list contains supported strings
    private List<String> validateNonNullOnEntry(MethodTree meth, List<? extends Element> myFieldElems, String[] fields) {
    	List<String> res = new LinkedList<String>();
    	
		for (String field : fields) {
			// whether a field with the name was already found
			boolean found = false;

			List<? extends Element> elemsToSearch;
			
			if (field.contains(".")) {
				// we only support single static field accesses, i.e. C.f
				String[] parts = field.split("\\.");
				if (parts.length!=2) {
					checker.report(Result.failure("dots.nullness.parse.error", field), meth);
					continue;
				}
				String className = parts[0];
				
				Element findClass = ElementUtils.enclosingClass(TreeUtils.elementFromDeclaration(meth));
				while (findClass!=null &&
					!findClass.getSimpleName().toString().equals(className)) {
					findClass=findClass.getEnclosingElement();
				}
				if (findClass==null) {
					checker.report(Result.failure("class.not.found.nullness.parse.error", field), meth);
					continue;
				}
				
				elemsToSearch = allFields(findClass);
			} else {
				elemsToSearch = myFieldElems;
			}

			
			for (Element el : elemsToSearch) {
				// whether one of the cases matched
				boolean matched = false;
				
				String elName = el.getSimpleName().toString();
				String elClass = el.getEnclosingElement().getSimpleName().toString();
				// System.out.println("field: " + field);
				// System.out.println("elName: " + elName);
								
				if (field.equals(elName)) {
					// if the field is static, add the class name
					if (el.getModifiers().contains(Modifier.STATIC)) {
						res.add(elClass + "." + elName);
					}
					res.add(elName);
					matched = true;
				} else if (field.equals("this." + elName)) {
					// remove the explicit "this"
					res.add(elName);
					matched = true;
				} else if (field.equals(elClass + "." + elName)) {
					if (!el.getModifiers().contains(Modifier.STATIC)) {
						checker.report(Result.failure("nonnull.nonstatic.with.class", field), meth);
						continue;
					}
					res.add(field);
					res.add(elName);
					matched = true;
				}
				
				if (matched) {
					if (found) {
						// We already found a field with the same name before -> hiding.
						checker.report(Result.failure("nonnull.hiding.violated", field), meth);
					} else {
						found = true;
					}
				}
			}
			// TODO: Method calls?
			
			if (!found) {
				checker.report(Result.failure("field.not.found.nullness.parse.error", field), meth);
			}
		}
		return res;
    }

    private void validateAsserts(MethodTree meth, List<? extends Element> myFieldElems, List<String> patterns) {
    	// System.out.println("TODO: look at Assert annot");
    }
    

    @Override
    public Void visitReturn(ReturnTree node, Void p) {
    	super.visitReturn(node, p);
    	
    	checkAssertsOnReturn(node);
    	
        return null;
    }

    private void checkAssertsOnReturn(ReturnTree ret) {
    	MethodTree meth = TreeUtils.enclosingMethod(factory.getPath(ret));
    	ExecutableElement methElem = TreeUtils.elementFromDeclaration(meth);
    	
    	if ( methElem.getAnnotation(AssertNonNullAfter.class) != null) {
    		checkAssertNonNullAfter(meth, methElem);
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
    private final boolean isNull(final Tree tree) {
        return tree != null && tree.getKind() == Tree.Kind.NULL_LITERAL;
    }

    /**
     * Convenience method: determine if the given tree might have a variable
     * element.
     *
     * @param tree the tree to check
     * @return true if the tree may have a variable element, false otherwise
     */
    private final boolean hasVar(final Tree tree) {
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
    private final Element var(Tree tree) {
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
     * Returns true if it's a method invocation of pure
     */
    private boolean isPure(Tree tree) {
        tree = TreeUtils.skipParens(tree);
        if (tree.getKind() != Tree.Kind.METHOD_INVOCATION)
            return false;
        ExecutableElement method = TreeUtils.elementFromUse((MethodInvocationTree)tree);
        return (method.getAnnotation(Pure.class)) != null;
    }

    @Override
    public Void visitConditionalExpression(ConditionalExpressionTree node,
            Void p) {

        // Split and merge as for an if/else.
    	SplitTuple res = scanCond(node.getCondition());

        List<String> prevNNExprs = new ArrayList<String>(nnExprs);

        GenKillBits<AnnotationMirror> before = res.annosWhenFalse;
        annos = res.annosWhenTrue;

        nnExprs = nnExprsWhenTrue;
        scanExpr(node.getTrueExpression());
        GenKillBits<AnnotationMirror> after = GenKillBits.copy(annos);
        annos = before;

        nnExprs = nnExprsWhenFalse;
        scanExpr(node.getFalseExpression());
        // annos.and(after);
        GenKillBits.andlub(annos, after, annoRelations);

        nnExprs = prevNNExprs;
        return null;
    }

}
