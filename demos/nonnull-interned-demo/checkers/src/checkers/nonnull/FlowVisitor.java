package checkers.nonnull;

import checkers.quals.*;
import checkers.types.*;

import com.sun.source.tree.*;
import com.sun.source.util.*;

import java.util.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;

import static checkers.types.InternalUtils.*;

/**
 * Performs a flow-sensitive {@code @NonNull} analysis on a portion of an AST.
 * Specifically, it checks if conditionals to determine if there is an explicit
 * null check (e.g., "if (x != null)"), and it determines a scope for which the
 * checked variable is nonnull. For local variables, the scope ends where the
 * variable is first reassigned; for fields and method parameters, the scope
 * ends where the variable is first reassigned or where it is first passed as
 * an argument to a method. 
 */
@DefaultQualifier("checkers.nullness.quals.NonNull")
class FlowVisitor extends TreePathScanner<Void, Void> {

    /** The root of the source tree (required for pathfinding). */
    private final CompilationUnitTree root;

    /** Used to determine source positions for variable scopes. */
    private final SourcePositions srcPos;

    /** A stack of scopes for nested conditionals. */
    private final Deque<FlowScope> scopes = new ArrayDeque<FlowScope>();

    /** A stack of scopes for method-wide checks (asserts, returns). */
    private final Deque<Set<FlowScope>> methodScopes 
        = new ArrayDeque<Set<FlowScope>>();

    /** The set of completed scopes. */
    private final Set<FlowScope> results = new HashSet<FlowScope>();

    /** For obtaining annotated types. */
    private final NonnullAnnotatedTypeFactory factory;

    /**
     * Creates a visitor for performing flow-sensitive nonnull analysis on a
     * subtree of the given tree root. It uses the provided {@link
     * SourcePositions} to determine a range of positions for which an element
     * is nonnull.
     * 
     * @param root the root of the subtree that will be checked
     * @param srcPos {@link SourcePositions} for producing ranges
     * @param factory the factory to use for determining the @NonNull-ness of a
     *                program element
     */
    public FlowVisitor(CompilationUnitTree root, SourcePositions srcPos,
            NonnullAnnotatedTypeFactory factory) {
        this.root = root;
        this.srcPos = srcPos;
        this.factory = factory;
    }
    
    /**
     * Retrieves the results of the completed analysis.
     *
     * @return an unmodifiable set of completed scopes
     */
    public Set<? extends FlowScope> getResults() {
        return Collections.<@NonNull FlowScope>unmodifiableSet(results);
    }
    
    @Override
    public @Nullable Void visitMethod(MethodTree node, Void p) {

        HashSet<FlowScope> thisMethodScopes = new HashSet<FlowScope>();
        methodScopes.push(thisMethodScopes);
        
        super.visitMethod(node, p);

        
        methodScopes.pop();

        for (FlowScope scope : thisMethodScopes) {
            if (!scope.isComplete())
                scope.complete(srcPos.getEndPosition(root, node));
        }

        results.addAll(thisMethodScopes);
        
        return null;
    }

    @Override
    public @Nullable Void visitBinary(BinaryTree node, Void p) {

        @Nullable FlowCondition cond = FlowCondition.create(node);
        if (cond == null)
            return super.visitBinary(node, p);

        ExpressionTree nnExpression = cond.getOperand();

        @Nullable Element nnElement = InternalUtils.symbol(nnExpression);
        if (nnElement == null)
            throw new AssertionError(nnExpression + " has no Element");
        
        FlowScope scope = new FlowScope(nnElement, 
                    srcPos.getEndPosition(root, nnExpression)); 
        scope.complete(srcPos.getEndPosition(root, node));
        results.add(scope);
        
        // TODO: this might not be necessary, as an optimization
        return super.visitBinary(node, p);
    }
    
    @Override
    public @Nullable Void visitAssert(AssertTree node, Void p) {

        @Nullable FlowCondition cond = FlowCondition.create(node.getCondition());
        if (cond == null)
            return super.visitAssert(node, p);

        ExpressionTree nnExpression = cond.getOperand();

        @Nullable Element nnElement = InternalUtils.symbol(nnExpression);
        if (nnElement == null)
            throw new AssertionError(nnExpression + " has no Element");
        
        FlowScope scope = new FlowScope(nnElement, 
                    srcPos.getEndPosition(root, node.getCondition())); 
        methodScopes.peek().add(scope);

        return super.visitAssert(node, p);
        
    }

    @Override
    public @Nullable Void visitWhileLoop(WhileLoopTree node, Void p) {

        @Nullable FlowCondition cond = FlowCondition.create(node.getCondition());

        if (cond == null)
            return super.visitWhileLoop(node, p);

        @Nullable StatementTree stmt = node.getStatement(); /*nnbug*/
        if (stmt == null)
            return super.visitWhileLoop(node, p);

        ExpressionTree nnExpression = cond.getOperand();
        
        @Nullable Element nnElement = InternalUtils.symbol(nnExpression);
        if (nnElement == null)
            throw new AssertionError(nnExpression + " has no Element");

        FlowScope scope = new FlowScope(nnElement,
                srcPos.getStartPosition(root, stmt));
        scopes.push(scope);

        boolean terminates = checkReturnOrThrow(stmt); 
        if (terminates) {
            FlowScope retScope = 
                new FlowScope(nnElement, srcPos.getEndPosition(root, node));
            methodScopes.peek().add(retScope);
        }

        super.visitWhileLoop(node, p);

        FlowScope done = scopes.pop();
        assert scope == done;

        if (!scope.isComplete())
            scope.complete(srcPos.getEndPosition(root, stmt));

        results.add(scope);
        return null;
    }
    
    @Override
    public @Nullable Void visitIf(IfTree node, Void p) {

        // scopeStatement is either the "then" condition or the "else"
        // condition, depending on whether it's an "if (x != null)" check or an
        // "if (x == null)" check, respectively.
        //
        // nnExpression is the variable that the check makes nonnull, e.g., "x"
        // in "if (x != null)". 
        
        @Nullable FlowCondition cond = FlowCondition.create(node.getCondition());
        if (cond == null)
            return super.visitIf(node, p);

        @Nullable StatementTree scopeStatement, otherStatement;
        ExpressionTree nnExpression = cond.getOperand();

        if (cond.isNonNull()) {
            scopeStatement = node.getThenStatement();
            otherStatement = node.getElseStatement();
        } else {
            scopeStatement = node.getElseStatement();
            otherStatement = node.getThenStatement();
        }

        // Push a new FlowScope for the checked variable that starts at the
        // beginning of scopeStatement.
        @Nullable Element nnElement = InternalUtils.symbol(nnExpression);
        if (nnElement == null)
            throw new AssertionError(nnExpression + " has no Element");

        FlowScope scope;
        if (scopeStatement != null) {
            scope = new FlowScope(nnElement,
                srcPos.getStartPosition(root, scopeStatement));
        } else {
            scope = new FlowScope(nnElement,
                srcPos.getEndPosition(root, otherStatement));
        }
        scopes.push(scope);

        boolean terminates = checkReturnOrThrow(otherStatement);
        if (terminates) {
            FlowScope retScope 
                = new FlowScope(nnElement, srcPos.getEndPosition(root, node)); 
            methodScopes.peek().add(retScope);
        }
            
        // Continue scanning the if statement's "then" and "else" blocks.
        super.visitIf(node, p);

        // Pop a scope from the stack -- it should be same as the one we
        // pushed.
        FlowScope done = scopes.pop();
        assert scope == done;

        // If the scope hasn't previously been completed (by reaching an
        // assignment (for all variables) or a method invocation (for
        // non-locals)), complete it with the end of scopeStatement (i.e.,
        // meaning that the checked variable is nonnull for the entire "then"
        // or "else" block.
        if (!scope.isComplete())
            scope.complete(srcPos.getEndPosition(root, scopeStatement));

        // Add the completed scope to the result.
        results.add(scope);

        return null;
    }

    private boolean terminates(StatementTree stmt) {
        if (stmt.getKind() == Tree.Kind.RETURN ||
                stmt.getKind() == Tree.Kind.THROW ||
                stmt.getKind() == Tree.Kind.BREAK ||
                stmt.getKind() == Tree.Kind.CONTINUE)
            return true;

        if (stmt.getKind() == Tree.Kind.EXPRESSION_STATEMENT) {
            ExpressionTree expr = ((ExpressionStatementTree)stmt).getExpression();
            if (expr.getKind() == Tree.Kind.METHOD_INVOCATION) {
                MethodInvocationTree method = (MethodInvocationTree)expr;
                @Nullable Element elt = InternalUtils.symbol(method.getMethodSelect());
                @Nullable Element enclElement;
                if (elt != null)
                    enclElement = elt.getEnclosingElement();
                else enclElement = null;
                if (elt != null && "exit".equals(elt.getSimpleName().toString())) {
                   if (enclElement != null && /*nnbug*/ // FIXME: flow workaround
                           "System".equals(enclElement.getSimpleName().toString()))
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean checkReturnOrThrow(StatementTree statement) { 

        if (statement == null)
            return false;
       
        if (statement.getKind() == Tree.Kind.BLOCK) {
           for (StatementTree s : ((BlockTree)statement).getStatements())
               if (terminates(s))
                   return true;
        } else if (terminates(statement))
            return true;

        return false;
    }

    private void appendScopeFromPosition(Element elt, long pos) {

        FlowScope scope = new FlowScope(elt, pos); 
        methodScopes.peek().add(scope);
    }

    @Override
    public Void visitVariable(VariableTree node, Void p) {

        if (node.getInitializer() == null)
            return super.visitVariable(node, p);

        ExpressionTree expr = node.getInitializer();
        AnnotatedClassType aExpr = factory.getClass(expr);
        if (expr != null && aExpr != null &&
                aExpr.hasAnnotationAt(NonNull.class, AnnotationLocation.RAW)) {

            @Nullable Element nnElement = InternalUtils.symbol(node);
            if (nnElement == null)
                throw new AssertionError(node.getName() + " has no Element");
            
            if (nnElement.getKind() != ElementKind.LOCAL_VARIABLE) /*nnbug*/
                return super.visitVariable(node, p);

            FlowScope scope = new FlowScope(nnElement, 
                        srcPos.getEndPosition(root, expr));
            if (!methodScopes.isEmpty())
                methodScopes.peek().add(scope);
        }

        return super.visitVariable(node, p);
    }
    
    @Override
    public Void visitAssignment(AssignmentTree node, Void p) {

        ExpressionTree variable = node.getVariable();

        Set<FlowScope> checkScopes = new HashSet<FlowScope>();
        if (!methodScopes.isEmpty())
            checkScopes.addAll(methodScopes.peek());
        checkScopes.add(scopes.peek());

        for (FlowScope scope : checkScopes) {

            // If the most recent scope hasn't been completed and the checked
            // variable is being assigned to, complete the scope just before the
            // assignment.
            if (scope != null && !scope.isComplete()) {
                @Nullable Element var = InternalUtils.symbol(variable);
                if (scope.getElement().equals(var))
                    scope.complete(srcPos.getStartPosition(root, variable) - 1);
                FlowScope exprScope = new FlowScope(var,
                        srcPos.getStartPosition(root, node.getExpression()));
                exprScope.complete(srcPos.getEndPosition(root, node.getExpression()));
                results.add(exprScope);
            }
        }

        ExpressionTree expr = node.getExpression();
        AnnotatedClassType aExpr = factory.getClass(expr);
        if (expr != null && aExpr != null &&
                aExpr.hasAnnotationAt(NonNull.class, AnnotationLocation.RAW)) {

            @Nullable Element nnElement = InternalUtils.symbol(variable);
            if (nnElement == null)
                throw new AssertionError(variable + " has no Element");
            
            FlowScope scope = new FlowScope(nnElement, 
                        srcPos.getEndPosition(root, expr));
            if (!methodScopes.isEmpty())
                methodScopes.peek().add(scope);
        }
        
        return super.visitAssignment(node, p);
    }

    public Void visitMethodInvocation(MethodInvocationTree node, Void p) {

        FlowScope scope = scopes.peek();

        // If the most recent scope hasn't been completed and there's a method
        // invocation (and the scope element isn't a local variable), complete
        // the scope just before the method invocation.
        if (scope != null && !scope.isComplete() 
                && scope.getElement().getKind() != ElementKind.LOCAL_VARIABLE)
            scope.complete(srcPos.getEndPosition(root, node.getMethodSelect()));
        
        return super.visitMethodInvocation(node, p);
    }

}
