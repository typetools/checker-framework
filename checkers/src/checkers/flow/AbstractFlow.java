package checkers.flow;

import java.io.PrintStream;
import java.util.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import checkers.basetype.BaseTypeChecker;
import checkers.source.SourceChecker;
import checkers.types.*;
import checkers.types.AnnotatedTypeMirror.AnnotatedDeclaredType;
import checkers.util.AnnotationUtils;
import checkers.util.ElementUtils;
import checkers.util.Pair;
import checkers.util.TreeUtils;
import checkers.nullness.quals.*;

import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;

/**
 * Provides a generalized flow-sensitive qualifier inference for the checkers
 * framework.
 *
 * <p>
 *
 * This implementation is based largely on {@code javac}'s dataflow analysis
 * module, which may be found in {@code com.sun.tools.javac.comp.Flow} from 13
 * Sep 2007. It differs from that class in two ways:
 *
 * <ol>
 * <li value="1">
 * It performs a GEN-KILL analysis for qualifiers that is similar to the
 * initialization/uninitialization analysis in {@code javac}'s {@code Flow}.
 * It does not perform exception analysis, and performs liveness analysis only
 * to the extent required for the GEN-KILL analysis.
 *
 * <li value="2">
 * Whenever possible, this implementation prefers the use of classes in the
 * public API ({@link BitSet}, the Compiler Tree API) over those in
 * {@code com.sun.tools.javac} (for these reasons, examining a diff against that
 * class would not be particularly enlightening).
 * </ol>
 *
 * As in {@code javac}'s {@code Flow} class, methods named "visit*" perform
 * analysis for a particular type of tree node, while methods named "scan*"
 * perform the analysis for a general, higher-level class of structural element.
 * Typically "visit*" methods delegate to "scan*" methods. As an example,
 * {@link #visitIf}, {@link #visitWhileLoop}, {@link #visitAssert}, and
 * {@link #visitConditionalExpression} all use {@link #scanCond} for analyzing
 * conditions and handling branching.
 *
 * <p>
 *
 * A separate instance of the analysis must be created for each compilation
 * unit.
 */
public abstract class AbstractFlow<ST extends FlowState> extends TreePathScanner<Void, Void>
implements Flow {

    /** Where to print debugging messages; set via {@link #setDebug}. */
    protected PrintStream debug = null;

    /** The checker to which this instance belongs. */
    protected final SourceChecker checker;

    /** The file that's being analyzed. */
    protected final CompilationUnitTree root;

    /** Utility class for determining annotated types. */
    protected final AnnotatedTypeFactory factory;

    /** Utility class for operations on annotated types. */
    protected final AnnotatedTypes atypes;

    /** Stores the results of the analysis (source location to qualifier). */
    protected final Map<Tree, Set<AnnotationMirror>> flowResults;

    /**
     * Tracks the state of the inference.
     * If this field is non-null, the flowState_whenTrue and flowState_whenFalse
     * fields are null/ignored.
     */
    protected ST flowState;

    /**
     * Tracks the state of the inference in a true branch. As in
     * {@code javac}'s {@code Flow}, saving/restoring via local variables
     * handles nested branches.
     * This field is only non-null/valid if flowState is null.
     *
     * @see #flowState
     */
    protected ST flowState_whenTrue;

    /**
     * Tracks the state of the inference in a false branch. As in
     * {@code javac}'s {@code Flow}, saving/restoring via local variables
     * handles nested branches.
     * This field is only non-null/valid if flowState is null.
     *
     * @see #flowState
     */
    protected ST flowState_whenFalse;


    /**
     * Stores the result of liveness analysis, required by the GEN-KILL analysis
     * for proper handling of jumps (break, return, throw, etc.).
     */
    protected boolean alive = true;

    /** Tracks the state in try blocks to support exceptions. */
    private final Deque<ST> tryBits;

    /** Visitor state; tracking is required for checking receiver types. */
    private final VisitorState visitorState;

    /** The hierarchy for the type qualifiers that this class infers. */
    protected final QualifierHierarchy annoRelations;

    /** Memoization for {@link #varDefHasAnnotation(AnnotationMirror, Element)}. */
    private final Map<Pair<Element, AnnotationMirror>, Boolean> annotatedVarDefs = new HashMap<Pair<Element, AnnotationMirror>, Boolean>();

    /**
     * Creates a new analysis. The analysis will use the given {@link
     * AnnotatedTypeFactory} to obtain annotated types.
     *
     * @param checker the current checker
     * @param root the compilation unit that will be scanned
     * @param annotations the annotations to track
     * @param factory the factory class that will be used to get annotated
     *        types, or {@code null} if the default factory should be used
     */
    public AbstractFlow(BaseTypeChecker checker, CompilationUnitTree root,
            Set<AnnotationMirror> annotations, AnnotatedTypeFactory factory) {

        this.checker = checker;
        ProcessingEnvironment env = checker.getProcessingEnvironment();
        this.root = root;

        if (factory == null) {
            this.factory = new AnnotatedTypeFactory(checker, root);
        } else {
            this.factory = factory;
        }

        this.atypes = new AnnotatedTypes(env, factory);
        this.visitorState = this.factory.getVisitorState();
        this.flowResults = new IdentityHashMap<Tree, Set<AnnotationMirror>>();
        this.tryBits = new LinkedList<ST>();
        this.annoRelations = checker.getQualifierHierarchy();
        this.flowState = createFlowState(annotations);
    }

    /**
     * Create the correct instance of FlowState.
     *
     * @param annotations The annotations that can be inferred.
     * @return A new instance of the FlowState to use.
     */
    protected abstract ST createFlowState(Set<AnnotationMirror> annotations);

    @Override
    public void setDebug(PrintStream debug) {
        this.debug = debug;
    }

    @Override
    public void scan(Tree tree) {
        this.scan(tree, null);
    }

    @Override
    public Void scan(Tree tree, Void p) {
        if (tree != null && tree.getKind() == Tree.Kind.COMPILATION_UNIT)
            return scan(checker.currentPath, p);
        if (tree != null && getCurrentPath() != null)
            this.visitorState.setPath(new TreePath(getCurrentPath(), tree));
        return super.scan(tree, p);
    }

    /**
     * Determines the inference result for tree.
     *
     * @param tree the tree to test
     * @return the annotation inferred for a tree, or null if no annotation was
     *         inferred for that tree
     */
    @Override
    public Set<AnnotationMirror> test(Tree tree) {
        while (tree.getKind() == Tree.Kind.ASSIGNMENT) {
            tree = ((AssignmentTree)tree).getVariable();
        }
        if (!flowResults.containsKey(tree)) {
            return null;
        }
        // a hack needs to be fixed
        // always follow variable declarations
        Set<AnnotationMirror> flowResult = flowResults.get(tree);

        return flowResult;
    }

    public static void addFlowResult(Map<Tree, Set<AnnotationMirror>> flowResults, Tree tree, AnnotationMirror anno) {
        // This method doesn't handle multiple annotations from one hierarchy correctly.
        Set<AnnotationMirror> set = AnnotationUtils.createAnnotationSet();
        if (flowResults.containsKey(tree)) {
            set.addAll(flowResults.get(tree));
        }
        set.add(anno);
        flowResults.put(tree, set);
    }

    public static void removeFlowResult(Map<Tree, Set<AnnotationMirror>> flowResults, Tree tree, AnnotationMirror anno) {
        Set<AnnotationMirror> set = AnnotationUtils.createAnnotationSet();
        if (flowResults.containsKey(tree)) {
            set.addAll(flowResults.get(tree));
        } else {
            return;
        }

        if (AnnotationUtils.containsSame(set, anno)) {
            // Careful, remove ignores annotation argument. Do your own check first.
            set.remove(anno);
        }

        if (!set.isEmpty()) {
            flowResults.put(tree, set);
        } else {
            flowResults.remove(tree);
        }
    }


    /**
     * Registers a new variable for flow tracking.
     *
     * @param tree the variable to register
     */
    protected abstract void newVar(VariableTree tree);

    /**
     * Moves bits as assignments are made.
     *
     * <p>
     *
     * If only type information (and not a {@link Tree}) is available, use
     * {@link #propagateFromType(Tree, AnnotatedTypeMirror)} instead.
     *
     * @param lhs the left-hand side of the assignment
     * @param rhs the right-hand side of the assignment
     */
    protected abstract void propagate(Tree lhs, ExpressionTree rhs);

    /**
     * Moves bits in an assignment using a type instead of a tree.
     *
     * <p>
     *
     * {@link #propagate(Tree, Tree)} is preferred, since it is able to use
     * extra information about the right-hand side (such as its element). This
     * method should only be used when a type (and nothing else) is available,
     * such as when checking the variable in an enhanced for loop against the
     * iterated type (which is the type argument of an {@link Iterable}).
     *
     * @param lhs the left-hand side of the assignment
     * @param rhs the type of the right-hand side of the assignment
     */
    abstract void propagateFromType(Tree lhs, AnnotatedTypeMirror rhs);

    /**
     * @param path the path to check
     * @return true if the path leaf is part of an expression used as an lvalue
     */
    private boolean isLValue(TreePath path) {
        Tree last = null;
        // In the following loop, "tree" refers to the current path element
        // and "last" refers to the most recent path element (which is a child
        // of "tree". The loop determines whether the path leaf is a variable
        // immediately enclosed by an assignment or compound assignment.
        for (Tree tree : path) {
            if (tree.getKind() == Tree.Kind.IDENTIFIER) {
                break; // TODO: do nothing
            } else if (tree instanceof AssignmentTree) {
                return last == ((AssignmentTree)tree).getVariable();
            } else if (tree instanceof CompoundAssignmentTree) {
                return last == ((CompoundAssignmentTree)tree).getVariable();
            }

            if (last != null) {
                break;
            }
            last = tree;
        }
        return false;
    }

    /**
     * Record the value of the annotation bit for the given usage of a
     * variable, so that a type-checker may use its value after the analysis
     * has finished.
     *
     * @param path
     */
    protected void recordBits(TreePath path) {
        if (isLValue(path))
            return;

        Tree tree = path.getLeaf();

        Element elt;
        if (tree instanceof MemberSelectTree) {
            elt = TreeUtils.elementFromUse((MemberSelectTree)tree);
        } else if (tree instanceof IdentifierTree) {
            elt = TreeUtils.elementFromUse((IdentifierTree)tree);
        } else if (tree instanceof VariableTree) {
            elt = TreeUtils.elementFromDeclaration((VariableTree)tree);
        } else {
            return;
        }

        recordBitsImps(tree, elt);
    }

    // **********************************************************************

    protected abstract void recordBitsImps(Tree tree, Element elt);

    /**
     * Called whenever a definition is scanned.
     *
     * @param tree the definition being scanned
     */
    protected void scanDef(Tree tree) {
        alive = true;
        scan(tree, null);
    }

    /**
     * Called whenever a statement is scanned.
     *
     * @param tree the statement being scanned
     */
    protected void scanStat(StatementTree tree) {
        alive = true;
        scan(tree, null);
    }

    /**
     * Called whenever a block of statements is scanned.
     *
     * @param trees the statements being scanned
     */
    protected void scanStats(List<? extends StatementTree> trees) {
        scan(trees, null);
    }

    /**
     * Called whenever a conditional expression is scanned.
     *
     * @param tree the condition being scanned
     */
    protected void scanCond(ExpressionTree tree) {
        alive = true;
        if (tree != null) {
            scan(tree, null);
        }
        if (flowState != null) {
            flowState_whenFalse = copyState(flowState);
            flowState_whenTrue = flowState;
            flowState = null;
        } else {
            assert false : "Incorrect call of scanCond!";
        }
    }

    /**
     * Copy the current state.
     * We need the unchecked cast, because there is no way to
     * type that "copy" will return the same type.
     *
     * @return A deep copy of the current state.
     */
    @SuppressWarnings("unchecked")
    protected ST copyState(ST in) {
        return (ST) in.copy();
    }


    /**
     * Called whenever an expression is scanned.
     *
     * @param tree the expression being scanned
     */
    protected void scanExpr(ExpressionTree tree) {
        alive = true;
        scan(tree, null);
        assert flowState != null;
    }

    // **********************************************************************

    @Override
    public Void visitClass(ClassTree node, Void p) {
        AnnotatedDeclaredType preClassType = visitorState.getClassType();
        ClassTree preClassTree = visitorState.getClassTree();
        AnnotatedDeclaredType preAMT = visitorState.getMethodReceiver();
        MethodTree preMT = visitorState.getMethodTree();

        visitorState.setClassType(factory.getAnnotatedType(node));
        visitorState.setClassTree(node);
        visitorState.setMethodReceiver(null);
        visitorState.setMethodTree(null);

        try {
            scan(node.getModifiers(), p);
            scan(node.getTypeParameters(), p);
            scan(node.getExtendsClause(), p);
            scan(node.getImplementsClause(), p);
            // Ensure that all fields are scanned before scanning methods.
            for (Tree t : node.getMembers()) {
                if (t.getKind() == Tree.Kind.METHOD) continue;
                scan(t, p);
            }
            for (Tree t : node.getMembers()) {
                if (t.getKind() != Tree.Kind.METHOD) continue;
                scan(t, p);
            }
            return null;
        } finally {
            this.visitorState.setClassType(preClassType);
            this.visitorState.setClassTree(preClassTree);
            this.visitorState.setMethodReceiver(preAMT);
            this.visitorState.setMethodTree(preMT);
        }
    }

    @Override
    public Void visitImport(ImportTree tree, Void p) {
        return null;
    }

    @Override
    public Void visitTypeCast(TypeCastTree node, Void p) {
        super.visitTypeCast(node, p);
        AnnotatedTypeMirror nodeType = factory.fromTypeTree(node.getType());
        if (nodeType.isAnnotated())
            return null;
        AnnotatedTypeMirror t = factory.getAnnotatedType(node.getExpression());

        if ((nodeType.getKind() == TypeKind.TYPEVAR ||
                nodeType.getKind() == TypeKind.WILDCARD) &&
                (t.getKind() != TypeKind.TYPEVAR &&
                t.getKind() != TypeKind.WILDCARD)) {
            // Do not propagate annotations from a non-type variable/wildcard
            // onto a type variable/wildcard.
            return null;
        }

        for (AnnotationMirror a : this.flowState.getAnnotations()) {
            if (t.hasAnnotation(a)) {
                addFlowResult(flowResults, node, a);
            }
        }
        return null;
    }

    @Override
    public Void visitAnnotation(AnnotationTree tree, Void p) {
        return null;
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, Void p) {
        super.visitIdentifier(node, p);
        recordBits(getCurrentPath());
        return null;
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree node, Void p) {
        super.visitMemberSelect(node, p);
        recordBits(getCurrentPath());
        return null;
    }

    @Override
    public Void visitVariable(VariableTree node, Void p) {
        newVar(node);
        ExpressionTree init = node.getInitializer();
        if (init != null) {
            scanExpr(init);
            VariableElement elem = TreeUtils.elementFromDeclaration(node);
            AnnotatedTypeMirror type = factory.fromMember(node);
            if (!isNonFinalField(elem) && !type.isAnnotated()) {
                // Set the assignment context for array component type inference.
                // Note that flow is performed before the BaseTypeVisitor could set this
                // information.
                Tree preAssCtxt = visitorState.getAssignmentContextTree();
                factory.getVisitorState().setAssignmentContextTree(node);
                try {
                    propagate(node, init);
                    recordBits(getCurrentPath());
                } finally {
                    factory.getVisitorState().setAssignmentContextTree(preAssCtxt);
                }
            }
        }
        return null;
    }

    @Override
    public Void visitAssignment(AssignmentTree node, Void p) {
        ExpressionTree var = node.getVariable();
        ExpressionTree expr = node.getExpression();
        if (!(var instanceof IdentifierTree))
            scanExpr(var);
        scanExpr(expr);
        propagate(var, expr);
        if (var instanceof IdentifierTree)
            this.scan(var, p);
        return null;
    }

    // This is an exact copy of visitAssignment()
    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, Void p) {
        ExpressionTree var = node.getVariable();
        ExpressionTree expr = node.getExpression();
        // if (!(var instanceof IdentifierTree))
        scanExpr(var);
        scanExpr(expr);
        propagate(var, node);
        // if (var instanceof IdentifierTree)
        //     this.scan(var, p);

        // WMD added this to get (s2 = (s1 += 1)) working.
        // Is something similar needed for other expressions?
        // I copied this from visitTypeCast, so maybe it's needed elsewhere, too.
        AnnotatedTypeMirror t = factory.getAnnotatedType(var);
        for (AnnotationMirror a : this.flowState.getAnnotations()) {
            if (t.hasAnnotation(a)) {
                addFlowResult(flowResults, node, a);
            }
        }

        return null;
    }

    protected static boolean containsKey(Tree tree, Collection<String> keys) {
        if (tree == null)
            return false;

        String treeStr = tree.toString();
        for (String key : keys) {
            if (treeStr.contains(key))
                return true;
        }
        return false;
    }

    /**
     * Determine whether information should be inferred from the assert tree.
     *
     * @return true iff information should be inferred.
     */
    protected static boolean inferFromAssert(AssertTree node, SourceChecker checker) {
        return containsKey(node.getDetail(), checker.getSuppressWarningsKey())
                || checker.getLintOption("flow:inferFromAsserts", false);
    }

    @Override
    public Void visitAssert(AssertTree node, Void p) {
        ST beforeAssert = copyState(flowState);
        scanCond(node.getCondition());
        ST whenTrue = flowState_whenTrue;
        ST whenFalse = flowState_whenFalse;

        flowState = whenFalse;
        scanExpr(node.getDetail());
        if (inferFromAssert(node, checker)) {
            flowState = whenTrue;
        } else {
            flowState = beforeAssert;
        }
        return null;
    }

    /*
     * What is the difference between the "alive" field and using isTerminating?
     * I move isTerminating from NullnessFlow to up here.
     */
    /*
    protected static boolean isTerminating(BlockTree stmt) {
        for (StatementTree tr : stmt.getStatements()) {
            if (isTerminating(tr))
                return true;
        }
        return false;
    }

    protected static boolean isTerminating(StatementTree stmt) {
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
    */

    @Override
    public Void visitIf(IfTree node, Void p) {
        scanCond(node.getCondition());
        ST whenTrue = flowState_whenTrue;
        ST whenFalse = flowState_whenFalse;

        ST beforeElse = whenFalse;

        flowState = whenTrue;

        boolean aliveBeforeThen = alive;
        scanStat(node.getThenStatement());

        StatementTree elseStmt = node.getElseStatement();
        if (elseStmt != null ) {
            boolean aliveAfterThen = alive;
            alive = aliveBeforeThen;
            ST afterThen = copyState(flowState);
            flowState = whenFalse;
            scanStat(elseStmt);

            if (!alive) {
                // the else branch is not alive at the end
                // we use the liveness-result from the then branch
                alive = aliveAfterThen;
                afterThen.or(flowState, annoRelations);
                flowState = afterThen;
            } else if (!aliveAfterThen) {
                // annos = annos;  // NOOP
                // TODO: what's the point of this branch?
                // We are at the end of an else branch, where the then branch is dead.
                // We continue to use the state at the end of the else branch.
            } else {
                // both branches are alive
                // alive = true;
                flowState.and(afterThen, annoRelations);
            }
        } else {
            if (!alive) {
                // there is no alias to beforeElse, so copy is not needed
                // flowState = copyState(beforeElse);
                flowState = beforeElse;
            } else {
                flowState.and(beforeElse, annoRelations);
            }
        }

        return null;
    }

    @Override
    public Void visitConditionalExpression(ConditionalExpressionTree node, Void p) {
        // Split and merge as for an if/else.
        scanCond(node.getCondition());
        ST whenTrue = flowState_whenTrue;
        ST whenFalse = flowState_whenFalse;

        flowState = whenTrue;
        scanExpr(node.getTrueExpression());
        ST after = copyState(flowState);

        flowState = whenFalse;
        scanExpr(node.getFalseExpression());
        flowState.and(after, annoRelations);

        return null;
    }

    @Override
    public Void visitWhileLoop(WhileLoopTree node, Void p) {
        ST stEntry;
        ST stCondTrue;
        ST stCondFalse;
        boolean pass = false;

        do {
            stEntry = copyState(flowState);
            scanCond(node.getCondition());
            stCondFalse = flowState_whenFalse;
            stCondTrue = flowState_whenTrue;
            flowState = flowState_whenTrue;
            scanStat(node.getStatement());

            if (pass) break;

            stCondTrue.and(stEntry, annoRelations);
            flowState.and(stEntry, annoRelations);
            pass = true;
        } while (true);

        if (alive) {
            // If the loop is exited without a "dead" instruction like break,
            // we can assume the negated condition.
            // TODO: is this check too simplistic?
            flowState = stCondFalse;
        } else {
            // If there is a break or something similar within the body of the
            // loop, we cannot ensure that the negated condition holds.
            flowState = stEntry;
        }
        return null;
    }

    @Override
    public Void visitDoWhileLoop(DoWhileLoopTree node, Void p) {
        boolean pass = false;
        ST stEntry;
        ST stCond;
        boolean aliveAfterStat;
        do {
            stEntry = copyState(flowState);
            scanStat(node.getStatement());
            aliveAfterStat = alive;
            scanCond(node.getCondition());
            stCond = flowState_whenFalse;
            flowState = flowState_whenTrue;

            if (pass) break;

            flowState.and(stEntry, annoRelations);
            pass = true;
        } while (true);

        if (aliveAfterStat) {
            flowState = stCond;
        } else {
            flowState = stEntry;
        }
        return null;
    }

    @Override
    public Void visitForLoop(ForLoopTree node, Void p) {
        boolean pass = false;
        for (StatementTree initalizer : node.getInitializer())
            scanStat(initalizer);
        ST stCondFalse;
        ST stCondTrue;
        ST stEntry;
        do {
            stEntry = copyState(flowState);
            scanCond(node.getCondition());
            stCondFalse = flowState_whenFalse;
            flowState = flowState_whenTrue;
            stCondTrue = flowState_whenTrue;

            scanStat(node.getStatement());
            for (StatementTree tree : node.getUpdate())
                scanStat(tree);

            if (pass) break;

            stCondTrue.and(stEntry, annoRelations);
            flowState.and(stEntry, annoRelations);
            pass = true;
        } while (true);

        if (alive) {
            flowState = stCondFalse;
        } else {
            flowState = stEntry;
        }
        return null;
    }

    @Override
    public Void visitEnhancedForLoop(EnhancedForLoopTree node, Void p) {
        scan(node.getVariable(), p);

        VariableTree var = node.getVariable();
        newVar(var);

        ExpressionTree expr = node.getExpression();
        scanExpr(expr);

        AnnotatedTypeMirror rhs = factory.getAnnotatedType(expr);
        AnnotatedTypeMirror iter = atypes.getIteratedType(rhs);

        if (iter != null) {
            propagateFromType(var, iter);
        } else {
            checker.errorAbort("AbstractFlow.visitEnahncedForLoop: could not determine iterated type!");
        }

        ST stEntry = copyState(flowState);

        // Visit the statement twice to account for the effect
        // the loop body might have on the enclosing state.
        scanStat(node.getStatement());
        flowState.and(stEntry, annoRelations);
        scanStat(node.getStatement());

        // The loop might never get executed -> restore state.
        flowState = stEntry;

        return null;
    }

    @Override
    public Void visitBreak(BreakTree node, Void p) {
        alive = false;
        //alive = true;
        return null;
    }

    @Override
    public Void visitContinue(ContinueTree node, Void p) {
        alive = false;
        //alive = true;
        return null;
    }

    @Override
    public Void visitReturn(ReturnTree node, Void p) {
        if (node.getExpression() != null)
            scanExpr(node.getExpression());
        alive = false;
        return null;
    }

    @Override
    public Void visitThrow(ThrowTree node, Void p) {
        scanExpr(node.getExpression());
        alive = false;
        return null;
    }

    @Override
    public Void visitTry(TryTree node, Void p) {
        tryBits.push(copyState(flowState));
        scan(node.getBlock(), p);
        ST stAfterBlock = copyState(flowState);
        ST result = tryBits.pop();

        flowState.and(result, annoRelations);

        if (node.getCatches() != null) {
            boolean catchAlive = false;
            for (CatchTree ct : node.getCatches()) {
                scan(ct, p);
                catchAlive |= alive;
            }
            // Conservative: only if there's no finally
            if (!catchAlive && node.getFinallyBlock() == null) {
                flowState = copyState(stAfterBlock);
            }
        }
        scan(node.getFinallyBlock(), p);
        return null;
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        super.visitMethodInvocation(node, p);

        ExecutableElement method = TreeUtils.elementFromUse(node);
        if (method.getSimpleName().contentEquals("exit")
                && method.getEnclosingElement().getSimpleName().contentEquals("System"))
            alive = false;

        this.clearOnCall(TreeUtils.enclosingMethod(getCurrentPath()), method);

        List<? extends TypeMirror> thrown = method.getThrownTypes();
        if (!thrown.isEmpty()
                && TreeUtils.enclosingOfKind(getCurrentPath(), Tree.Kind.TRY) != null) {
            if (!tryBits.isEmpty())
                tryBits.peek().and(flowState, annoRelations);
        }

        return null;
    }

    /**
     * Clear whatever part of the state that gets invalidated by
     * invoking the method.
     *
     * @param enclMeth The method within which "method" is called.
     *   Might be null if the invocation is in a field initializer.
     * @param method The invoked method.
     */
    protected abstract void clearOnCall(/*@Nullable*/ MethodTree enclMeth, ExecutableElement method);

    @Override
    public Void visitBlock(BlockTree node, Void p) {
        if (node.isStatic()) {
            ST prev = copyState(flowState);
            try {
                super.visitBlock(node, p);
                return null;
            } finally {
                flowState = prev;
            }
        }
        return super.visitBlock(node, p);
    }

    @Override
    public Void visitMethod(MethodTree node, Void p) {
        AnnotatedDeclaredType preMRT = visitorState.getMethodReceiver();
        MethodTree preMT = visitorState.getMethodTree();
        visitorState.setMethodReceiver(
                factory.getAnnotatedType(node).getReceiverType());
        visitorState.setMethodTree(node);

        // Intraprocedural, so save and restore bits.
        ST prev = copyState(flowState);

        try {
            super.visitMethod(node, p);
            return null;
        } finally {
            visitMethodEndCallback(node);
            flowState = prev;
            visitorState.setMethodReceiver(preMRT);
            visitorState.setMethodTree(preMT);
        }
    }

    /**
     * This method is invoked by visitMethod before restoring the previous
     * state before visiting the method.
     * This method is used to investigate the state at that point.
     *
     * @param node
     */
    public void visitMethodEndCallback(MethodTree node) {
        // Usually there is nothing to do.
    }

    // **********************************************************************

    /**
     * Determines whether a variable definition has been annotated.
     *
     * @param enclMeth the method within which the check happens;
     *   null e.g. in field initializers
     * @param annotation the annotation to check for
     * @param var the variable to check
     * @return true if the variable has the given annotation, false otherwise
     */
    protected boolean varDefHasAnnotation(/*@Nullable*/ MethodTree enclMeth,
            AnnotationMirror annotation, Element var) {
        Pair<Element, AnnotationMirror> key = Pair.of(var, annotation);
        if (annotatedVarDefs.containsKey(key)) {
            return annotatedVarDefs.get(key);
        }

        boolean result = factory.getAnnotatedType(var).hasAnnotation(annotation);
        annotatedVarDefs.put(key, result);
        return result;
    }

    /**
     * Tests whether the element is of a non-final field
     *
     * @return true iff element is a non-final field
     */
    protected static final boolean isNonFinalField(Element element) {
        return (element.getKind().isField()
                && !ElementUtils.isFinal(element));
    }
}
