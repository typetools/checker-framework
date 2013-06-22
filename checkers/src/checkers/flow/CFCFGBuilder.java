package checkers.flow;

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
import dataflow.cfg.CFGBuilder;
import dataflow.cfg.ControlFlowGraph;
import dataflow.cfg.UnderlyingAST;
import dataflow.cfg.node.ArrayAccessNode;
import dataflow.cfg.node.AssignmentNode;
import dataflow.cfg.node.FieldAccessNode;
import dataflow.cfg.node.IntegerLiteralNode;
import dataflow.cfg.node.LessThanNode;
import dataflow.cfg.node.LocalVariableNode;
import dataflow.cfg.node.MethodAccessNode;
import dataflow.cfg.node.MethodInvocationNode;
import dataflow.cfg.node.Node;
import dataflow.cfg.node.NumericalAdditionNode;
import dataflow.cfg.node.VariableDeclarationNode;

import checkers.basetype.BaseTypeChecker;
import checkers.types.AnnotatedTypeFactory;
import checkers.types.AnnotatedTypeMirror;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
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
                .containsKey("assumeAssertionsAreEnabled"),
              checker.getProcessingEnvironment().getOptions()
                .containsKey("assumeAssertionsAreDisabled"));
        if (assumeAssertionsEnabled && assumeAssertionsDisabled) {
            ErrorReporter.errorAbort("Assertions cannot be assumed to be enabled and disabled at the same time.");
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
                Collection<String> warningKeys = checker.getSuppressWarningsKeys();
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
        public void handleArtificialTree(Tree tree) {
            // Record the method that encloses the newly created tree.
            MethodTree enclosingMethod = TreeUtils
                .enclosingMethod(getCurrentPath());
            Element methodElement = TreeUtils
                .elementFromDeclaration(enclosingMethod);
            factory.setPathHack(tree, methodElement);
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

            if (types.isSubtype(exprType, iterableType)) {
                // Take the upper bound of a type variable or wildcard
                exprType = TypesUtils.upperBound(exprType);

                assert (exprType instanceof DeclaredType) : "an Iterable must be a DeclaredType";
                DeclaredType declaredExprType = (DeclaredType) exprType;
                declaredExprType.getTypeArguments();

                MemberSelectTree iteratorSelect =
                    treeBuilder.buildIteratorMethodAccess(expression);
                handleArtificialTree(iteratorSelect);

                MethodInvocationTree iteratorCall =
                    treeBuilder.buildMethodInvocation(iteratorSelect);
                handleArtificialTree(iteratorCall);

                AnnotatedTypeMirror annotatedIteratorType =
                    factory.getAnnotatedType(iteratorCall);

                Tree annotatedIteratorTypeTree =
                    ((CFTreeBuilder)treeBuilder).buildAnnotatedType(annotatedIteratorType);
                handleArtificialTree(annotatedIteratorTypeTree);

                // Declare and initialize a new, unique iterator variable
                VariableTree iteratorVariable =
                    treeBuilder.buildVariableDecl(annotatedIteratorTypeTree,
                                                  uniqueName("iter"),
                                                  variableElement.getEnclosingElement(),
                                                  iteratorCall);
                handleArtificialTree(iteratorVariable);

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
                handleArtificialTree(iteratorUse1);

                LocalVariableNode iteratorReceiverNode =
                    extendWithNode(new LocalVariableNode(iteratorUse1));

                MemberSelectTree hasNextSelect =
                    treeBuilder.buildHasNextMethodAccess(iteratorUse1);
                handleArtificialTree(hasNextSelect);

                MethodAccessNode hasNextAccessNode =
                    extendWithNode(new MethodAccessNode(hasNextSelect, iteratorReceiverNode));

                MethodInvocationTree hasNextCall =
                    treeBuilder.buildMethodInvocation(hasNextSelect);
                handleArtificialTree(hasNextCall);

                extendWithNode(new MethodInvocationNode(hasNextCall, hasNextAccessNode,
                                                        Collections.<Node>emptyList(), getCurrentPath()));
                extendWithExtendedNode(new ConditionalJump(loopEntry, loopExit));

                // Loop body, starting with declaration of the loop iteration variable
                addLabelForNextNode(loopEntry);
                extendWithNode(new VariableDeclarationNode(variable));

                IdentifierTree iteratorUse2 =
                    treeBuilder.buildVariableUse(iteratorVariable);
                handleArtificialTree(iteratorUse2);

                LocalVariableNode iteratorReceiverNode2 =
                    extendWithNode(new LocalVariableNode(iteratorUse2));

                MemberSelectTree nextSelect =
                    treeBuilder.buildNextMethodAccess(iteratorUse2);
                handleArtificialTree(nextSelect);

                MethodAccessNode nextAccessNode =
                    extendWithNode(new MethodAccessNode(nextSelect, iteratorReceiverNode2));

                MethodInvocationTree nextCall =
                    treeBuilder.buildMethodInvocation(nextSelect);
                handleArtificialTree(nextCall);

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
                handleArtificialTree(annotatedArrayTypeTree);

                // Declare and initialize a temporary array variable
                VariableTree arrayVariable =
                    treeBuilder.buildVariableDecl(annotatedArrayTypeTree,
                                                  uniqueName("array"),
                                                  variableElement.getEnclosingElement(),
                                                  expression);
                handleArtificialTree(arrayVariable);

                extendWithNode(new VariableDeclarationNode(arrayVariable));
                Node expressionNode = scan(expression, p);

                translateAssignment(arrayVariable,
                                    new LocalVariableNode(arrayVariable),
                                    expressionNode);

                // Declare and initialize the loop index variable
                TypeMirror intType = types.getPrimitiveType(TypeKind.INT);

                LiteralTree zero =
                    treeBuilder.buildLiteral(new Integer(0));
                handleArtificialTree(zero);

                VariableTree indexVariable =
                    treeBuilder.buildVariableDecl(intType,
                                                  uniqueName("index"),
                                                  variableElement.getEnclosingElement(),
                                                  zero);
                handleArtificialTree(indexVariable);
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
                handleArtificialTree(indexUse1);
                LocalVariableNode indexNode1 =
                    extendWithNode(new LocalVariableNode(indexUse1));

                IdentifierTree arrayUse1 =
                    treeBuilder.buildVariableUse(arrayVariable);
                handleArtificialTree(arrayUse1);
                LocalVariableNode arrayNode1 =
                    extendWithNode(new LocalVariableNode(arrayUse1));

                MemberSelectTree lengthSelect =
                    treeBuilder.buildArrayLengthAccess(arrayUse1);
                handleArtificialTree(lengthSelect);
                FieldAccessNode lengthAccessNode =
                    extendWithNode(new FieldAccessNode(lengthSelect, arrayNode1));

                BinaryTree lessThan =
                    treeBuilder.buildLessThan(indexUse1, lengthSelect);
                handleArtificialTree(lessThan);

                extendWithNode(new LessThanNode(lessThan, indexNode1, lengthAccessNode));
                extendWithExtendedNode(new ConditionalJump(loopEntry, loopExit));

                // Loop body, starting with declaration of the loop iteration variable
                addLabelForNextNode(loopEntry);
                extendWithNode(new VariableDeclarationNode(variable));

                IdentifierTree arrayUse2 =
                    treeBuilder.buildVariableUse(arrayVariable);
                handleArtificialTree(arrayUse2);
                LocalVariableNode arrayNode2 =
                    extendWithNode(new LocalVariableNode(arrayUse2));

                IdentifierTree indexUse2 =
                    treeBuilder.buildVariableUse(indexVariable);
                handleArtificialTree(indexUse2);
                LocalVariableNode indexNode2 =
                    extendWithNode(new LocalVariableNode(indexUse2));

                ArrayAccessTree arrayAccess =
                    treeBuilder.buildArrayAccess(arrayUse2, indexUse2);
                handleArtificialTree(arrayAccess);
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
                handleArtificialTree(indexUse3);
                LocalVariableNode indexNode3 =
                    extendWithNode(new LocalVariableNode(indexUse3));

                LiteralTree oneTree = treeBuilder.buildLiteral(Integer.valueOf(1));
                handleArtificialTree(oneTree);
                Node one = new IntegerLiteralNode(oneTree);
                extendWithNode(one);

                BinaryTree addOneTree = treeBuilder.buildBinary(intType, Tree.Kind.PLUS,
                        indexUse3, oneTree);
                handleArtificialTree(addOneTree);
                Node addOneNode = new NumericalAdditionNode(addOneTree, indexNode3, one);
                extendWithNode(addOneNode);

                AssignmentTree assignTree = treeBuilder.buildAssignment(indexUse3, addOneTree);
                handleArtificialTree(assignTree);
                Node assignNode = new AssignmentNode(assignTree, indexNode3, addOneNode);
                extendWithNode(assignNode);

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
