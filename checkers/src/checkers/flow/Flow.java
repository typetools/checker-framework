package checkers.flow;

import checkers.basetype.BaseTypeChecker;
import checkers.nullness.quals.Pure;
import checkers.source.SourceChecker;
import checkers.types.*;
import checkers.types.AnnotatedTypeMirror.*;
import checkers.util.*;

import java.io.PrintStream;
import java.util.*;

import com.sun.source.tree.*;
import com.sun.source.util.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.Elements;

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
public class Flow extends TreePathScanner<Void, Void> {

    /** Where to print debugging messages; set via {@link #setDebug}. */
    protected PrintStream debug = null;

    /** The checker to which this instance belongs. */
    protected final SourceChecker checker;

    /** The processing environment to use. */
    protected final ProcessingEnvironment env;

    /** The file that's being analyzed. */
    protected final CompilationUnitTree root;

    /**
     * The annotations (qualifiers) to infer. The relationship among them is
     * determined using {@link BaseTypeChecker#getQualifierHierarchy()}. By
     * consulting the hierarchy, the analysis will only infer a qualifier on a
     * type if it is more restrictive (i.e. a subtype) than the existing
     * qualifier for that type.
     */
    protected final Set<AnnotationMirror> annotations;

    /** Utility class for determining annotated types. */
    protected final AnnotatedTypeFactory factory;

    /** Utility class for operations on annotated types. */
    protected final AnnotatedTypes atypes;

    /** Stores the results of the analysis (source location to qualifier). */
    protected final Map<Tree, AnnotationMirror> flowResults;

    /**
     * Maps variables to a bit index. This index is also used as the bit
     * index to determine a variable's annotatedness using
     * annos/annosWhenTrue/annosWhenFalse.
     * @see #annos
     * @see #annosWhenTrue
     * @see #annosWhenFalse
     */
    protected final List<VariableElement> vars;

    /**
     * Tracks the annotated state of each variable during flow. Bit indices
     * correspond exactly to indices in {@link #vars}. This field is set to
     * null immediately after splitting for a branch, and is set to some
     * combination (usually boolean "and") of {@link #annosWhenTrue} and
     * {@link #annosWhenFalse} after merging. Since it is used when visiting the
     * true and false branches, however, it may be non-null concurrently with
     * {@link #annosWhenTrue} and {@link #annosWhenFalse}.
     */
    protected GenKillBits<AnnotationMirror> annos;

    /**
     * Tracks the annotated state of each variable in a true branch. As in
     * {@code javac}'s {@code Flow}, saving/restoring via local variables
     * handles nested branches. Bit indices correspond exactly to indices in
     * {@link #vars}. This field is copied from {@link #annos} when splitting
     * for a branch and is set to null immediately after merging.
     *
     * @see #annos
     */
    // protected GenKillBits<AnnotationMirror> annosWhenTrue;

    /**
     * Tracks the annotated state of each variable in a false branch. As in
     * {@code javac}'s {@code Flow}, saving/restoring via local variables
     * handles nested branches. Bit indices correspond exactly to indices in
     * {@link #vars}. This field is copied from {@link #annos} when splitting
     * for a branch and is set to null immediately after merging.
     *
     * @see #annos
     */
    // protected GenKillBits<AnnotationMirror> annosWhenFalse;

    public static class SplitTuple {
    	public GenKillBits<AnnotationMirror> annosWhenTrue;
    	public GenKillBits<AnnotationMirror> annosWhenFalse;
    }
    
    
    /**
     * Stores the result of liveness analysis, required by the GEN-KILL analysis
     * for proper handling of jumps (break, return, throw, etc.).
     */
    protected boolean alive = true;

    /** Tracks annotations in try blocks to support exceptions. */
    private final Deque<GenKillBits<AnnotationMirror>> tryBits;

    /** Visitor state; tracking is required for checking receiver types. */
    private final VisitorState visitorState;

    /** The hierarchy for the type qualifiers that this class infers. */
    protected final QualifierHierarchy annoRelations;

    /** Utilities for {@link Element}s. */
    protected final Elements elements;

    /** Memoization for {@link #varDefHasAnnotation(AnnotationMirror, Element)}. */
    private Map<Element, Boolean> annotatedVarDefs = new HashMap<Element, Boolean>();

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
    public Flow(BaseTypeChecker checker, CompilationUnitTree root,
            Set<AnnotationMirror> annotations, AnnotatedTypeFactory factory) {

        this.checker = checker;
        this.env = checker.getProcessingEnvironment();
        this.root = root;
        this.annotations = annotations;

        if (factory == null)
            this.factory = new AnnotatedTypeFactory(checker, root);
        else
            this.factory = factory;

        this.atypes = new AnnotatedTypes(env, factory);

        this.visitorState = this.factory.getVisitorState();

        this.vars = new ArrayList<VariableElement>();
        this.flowResults = new IdentityHashMap<Tree, AnnotationMirror>();

        this.annos = new GenKillBits<AnnotationMirror>(this.annotations);
        // this.annosWhenTrue = null;
        // this.annosWhenFalse = null;

        this.tryBits = new LinkedList<GenKillBits<AnnotationMirror>>();

        this.annoRelations = checker.getQualifierHierarchy();
        elements = env.getElementUtils();
    }

    /**
     * Sets the {@link PrintStream} for printing debug messages, such as
     * {@link System#out} or {@link System#err}, or null if no debugging output
     * should be emitted.
     */
    public void setDebug(PrintStream debug) {
        this.debug = debug;
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
    public AnnotationMirror test(Tree tree) {
        while (tree.getKind() == Tree.Kind.ASSIGNMENT)
            tree = ((AssignmentTree)tree).getVariable();
        if (!flowResults.containsKey(tree)) {
            return null;
        }
        // a hack needs to be fixed
        // always follow variable declarations
        AnnotationMirror flowResult = flowResults.get(tree);

        return flowResult;
    }

    /**
     * Registers a new variable for flow tracking.
     *
     * @param tree the variable to register
     */
    void newVar(VariableTree tree) {

        VariableElement var = TreeUtils.elementFromDeclaration(tree);
        assert var != null : "no symbol from tree";

        if (vars.contains(var)) {
            if (debug != null)
                debug.println("Flow: newVar(" + tree + ") reusing index");
            return;
        }

        int idx = vars.size();
        vars.add(var);

        AnnotatedTypeMirror type = factory.getAnnotatedType(tree);
        assert type != null : "no type from symbol";

        if (debug != null)
            debug.println("Flow: newVar(" + tree + ") -- " + type);

        // Determine the initial status of the variable by checking its
        // annotated type.
        for (AnnotationMirror annotation : annotations) {
            if (hasAnnotation(type, annotation))
                annos.set(annotation, idx);
            else
                annos.clear(annotation, idx);
        }
    }

    /**
     * Determines whether a type has an annotation. If the type is not a
     * wildcard, it checks the type directly; if it is a wildcard, it checks the
     * wildcard's "extends" bound (if it has one).
     *
     * @param type the type to check
     * @param annotation the annotation to check for
     * @return true if the (non-wildcard) type has the annotation or, if a
     *         wildcard, the type has the annotation on its extends bound
     */
    private boolean hasAnnotation(AnnotatedTypeMirror type,
            AnnotationMirror annotation) {
        if (!(type instanceof AnnotatedWildcardType))
            return type.hasAnnotation(annotation);
        AnnotatedWildcardType wc = (AnnotatedWildcardType) type;
        AnnotatedTypeMirror bound = wc.getExtendsBound();
        if (bound != null && bound.hasAnnotation(annotation))
            return true;
        return false;
    }

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
    void propagate(Tree lhs, ExpressionTree rhs) {

        if (debug != null)
            debug.println("Flow: try propagate from " + rhs);

        // Skip assignment to arrays.
        if (lhs.getKind() == Tree.Kind.ARRAY_ACCESS)
            return;

        // Get the element for the left-hand side.
        Element elt = InternalUtils.symbol(lhs);
        assert elt != null;
        AnnotatedTypeMirror eltType = factory.getAnnotatedType(elt);

        // Get the annotated type of the right-hand side.
        AnnotatedTypeMirror type = factory.getAnnotatedType(rhs);
        if (TreeUtils.skipParens(rhs).getKind() == Tree.Kind.ARRAY_ACCESS) {
            propagateFromType(lhs, type);
            return;
        }
        assert type != null;

        int idx = vars.indexOf(elt);
        if (idx < 0) return;

        // Get the element for the right-hand side.
        Element rElt = InternalUtils.symbol(rhs);
        int rIdx = vars.indexOf(rElt);

        if (eltType.isAnnotated() && type.isAnnotated()
            && !annoRelations.isSubtype(type.getAnnotations(), eltType.getAnnotations()))
            return;

        for (AnnotationMirror annotation : annotations) {
            // Propagate/clear the annotation if it's annotated or an annotation
            // had been inferred previously.
            if (hasAnnotation(type, annotation)
                    && annoRelations.isSubtype(type.getAnnotations(), eltType.getAnnotations())) {
                annos.set(annotation, idx);
                // to ensure that there is always just one annotation set, we clear the
                // annotation that was previously used
                // for (AnnotationMirror oldsuper : eltType.getAnnotations()) {
                for (AnnotationMirror other : annotations) {
                	if (!other.equals(annotation) &&
                			annos.contains(other)) {
                		// The get is not necessary and might observe annos in an invalid state.
                		// annos.get(other, idx)
                		annos.clear(other, idx);
                	}
                }
            } else if (rIdx >= 0 && annos.get(annotation, rIdx)) {
                annos.set(annotation, idx);
            } else {
            	annos.clear(annotation, idx);
            }
        }
        // just to make sure everything worked correctly
        annos.valid();
    }

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
    void propagateFromType(Tree lhs, AnnotatedTypeMirror rhs) {

        if (lhs.getKind() == Tree.Kind.ARRAY_ACCESS)
            return;

        Element elt = InternalUtils.symbol(lhs);

        int idx = vars.indexOf(elt);
        if (idx < 0) return;

        // WMD: if we're setting something, can the GenKillBits invariant be violated? 
        for (AnnotationMirror annotation : annotations) {
            if (hasAnnotation(rhs, annotation))
                annos.set(annotation, idx);
            else
            	annos.clear(annotation, idx);
        }
    }

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
            if (tree.getKind() == Tree.Kind.IDENTIFIER) { break; } // TODO: do nothing
            else if (tree instanceof AssignmentTree)
                return last == ((AssignmentTree)tree).getVariable();
            else if (tree instanceof CompoundAssignmentTree)
                return last == ((CompoundAssignmentTree)tree).getVariable();
            if (last != null) break;
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
        if (tree instanceof MemberSelectTree)
            elt = TreeUtils.elementFromUse((MemberSelectTree)tree);
        else if (tree instanceof IdentifierTree)
            elt = TreeUtils.elementFromUse((IdentifierTree)tree);
        else if (tree instanceof VariableTree)
            elt = TreeUtils.elementFromDeclaration((VariableTree)tree);
        else
            return;

        int idx = vars.indexOf(elt);
        // If the variable has not been previously encountered, add it to the
        // list of variables. (We can't use newVar here since we don't have the
        // declaration tree.)
        if (idx < 0 && elt instanceof VariableElement) {
            idx = vars.size();
            vars.add((VariableElement)elt);
        }

        if (idx >= 0) {
            for (AnnotationMirror annotation : annotations) {
                if (debug != null)
                    debug.println("Flow: recordBits(" + tree + ") + " + annotation + " "
                            + annos.get(annotation, idx) + " as " + tree.getKind());
                if (annos.get(annotation, idx)) {
                    AnnotationMirror existing = flowResults.get(tree);

                    // Don't replace the existing annotation unless the current
                    // annotation is *more* specific than the existing one.
                    if (existing == null || annoRelations.isSubtype(existing, annotation))
                        flowResults.put(tree, annotation);
                } else if (flowResults.get(tree) == annotation) {
                    // We inferred an annotation in this location that is not
                    // applicable anymore
                    // occurs in loop where an assignment invalidates the
                    // condition in the next round
                    flowResults.remove(tree);
                }
            }
        }
    }

    // **********************************************************************

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
    protected SplitTuple scanCond(Tree tree) {
        alive = true;
        if (tree != null) {
            scan(tree, null);
        }
        if (annos != null) {
        	SplitTuple res = split();
        	return res;
        }
        return new SplitTuple();
    }

    /**
     * Split the bitset before a conditional branch.
     */
    protected SplitTuple split() {
    	SplitTuple res = new SplitTuple();
        res.annosWhenFalse = GenKillBits.copy(annos);
        res.annosWhenTrue = annos;
    	annos = null;
        return res;
    }

    /**
     * Merge the bitset after a conditional branch.
     *//*
    protected void merge() {
        annos = GenKillBits.copy(annosWhenTrue);
        //annos.and(annosWhenFalse);
        GenKillBits.andlub(annos, annosWhenFalse, annoRelations);
        annosWhenTrue = annosWhenFalse = null;
    }*/

    /**
     * Called whenever an expression is scanned.
     *
     * @param tree the expression being scanned
     */
    protected void scanExpr(ExpressionTree tree) {
        alive = true;
        scan(tree, null);
        assert annos != null;
        // if (annos == null) merge();
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
        if (factory.fromTypeTree(node.getType()).isAnnotated())
            return null;
        AnnotatedTypeMirror t = factory.getAnnotatedType(node.getExpression());
        for (AnnotationMirror a : annotations)
            if (hasAnnotation(t, a))
                flowResults.put(node, a);
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
                propagate(node, init);
                recordBits(getCurrentPath());
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
    	// System.err.println("in vCA: " + node);

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
        for (AnnotationMirror a : annotations) {
            if (hasAnnotation(t, a)) {
            	flowResults.put(node, a);
            }
        }

        // Clearing out the Annotations is only the correct solution for the
        // Interning Checker, where the result of a compound assignment is never
        // interned. For other checkers, this behavior is wrong, e.g. in the Fenum Checker
        // it leads to FenumTop being determined instead of a FenumUnqualified.
        // if (var.getKind() != Tree.Kind.ARRAY_ACCESS)
        // 	 clearAnnos(var);

        return null;
	}
    
    /*
    private void clearAnnos(Tree tree) {
        Element elt = InternalUtils.symbol(tree);
        if (elt == null)
            return;
        int idx = vars.indexOf(elt);
        if (idx >= 0) {
            for (AnnotationMirror anno : annotations) {
                annos.clear(anno, idx);
            }
        }
    }
    */
    
    @Override
    public Void visitEnhancedForLoop(EnhancedForLoopTree node, Void p) {
        scan(node.getVariable(), p);

        VariableTree var = node.getVariable();
        newVar(var);

        ExpressionTree expr = node.getExpression();
        scanExpr(expr);

        AnnotatedTypeMirror rhs = factory.getAnnotatedType(expr);
        AnnotatedTypeMirror iter = atypes.getIteratedType(rhs);
        if (iter != null)
            propagateFromType(var, iter);

        // only visit statement. skip variable and expression..
        // visited variable and expression already
        scanStat(node.getStatement());
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

    protected void pushNewLevel() { }
    protected void popLastLevel() { }

    @Override
    public Void visitAssert(AssertTree node, Void p) {
        boolean inferFromAsserts = containsKey(node.getDetail(), checker.getSuppressWarningsKey());
        GenKillBits<AnnotationMirror> annosAfterAssert = GenKillBits.copy(annos);
        pushNewLevel();
        SplitTuple split = scanCond(node.getCondition());
        if (inferFromAsserts)
            annosAfterAssert = GenKillBits.copy(split.annosWhenTrue);
        annos = GenKillBits.copy(split.annosWhenFalse);
        scanExpr(node.getDetail());
        annos = annosAfterAssert;
        popLastLevel();
        return null;
    }

    protected void whenConditionFalse(ExpressionTree condition, Void p) {
    }

    @Override
    public Void visitIf(IfTree node, Void p) {
        pushNewLevel();

        SplitTuple split = scanCond(node.getCondition());

        GenKillBits<AnnotationMirror> annosBeforeElse = split.annosWhenFalse;
        annos = split.annosWhenTrue;
        // annosWhenTrue = annosWhenFalse = null;
        
        boolean aliveBeforeThen = alive;

        scanStat(node.getThenStatement());
        popLastLevel();
        
        pushNewLevel();
        StatementTree elseStmt = node.getElseStatement();
        if (elseStmt != null ) {
            whenConditionFalse(node.getCondition(), p);
            boolean aliveAfterThen = alive;
            alive = aliveBeforeThen;
            GenKillBits<AnnotationMirror> annosAfterThen = GenKillBits.copy(annos);
            annos = annosBeforeElse;
            scanStat(elseStmt);

            if (!alive) {
            	// the else branch is not alive at the end
            	// we use the liveness-result from the then branch
                alive = aliveAfterThen;
                // annosAfterThen.or(annos);
                GenKillBits.orlub(annosAfterThen, annos, annoRelations);
                annos = GenKillBits.copy(annosAfterThen);
            } else if (!aliveAfterThen) {
                // annos = annos;  // NOOP
                // TODO: what's the point of this branch?
            } else {
                // both branches are alive
                // alive = true;
                GenKillBits.andlub(annos, annosAfterThen, annoRelations);
            }
        } else {
            if (!alive)
                annos = GenKillBits.copy(annosBeforeElse);
            else {
                GenKillBits.andlub(annos, annosBeforeElse, annoRelations);
            }
        }
        popLastLevel();

        return null;
    }

    @Override
    public Void visitConditionalExpression(ConditionalExpressionTree node,
            Void p) {

        // Split and merge as for an if/else.
    	SplitTuple split = scanCond(node.getCondition());

        GenKillBits<AnnotationMirror> before = split.annosWhenFalse;
        annos = split.annosWhenTrue;

        scanExpr(node.getTrueExpression());
        GenKillBits<AnnotationMirror> after = GenKillBits.copy(annos);
        annos = before;

        scanExpr(node.getFalseExpression());
        // annos.and(after);
        GenKillBits.andlub(annos, after, annoRelations);
        
        return null;
    }

    @Override
    public Void visitWhileLoop(WhileLoopTree node, Void p) {
        GenKillBits<AnnotationMirror> annoCond;
        GenKillBits<AnnotationMirror> annoEntry;
        GenKillBits<AnnotationMirror> annoCondTrue;
        
        // 1:
        annoEntry = GenKillBits.copy(annos);
        SplitTuple split = scanCond(node.getCondition());
        annoCond = split.annosWhenFalse;
        annoCondTrue = split.annosWhenTrue;
        annos = split.annosWhenTrue;
        scanStat(node.getStatement());

        // 2:
        // annosWhenTrue.and(annoEntry);
        GenKillBits.andlub(annoCondTrue, annoEntry, annoRelations);
        // annos.and(annoEntry);
        GenKillBits.andlub(annos, annoEntry, annoRelations);
        
        // 3 (like 1):    
        annoEntry = GenKillBits.copy(annos);
        split = scanCond(node.getCondition());
        annoCond = split.annosWhenFalse;
        annos = split.annosWhenTrue;
        scanStat(node.getStatement());

        
        annos = annoCond;
        return null;
    }

    @Override
    public Void visitDoWhileLoop(DoWhileLoopTree node, Void p) {
        boolean pass = false;
        GenKillBits<AnnotationMirror> annoCond;
        do {
            GenKillBits<AnnotationMirror> annoEntry = GenKillBits.copy(annos);
            scanStat(node.getStatement());
            SplitTuple split = scanCond(node.getCondition());
            annoCond = split.annosWhenFalse;
            annos = split.annosWhenTrue;
            if (pass) break;
            // annosWhenTrue.and(annoEntry);
            GenKillBits.andlub(split.annosWhenTrue, annoEntry, annoRelations);
            pass = true;
        } while (true);
        annos = annoCond;
        return null;
    }

    @Override
    public Void visitForLoop(ForLoopTree node, Void p) {
        boolean pass = false;
        for (StatementTree initalizer : node.getInitializer())
            scanStat(initalizer);
        GenKillBits<AnnotationMirror> annoCond;
        GenKillBits<AnnotationMirror> annoCondTrue;
        do {
            GenKillBits<AnnotationMirror> annoEntry = GenKillBits.copy(annos);
            SplitTuple split = scanCond(node.getCondition());
            annoCond = split.annosWhenFalse;
            annos = split.annosWhenTrue;
            annoCondTrue = split.annosWhenTrue;
            
            scanStat(node.getStatement());
            for (StatementTree tree : node.getUpdate())
                scanStat(tree);
            
            if (pass) break;
            
            // annosWhenTrue.and(annoEntry);
            GenKillBits.andlub(annoCondTrue, annoEntry, annoRelations);
            // annos.and(annoEntry);
            GenKillBits.andlub(annos, annoEntry, annoRelations);
            pass = true;
        } while (true);
        annos = annoCond;
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
        tryBits.push(GenKillBits.copy(annos));
        scan(node.getBlock(), p);
        GenKillBits<AnnotationMirror> annoAfterBlock = GenKillBits.copy(annos);
        pushNewLevel();
        GenKillBits<AnnotationMirror> result = tryBits.pop();
        // annos.and(result);
        GenKillBits.andlub(annos, result, annoRelations);
        popLastLevel();
        if (node.getCatches() != null) {
            boolean catchAlive = false;
            for (CatchTree ct : node.getCatches()) {
                scan(ct, p);
                catchAlive |= alive;
            }
            // Conservative: only if there's no finally
            if (!catchAlive && node.getFinallyBlock() == null)
                annos = GenKillBits.copy(annoAfterBlock);
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

        final String methodPackage = elements.getPackageOf(method).getQualifiedName().toString();
        boolean isJDKMethod = methodPackage.startsWith("java") || methodPackage.startsWith("com.sun");
        boolean isPure = method.getAnnotation(Pure.class) != null;
        for (int i = 0; i < vars.size(); i++) {
            Element var = vars.get(i);
            for (AnnotationMirror a : annotations)
                if (!isJDKMethod && isNonFinalField(var) && !varDefHasAnnotation(a, var) && !isPure)
                    annos.clear(a, i);
        }


        List<? extends TypeMirror> thrown = method.getThrownTypes();
        if (!thrown.isEmpty()
                && TreeUtils.enclosingOfKind(getCurrentPath(), Tree.Kind.TRY) != null) {
            if (!tryBits.isEmpty())
                // tryBits.peek().and(annos);
            	GenKillBits.andlub(tryBits.peek(), annos, annoRelations);
        }

        return null;
    }

    @Override
    public Void visitBlock(BlockTree node, Void p) {
        if (node.isStatic()) {
            pushNewLevel();
            GenKillBits<AnnotationMirror> prev = GenKillBits.copy(annos);
            try {
                super.visitBlock(node, p);
                return null;
            } finally {
                annos = prev;
                popLastLevel();
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
        GenKillBits<AnnotationMirror> prev = GenKillBits.copy(annos);
        try {
            super.visitMethod(node, p);
            return null;
        } finally {
            annos = prev;
            visitorState.setMethodReceiver(preMRT);
            visitorState.setMethodTree(preMT);
        }
    }

    // **********************************************************************

    /**
     * Determines whether a variable definition has been annotated.
     *
     * @param annotation the annotation to check for
     * @param var the variable to check
     * @return true if the variable has the given annotation, false otherwise
     */
    private boolean varDefHasAnnotation(AnnotationMirror annotation, Element var) {

        if (annotatedVarDefs.containsKey(var))
            return annotatedVarDefs.get(var);

        boolean result = hasAnnotation(factory.getAnnotatedType(var), annotation);
        annotatedVarDefs.put(var, result);
        return result;
    }

    /**
     * Tests whether the element is of a non-final field
     *
     * @return true iff element is a non-final field
     */
    private static final boolean isNonFinalField(Element element) {
        return (element.getKind().isField()
                && !ElementUtils.isFinal(element));
    }
}
