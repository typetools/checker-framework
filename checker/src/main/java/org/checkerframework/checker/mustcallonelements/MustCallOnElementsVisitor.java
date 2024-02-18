package org.checkerframework.checker.mustcallonelements;

import com.sun.source.tree.ArrayAccessTree;
import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ForLoopTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.UnaryTree;
import com.sun.source.tree.VariableTree;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import org.checkerframework.checker.mustcallonelements.qual.OwningArray;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;

/**
 * The visitor for the Must Call Checker. This visitor is similar to BaseTypeVisitor, but overrides
 * methods that don't work well with the MustCall type hierarchy because it doesn't use the top type
 * as the default type.
 */
public class MustCallOnElementsVisitor
    extends BaseTypeVisitor<MustCallOnElementsAnnotatedTypeFactory> {

  /** True if -AnoLightweightOwnership was passed on the command line. */
  private final boolean noLightweightOwnership;

  /**
   * Creates a new MustCallVisitor.
   *
   * @param checker the type-checker associated with this visitor
   */
  public MustCallOnElementsVisitor(BaseTypeChecker checker) {
    super(checker);
    noLightweightOwnership = checker.hasOption(MustCallOnElementsChecker.NO_LIGHTWEIGHT_OWNERSHIP);
  }

  /**
   * Checks whether the loop either: - initializes entries of an @Owning array - calls a method on
   * entries of an @OwningArray array The pattern-match checks: - does the loop have a single
   * statement? - is the statement an assignment? - is the LHS an element of an @OwningArray? - is
   * the RHS a newly constructed Resource (of the form: new Resource();)? ...
   */
  @Override
  public Void visitForLoop(ForLoopTree tree, Void p) {
    BlockTree blockT = (BlockTree) tree.getStatement();
    // pattern match the initializer, condition and update
    if (blockT.getStatements().size() != 1 // ensure loop body has only one statement
        || !(blockT.getStatements().get(0) instanceof ExpressionStatementTree)
        || tree.getCondition().getKind() != Tree.Kind.LESS_THAN // ensure condition is: <
        || tree.getUpdate().size() != 1
        || tree.getInitializer().size() != 1) // ensure there's only one loop variable
    return super.visitForLoop(tree, p);
    if (!(blockT.getStatements().get(0) instanceof ExpressionStatementTree))
      return super.visitForLoop(tree, p);

    // pattern-match the method body
    ExpressionTree stmtTree =
        ((ExpressionStatementTree) blockT.getStatements().get(0)).getExpression();
    if (stmtTree instanceof AssignmentTree) {
      // pattern match opening an obligation
      AssignmentTree assgn = (AssignmentTree) stmtTree;

      // verifiy lhs is an index of @OwningArray
      ExpressionTree lhs = assgn.getVariable();
      Element lhsElt = TreeUtils.elementFromTree(lhs);
      boolean lhsIsOwningArray = atypeFactory.getDeclAnnotation(lhsElt, OwningArray.class) != null;
      // ensure lhs contains @OwningArray and is an array access
      if (!lhsIsOwningArray || lhs.getKind() != Tree.Kind.ARRAY_ACCESS)
        return super.visitForLoop(tree, p);
      ArrayAccessTree arrayAccT = (ArrayAccessTree) lhs;
      // ensure index is same as the one initialized in the loop header
      StatementTree init = tree.getInitializer().get(0);
      ExpressionTree idx = arrayAccT.getIndex();
      if (!(init instanceof VariableTree)
          || !(idx instanceof IdentifierTree)
          || !((IdentifierTree) idx).getName().equals(((VariableTree) init).getName()))
        return super.visitForLoop(tree, p);
      // ensure indexed array is the same as the one we took the length of in loop condition
      Name arrayNameInBody = arrayNameFromExpression(arrayAccT.getExpression());
      Name arrayNameInHeader =
          verifyAllElementsAreCalledOn(
              (StatementTree) tree.getInitializer().get(0),
              (BinaryTree) tree.getCondition(),
              (ExpressionStatementTree) tree.getUpdate().get(0));
      if (arrayNameInHeader == null) {
        // header is not as expected, but loop body correctly initializes a resource
        checker.reportWarning(
            tree, "patternmatch.unsuccessful");
        return super.visitForLoop(tree, p);
      }
      if (arrayNameInHeader != arrayNameInBody) {
        // array name in header and footer not equal

        return super.visitForLoop(tree, p);
      }
    }
    return super.visitForLoop(tree, p);

    // ExpressionTree rhs = assgn.getExpression();
    // if (rhs instanceof NewClassTree) {
    //   ExpressionTree className = ((NewClassTree) rhs).getIdentifier();
    //   Element lhsElt = TreeUtils.elementFromTree(lhs);
    //   Element rhsElt = TreeUtils.elementFromTree(className);
    //   if (lhs instanceof ArrayAccessTree) {
    //     boolean lhsIsOwningArray =
    //         atypeFactory.getDeclAnnotation(lhsElt, OwningArray.class) != null;
    //     if (lhsIsOwningArray) {
    //       ExpressionTree condition = tree.getCondition();
    //       if (condition.getKind() == Tree.Kind.LESS_THAN) {
    //         MustCallAnnotatedTypeFactory mcTypeFactory = atypeFactory;
    //         AnnotationMirror mcAnno =
    //             mcTypeFactory.getAnnotatedType(rhsElt).getPrimaryAnnotation(MustCall.class);
    //         List<String> mcValues =
    //             AnnotationUtils.getElementValueArray(
    //                 mcAnno, mcTypeFactory.getMustCallValueElement(), String.class);
    //         // System.out.println(
    //         //                    "Annotation type in the pattern matcher is " +
    //         //                    mcValues.toString());
    //         if (mcValues != null) {
    //           MustCallAnnotatedTypeFactory.createArrayObligationForAssignment(assgn);
    //           MustCallAnnotatedTypeFactory.createArrayObligationForLessThan(condition, mcValues);
    //           MustCallAnnotatedTypeFactory.putArrayAffectedByLoopWithThisCondition(
    //               condition, ((ArrayAccessTree)lhs).getExpression());
    //         }
    //       }
    //     }
    //   }
    // }
    // } else if (stmtTree instanceof MemberSelectTree) {
    //   // pattern match a closing of a method
    //   ExpressionTree lhs = ((MemberSelectTree) exprT).getExpression();
    //   Element lhsElt = TreeUtils.elementFromTree(lhs);
    //   boolean lhsIsOwningArray =
    //       atypeFactory.getDeclAnnotation(lhsElt, OwningArray.class) != null;
    //   if (lhsIsOwningArray) {
    //     MustCallAnnotatedTypeFactory.fulfillArrayObligationForMethodAccess(stmtTree);
    //   }
  }

  /**
   * Decides for a for-loop header whether the loop iterates over all elements of some array based
   * on a pattern-match.
   *
   * @param init the initializer of the loop
   * @param condition the loop condition
   * @param update the loop update
   * @return Name of the array the loop iterates over all elements of, or null if the pattern match
   *     fails
   */
  protected Name verifyAllElementsAreCalledOn(
      StatementTree init, BinaryTree condition, ExpressionStatementTree update) {
    Tree.Kind updateKind = update.getExpression().getKind();
    if (updateKind == Tree.Kind.PREFIX_INCREMENT || updateKind == Tree.Kind.POSTFIX_INCREMENT) {
      UnaryTree inc = (UnaryTree) update.getExpression();
      // verify update is of form i++ or ++i and init is variable initializer
      if (!(init instanceof VariableTree) || !(inc.getExpression() instanceof IdentifierTree))
        return null;
      VariableTree initVar = (VariableTree) init;
      // verify that intializer is i=0
      if (!(initVar.getInitializer() instanceof LiteralTree)
          || !((LiteralTree) initVar.getInitializer()).getValue().equals(0)) {
        return null;
      }
      // verify that condition is of the form: i<expr.identifier
      if (!(condition.getRightOperand() instanceof MemberSelectTree)
          || !(condition.getLeftOperand() instanceof IdentifierTree)) return null;
      MemberSelectTree lengthAccess = (MemberSelectTree) condition.getRightOperand();
      Name arrayName = arrayNameFromExpression(lengthAccess.getExpression());
      if (initVar.getName()
              == ((IdentifierTree) condition.getLeftOperand()).getName() // i=0 and i<n are same "i"
          && initVar.getName()
              == ((IdentifierTree) inc.getExpression()).getName() // i=0 and i++ are same "i"
          && lengthAccess
              .getIdentifier()
              .toString()
              .contentEquals("length")) { // condition is i<arr.length
        return arrayName;
      } else {
        return null;
      }
    }
    return null;
  }

  /**
   * Get array name from an ExpressionTree expected to evaluate to an array
   *
   * @param arrayExpr ExpressionTree allegedly containing an array
   * @return Name of the array the expression evaluates to or null if it doesn't
   */
  protected Name arrayNameFromExpression(ExpressionTree arrayExpr) {
    if (arrayExpr.getKind() == Tree.Kind.IDENTIFIER) {
      return ((IdentifierTree) arrayExpr).getName();
    }
    return null;
  }
}
