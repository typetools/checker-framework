package org.checkerframework.checker.signedness;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.TreePath;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.interning.qual.InternedDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeSystemError;
import org.plumelib.util.IPair;

/**
 * This file contains code to special-case shifts whose result does not depend on the MSB of the
 * first argument, due to subsequent masking or casts.
 *
 * @checker_framework.manual #signedness-checker Signedness Checker
 */
public class SignednessShifts {

  /** Do not instantiate SignednessShifts. */
  private SignednessShifts() {
    throw new Error("Do not instantiate SignednessShifts");
  }

  /**
   * Returns true iff the given tree node is a mask operation (&amp; or |).
   *
   * @param tree a tree to test
   * @return true iff node is a mask operation (&amp; or |)
   */
  private static boolean isMask(Tree tree) {
    Tree.Kind kind = tree.getKind();

    return kind == Tree.Kind.AND || kind == Tree.Kind.OR;
  }

  // TODO: Return a TypeKind rather than a PrimitiveTypeTree?
  /**
   * Returns the type of a primitive cast, or null if the argument is not a cast to a primitive.
   *
   * @param tree a tree that might be a cast to a primitive
   * @return type of a primitive cast, or null if not a cast to a primitive
   */
  private static @Nullable PrimitiveTypeTree primitiveTypeCast(Tree tree) {
    if (!(tree instanceof TypeCastTree)) {
      return null;
    }

    TypeCastTree cast = (TypeCastTree) tree;
    Tree castType = cast.getType();

    Tree underlyingType;
    if (castType instanceof AnnotatedTypeTree) {
      underlyingType = ((AnnotatedTypeTree) castType).getUnderlyingType();
    } else {
      underlyingType = castType;
    }

    if (!(underlyingType instanceof PrimitiveTypeTree)) {
      return null;
    }

    return (PrimitiveTypeTree) underlyingType;
  }

  /**
   * Returns true iff the given tree is a literal.
   *
   * @param expr a tree to test
   * @return true iff expr is a literal
   */
  private static boolean isLiteral(ExpressionTree expr) {
    return expr instanceof LiteralTree;
  }

  /**
   * Returns the long value of an Integer or a Long.
   *
   * @param obj either an Integer or a Long
   * @return the long value of obj
   */
  private static long getLong(Object obj) {
    return ((Number) obj).longValue();
  }

  /**
   * Given a masking operation of the form {@code expr & maskLit} or {@code expr | maskLit}, return
   * true iff the masking operation results in the same output regardless of the value of the
   * shiftAmount most significant bits of expr. This is if the shiftAmount most significant bits of
   * mask are 0 for AND, and 1 for OR. For example, assuming that shiftAmount is 4, the following is
   * true about AND and OR masks:
   *
   * <p>{@code expr & 0x0[anything] == 0x0[something] ;}
   *
   * <p>{@code expr | 0xF[anything] == 0xF[something] ;}
   *
   * @param maskKind the kind of mask (AND or OR)
   * @param shiftAmountLit the LiteralTree whose value is shiftAmount
   * @param maskLit the LiteralTree whose value is mask
   * @param shiftedTypeKind the type of shift operation; int or long
   * @return true iff the shiftAmount most significant bits of mask are 0 for AND, and 1 for OR
   */
  private static boolean maskIgnoresMSB(
      Tree.Kind maskKind,
      LiteralTree shiftAmountLit,
      LiteralTree maskLit,
      TypeKind shiftedTypeKind) {
    long shiftAmount = getLong(shiftAmountLit.getValue());

    // Shift of zero is a nop
    if (shiftAmount == 0) {
      return true;
    }

    long mask = getLong(maskLit.getValue());
    // Shift the shiftAmount most significant bits to become the shiftAmount least significant
    // bits, zeroing out the rest.
    if (shiftedTypeKind == TypeKind.INT) {
      mask <<= 32;
    }
    mask >>>= (64 - shiftAmount);

    if (maskKind == Tree.Kind.AND) {
      // Check that the shiftAmount most significant bits of the mask were 0.
      return mask == 0;
    } else if (maskKind == Tree.Kind.OR) {
      // Check that the shiftAmount most significant bits of the mask were 1.
      return mask == (1 << shiftAmount) - 1;
    } else {
      throw new TypeSystemError("Invalid Masking Operation");
    }
  }

  /**
   * Given a casted right shift of the form {@code (type) (baseExpr >> shiftAmount)} or {@code
   * (type) (baseExpr >>> shiftAmount)}, return true iff the expression's value is the same
   * regardless of the type of right shift (signed or unsigned). This is true if the cast ignores
   * the shiftAmount most significant bits of the shift result -- that is, if the cast ignores all
   * the new bits that the right shift introduced on the left.
   *
   * <p>For example, the function returns true for
   *
   * <pre>{@code (short) (myInt >> 16)}</pre>
   *
   * and for
   *
   * <pre>{@code (short) (myInt >>> 16)}</pre>
   *
   * because these two expressions are guaranteed to have the same result.
   *
   * @param shiftTypeKind the kind of the type of the shift literal (BYTE, CHAR, SHORT, INT, or
   *     LONG)
   * @param castTypeKind the kind of the cast target type (BYTE, CHAR, SHORT, INT, or LONG)
   * @param shiftAmountLit the LiteralTree whose value is shiftAmount
   * @return true iff introduced bits are discarded
   */
  private static boolean castIgnoresMSB(
      TypeKind shiftTypeKind, TypeKind castTypeKind, LiteralTree shiftAmountLit) {

    // Determine number of bits in the shift type, note shifts upcast to int.
    // Also determine the shift amount as it is dependent on the shift type.
    long shiftBits;
    long shiftAmount;
    switch (shiftTypeKind) {
      case INT:
        shiftBits = 32;
        // When the LHS of the shift is an int, the 5 lower order bits of the RHS are used.
        shiftAmount = 0x1F & getLong(shiftAmountLit.getValue());
        break;
      case LONG:
        shiftBits = 64;
        // When the LHS of the shift is a long, the 6 lower order bits of the RHS are used.
        shiftAmount = 0x3F & getLong(shiftAmountLit.getValue());
        break;
      default:
        throw new TypeSystemError("Invalid shift type");
    }

    // Determine number of bits in the cast type
    long castBits;
    switch (castTypeKind) {
      case BYTE:
        castBits = 8;
        break;
      case CHAR:
        castBits = 8;
        break;
      case SHORT:
        castBits = 16;
        break;
      case INT:
        castBits = 32;
        break;
      case LONG:
        castBits = 64;
        break;
      default:
        throw new TypeSystemError("Invalid cast target");
    }

    long bitsDiscarded = shiftBits - castBits;

    return shiftAmount <= bitsDiscarded || shiftAmount == 0;
  }

  /**
   * Returns true if a right shift operation, {@code >>} or {@code >>>}, is masked with a masking
   * operation of the form {@code shiftExpr & maskLit} or {@code shiftExpr | maskLit} such that the
   * mask renders the shift signedness ({@code >>} vs {@code >>>}) irrelevant by destroying the bits
   * duplicated into the shift result. For example, the following pairs of right shifts on {@code
   * byte b} both produce the same results under any input, because of their masks:
   *
   * <p>{@code (b >> 4) & 0x0F == (b >>> 4) & 0x0F;}
   *
   * <p>{@code (b >> 4) | 0xF0 == (b >>> 4) | 0xF0;}
   *
   * @param shiftExpr a right shift expression: {@code expr1 >> expr2} or {@code expr1 >>> expr2}
   * @param path the path to {@code shiftExpr}
   * @return true iff the right shift is masked such that a signed or unsigned right shift has the
   *     same effect
   */
  /*package-private*/ static boolean isMaskedShiftEitherSignedness(
      BinaryTree shiftExpr, TreePath path) {
    IPair<Tree, Tree> enclosingPair = TreePathUtil.enclosingNonParen(path);
    // enclosing immediately contains shiftExpr or a parenthesized version of shiftExpr
    Tree enclosing = enclosingPair.first;
    // enclosingChild is a child of enclosing:  shiftExpr or a parenthesized version of it.
    @SuppressWarnings("interning:assignment") // comparing AST nodes
    @InternedDistinct Tree enclosingChild = enclosingPair.second;

    if (!isMask(enclosing)) {
      return false;
    }

    BinaryTree maskExpr = (BinaryTree) enclosing;
    ExpressionTree shiftAmountExpr = shiftExpr.getRightOperand();

    // Determine which child of maskExpr leads to shiftExpr. The other one is the mask.
    ExpressionTree mask =
        maskExpr.getRightOperand() == enclosingChild
            ? maskExpr.getLeftOperand()
            : maskExpr.getRightOperand();

    // Strip away the parentheses from the mask if any exist
    mask = TreeUtils.withoutParens(mask);

    if (!isLiteral(shiftAmountExpr) || !isLiteral(mask)) {
      return false;
    }

    LiteralTree shiftLit = (LiteralTree) shiftAmountExpr;
    LiteralTree maskLit = (LiteralTree) mask;

    return maskIgnoresMSB(
        maskExpr.getKind(), shiftLit, maskLit, TreeUtils.typeOf(shiftExpr).getKind());
  }

  /**
   * Returns true if a right shift operation, {@code >>} or {@code >>>}, is type casted such that
   * the cast renders the shift signedness ({@code >>} vs {@code >>>}) irrelevant by discarding the
   * bits duplicated into the shift result. For example, the following pair of right shifts on
   * {@code short s} both produce the same results under any input, because of type casting:
   *
   * <p>{@code (byte)(s >> 8) == (byte)(b >>> 8);}
   *
   * @param shiftExpr a right shift expression: {@code expr1 >> expr2} or {@code expr1 >>> expr2}
   * @param path the path to {@code shiftExpr}
   * @return true iff the right shift is type casted such that a signed or unsigned right shift has
   *     the same effect
   */
  /*package-private*/ static boolean isCastedShiftEitherSignedness(
      BinaryTree shiftExpr, TreePath path) {
    // enclosing immediately contains shiftExpr or a parenthesized version of shiftExpr
    Tree enclosing = TreePathUtil.enclosingNonParen(path).first;

    PrimitiveTypeTree castPrimitiveType = primitiveTypeCast(enclosing);
    if (castPrimitiveType == null) {
      return false;
    }
    TypeKind castTypeKind = castPrimitiveType.getPrimitiveTypeKind();

    // Determine the type of the shift result
    TypeKind shiftTypeKind = TreeUtils.typeOf(shiftExpr).getKind();

    // Determine shift literal
    ExpressionTree shiftAmountExpr = shiftExpr.getRightOperand();
    if (!isLiteral(shiftAmountExpr)) {
      return false;
    }
    LiteralTree shiftLit = (LiteralTree) shiftAmountExpr;

    boolean result = castIgnoresMSB(shiftTypeKind, castTypeKind, shiftLit);
    return result;
  }
}
