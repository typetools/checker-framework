package org.checkerframework.afu.annotator.find;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.AssertTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CatchTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.DoWhileLoopTree;
import com.sun.source.tree.EnhancedForLoopTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.LabeledStatementTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.SwitchTree;
import com.sun.source.tree.SynchronizedTree;
import com.sun.source.tree.ThrowTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TryTree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.UnionTypeTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.tree.WhileLoopTree;
import com.sun.source.tree.WildcardTree;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.tree.JCTree;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.afu.annotator.Main;
import org.checkerframework.afu.scenelib.io.ASTPath;

/** A criterion to determine if a node matches a path through the AST. */
public class ASTPathCriterion implements Criterion {

  public static boolean debug = Main.debug;

  /** The path through the AST to match. */
  ASTPath astPath;

  /**
   * Constructs a new ASTPathCriterion to match the given AST path.
   *
   * <p>This assumes that the astPath is valid. Specifically, that all of its arguments have been
   * previously validated.
   *
   * @param astPath the AST path to match
   */
  public ASTPathCriterion(ASTPath astPath) {
    this.astPath = astPath;
  }

  @Override
  public boolean isSatisfiedBy(TreePath path, Tree leaf) {
    if (path == null) {
      return false;
    }
    assert path.getLeaf() == leaf;
    return isSatisfiedBy(path);
  }

  @Override
  public boolean isSatisfiedBy(TreePath path) {
    if (path == null) {
      return false;
    }

    // actualPath stores the path through the source code AST to this
    // location (specified by the "path" parameter to this method). It is
    // computed by traversing from this location up the source code AST
    // until it reaches a method node (this gets only the part of the path
    // within a method) or class node (this gets only the part of the path
    // within a field).
    List<Tree> actualPath = new ArrayList<>();
    Tree leaf = path.getLeaf();
    Tree.Kind kind = leaf.getKind();
    while (kind != Tree.Kind.METHOD && !ASTPath.isClassEquiv(kind)) {
      actualPath.add(0, leaf);
      path = path.getParentPath();
      if (path == null) {
        break;
      }
      leaf = path.getLeaf();
      kind = leaf.getKind();
    }

    // If astPath starts with Method.* or Class.*, include the
    // MethodTree or ClassTree on actualPath.
    if (path != null && !astPath.isEmpty()) {
      Tree.Kind entryKind = astPath.get(0).getTreeKind();
      if ((entryKind == Tree.Kind.METHOD && kind == Tree.Kind.METHOD)
          || (entryKind == Tree.Kind.CLASS && ASTPath.isClassEquiv(kind))) {
        actualPath.add(0, leaf);
      }
    }

    if (debug) {
      System.out.println("ASTPathCriterion.isSatisfiedBy");
      System.out.println("  path=" + astPath);
      System.out.println("  path elements:");
      for (Tree t : actualPath) {
        System.out.println("  " + t.getKind() + ": " + Main.treeToString(t));
      }
    }

    int astPathLen = astPath.size();
    int actualPathLen = actualPath.size();
    if (astPathLen == 0 || actualPathLen == 0) {
      return false;
    }
    // if (actualPathLen != astPathLen + (isOnNewArrayType ? 0 : 1)) {
    //    return false;
    // }

    Tree next = null;
    int i = 0;
    while (true) {
      ASTPath.ASTEntry astNode = astPath.get(i);
      Tree actualNode = actualPath.get(i);
      if (!kindsMatch(astNode.getTreeKind(), actualNode.getKind())) {
        return isBoundableWildcard(actualPath, i);
      }

      if (debug) {
        System.out.println("astNode: " + astNode);
        System.out.println("actualNode: " + actualNode.getKind());
      }

      // Based on the child selector and (optional) argument in "astNode",
      // "next" will get set to the next source node below "actualNode".
      // Then "next" will be compared with the node following "astNode"
      // in "actualPath". If it's not a match, this is not the correct
      // location. If it is a match, keep going.
      next = getNext(actualNode, astPath, i);
      if (next == null) {
        return checkNull(actualPath, i);
      }
      if (!(next instanceof JCTree)) {
        // converted from array type, not in source AST...
        if (actualPathLen == i + 1) {
          // need to extend actualPath with "artificial" node
          actualPath.add(next);
          ++actualPathLen;
        }
      }
      if (debug) {
        System.out.println("next: " + next);
      }

      // if (++i >= astPathLen || i >= actualPathLen) { break; }
      if (++i >= astPathLen) {
        break;
      }
      if (i >= actualPathLen) {
        return checkNull(actualPath, i - 1);
      }
      if (!matchNext(next, actualPath.get(i))) {
        if (debug) {
          System.out.println("no next match");
        }
        return false;
      }
    }

    if ((i < actualPathLen && matchNext(next, actualPath.get(i)))
        || (i <= actualPathLen && next instanceof NewArrayTree)) {
      return true;
    }

    if (debug) {
      System.out.println("no next match");
    }
    return false;
  }

  @Override
  public boolean isOnlyTypeAnnotationCriterion() {
    // TODO: This return value is conservative.  Can do better by examining the path.
    return false;
  }

  /**
   * Returns true if the given trees match.
   *
   * @param next a tree
   * @param node a tree
   * @return true if the given trees match
   */
  private boolean matchNext(Tree next, Tree node) {
    boolean b1 = next instanceof JCTree;
    boolean b2 = node instanceof JCTree;
    if (b1 && !b2) {
      next = Insertions.TypeTree.fromJCTree((JCTree) next);
    } else if (b2 && !b1) {
      node = Insertions.TypeTree.fromJCTree((JCTree) node);
    }

    try {
      return next.accept(
          new SimpleTreeVisitor<Boolean, Tree>() {
            @Override
            public Boolean defaultAction(Tree t1, Tree t2) {
              return t1 == t2;
            }

            @Override
            public Boolean visitIdentifier(IdentifierTree v, Tree t) {
              return v == t;
              // IdentifierTree i2 = (IdentifierTree) t;
              // return i1.getName().toString()
              //        .equals(i2.getName().toString());
            }

            @Override
            public Boolean visitAnnotatedType(AnnotatedTypeTree a1, Tree t) {
              AnnotatedTypeTree a2 = (AnnotatedTypeTree) t;
              return matchNext(a1.getUnderlyingType(), a2.getUnderlyingType());
            }

            // @Override
            // public Boolean
            // visitArrayType(ArrayTypeTree b1, Tree t) {
            //    ArrayTypeTree b2 = (ArrayTypeTree) t;
            //    return matchNext(b1.getType(), b2.getType());
            // }

            @Override
            public Boolean visitMemberSelect(MemberSelectTree c1, Tree t) {
              MemberSelectTree c2 = (MemberSelectTree) t;
              return c1.getIdentifier().toString().equals(c2.getIdentifier().toString())
                  && matchNext(c1.getExpression(), c2.getExpression());
            }

            @Override
            public Boolean visitWildcard(WildcardTree d1, Tree t) {
              return d1 == (WildcardTree) t;
              // WildcardTree d2 = (WildcardTree) t;
              // Tree bound2 = d2.getBound();
              // Tree bound1 = d1.getBound();
              // return bound1 == bound2 || matchNext(bound1, bound2);
            }

            @Override
            public Boolean visitParameterizedType(ParameterizedTypeTree e1, Tree t) {
              ParameterizedTypeTree e2 = (ParameterizedTypeTree) t;
              List<? extends Tree> l2 = e2.getTypeArguments();
              List<? extends Tree> l1 = e1.getTypeArguments();
              if (l1.size() == l2.size()) {
                int i = 0;
                for (Tree t1 : l1) {
                  Tree t2 = l2.get(i++);
                  if (!matchNext(t1, t2)) {
                    return false;
                  }
                }
                return matchNext(e1.getType(), e2.getType());
              }
              return false;
            }
          },
          node);
    } catch (RuntimeException ex) {
      return false;
    }
  }

  private Tree getNext(Tree actualNode, ASTPath astPath, int ix) {
    try {
      ASTPath.ASTEntry astNode = astPath.get(ix);
      switch (actualNode.getKind()) {
        case ANNOTATED_TYPE:
          {
            AnnotatedTypeTree annotatedType = (AnnotatedTypeTree) actualNode;
            if (astNode.childSelectorIs(ASTPath.ANNOTATION)) {
              int arg = astNode.getArgument();
              List<? extends AnnotationTree> annos = annotatedType.getAnnotations();
              if (arg >= annos.size()) {
                return null;
              }
              return annos.get(arg);
            } else {
              return annotatedType.getUnderlyingType();
            }
          }
        case ARRAY_ACCESS:
          {
            ArrayAccessTree arrayAccess = (ArrayAccessTree) actualNode;
            if (astNode.childSelectorIs(ASTPath.EXPRESSION)) {
              return arrayAccess.getExpression();
            } else {
              return arrayAccess.getIndex();
            }
          }
        case ARRAY_TYPE:
          {
            ArrayTypeTree arrayType = (ArrayTypeTree) actualNode;
            return arrayType.getType();
          }
        case ASSERT:
          {
            AssertTree azzert = (AssertTree) actualNode;
            if (astNode.childSelectorIs(ASTPath.CONDITION)) {
              return azzert.getCondition();
            } else {
              return azzert.getDetail();
            }
          }
        case ASSIGNMENT:
          {
            AssignmentTree assignment = (AssignmentTree) actualNode;
            if (astNode.childSelectorIs(ASTPath.VARIABLE)) {
              return assignment.getVariable();
            } else {
              return assignment.getExpression();
            }
          }
        case BLOCK:
          {
            BlockTree block = (BlockTree) actualNode;
            int arg = astNode.getArgument();
            List<? extends StatementTree> statements = block.getStatements();
            if (arg >= block.getStatements().size()) {
              return null;
            }
            return statements.get(arg);
          }
        case CASE:
          {
            CaseTree caze = (CaseTree) actualNode;
            int arg = astNode.getArgument();
            if (astNode.childSelectorIs(ASTPath.EXPRESSION)) {
              List<? extends ExpressionTree> expressions = CaseUtils.caseTreeGetExpressions(caze);
              if (arg >= expressions.size()) {
                return null;
              }
              return expressions.get(arg);
            } else {
              List<? extends StatementTree> statements = caze.getStatements();
              if (arg >= statements.size()) {
                return null;
              }
              return statements.get(arg);
            }
          }
        case CATCH:
          {
            CatchTree cach = (CatchTree) actualNode;
            if (astNode.childSelectorIs(ASTPath.PARAMETER)) {
              return cach.getParameter();
            } else {
              return cach.getBlock();
            }
          }
        case ANNOTATION:
        case CLASS:
        case ENUM:
        case INTERFACE:
          {
            ClassTree clazz = (ClassTree) actualNode;
            int arg = astNode.getArgument();
            if (astNode.childSelectorIs(ASTPath.TYPE_PARAMETER)) {
              return clazz.getTypeParameters().get(arg);
            } else if (astNode.childSelectorIs(ASTPath.INITIALIZER)) {
              int i = 0;
              for (Tree member : clazz.getMembers()) {
                if (member instanceof BlockTree && arg == i++) {
                  return member;
                }
              }
              return null;
            } else if (astNode.childSelectorIs(ASTPath.BOUND)) {
              return arg < 0 ? clazz.getExtendsClause() : clazz.getImplementsClause().get(arg);
            } else {
              return null;
            }
          }
        case CONDITIONAL_EXPRESSION:
          {
            ConditionalExpressionTree conditionalExpression =
                (ConditionalExpressionTree) actualNode;
            if (astNode.childSelectorIs(ASTPath.CONDITION)) {
              return conditionalExpression.getCondition();
            } else if (astNode.childSelectorIs(ASTPath.TRUE_EXPRESSION)) {
              return conditionalExpression.getTrueExpression();
            } else {
              return conditionalExpression.getFalseExpression();
            }
          }
        case DO_WHILE_LOOP:
          {
            DoWhileLoopTree doWhileLoop = (DoWhileLoopTree) actualNode;
            if (astNode.childSelectorIs(ASTPath.CONDITION)) {
              return doWhileLoop.getCondition();
            } else {
              return doWhileLoop.getStatement();
            }
          }
        case ENHANCED_FOR_LOOP:
          {
            EnhancedForLoopTree enhancedForLoop = (EnhancedForLoopTree) actualNode;
            if (astNode.childSelectorIs(ASTPath.VARIABLE)) {
              return enhancedForLoop.getVariable();
            } else if (astNode.childSelectorIs(ASTPath.EXPRESSION)) {
              return enhancedForLoop.getExpression();
            } else {
              return enhancedForLoop.getStatement();
            }
          }
        case EXPRESSION_STATEMENT:
          {
            ExpressionStatementTree expressionStatement = (ExpressionStatementTree) actualNode;
            return expressionStatement.getExpression();
          }
        case FOR_LOOP:
          {
            ForLoopTree forLoop = (ForLoopTree) actualNode;
            if (astNode.childSelectorIs(ASTPath.INITIALIZER)) {
              int arg = astNode.getArgument();
              List<? extends StatementTree> inits = forLoop.getInitializer();
              if (arg >= inits.size()) {
                return null;
              }
              return inits.get(arg);
            } else if (astNode.childSelectorIs(ASTPath.CONDITION)) {
              return forLoop.getCondition();
            } else if (astNode.childSelectorIs(ASTPath.UPDATE)) {
              int arg = astNode.getArgument();
              List<? extends ExpressionStatementTree> updates = forLoop.getUpdate();
              if (arg >= updates.size()) {
                return null;
              }
              return updates.get(arg);
            } else {
              return forLoop.getStatement();
            }
          }
        case IF:
          {
            IfTree iff = (IfTree) actualNode;
            if (astNode.childSelectorIs(ASTPath.CONDITION)) {
              return iff.getCondition();
            } else if (astNode.childSelectorIs(ASTPath.THEN_STATEMENT)) {
              return iff.getThenStatement();
            } else {
              return iff.getElseStatement();
            }
          }
        case INSTANCE_OF:
          {
            InstanceOfTree instanceOf = (InstanceOfTree) actualNode;
            if (astNode.childSelectorIs(ASTPath.EXPRESSION)) {
              return instanceOf.getExpression();
            } else {
              return instanceOf.getType();
            }
          }
        case LABELED_STATEMENT:
          {
            LabeledStatementTree labeledStatement = (LabeledStatementTree) actualNode;
            return labeledStatement.getStatement();
          }
        case LAMBDA_EXPRESSION:
          {
            LambdaExpressionTree lambdaExpression = (LambdaExpressionTree) actualNode;
            if (astNode.childSelectorIs(ASTPath.PARAMETER)) {
              int arg = astNode.getArgument();
              List<? extends VariableTree> params = lambdaExpression.getParameters();
              if (arg >= params.size()) {
                return null;
              }
              return params.get(arg);
            } else {
              return lambdaExpression.getBody();
            }
          }
        case MEMBER_REFERENCE:
          {
            MemberReferenceTree memberReference = (MemberReferenceTree) actualNode;
            if (astNode.childSelectorIs(ASTPath.QUALIFIER_EXPRESSION)) {
              return memberReference.getQualifierExpression();
            } else {
              int arg = astNode.getArgument();
              List<? extends ExpressionTree> typeArgs = memberReference.getTypeArguments();
              if (arg >= typeArgs.size()) {
                return null;
              }
              return typeArgs.get(arg);
            }
          }
        case MEMBER_SELECT:
          {
            MemberSelectTree memberSelect = (MemberSelectTree) actualNode;
            return memberSelect.getExpression();
          }
        case METHOD:
          {
            MethodTree method = (MethodTree) actualNode;
            if (astNode.childSelectorIs(ASTPath.TYPE)) {
              return method.getReturnType();
            } else if (astNode.childSelectorIs(ASTPath.PARAMETER)) {
              int arg = astNode.getArgument();
              List<? extends VariableTree> params = method.getParameters();
              return arg < 0
                  ? method.getReceiverParameter()
                  : arg < params.size() ? params.get(arg) : null;
            } else if (astNode.childSelectorIs(ASTPath.TYPE_PARAMETER)) {
              int arg = astNode.getArgument();
              return method.getTypeParameters().get(arg);
            } else { // BODY
              return method.getBody();
            }
          }
        case METHOD_INVOCATION:
          {
            MethodInvocationTree methodInvocation = (MethodInvocationTree) actualNode;
            if (astNode.childSelectorIs(ASTPath.TYPE_ARGUMENT)) {
              int arg = astNode.getArgument();
              List<? extends Tree> typeArgs = methodInvocation.getTypeArguments();
              if (arg >= typeArgs.size()) {
                return null;
              }
              return typeArgs.get(arg);
            } else if (astNode.childSelectorIs(ASTPath.METHOD_SELECT)) {
              return methodInvocation.getMethodSelect();
            } else {
              int arg = astNode.getArgument();
              List<? extends ExpressionTree> args = methodInvocation.getArguments();
              if (arg >= args.size()) {
                return null;
              }
              return args.get(arg);
            }
          }
        case NEW_ARRAY:
          {
            NewArrayTree newArray = (NewArrayTree) actualNode;
            if (astNode.childSelectorIs(ASTPath.TYPE)) {
              Type type = ((JCTree.JCNewArray) newArray).type;
              Tree typeTree = Insertions.TypeTree.fromJavacType(type);
              int arg = astNode.getArgument();
              if (arg == 0 && astPath.size() == ix + 1) {
                return newArray;
                // if (astPath.size() != ix+1) { return null; }
                // return typeTree;
                // return ((ArrayTypeTree) typeTree).getType();
                // return newArray;
              }
              typeTree = ((NewArrayTree) typeTree).getType();
              while (--arg > 0) {
                if (!(typeTree instanceof ArrayTypeTree)) {
                  return null;
                }
                typeTree = ((ArrayTypeTree) typeTree).getType();
              }
              return typeTree;
            } else if (astNode.childSelectorIs(ASTPath.DIMENSION)) {
              int arg = astNode.getArgument();
              List<? extends ExpressionTree> dims = newArray.getDimensions();
              return arg < dims.size() ? dims.get(arg) : null;
            } else {
              int arg = astNode.getArgument();
              List<? extends ExpressionTree> inits = newArray.getInitializers();
              return arg < inits.size() ? inits.get(arg) : null;
            }
          }
        case NEW_CLASS:
          {
            NewClassTree newClass = (NewClassTree) actualNode;
            if (astNode.childSelectorIs(ASTPath.ENCLOSING_EXPRESSION)) {
              return newClass.getEnclosingExpression();
            } else if (astNode.childSelectorIs(ASTPath.TYPE_ARGUMENT)) {
              int arg = astNode.getArgument();
              List<? extends Tree> typeArgs = newClass.getTypeArguments();
              if (arg >= typeArgs.size()) {
                return null;
              }
              return typeArgs.get(arg);
            } else if (astNode.childSelectorIs(ASTPath.IDENTIFIER)) {
              return newClass.getIdentifier();
            } else if (astNode.childSelectorIs(ASTPath.ARGUMENT)) {
              int arg = astNode.getArgument();
              List<? extends ExpressionTree> args = newClass.getArguments();
              if (arg >= args.size()) {
                return null;
              }
              return args.get(arg);
            } else {
              return newClass.getClassBody(); // For anonymous classes
            }
          }
        case PARAMETERIZED_TYPE:
          {
            ParameterizedTypeTree parameterizedType = (ParameterizedTypeTree) actualNode;
            if (astNode.childSelectorIs(ASTPath.TYPE)) {
              return parameterizedType.getType();
            } else {
              int arg = astNode.getArgument();
              List<? extends Tree> typeArgs = parameterizedType.getTypeArguments();
              if (arg >= typeArgs.size()) {
                return null;
              }
              return typeArgs.get(arg);
            }
          }
        case PARENTHESIZED:
          {
            ParenthesizedTree parenthesized = (ParenthesizedTree) actualNode;
            return parenthesized.getExpression();
          }
        case RETURN:
          {
            ReturnTree returnn = (ReturnTree) actualNode;
            return returnn.getExpression();
          }
        case SWITCH:
          {
            SwitchTree zwitch = (SwitchTree) actualNode;
            if (astNode.childSelectorIs(ASTPath.EXPRESSION)) {
              return zwitch.getExpression();
            } else {
              int arg = astNode.getArgument();
              List<? extends CaseTree> cases = zwitch.getCases();
              if (arg >= cases.size()) {
                return null;
              }
              return cases.get(arg);
            }
          }
        case SYNCHRONIZED:
          {
            SynchronizedTree synchronizzed = (SynchronizedTree) actualNode;
            if (astNode.childSelectorIs(ASTPath.EXPRESSION)) {
              return synchronizzed.getExpression();
            } else {
              return synchronizzed.getBlock();
            }
          }
        case THROW:
          {
            ThrowTree throww = (ThrowTree) actualNode;
            return throww.getExpression();
          }
        case TRY:
          {
            TryTree tryy = (TryTree) actualNode;
            if (astNode.childSelectorIs(ASTPath.BLOCK)) {
              return tryy.getBlock();
            } else if (astNode.childSelectorIs(ASTPath.CATCH)) {
              int arg = astNode.getArgument();
              List<? extends CatchTree> catches = tryy.getCatches();
              if (arg >= catches.size()) {
                return null;
              }
              return catches.get(arg);
            } else if (astNode.childSelectorIs(ASTPath.FINALLY_BLOCK)) {
              return tryy.getFinallyBlock();
            } else {
              int arg = astNode.getArgument();
              List<? extends Tree> resources = tryy.getResources();
              if (arg >= resources.size()) {
                return null;
              }
              return resources.get(arg);
            }
          }
        case TYPE_CAST:
          {
            TypeCastTree typeCast = (TypeCastTree) actualNode;
            if (astNode.childSelectorIs(ASTPath.TYPE)) {
              return typeCast.getType();
            } else {
              return typeCast.getExpression();
            }
          }
        case TYPE_PARAMETER:
          {
            TypeParameterTree typeParam = (TypeParameterTree) actualNode;
            List<? extends Tree> bounds = typeParam.getBounds();
            int arg = astNode.getArgument();
            return bounds.get(arg);
          }
        case UNION_TYPE:
          {
            UnionTypeTree unionType = (UnionTypeTree) actualNode;
            int arg = astNode.getArgument();
            List<? extends Tree> typeAlts = unionType.getTypeAlternatives();
            if (arg >= typeAlts.size()) {
              return null;
            }
            return typeAlts.get(arg);
          }
        case VARIABLE:
          {
            // A VariableTree can have modifiers, but we only look at
            // the initializer and type because modifiers can't be
            // annotated. Any annotations on the LHS must be on the type.
            VariableTree var = (VariableTree) actualNode;
            if (astNode.childSelectorIs(ASTPath.INITIALIZER)) {
              return var.getInitializer();
            } else if (astNode.childSelectorIs(ASTPath.TYPE)) {
              return var.getType();
            } else {
              return null;
            }
          }
        case WHILE_LOOP:
          {
            WhileLoopTree whileLoop = (WhileLoopTree) actualNode;
            if (astNode.childSelectorIs(ASTPath.CONDITION)) {
              return whileLoop.getCondition();
            } else {
              return whileLoop.getStatement();
            }
          }
        default:
          {
            if (ASTPath.isBinaryOperator(actualNode.getKind())) {
              BinaryTree binary = (BinaryTree) actualNode;
              if (astNode.childSelectorIs(ASTPath.LEFT_OPERAND)) {
                return binary.getLeftOperand();
              } else {
                return binary.getRightOperand();
              }
            } else if (ASTPath.isCompoundAssignment(actualNode.getKind())) {
              CompoundAssignmentTree compoundAssignment = (CompoundAssignmentTree) actualNode;
              if (astNode.childSelectorIs(ASTPath.VARIABLE)) {
                return compoundAssignment.getVariable();
              } else {
                return compoundAssignment.getExpression();
              }
            } else if (ASTPath.isUnaryOperator(actualNode.getKind())) {
              UnaryTree unary = (UnaryTree) actualNode;
              return unary.getExpression();
            } else if (isWildcard(actualNode.getKind())) {
              WildcardTree wildcard = (WildcardTree) actualNode;
              return wildcard.getBound();
            } else {
              throw new IllegalArgumentException("Illegal kind: " + actualNode.getKind());
            }
          }
      }
    } catch (RuntimeException ex) {
      return null;
    }
  }

  private boolean checkNull(List<Tree> path, int ix) {
    Tree node = path.get(path.size() - 1);
    int last = astPath.size() - 1;
    ASTPath.ASTEntry entry = astPath.get(ix);
    Tree.Kind kind = entry.getTreeKind();

    switch (kind) {
      // case ANNOTATION:
      // case INTERFACE:
      case CLASS: // "extends" clause?
        return ASTPath.isClassEquiv(kind)
            && ix == last
            && entry.getArgument() == -1
            && entry.childSelectorIs(ASTPath.BOUND);
      case TYPE_PARAMETER:
        return node instanceof TypeParameterTree
            && ix == last
            && entry.getArgument() == 0
            && entry.childSelectorIs(ASTPath.BOUND);
      case METHOD: // nullary constructor? receiver?
        if (!(node instanceof MethodTree)) {
          return false;
        }
        MethodTree method = (MethodTree) node;
        List<? extends VariableTree> params = method.getParameters();
        if ("<init>".equals(method.getName().toString())) {
          if (ix == last) {
            return true;
          }
          ASTPath.ASTEntry next = astPath.get(++ix);
          String selector = next.getChildSelector();
          Tree typeTree =
              ASTPath.TYPE_PARAMETER.equals(selector)
                  ? method.getTypeParameters().get(next.getArgument())
                  : ASTPath.PARAMETER.equals(selector)
                      ? params.get(next.getArgument()).getType()
                      : null;
          return typeTree != null && checkTypePath(ix, typeTree);
        } else if (entry.childSelectorIs(ASTPath.PARAMETER) && entry.getArgument() == -1) {
          if (ix == last) {
            return true;
          }
          VariableTree rcvrParam = method.getReceiverParameter();
          if (rcvrParam == null) { // TODO
            // ClassTree clazz = methodReceiverType(path);
            // return checkReceiverType(ix,
            //    ((JCTree.JCClassDecl) clazz).type);
          } else {
            return checkTypePath(ix + 1, rcvrParam.getType());
          }
        }
        return false;
      case NEW_ARRAY:
        if (!(node instanceof NewArrayTree)) {
          return false;
        }
        NewArrayTree newArray = (NewArrayTree) node;
        int arg = entry.getArgument();
        if (entry.childSelectorIs(ASTPath.TYPE)) {
          if (ix == last) {
            return true;
          }
          // Tree t = newArray.getType();
          // int depth = 1;
          // while (t.getKind() == Tree.Kind.ARRAY_TYPE) {
          //    t = ((ArrayTypeTree) t).getType();
          //    ++depth;
          // }
          return arg == arrayDepth(newArray);
        } else {
          List<? extends ExpressionTree> typeTrees =
              entry.childSelectorIs(ASTPath.DIMENSION)
                  ? newArray.getDimensions()
                  : entry.childSelectorIs(ASTPath.INITIALIZER) ? newArray.getInitializers() : null;
          return typeTrees != null
              && arg < typeTrees.size()
              && checkTypePath(ix + 1, typeTrees.get(arg));
        }
      case UNBOUNDED_WILDCARD:
        return isBoundableWildcard(path, path.size() - 1);
      default: // TODO: casts?
        return false;
    }
  }

  /**
   * Returns the array depth of the given tree.
   *
   * @param tree a tree
   * @return the array depth of the given tree
   */
  private static int arrayDepth(Tree tree) {
    if (tree instanceof NewArrayTree) {
      NewArrayTree newArray = (NewArrayTree) tree;
      Tree type = newArray.getType();
      if (type != null) {
        return type.accept(
            new SimpleTreeVisitor<Integer, Integer>() {
              @Override
              public Integer visitArrayType(ArrayTypeTree t, Integer i) {
                return t.getType().accept(this, i + 1);
              }

              @Override
              public Integer defaultAction(Tree t, Integer i) {
                return i;
              }
            },
            1);
      }
      int depth = newArray.getDimensions().size();
      for (ExpressionTree elem : newArray.getInitializers()) {
        Tree.Kind kind = elem.getKind();
        if (kind == Tree.Kind.NEW_ARRAY || kind == Tree.Kind.ARRAY_TYPE) {
          depth = Math.max(depth, arrayDepth(elem) + 1);
        }
      }
      return depth;
    } else if (tree instanceof AnnotatedTypeTree) {
      return arrayDepth(((AnnotatedTypeTree) tree).getUnderlyingType());
    } else if (tree instanceof ArrayTypeTree) {
      return 1 + arrayDepth(((ArrayTypeTree) tree).getType());
    } else {
      return 0;
    }
  }

  @SuppressWarnings("EmptyCatch") // See comment at the catch block
  private boolean checkTypePath(int i, Tree typeTree) {
    try {
      loop:
      while (typeTree != null && i < astPath.size()) {
        ASTPath.ASTEntry entry = astPath.get(i);
        Tree.Kind kind = entry.getTreeKind();
        switch (kind) {
          case ANNOTATED_TYPE:
            typeTree = ((AnnotatedTypeTree) typeTree).getUnderlyingType();
            continue;
          case ARRAY_TYPE:
            typeTree = ((ArrayTypeTree) typeTree).getType();
            break;
          case MEMBER_SELECT:
            typeTree = ((MemberSelectTree) typeTree).getExpression();
            break;
          case PARAMETERIZED_TYPE:
            if (entry.childSelectorIs(ASTPath.TYPE_ARGUMENT)) {
              int arg = entry.getArgument();
              typeTree = ((ParameterizedTypeTree) typeTree).getTypeArguments().get(arg);
            } else { // TYPE
              typeTree = ((ParameterizedTypeTree) typeTree).getType();
            }
            break;
          default:
            if (isWildcard(kind)) {
              return ++i == astPath.size(); // ???
            }
            break loop;
        }
        ++i;
      }
    } catch (RuntimeException ex) {
      // Ignore the exception.  We think this is the right behavior based on comments above the call
      // to `checkNull` (which calls this) in `isSatisfiedBy`.
    }
    return false;
  }

  /**
   * Determines if the given kinds match, false otherwise. Two kinds match if they're exactly the
   * same or if the two kinds are both compound assignments, unary operators, binary operators or
   * wildcards.
   *
   * <p>This is necessary because in the JAIF file these kinds are represented by their general
   * types (i.e. BinaryOperator, CompoundOperator, etc.) rather than their kind (i.e. PLUS, MINUS,
   * PLUS_ASSIGNMENT, XOR_ASSIGNMENT, etc.). Internally, a single kind is used to represent each
   * general type (i.e. PLUS is used for BinaryOperator, PLUS_ASSIGNMENT is used for
   * CompoundAssignment, etc.). Yet, the actual source nodes have the correct kind. So if an AST
   * path entry has a PLUS kind, that really means it could be any BinaryOperator, resulting in PLUS
   * matching any other BinaryOperator.
   *
   * @param kind1 the first kind to match
   * @param kind2 the second kind to match
   * @return {@code true} if the kinds match as described above, {@code false} otherwise
   */
  private boolean kindsMatch(Tree.Kind kind1, Tree.Kind kind2) {
    return kind1 == kind2
        ? true
        : ASTPath.isClassEquiv(kind1)
            ? ASTPath.isClassEquiv(kind2)
            : ASTPath.isCompoundAssignment(kind1)
                ? ASTPath.isCompoundAssignment(kind2)
                : ASTPath.isUnaryOperator(kind1)
                    ? ASTPath.isUnaryOperator(kind2)
                    : ASTPath.isBinaryOperator(kind1)
                        ? ASTPath.isBinaryOperator(kind2)
                        : ASTPath.isWildcard(kind1) ? ASTPath.isWildcard(kind2) : false;
  }

  /**
   * Determines if the given kind is a binary operator.
   *
   * @param kind the kind to test
   * @return true if the given kind is a binary operator
   */
  public boolean isBinaryOperator(Tree.Kind kind) {
    return kind == Tree.Kind.MULTIPLY
        || kind == Tree.Kind.DIVIDE
        || kind == Tree.Kind.REMAINDER
        || kind == Tree.Kind.PLUS
        || kind == Tree.Kind.MINUS
        || kind == Tree.Kind.LEFT_SHIFT
        || kind == Tree.Kind.RIGHT_SHIFT
        || kind == Tree.Kind.UNSIGNED_RIGHT_SHIFT
        || kind == Tree.Kind.LESS_THAN
        || kind == Tree.Kind.GREATER_THAN
        || kind == Tree.Kind.LESS_THAN_EQUAL
        || kind == Tree.Kind.GREATER_THAN_EQUAL
        || kind == Tree.Kind.EQUAL_TO
        || kind == Tree.Kind.NOT_EQUAL_TO
        || kind == Tree.Kind.AND
        || kind == Tree.Kind.XOR
        || kind == Tree.Kind.OR
        || kind == Tree.Kind.CONDITIONAL_AND
        || kind == Tree.Kind.CONDITIONAL_OR;
  }

  public boolean isExpression(Tree.Kind kind) {
    switch (kind) {
      case ARRAY_ACCESS:
      case ASSIGNMENT:
      case CONDITIONAL_EXPRESSION:
      case EXPRESSION_STATEMENT:
      case MEMBER_SELECT:
      case MEMBER_REFERENCE:
      case IDENTIFIER:
      case INSTANCE_OF:
      case METHOD_INVOCATION:
      case NEW_ARRAY:
      case NEW_CLASS:
      case LAMBDA_EXPRESSION:
      case PARENTHESIZED:
      case TYPE_CAST:
      case POSTFIX_INCREMENT:
      case POSTFIX_DECREMENT:
      case PREFIX_INCREMENT:
      case PREFIX_DECREMENT:
      case UNARY_PLUS:
      case UNARY_MINUS:
      case BITWISE_COMPLEMENT:
      case LOGICAL_COMPLEMENT:
      case MULTIPLY:
      case DIVIDE:
      case REMAINDER:
      case PLUS:
      case MINUS:
      case LEFT_SHIFT:
      case RIGHT_SHIFT:
      case UNSIGNED_RIGHT_SHIFT:
      case LESS_THAN:
      case GREATER_THAN:
      case LESS_THAN_EQUAL:
      case GREATER_THAN_EQUAL:
      case EQUAL_TO:
      case NOT_EQUAL_TO:
      case AND:
      case XOR:
      case OR:
      case CONDITIONAL_AND:
      case CONDITIONAL_OR:
      case MULTIPLY_ASSIGNMENT:
      case DIVIDE_ASSIGNMENT:
      case REMAINDER_ASSIGNMENT:
      case PLUS_ASSIGNMENT:
      case MINUS_ASSIGNMENT:
      case LEFT_SHIFT_ASSIGNMENT:
      case RIGHT_SHIFT_ASSIGNMENT:
      case UNSIGNED_RIGHT_SHIFT_ASSIGNMENT:
      case AND_ASSIGNMENT:
      case XOR_ASSIGNMENT:
      case OR_ASSIGNMENT:
      case INT_LITERAL:
      case LONG_LITERAL:
      case FLOAT_LITERAL:
      case DOUBLE_LITERAL:
      case BOOLEAN_LITERAL:
      case CHAR_LITERAL:
      case STRING_LITERAL:
      case NULL_LITERAL:
        return true;
      default:
        return false;
    }
  }

  /**
   * Determines if the given kind is a wildcard.
   *
   * @param kind the kind to test
   * @return true if the given kind is a wildcard
   */
  private boolean isWildcard(Tree.Kind kind) {
    return kind == Tree.Kind.UNBOUNDED_WILDCARD
        || kind == Tree.Kind.EXTENDS_WILDCARD
        || kind == Tree.Kind.SUPER_WILDCARD;
  }

  // The following check is necessary because Oracle has decided that
  //   x instanceof Class<? extends Object>
  // will remain illegal even though it means the same thing as
  //   x instanceof Class<?>.
  private boolean isBoundableWildcard(List<Tree> actualPath, int i) {
    if (i <= 0) {
      return false;
    }
    Tree actualNode = actualPath.get(i);
    if (actualNode.getKind() == Tree.Kind.UNBOUNDED_WILDCARD) {
      // isWildcard(actualNode.getKind())
      // TODO: refactor GenericArrayLoc to use same code?
      Tree ancestor = actualPath.get(i - 1);
      if (ancestor instanceof InstanceOfTree) {
        TreeFinder.warn.debug(
            "WARNING: wildcard bounds not allowed "
                + "in 'instanceof' expression; skipping insertion%n");
        return false;
      } else if (i > 1 && ancestor instanceof ParameterizedTypeTree) {
        ancestor = actualPath.get(i - 2);
        if (ancestor instanceof ArrayTypeTree) {
          TreeFinder.warn.debug(
              "WARNING: wildcard bounds not allowed "
                  + "in 'instanceof' expression; skipping insertion%n");
          return false;
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public Kind getKind() {
    return Kind.AST_PATH;
  }

  @Override
  public String toString() {
    return "ASTPathCriterion: " + astPath;
  }
}
