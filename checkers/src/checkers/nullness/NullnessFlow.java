package checkers.nullness;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.processing.Messager;
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
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.processing.JavacMessager;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.comp.Attr;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Enter;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.comp.MemberEnter;

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

    /*
    protected Context context;
    protected TreeMaker maker;
    protected Names names;
    protected Attr attr;
    protected Enter enter;
    protected MemberEnter memberEnter;
    */

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
      
        /*
        JavacProcessingEnvironment env = (JavacProcessingEnvironment)checker.getProcessingEnvironment();
        context = env.getContext();
        maker = TreeMaker.instance(context);
        names = Names.instance(context);
        attr = Attr.instance(context);
        enter = Enter.instance(context);
        memberEnter = MemberEnter.instance(context);
        */
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

            // TODO: why is this from the surrounding class and not this.nonnullExpressions???
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
            // TODO: why is this from the surrounding class and not this.nonnullExpressions???
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
    private List<String> substitutePatternsDecl(MethodTree method, String[] annoValues) {
    	List<? extends VariableTree> paramTrees = method.getParameters();
    	List<String> params = new ArrayList<String>(paramTrees.size());
    	for(VariableTree vt : paramTrees) {
    		params.add(vt.getName().toString());
    	}
    	
    	return substitutePatternsGeneric(method, null, params, annoValues);
    }
    
    // substitute patterns and ensure that the Strings are formatted according to our conventions
    private List<String> substitutePatternsCall(MethodInvocationTree methodInvok, String[] annoValues) {
    	String receiver = receiver(methodInvok);
    	
    	List<? extends ExpressionTree> argExps = methodInvok.getArguments();
    	List<String> args = new ArrayList<String>(argExps.size());
    	for(ExpressionTree et : argExps) {
    		args.add(et.toString());
    	}
    	
    	return substitutePatternsGeneric(methodInvok, receiver, args, annoValues);
    }
    
    private List<String> substitutePatternsGeneric(Tree node, String receiver, List<String> argparams, String[] annoValues) {
        List<String> asserts = new ArrayList<String>();
        
        fields: for (String s : annoValues) {
            if (parameterPtn.matcher(s).matches()) {
            	// exactly one parameter index, e.g. "#0"
                int param = Integer.valueOf(s.substring(1));
                if (param < argparams.size()) {
                    asserts.add(argparams.get(param).toString());
                } else {
                	checker.report(Result.failure("param.index.nullness.parse.error", s), node);
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
	                	checker.report(Result.failure("param.index.nullness.parse.error", s), node);
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
        if (method.getAnnotation(AssertNonNullIfTrue.class) != null) {
            AssertNonNullIfTrue anno = method.getAnnotation(AssertNonNullIfTrue.class);
            asserts = substitutePatternsCall(methodInvok, anno.value());
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
            asserts = substitutePatternsCall(methodInvok, anno.value());
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
            asserts = substitutePatternsCall(methodInvok, anno.value());
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
            asserts = substitutePatternsCall(methodInvok, anno.value());
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
			
			// AssertNonNullIfXX is checked in visitReturn
			// AssertNonNullAfter is checked in visitMethodEndCallback and visitReturn
			
			if (elem.getAnnotation(AssertNonNullIfTrue.class) != null
					|| elem.getAnnotation(AssertNonNullIfFalse.class) != null) {

				if (factory.getAnnotatedType(meth.getReturnType()).getKind() != TypeKind.BOOLEAN) {
					checker.report(
							Result.failure("assertifxxx.only.on.boolean"),
							meth);
				}
			}
			
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

    private void checkAssertNonNullIfTrue(MethodTree meth, ExecutableElement methElem, ReturnTree ret) {
    	String[] annoValues = methElem.getAnnotation(AssertNonNullIfTrue.class).value();
    	checkAssertNonNullIfXXX(meth, methElem, ret, annoValues, true);
    }
    
    
    private void checkAssertNonNullIfFalse(MethodTree meth, ExecutableElement methElem, ReturnTree ret) {
    	String[] annoValues = methElem.getAnnotation(AssertNonNullIfFalse.class).value();
    	checkAssertNonNullIfXXX(meth, methElem, ret, annoValues, false);
    }
    
    private void checkAssertNonNullIfXXX(MethodTree meth, ExecutableElement methElem, ReturnTree ret,
    		String[] annoValues, boolean ifTrue) {
    	ExpressionTree retExp = ret.getExpression();
    	if (factory.getAnnotatedType(retExp).getKind() != TypeKind.BOOLEAN) {
    		checker.report(Result.failure("assertifxxx.only.on.boolean"), meth);
			return;
    	}
    	
    	List<String> toCheck = substitutePatternsDecl(meth, annoValues);
            	
        Conditions conds = new Conditions();
        conds.visit(retExp, null);

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
					boolean found = false;
					for (VariableElement ve : this.vars) {
						if (ve.getSimpleName().toString().equals(check)) {
							found = true;
							if (!annos.get(NONNULL, vars.indexOf(ve))
									&& !nnExprs.contains(check)) {
								if (ifTrue) {
									checker.report(Result.failure("assertiftrue.postcondition.not.satisfied", check), ret);
								} else {
									checker.report(Result.failure("assertiffalse.postcondition.not.satisfied", check), ret);
								}
							}
						}
					}
					if (!found) {
						if (ifTrue) {
							checker.report(Result.failure("assertiftrue.postcondition.not.satisfied", check), ret);
						} else {
							checker.report(Result.failure("assertiffalse.postcondition.not.satisfied", check), ret);
						}
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
			
			for (VariableElement ve : this.vars) {
				if (ve.getSimpleName().toString().equals(check)) {
					if (annos.get(NONNULL, vars.indexOf(ve))
							|| nnExprs.contains(check)) {
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
			boolean found = false;
			
			if (ifTrue) {
				for (VariableElement ve : conds.getNonnullElements()) {
					if (ve.getSimpleName().toString().equals(check)) {
						found = true;
					}
				}
			} else {
				for (VariableElement ve : conds.getNullableElements()) {
					if (ve.getSimpleName().toString().equals(check)) {
						found = true;
					}
				}
			}

			if (!found) {
				if (ifTrue) {
					checker.report(Result.failure("assertiftrue.postcondition.not.satisfied", check), ret);
				} else {
					checker.report(Result.failure("assertiffalse.postcondition.not.satisfied", check), ret);
				}
			}			
		}
            
    }
    
    /*
    // at call sites, ensure that the NNOE entries hold
    private void checkNonNullOnEntryNew(MethodInvocationTree call) {
        ExecutableElement method = TreeUtils.elementFromUse(call);

		if (method.getAnnotation(NonNullOnEntry.class) != null) {
			if (debug != null) {
				debug.println("NullnessFlow::checkNonNullOnEntry: Looking at call: " + call);
			}

			String[] fields = method.getAnnotation(NonNullOnEntry.class).value();
			ExpressionTree recvTree = TreeUtils.getReceiverTree(call);
			
			// fieldloop:
			for (String field : fields) {				
				Element el = findElementCall(call, field);
				if (el!=null) {
					int index = vars.indexOf(el);
					if (index == -1 || !annos.get(NONNULL, index)) {
						// matching on nnExprs might be unsound, e.g. if both the
						// caller and callee have a field with the same name
						if (!this.nnExprs.contains(field)
								&& !this.nnExprs.contains(recvTree + "." + field)) {
							checker.report(Result.failure("nonnullonentry.precondition.not.satisfied", field), call);
						}
					}
				}
			}
		}
    }
    */
    
    
    /*
    // at call sites, ensure that the NNOE entries hold
    private void checkNonNullOnEntryOld(MethodInvocationTree call) {
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
    */
    
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
				Element el = findElementInCall(recvElem, recvFieldElems, call, field);
				if (el==null) {
					// we've already output an error message
					continue;
				}
				
				String elName = el.getSimpleName().toString();
				String elClass = el.getEnclosingElement().getSimpleName().toString();
				
				int index = vars.indexOf(el);
				if (index == -1 || !annos.get(NONNULL, index)) {
					if (!this.nnExprs.contains(elName)
							&& !this.nnExprs.contains(elClass + "."	+ elName)) {
						checker.report(Result.failure("nonnullonentry.precondition.not.satisfied", field), call);
					}
				} else {
					// System.out.println("Success!");
				}
			}
		}
    }

    private Element findElementInCall(Element recvElem, List<? extends Element> recvFieldElems,
    		MethodInvocationTree call, String field) {
		List<? extends Element> elemsToSearch;
		String fieldName;
    
		if (field.contains(".")) {
			// we only support single static field accesses, i.e. C.f
			String[] parts = field.split("\\.");
			if (parts.length!=2) {
				checker.report(Result.failure("dots.nullness.parse.error", field), call);
				return null;
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
					// ||	field.equals(elClass + "." + elName)) {
				// TODO: remove checks for hiding?
				if (found) {
					// We already found a field with the same name
					// before -> hiding.
					checker.report(Result.failure("nonnull.hiding.violated", field), call);
					return null;
				} else {
					found = true;
					res = el;
				}
			}
		}
		
		if (!found) {
			checker.report(Result.failure("nullness.parse.error", field), call);
		}
		return res;
    }

    
    /*
    private Element findElementCall(MethodInvocationTree call, String field) {
		Element elem;

		ExpressionTree recvTree = TreeUtils.getReceiverTree(call);

		elem = findElementCallHelper(call, field);
		if (elem != null) {
			if (elem.getModifiers().contains(Modifier.STATIC)) {
				System.out.println("static");
				// TODO: I think we might have taken the wrong static field,
				// if the field has the same name in two classes and was
				// specified without the class name.
				// How can we disambiguate??
				return elem;
			} else if (recvTree == null) {
				System.out.println("on this");
				// It was a "this" access -> done
				return elem;
			}
		}
		// If elem is null or it's not a static field/this access, it's
		// certainly the wrong field.
		// Add the receiver expression and try again.
		elem = findElementCallHelper(call, recvTree + "." + field);

		System.out.println("Elem: " + elem.getModifiers() + " "
				+ elem.getSimpleName());
		System.out.println("Elem: " + elem.getEnclosingElement());

		return elem;
	}

    private Element findElementCallHelper(MethodInvocationTree call, String field) {
    	// problems: method elementFromUse always returns an element, even if it
    	// wasn't found
    	// I also didn't find a way to prevent the error message.
    	Element elem;
		try {
			JCTree.JCExpression exp = this.dotsExp(field);
			System.out.println("here: " + exp);
			this.attribute(exp, (JCTree.JCExpression) call, Type.noType);
			System.out.println("or here: " + exp);
			elem = TreeUtils.elementFromUse(exp);
			System.out.println("result: " + exp);
		} catch (Throwable e) {
			System.out
					.println("Ok, \"" + field + "\" is not valid for " + call);
			System.out.println("Exception: " + e);
			e.printStackTrace();
			elem = null;
		}
		return elem;
	}    
    */
    
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


    @Override
    public Void visitReturn(ReturnTree node, Void p) {
    	super.visitReturn(node, p);
    	
    	checkAssertsOnReturn(node);
    	
        return null;
    }

    private void checkAssertsOnReturn(ReturnTree ret) {
    	MethodTree meth = TreeUtils.enclosingMethod(factory.getPath(ret));
    	ExecutableElement methElem = TreeUtils.elementFromDeclaration(meth);
    	
    	if (methElem.getAnnotation(AssertNonNullAfter.class) != null) {
    		checkAssertNonNullAfter(meth, methElem);
    	}
    	
		if (methElem.getAnnotation(AssertNonNullIfTrue.class) != null) {
			checkAssertNonNullIfTrue(meth, methElem, ret);
		}

		if (methElem.getAnnotation(AssertNonNullIfFalse.class) != null) {
			checkAssertNonNullIfFalse(meth, methElem, ret);
		}
    }
  
    
    /*********************************************/
    // Begin tree manipulation section
    // This was largely taken from the EnerJ HelpfulTreeTranslator,
    // which will be integrated into the checker-framework-runtime project
    // TODO: remove redundancies.
    
    /*
     * 
     
    // Create an expression from a string consisting of "dot" accessess --
    // package/subpackage accesses, field accesses, etc.
    // For example: dotsExp("java.util.List")
    protected JCTree.JCExpression dotsExp(String chain) {
        String[] symbols = chain.split("\\.");
        JCTree.JCExpression node = maker.Ident(names.fromString(symbols[0]));
        for (int i = 1; i < symbols.length; i++) {
            Name nextName = names.fromString(symbols[i]);
            node = maker.Select(node, nextName);
        }
        return node;
    }
    
    // Succinctly attribute expressions and statements.
    public void attribute(JCTree.JCExpression expr, JCTree.JCExpression repl, Type type) {        
        attr.attribExpr(expr, getAttrEnv(repl), type);
    }
    
    public void attribute(JCTree.JCExpression expr, JCTree.JCExpression repl) {
        attribute(expr, repl, repl.type);
    }

    protected Env<AttrContext> getAttrEnv(JCTree leaf) {
        JCTree.JCClassDecl class_ = null;
        JCTree.JCMethodDecl method = null;
        
        TreePath path = factory.getPath(leaf);
        class_ = (JCTree.JCClassDecl) TreeUtils.enclosingClass(path);
        method = (JCTree.JCMethodDecl) TreeUtils.enclosingMethod(path);
                
        Env<AttrContext> env = enter.getClassEnv(class_.sym);
        
        env = memberEnter.getMethodEnv(method, env);

        // TODO: local variables from blocks, loops, etc.
        
        return env;
    }
    */
    // End of Tree manipulation section
    /*********************************************/
    
    
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
