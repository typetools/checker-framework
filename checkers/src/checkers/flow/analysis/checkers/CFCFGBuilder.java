package checkers.flow.analysis.checkers;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;

import javacutils.ErrorReporter;
import javacutils.InternalUtils;
import javacutils.TreeUtils;
import javacutils.TypesUtils;
import javacutils.trees.TreeBuilder;

import dataflow.cfg.CFGBuilder;
import dataflow.cfg.ControlFlowGraph;
import dataflow.cfg.UnderlyingAST;
import dataflow.cfg.node.ArrayAccessNode;
import dataflow.cfg.node.FieldAccessNode;
import dataflow.cfg.node.IntegerLiteralNode;
import dataflow.cfg.node.LessThanNode;
import dataflow.cfg.node.LocalVariableNode;
import dataflow.cfg.node.MethodAccessNode;
import dataflow.cfg.node.MethodInvocationNode;
import dataflow.cfg.node.Node;
import dataflow.cfg.node.PostfixIncrementNode;
import dataflow.cfg.node.VariableDeclarationNode;

import checkers.basetype.BaseTypeChecker;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;


/**
 * A control-flow graph builder (see {@link CFGBuilder}) that knows
 * about the Checker Framework annotations and their representation as
 * {@link AnnotatedTypeMirror}s.
 *
 * @author Stefan Heule
 */
public class CFCFGBuilder extends CFGBuilder {

    /** The associated checker. */
    protected final BaseTypeChecker checker;

    /** Type factory to provide types used during CFG building. */
    protected final AnnotatedTypeFactory factory;

    public CFCFGBuilder(BaseTypeChecker checker, AnnotatedTypeFactory factory) {
        super(checker.getProcessingEnvironment().getOptions()
                .containsKey("assumeAssertionsAreEnabled"), checker
                .getProcessingEnvironment().getOptions()
                .containsKey("assumeAssertionsAreDisabled"));
        if (assumeAssertionsEnabled && assumeAssertionsDisabled) {
            ErrorReporter
                    .errorAbort("Assertions cannot be assumed to be enabled and disabled at the same time.");
        }
        this.checker = checker;
        this.factory = factory;
    }

    /**
     * Build the control flow graph of some code.
     */
    @Override
    public ControlFlowGraph run(
            CompilationUnitTree root, ProcessingEnvironment env,
            UnderlyingAST underlyingAST) {
        declaredClasses = new LinkedList<>();
        CFTreeBuilder builder = new CFTreeBuilder(env);
        PhaseOneResult phase1result = new CFCFGTranslationPhaseOne().process(
                root, env, underlyingAST, exceptionalExitLabel, builder, factory);
        ControlFlowGraph phase2result = new CFGTranslationPhaseTwo()
                .process(phase1result);
        ControlFlowGraph phase3result = CFGTranslationPhaseThree
                .process(phase2result);
        return phase3result;
    }

    public class CFCFGTranslationPhaseOne extends CFGTranslationPhaseOne {

        @Override
        protected boolean assumeAssertionsEnabledFor(AssertTree tree) {
            ExpressionTree detail = tree.getDetail();
            if (detail != null) {
                String msg = detail.toString();
                Collection<String> warningKeys = checker
                        .getSuppressWarningsKey();
                for (String warningKey : warningKeys) {
                    String key = "@AssumeAssertion(" + warningKey + ")";
                    if (msg.contains(key)) {
                        return true;
                    }
                }
            }
            return super.assumeAssertionsEnabledFor(tree);
        }

        @Override
        public Node visitEnhancedForLoop(EnhancedForLoopTree tree, Void p) {
            // see JLS 14.14.2
            assert !conditionalMode;

            Name parentLabel = getLabel(getCurrentPath());

            Label conditionStart = new Label();
            Label loopEntry = new Label();
            Label loopExit = new Label();

            // If the loop is a labeled statement, then its continue
            // target is identical for continues with no label and
            // continues with the loop's label.
            Label updateStart;
            if (parentLabel != null) {
                updateStart = continueLabels.get(parentLabel);
            } else {
                updateStart = new Label();
            }

            Label oldBreakTargetL = breakTargetL;
            breakTargetL = loopExit;

            Label oldContinueTargetL = continueTargetL;
            continueTargetL = updateStart;

            // Distinguish loops over Iterables from loops over arrays.

            TypeElement iterableElement = elements.getTypeElement("java.lang.Iterable");
            TypeMirror iterableType = types.erasure(iterableElement.asType());

            VariableTree variable = tree.getVariable();
            VariableElement variableElement =
                TreeUtils.elementFromDeclaration(variable);
            ExpressionTree expression = tree.getExpression();
            StatementTree statement = tree.getStatement();

            TypeMirror exprType = InternalUtils.typeOf(expression);

            // Find enclosing method tree.
            MethodTree enclosingMethod = TreeUtils
                    .enclosingMethod(getCurrentPath());
            Element methodElement = TreeUtils
                    .elementFromDeclaration(enclosingMethod);

            if (types.isSubtype(exprType, iterableType)) {
                // Take the upper bound of a type variable or wildcard
                exprType = TypesUtils.upperBound(exprType);

                assert (exprType instanceof DeclaredType) : "an Iterable must be a DeclaredType";
                DeclaredType declaredExprType = (DeclaredType) exprType;
                declaredExprType.getTypeArguments();

                MemberSelectTree iteratorSelect =
                    treeBuilder.buildIteratorMethodAccess(expression);
                factory.setPathHack(iteratorSelect, methodElement);

                MethodInvocationTree iteratorCall =
                    treeBuilder.buildMethodInvocation(iteratorSelect);
                factory.setPathHack(iteratorCall, methodElement);

                AnnotatedTypeMirror annotatedIteratorType =
                    factory.getAnnotatedType(iteratorCall);

                Tree annotatedIteratorTypeTree =
                    ((CFTreeBuilder)treeBuilder).buildAnnotatedType(annotatedIteratorType);
                factory.setPathHack(annotatedIteratorTypeTree, methodElement);

                // Declare and initialize a new, unique iterator variable
                VariableTree iteratorVariable =
                    treeBuilder.buildVariableDecl(annotatedIteratorTypeTree,
                                                  uniqueName("iter"),
                                                  variableElement.getEnclosingElement(),
                                                  iteratorCall);
                factory.setPathHack(iteratorVariable, methodElement);

                extendWithNode(new VariableDeclarationNode(iteratorVariable));

                Node expressionNode = scan(expression, p);

                MethodAccessNode iteratorAccessNode =
                    extendWithNode(new MethodAccessNode(iteratorSelect, expressionNode));
                MethodInvocationNode iteratorCallNode =
                    extendWithNode(new MethodInvocationNode(iteratorCall, iteratorAccessNode,
                                                            Collections.<Node>emptyList(), getCurrentPath()));

                translateAssignment(iteratorVariable,
                                    new LocalVariableNode(iteratorVariable),
                                    iteratorCallNode);

                // Test the loop ending condition
                addLabelForNextNode(conditionStart);
                IdentifierTree iteratorUse1 =
                    treeBuilder.buildVariableUse(iteratorVariable);
                factory.setPathHack(iteratorUse1, methodElement);

                LocalVariableNode iteratorReceiverNode =
                    extendWithNode(new LocalVariableNode(iteratorUse1));

                MemberSelectTree hasNextSelect =
                    treeBuilder.buildHasNextMethodAccess(iteratorUse1);
                factory.setPathHack(hasNextSelect, methodElement);

                MethodAccessNode hasNextAccessNode =
                    extendWithNode(new MethodAccessNode(hasNextSelect, iteratorReceiverNode));

                MethodInvocationTree hasNextCall =
                    treeBuilder.buildMethodInvocation(hasNextSelect);
                factory.setPathHack(hasNextCall, methodElement);

                extendWithNode(new MethodInvocationNode(hasNextCall, hasNextAccessNode,
                                                        Collections.<Node>emptyList(), getCurrentPath()));
                extendWithExtendedNode(new ConditionalJump(loopEntry, loopExit));

                // Loop body, starting with declaration of the loop iteration variable
                addLabelForNextNode(loopEntry);
                extendWithNode(new VariableDeclarationNode(variable));

                IdentifierTree iteratorUse2 =
                    treeBuilder.buildVariableUse(iteratorVariable);
                factory.setPathHack(iteratorUse2, methodElement);

                LocalVariableNode iteratorReceiverNode2 =
                    extendWithNode(new LocalVariableNode(iteratorUse2));

                MemberSelectTree nextSelect =
                    treeBuilder.buildNextMethodAccess(iteratorUse2);
                factory.setPathHack(nextSelect, methodElement);

                MethodAccessNode nextAccessNode =
                    extendWithNode(new MethodAccessNode(nextSelect, iteratorReceiverNode2));

                MethodInvocationTree nextCall =
                    treeBuilder.buildMethodInvocation(nextSelect);
                factory.setPathHack(nextCall, methodElement);

                extendWithNode(new MethodInvocationNode(nextCall, nextAccessNode,
                    Collections.<Node>emptyList(), getCurrentPath()));

                translateAssignment(variable,
                                    new LocalVariableNode(variable),
                                    nextCall);

                if (statement != null) {
                    scan(statement, p);
                }

                // Loop back edge
                addLabelForNextNode(updateStart);
                extendWithExtendedNode(new UnconditionalJump(conditionStart));

            } else {
                // TODO: Shift any labels after the initialization of the
                // temporary array variable.

                AnnotatedTypeMirror annotatedArrayType =
                    factory.getAnnotatedType(expression);

                assert (annotatedArrayType instanceof AnnotatedTypeMirror.AnnotatedArrayType) :
                    "ArrayType must be represented by AnnotatedArrayType";

                Tree annotatedArrayTypeTree =
                    ((CFTreeBuilder)treeBuilder).buildAnnotatedType(annotatedArrayType);
                factory.setPathHack(annotatedArrayTypeTree, methodElement);

                // Declare and initialize a temporary array variable
                VariableTree arrayVariable =
                    treeBuilder.buildVariableDecl(annotatedArrayTypeTree,
                                                  uniqueName("array"),
                                                  variableElement.getEnclosingElement(),
                                                  expression);
                factory.setPathHack(arrayVariable, methodElement);

                extendWithNode(new VariableDeclarationNode(arrayVariable));
                Node expressionNode = scan(expression, p);

                translateAssignment(arrayVariable,
                                    new LocalVariableNode(arrayVariable),
                                    expressionNode);

                // Declare and initialize the loop index variable
                TypeMirror intType = types.getPrimitiveType(TypeKind.INT);

                LiteralTree zero =
                    treeBuilder.buildLiteral(new Integer(0));
                factory.setPathHack(zero, methodElement);

                VariableTree indexVariable =
                    treeBuilder.buildVariableDecl(intType,
                                                  uniqueName("index"),
                                                  variableElement.getEnclosingElement(),
                                                  zero);
                factory.setPathHack(indexVariable, methodElement);
                extendWithNode(new VariableDeclarationNode(indexVariable));
                IntegerLiteralNode zeroNode =
                    extendWithNode(new IntegerLiteralNode(zero));

                translateAssignment(indexVariable,
                                    new LocalVariableNode(indexVariable),
                                    zeroNode);

                // Compare index to array length
                addLabelForNextNode(conditionStart);
                IdentifierTree indexUse1 =
                    treeBuilder.buildVariableUse(indexVariable);
                factory.setPathHack(indexUse1, methodElement);
                LocalVariableNode indexNode1 =
                    extendWithNode(new LocalVariableNode(indexUse1));

                IdentifierTree arrayUse1 =
                    treeBuilder.buildVariableUse(arrayVariable);
                factory.setPathHack(arrayUse1, methodElement);
                LocalVariableNode arrayNode1 =
                    extendWithNode(new LocalVariableNode(arrayUse1));

                MemberSelectTree lengthSelect =
                    treeBuilder.buildArrayLengthAccess(arrayUse1);
                factory.setPathHack(lengthSelect, methodElement);
                FieldAccessNode lengthAccessNode =
                    extendWithNode(new FieldAccessNode(lengthSelect, arrayNode1));

                BinaryTree lessThan =
                    treeBuilder.buildLessThan(indexUse1, lengthSelect);
                factory.setPathHack(lessThan, methodElement);

                extendWithNode(new LessThanNode(lessThan, indexNode1, lengthAccessNode));
                extendWithExtendedNode(new ConditionalJump(loopEntry, loopExit));

                // Loop body, starting with declaration of the loop iteration variable
                addLabelForNextNode(loopEntry);
                extendWithNode(new VariableDeclarationNode(variable));

                IdentifierTree arrayUse2 =
                    treeBuilder.buildVariableUse(arrayVariable);
                factory.setPathHack(arrayUse2, methodElement);
                LocalVariableNode arrayNode2 =
                    extendWithNode(new LocalVariableNode(arrayUse2));

                IdentifierTree indexUse2 =
                    treeBuilder.buildVariableUse(indexVariable);
                factory.setPathHack(indexUse2, methodElement);
                LocalVariableNode indexNode2 =
                    extendWithNode(new LocalVariableNode(indexUse2));

                ArrayAccessTree arrayAccess =
                    treeBuilder.buildArrayAccess(arrayUse2, indexUse2);
                factory.setPathHack(arrayAccess, methodElement);
                ArrayAccessNode arrayAccessNode =
                    extendWithNode(new ArrayAccessNode(arrayAccess, arrayNode2,
                                                       indexNode2));
                translateAssignment(variable,
                                    new LocalVariableNode(variable),
                                    arrayAccessNode);

                if (statement != null) {
                    scan(statement, p);
                }

                // Loop back edge
                addLabelForNextNode(updateStart);

                IdentifierTree indexUse3 =
                    treeBuilder.buildVariableUse(indexVariable);
                factory.setPathHack(indexUse3, methodElement);
                LocalVariableNode indexNode3 =
                    extendWithNode(new LocalVariableNode(indexUse3));

                UnaryTree postfixIncrement =
                    treeBuilder.buildPostfixIncrement(indexUse3);
                factory.setPathHack(postfixIncrement, methodElement);
                extendWithNode(new PostfixIncrementNode(postfixIncrement,
                                                            indexNode3));
                extendWithExtendedNode(new UnconditionalJump(conditionStart));
            }

            // Loop exit
            addLabelForNextNode(loopExit);

            breakTargetL = oldBreakTargetL;
            continueTargetL = oldContinueTargetL;

            return null;
        }
    }
}
