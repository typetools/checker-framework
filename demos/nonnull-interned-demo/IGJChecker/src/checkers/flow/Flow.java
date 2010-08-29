package checkers.flow;

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

/**
 * Provides a generalized flow-sensitive qualifier inference for the checkers
 * framework.
 *
 * <p>
 *
 * This implementation is based largely on {@code javac}'s dataflow analysis
 * module, which may be found in {@code com.sun.tools.javac.comp.Flow}. Note
 * that, whenever possible, this implementation prefers the use of classes in
 * the public API ({@link BitSet}, the Compiler Tree API) over those in
 * {@code com.sun.tools.javac}.
 *
 * <p>
 *
 * A separate instance of the analysis must be created for each compilation
 * unit.
 */
public class Flow extends TreePathScanner<Void, Void> {

    /** Where to print debugging messages, if non-null. */
    public static PrintStream debug = null;

    /** The processing environment to use. */
    protected final ProcessingEnvironment env;

    /** The file that's being analyzed. */
    protected final CompilationUnitTree root;

    /** The annotation to track. */
    protected final AnnotationMirror annotation;

    /** Utility class for getting source positions. */
    protected final SourcePositions source;

    /** Utility class for determining annotated types. */
    protected final AnnotatedTypeFactory factory;

    /** Maps variables to a bit index. */
    protected final List<VariableElement> vars;

    /** Stores the results of the analysis (source position to status bit). */
    protected final Map<Long, Boolean> record;

    /** Tracks the annotated state of each variable during flow. */
    protected BitSet anno;

    /** Tracks the annotated state of each variable in a true branch. */
    protected BitSet annoWhenTrue;

    /** Tracks the annotated state of each variable in a false branch. */
    protected BitSet annoWhenFalse;

    /** Minimal liveness bit, used for better tracking with jumps. */
    private boolean alive = true;

    private boolean tryAlive = true;

    private Stack<Tree> tries = new Stack<Tree>();

    /** Visitor state; tracking is required for checking receiver types. */
    private final VisitorState visitorState;

    /**
     * Creates a new analysis. The analysis will use the default {@link
     * AnnotatedTypeFactory} to obtain annotated types.
     *
     * @param env the checker's current processing environment
     * @param root the compilation unit that will be scanned
     * @param annotation the annotation to track
     */
    public Flow(ProcessingEnvironment env, CompilationUnitTree root,
            AnnotationMirror annotation) {
        this(env, root, annotation, null);
    }

    /**
     * Creates a new analysis. The analysis will use the given {@link
     * AnnotatedTypeFactory} to obtain annotated types.
     *
     * @param env the checker's current processing environment
     * @param root the compilation unit that will be scanned
     * @param annotation the annotation to track
     * @param factory the factory class that will be used to get annotated
     *        types, or {@code null} if the default factory should be used
     */
    public Flow(ProcessingEnvironment env, CompilationUnitTree root,
            AnnotationMirror annotation, AnnotatedTypeFactory factory) {

        this.env = env;
        this.root = root;
        this.annotation = annotation;

        this.source = Trees.instance(env).getSourcePositions();
        if (factory == null)
            this.factory = new AnnotatedTypeFactory(env, root);
        else this.factory = factory;
        
        this.visitorState = factory.getVisitorState();

        this.vars = new ArrayList<VariableElement>();
        this.record = new HashMap<Long, Boolean>();

        this.anno = new BitSet();
        this.annoWhenTrue = null;
        this.annoWhenFalse = null;
    }

    /**
     * Tests a result.
     *
     * @param pos the source position of the tree or element to test
     * @return {@link Boolean#TRUE} if the bit for that position is set, {@link
     * Boolean#FALSE} if the bit for that position is not set, or null if there
     * is no result for that position
     */
    public Boolean test(long pos) {
        if (!record.containsKey(pos))
            return null;
        else return record.get(pos);
    }

    /**
     * Registers a new variable for flow tracking using its definition.
     *
     * @param tree the variable to register
     */
    void newVar(VariableTree tree) {

        VariableElement var = factory.elementFromDeclaration(tree);
        assert var != null : "no symbol from tree";

        if (vars.contains(var)) {
            if (debug != null)
                debug.println("Flow: newVar(" + tree + ") reusing index");
            return;
        }

        int idx = vars.size();
        vars.add(var);

        AnnotatedTypeMirror type = factory.getAnnotatedType(var);
        assert type != null : "no type from symbol";

        if (debug != null)
            debug.println("Flow: newVar(" + tree + ") -- " + type);

        // Determine the initial status of the variable by checking its
        // annotated type.
        if (type.hasAnnotation(annotation))
            anno.set(idx);
        else
            anno.clear(idx);
    }

    /**
     * Moves bits as assignments are made.
     *
     * @param lhs the left-hand side of the assignment
     * @param rhs the right-hand side of the assignment
     */
    void propagate(Tree lhs, Tree rhs) {

	if (!tryAlive) return;

	// Skip assignment to arrays.
	if (lhs.getKind() == Tree.Kind.ARRAY_ACCESS)
	    return;
	
        // Get the element for the left-hand side.
        Element elt = InternalUtils.symbol(lhs);
        assert elt != null;

        // Get the element for the right hand side.
        AnnotatedTypeMirror type = factory.getAnnotatedType(rhs);
        assert type != null;

        int idx = vars.indexOf(elt);
        // FIXME: this assertion is valid pending a fix for some field access
        // issues.
        // assert idx >= 0;
        if (idx < 0) return;

        Element rElt = InternalUtils.symbol(rhs);/* TODO type.getElement();*/
        int rIdx = vars.indexOf(rElt);

        if (debug != null)
            debug.println("Flow: propagate from " + type + (rIdx >= 0 ? "/" + anno.get(rIdx) : ""));

        // Propagate/clear the bit if it's annotated or had its bit set.
        if (type.hasAnnotation(this.annotation) || (rIdx >= 0 && anno.get(rIdx)))
            anno.set(idx);
        else anno.clear(idx);
    }

    /**
     * Split the bitset before a conditional branch.
     */
    void split() {
        annoWhenFalse = (BitSet)anno.clone();
        annoWhenTrue = anno;
        anno = null;
    }

    /**
     * Merge the bitset after a conditional branch.
     */
    void merge() {
        anno = (BitSet)annoWhenTrue.clone();
        anno.and(annoWhenFalse);
        //annoWhenTrue = annoWhenFalse = null;
    }

    /**
     * Record the value of the annotation bit for the given usage of a
     * variable, so that a type-checker may use its value after the analysis
     * has finished.
     *
     * @param tree
     */
    void recordBits(Tree tree) {

        // TODO: skip if tree is an lvalue?

        Element elt = null;
        if (tree instanceof MemberSelectTree)
            elt = factory.elementFromUse((MemberSelectTree)tree);
        else if (tree instanceof IdentifierTree)
            elt = factory.elementFromUse((IdentifierTree)tree);
        else return;

        int idx = vars.indexOf(elt);

        if (idx >= 0) {
            long pos = source.getStartPosition(root, tree);
            if (debug != null)
                debug.println("Flow: recordBits(" + tree + ") @" + pos + " "
                        + anno.get(idx));
            record.put(pos, anno.get(idx));
        }

    }

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
    protected void scanCond(Tree tree) {
        alive = true;
        scan(tree, null);
        if (anno != null) split();
        anno = null;
    }

    /**
     * Called whenever an expression is scanned.
     *
     * @param tree the expression being scanned
     */
    protected void scanExpr(ExpressionTree tree) {
        alive = true;
        scan(tree, null);
        if (anno == null) merge();
    }

    /**
     * Called whenever a group of expressions is scanned.
     *
     * @param trees the expressions being scanned
     */
    protected void scanExprs(List<? extends ExpressionTree> trees) {
        for (ExpressionTree tree : trees)
            scanExpr(tree);

    }

    @Override
    public Void visitClass(ClassTree node, Void p) {
        AnnotatedDeclaredType preACT = visitorState.getClassType();
        ClassTree preCT = visitorState.getClassTree();

        visitorState.setClassType((AnnotatedDeclaredType)factory.getAnnotatedType(node));
        visitorState.setClassTree(node);

        try { 
            return super.visitClass(node, p);
        } finally {
            this.visitorState.setClassType(preACT);
            this.visitorState.setClassTree(preCT);
        }
    }

    @Override
    public Void visitImport(ImportTree tree, Void p) {
        return null;
    }

    @Override
    public Void visitAnnotation(AnnotationTree tree, Void p) {
        return null;
    }

    @Override
    public Void visitIdentifier(IdentifierTree node, Void p) {
        super.visitIdentifier(node, p);
        recordBits(node);
        return null;
    }

    @Override
    public Void visitMemberSelect(MemberSelectTree node, Void p) {
        super.visitMemberSelect(node, p);
        recordBits(node);
        return null;
    }

    @Override
    public Void visitVariable(VariableTree node, Void p) {
        newVar(node);
        ExpressionTree init = node.getInitializer();
        if (init != null) {
            scanExpr(init);
            propagate(node, init);
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
        return null;
    }


    @Override
    public Void visitAssert(AssertTree node, Void p) {
        scanCond(node.getCondition());
        anno = (BitSet)annoWhenTrue.clone();
        return null;
    }

    @Override
    public Void visitIf(IfTree node, Void p) {
        scanCond(node.getCondition());

        BitSet before = annoWhenFalse;
        anno = annoWhenTrue;

        boolean aliveBefore = alive;

        scanStat(node.getThenStatement());
        StatementTree elseStmt = node.getElseStatement();
        if (elseStmt != null) {
            boolean aliveAfter = alive;
            alive = aliveBefore;
            BitSet after = (BitSet)anno.clone();
            anno = before;
            scanStat(elseStmt);
            alive &= aliveAfter;
            if (!alive) {
                anno.clear();
                anno.or(after);
            } else
                anno.and(after);
        } else {
            alive &= aliveBefore;
            if (!alive) {
                anno.clear();
                anno.or(before);
            } else
                anno.and(before);
        }

        return null;
    }
    
    @Override
    public Void visitConditionalExpression(ConditionalExpressionTree node,
            Void p) {
        
        // Split and merge as for an if/else.
        scanCond(node.getCondition());
        
        BitSet before = annoWhenFalse;
        anno = annoWhenTrue;

        scanExpr(node.getTrueExpression());
        BitSet after = (BitSet)anno.clone();
        anno = before;
        
        scanExpr(node.getFalseExpression());
        anno.and(after);
        
        return null;
    }

    @Override
    public Void visitWhileLoop(WhileLoopTree node, Void p) {
        boolean pass = false;
        BitSet annoCond;
        do {
            BitSet annoEntry = (BitSet)anno.clone();
            scanCond(node.getCondition());
            annoCond = annoWhenFalse;
            anno = annoWhenTrue;
            scanStat(node.getStatement());
            if (pass) break;
            annoWhenTrue.and(annoEntry);
            pass = true;
        } while (true);
        anno = annoCond;
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
        alive = false;
        return null;
    }

    @Override
    public Void visitTry(TryTree node, Void p) {
	tries.push(node);
	super.visitTry(node, p);
	tries.pop();
	if (tries.isEmpty())
	    tryAlive = true;
	return null;
    }

    // TODO: visit class to ensure visiting fields before methods
    // TODO: should really check for "java.lang.System", not just "System"
    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
        super.visitMethodInvocation(node, p);

        ExecutableElement method = factory.elementFromUse(node);
        if (method.getSimpleName().contentEquals("exit")
                && method.getEnclosingElement().getSimpleName().contentEquals("System"))
            alive = false;

	List<? extends TypeMirror> thrown = method.getThrownTypes();
	if (!thrown.isEmpty())
	    tryAlive = false;

        return null;
    }

    @Override
    public Void visitMethod(MethodTree node, Void p) {
        AnnotatedDeclaredType preMRT = visitorState.getMethodReceiver();
        MethodTree preMT = visitorState.getMethodTree();
        visitorState.setMethodReceiver(
                ((AnnotatedExecutableType)factory.getAnnotatedType(node)).getReceiverType());
        visitorState.setMethodTree(node);
        
        try {
            // Intraprocedural, so save and restore bits.
            BitSet prev = (BitSet)anno.clone();
            super.visitMethod(node, p);
            anno = prev;
            return null;
        } finally {
            visitorState.setMethodReceiver(preMRT);
            visitorState.setMethodTree(preMT);
        }
    }
}
