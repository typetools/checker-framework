package org.checkerframework.checker.signedness;

import com.sun.source.tree.AnnotatedTypeTree;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.util.TreePath;
import java.io.Serializable;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.interning.qual.InternedDistinct;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signedness.qual.PolySigned;
import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.SignedPositive;
import org.checkerframework.checker.signedness.qual.SignednessBottom;
import org.checkerframework.checker.signedness.qual.SignednessGlb;
import org.checkerframework.checker.signedness.qual.Unsigned;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.value.ValueAnnotatedTypeFactory;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.common.value.ValueCheckerUtils;
import org.checkerframework.common.value.qual.IntRangeFromNonNegative;
import org.checkerframework.common.value.qual.IntRangeFromPositive;
import org.checkerframework.common.value.util.Range;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.poly.DefaultQualifierPolymorphism;
import org.checkerframework.framework.type.poly.QualifierPolymorphism;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.PropagationTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreePathUtil;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeKindUtils;
import org.checkerframework.javacutil.TypeSystemError;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.IPair;

/**
 * The type factory for the Signedness Checker.
 *
 * @checker_framework.manual #signedness-checker Signedness Checker
 */
public class SignednessAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /** The @Signed annotation. */
  protected final AnnotationMirror SIGNED = AnnotationBuilder.fromClass(elements, Signed.class);

  /** The @Unsigned annotation. */
  private final AnnotationMirror UNSIGNED = AnnotationBuilder.fromClass(elements, Unsigned.class);

  /** The @SignednessGlb annotation. Do not use @SignedPositive; use this instead. */
  private final AnnotationMirror SIGNEDNESS_GLB =
      AnnotationBuilder.fromClass(elements, SignednessGlb.class);

  /** The @SignedPositive annotation. */
  protected final AnnotationMirror SIGNED_POSITIVE =
      AnnotationBuilder.fromClass(elements, SignedPositive.class);

  /** The @SignednessBottom annotation. */
  protected final AnnotationMirror SIGNEDNESS_BOTTOM =
      AnnotationBuilder.fromClass(elements, SignednessBottom.class);

  /** The @PolySigned annotation. */
  protected final AnnotationMirror POLY_SIGNED =
      AnnotationBuilder.fromClass(elements, PolySigned.class);

  /** The @NonNegative annotation of the Index Checker, as represented by the Value Checker. */
  private final AnnotationMirror INT_RANGE_FROM_NON_NEGATIVE =
      AnnotationBuilder.fromClass(elements, IntRangeFromNonNegative.class);

  /** The @Positive annotation of the Index Checker, as represented by the Value Checker. */
  private final AnnotationMirror INT_RANGE_FROM_POSITIVE =
      AnnotationBuilder.fromClass(elements, IntRangeFromPositive.class);

  /** The Serializable type mirror. */
  private final TypeMirror serializableTM =
      elements.getTypeElement(Serializable.class.getCanonicalName()).asType();

  /** The Comparable type mirror. */
  private final TypeMirror comparableTM =
      elements.getTypeElement(Comparable.class.getCanonicalName()).asType();

  /** The Number type mirror. */
  private final TypeMirror numberTM =
      elements.getTypeElement(Number.class.getCanonicalName()).asType();

  /** A set containing just {@code @Signed}. */
  private final AnnotationMirrorSet SIGNED_SINGLETON = new AnnotationMirrorSet(SIGNED);

  /** A set containing just {@code @Unsigned}. */
  private final AnnotationMirrorSet UNSIGNED_SINGLETON = new AnnotationMirrorSet(UNSIGNED);

  /**
   * Create a SignednessAnnotatedTypeFactory.
   *
   * @param checker the type-checker associated with this type factory
   */
  public SignednessAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);

    addAliasedTypeAnnotation("jdk.jfr.Unsigned", UNSIGNED);

    postInit();
  }

  @Override
  protected void addComputedTypeAnnotations(Tree tree, AnnotatedTypeMirror type, boolean iUseFlow) {
    Tree.Kind treeKind = tree.getKind();
    if (treeKind == Tree.Kind.INT_LITERAL) {
      int literalValue = (int) ((LiteralTree) tree).getValue();
      if (literalValue >= 0) {
        type.replaceAnnotation(SIGNED_POSITIVE);
      } else {
        type.replaceAnnotation(SIGNEDNESS_GLB);
      }
    } else if (treeKind == Tree.Kind.LONG_LITERAL) {
      long literalValue = (long) ((LiteralTree) tree).getValue();
      if (literalValue >= 0) {
        type.replaceAnnotation(SIGNED_POSITIVE);
      } else {
        type.replaceAnnotation(SIGNEDNESS_GLB);
      }
    } else if (!computingAnnotatedTypeMirrorOfLHS) {
      addSignedPositiveAnnotation(tree, type);
    }

    super.addComputedTypeAnnotations(tree, type, iUseFlow);
  }

  /**
   * True when the AnnotatedTypeMirror currently being computed is the left hand side of an
   * assignment or pseudo-assignment.
   *
   * @see #addComputedTypeAnnotations(Tree, AnnotatedTypeMirror, boolean)
   * @see #getAnnotatedTypeLhs(Tree)
   */
  private boolean computingAnnotatedTypeMirrorOfLHS = false;

  @Override
  public AnnotatedTypeMirror getAnnotatedTypeLhs(Tree lhsTree) {
    boolean oldComputingAnnotatedTypeMirrorOfLHS = computingAnnotatedTypeMirrorOfLHS;
    computingAnnotatedTypeMirrorOfLHS = true;
    AnnotatedTypeMirror result = super.getAnnotatedTypeLhs(lhsTree);
    computingAnnotatedTypeMirrorOfLHS = oldComputingAnnotatedTypeMirrorOfLHS;
    return result;
  }

  /**
   * Refines an integer expression to @SignedPositive if its value is within the signed positive
   * range (i.e. its MSB is zero). Does not refine the type of cast expressions.
   *
   * @param tree an AST node, whose type may be refined
   * @param type the type of the tree
   */
  private void addSignedPositiveAnnotation(Tree tree, AnnotatedTypeMirror type) {
    if (tree.getKind() == Tree.Kind.TYPE_CAST) {
      return;
    }
    TypeMirror javaType = type.getUnderlyingType();
    TypeKind javaTypeKind = javaType.getKind();
    if (tree.getKind() != Tree.Kind.VARIABLE) {
      if (javaTypeKind == TypeKind.BYTE
          || javaTypeKind == TypeKind.CHAR
          || javaTypeKind == TypeKind.SHORT
          || javaTypeKind == TypeKind.INT
          || javaTypeKind == TypeKind.LONG) {
        ValueAnnotatedTypeFactory valueFactory = getTypeFactoryOfSubchecker(ValueChecker.class);
        AnnotatedTypeMirror valueATM = valueFactory.getAnnotatedType(tree);
        // These annotations are trusted rather than checked.  Maybe have an option to
        // disable using them?
        if ((valueATM.hasPrimaryAnnotation(INT_RANGE_FROM_NON_NEGATIVE)
                || valueATM.hasPrimaryAnnotation(INT_RANGE_FROM_POSITIVE))
            && type.hasPrimaryAnnotation(SIGNED)) {
          type.replaceAnnotation(SIGNED_POSITIVE);
        } else {
          Range treeRange = ValueCheckerUtils.getPossibleValues(valueATM, valueFactory);

          if (treeRange != null) {
            switch (javaType.getKind()) {
              case BYTE:
              case CHAR:
                if (treeRange.isWithin(0, Byte.MAX_VALUE)) {
                  type.replaceAnnotation(SIGNED_POSITIVE);
                }
                break;
              case SHORT:
                if (treeRange.isWithin(0, Short.MAX_VALUE)) {
                  type.replaceAnnotation(SIGNED_POSITIVE);
                }
                break;
              case INT:
                if (treeRange.isWithin(0, Integer.MAX_VALUE)) {
                  type.replaceAnnotation(SIGNED_POSITIVE);
                }
                break;
              case LONG:
                if (treeRange.isWithin(0, Long.MAX_VALUE)) {
                  type.replaceAnnotation(SIGNED_POSITIVE);
                }
                break;
              default:
                // Nothing
            }
          }
        }
      }
    }
  }

  @Override
  public AnnotationMirrorSet getWidenedAnnotations(
      AnnotationMirrorSet annos, TypeKind typeKind, TypeKind widenedTypeKind) {
    assert annos.size() == 1;

    AnnotationMirrorSet result = new AnnotationMirrorSet();
    if (TypeKindUtils.isFloatingPoint(widenedTypeKind)) {
      result.add(SIGNED);
      return result;
    }
    if (widenedTypeKind == TypeKind.CHAR) {
      result.add(UNSIGNED);
      return result;
    }
    if ((widenedTypeKind == TypeKind.INT || widenedTypeKind == TypeKind.LONG)
        && typeKind == TypeKind.CHAR) {
      result.add(SIGNED_POSITIVE);
      return result;
    }
    return annos;
  }

  @Override
  public AnnotationMirrorSet getNarrowedAnnotations(
      AnnotationMirrorSet annos, TypeKind typeKind, TypeKind narrowedTypeKind) {
    assert annos.size() == 1;

    AnnotationMirrorSet result = new AnnotationMirrorSet();

    if (narrowedTypeKind == TypeKind.CHAR) {
      result.add(SIGNED);
      return result;
    }

    return annos;
  }

  @Override
  protected TreeAnnotator createTreeAnnotator() {
    return new ListTreeAnnotator(new SignednessTreeAnnotator(this), super.createTreeAnnotator());
  }

  @Override
  public AnnotationMirrorSet annotationsForIrrelevantJavaType(TypeMirror tm) {
    if (TypesUtils.isCharOrCharacter(tm)) {
      return UNSIGNED_SINGLETON;
    } else {
      return SIGNED_SINGLETON;
    }
  }

  /**
   * This TreeAnnotator ensures that:
   *
   * <ul>
   *   <li>boolean expressions are not given Unsigned or Signed annotations by {@link
   *       PropagationTreeAnnotator},
   *   <li>shift results take on the type of their left operand,
   *   <li>the types of identifiers are refined based on the results of the Value Checker.
   *   <li>casts take types related to widening
   * </ul>
   */
  private class SignednessTreeAnnotator extends TreeAnnotator {

    public SignednessTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
      super(atypeFactory);
    }

    @Override
    public Void visitBinary(BinaryTree tree, AnnotatedTypeMirror type) {
      switch (tree.getKind()) {
        case LEFT_SHIFT:
        case RIGHT_SHIFT:
        case UNSIGNED_RIGHT_SHIFT:
          TreePath path = getPath(tree);
          if (path != null
              && (isMaskedShiftEitherSignedness(tree, path)
                  || isCastedShiftEitherSignedness(tree, path))) {
            type.replaceAnnotation(SIGNED_POSITIVE);
          } else {
            AnnotatedTypeMirror lht = getAnnotatedType(tree.getLeftOperand());
            type.replaceAnnotations(lht.getPrimaryAnnotations());
          }
          break;
        default:
          // Do nothing
      }
      return null;
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree tree, AnnotatedTypeMirror type) {
      if (TreeUtils.isStringCompoundConcatenation(tree)) {
        if (TypesUtils.isCharOrCharacter(TreeUtils.typeOf(tree.getExpression()))) {
          type.replaceAnnotation(SIGNED);
        }
      }
      return null;
    }

    @Override
    public Void visitTypeCast(TypeCastTree tree, AnnotatedTypeMirror type) {
      // Don't change the annotation on a cast with an explicit annotation.
      if (TypesUtils.isCharOrCharacter(type.getUnderlyingType())) {
        type.replaceAnnotation(UNSIGNED);
      } else if (type.getPrimaryAnnotations().isEmpty() && !maybeIntegral(type)) {
        AnnotatedTypeMirror exprType = atypeFactory.getAnnotatedType(tree.getExpression());
        if ((type.getKind() != TypeKind.TYPEVAR || exprType.getKind() != TypeKind.TYPEVAR)
            && !AnnotationUtils.containsSame(exprType.getEffectiveAnnotations(), UNSIGNED)) {
          type.addAnnotation(SIGNED);
        }
      }
      log("SATF.visitTypeCast(%s, ...) final: %s%n", tree, type);
      log("SATF: treeAnnotator=%s%n", treeAnnotator);
      return null;
    }
  }

  /**
   * Returns true if {@code type}'s underlying type might be integral: it is a number, char, or a
   * supertype of them.
   *
   * @param type a type
   * @return true if {@code type}'s underlying type might be integral
   */
  public boolean maybeIntegral(AnnotatedTypeMirror type) {

    TypeKind kind = type.getKind();

    switch (kind) {
      case BOOLEAN:
        return false;
      case BYTE:
      case SHORT:
      case INT:
      case LONG:
      case CHAR:
        return true;
      case FLOAT:
      case DOUBLE:
        return false;

      case DECLARED:
      case TYPEVAR:
      case WILDCARD:
        TypeMirror erasedType = types.erasure(type.getUnderlyingType());
        return (TypesUtils.isBoxedPrimitive(erasedType)
            || TypesUtils.isObject(erasedType)
            || TypesUtils.isErasedSubtype(numberTM, erasedType, types)
            || TypesUtils.isErasedSubtype(serializableTM, erasedType, types)
            || TypesUtils.isErasedSubtype(comparableTM, erasedType, types));

      default:
        return false;
    }
  }

  @Override
  protected void adaptGetClassReturnTypeToReceiver(
      AnnotatedExecutableType getClassType, AnnotatedTypeMirror receiverType, ExpressionTree tree) {
    super.adaptGetClassReturnTypeToReceiver(getClassType, receiverType, tree);
    // Make the captured wildcard always @Signed, regardless of the declared type.
    AnnotatedDeclaredType returnAdt = (AnnotatedDeclaredType) getClassType.getReturnType();
    List<AnnotatedTypeMirror> typeArgs = returnAdt.getTypeArguments();
    AnnotatedTypeVariable classWildcardArg = (AnnotatedTypeVariable) typeArgs.get(0);
    classWildcardArg.getUpperBound().replaceAnnotation(SIGNED);
  }

  @Override
  protected void addAnnotationsFromDefaultForType(
      @Nullable Element element, AnnotatedTypeMirror type) {
    TypeMirror underlying = type.getUnderlyingType();
    if (TypesUtils.isFloatingPrimitive(underlying)
        || TypesUtils.isBoxedFloating(underlying)
        || TypesUtils.isCharOrCharacter(underlying)) {
      // Floats are always signed and chars are always unsigned.
      super.addAnnotationsFromDefaultForType(null, type);
    } else {
      super.addAnnotationsFromDefaultForType(element, type);
    }
  }

  // The remainder of this file contains code to special-case shifts whose result does not depend
  // on the MSB of the first argument, due to subsequent masking or casts.

  /**
   * Returns true iff the given tree node is a mask operation (&amp; or |).
   *
   * @param tree a tree to test
   * @return true iff node is a mask operation (&amp; or |)
   */
  private boolean isMask(Tree tree) {
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
  private @Nullable PrimitiveTypeTree primitiveTypeCast(Tree tree) {
    if (tree.getKind() != Tree.Kind.TYPE_CAST) {
      return null;
    }

    TypeCastTree cast = (TypeCastTree) tree;
    Tree castType = cast.getType();

    Tree underlyingType;
    if (castType.getKind() == Tree.Kind.ANNOTATED_TYPE) {
      underlyingType = ((AnnotatedTypeTree) castType).getUnderlyingType();
    } else {
      underlyingType = castType;
    }

    if (underlyingType.getKind() != Tree.Kind.PRIMITIVE_TYPE) {
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
  private boolean isLiteral(ExpressionTree expr) {
    return expr instanceof LiteralTree;
  }

  /**
   * Returns the long value of an Integer or a Long
   *
   * @param obj either an Integer or a Long
   * @return the long value of obj
   */
  private long getLong(Object obj) {
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
  private boolean maskIgnoresMSB(
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
  private boolean castIgnoresMSB(
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
   * Determines if a right shift operation, {@code >>} or {@code >>>}, is masked with a masking
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
  /*package-private*/ boolean isMaskedShiftEitherSignedness(BinaryTree shiftExpr, TreePath path) {
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
   * Determines if a right shift operation, {@code >>} or {@code >>>}, is type casted such that the
   * cast renders the shift signedness ({@code >>} vs {@code >>>}) irrelevant by discarding the bits
   * duplicated into the shift result. For example, the following pair of right shifts on {@code
   * short s} both produce the same results under any input, because of type casting:
   *
   * <p>{@code (byte)(s >> 8) == (byte)(b >>> 8);}
   *
   * @param shiftExpr a right shift expression: {@code expr1 >> expr2} or {@code expr1 >>> expr2}
   * @param path the path to {@code shiftExpr}
   * @return true iff the right shift is type casted such that a signed or unsigned right shift has
   *     the same effect
   */
  /*package-private*/ boolean isCastedShiftEitherSignedness(BinaryTree shiftExpr, TreePath path) {
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

  // End of special-case code for shifts that do not depend on the MSB of the first argument.

  /**
   * Requires that, when two formal parameter types are annotated with {@code @PolySigned}, the two
   * arguments must have the same signedness type annotation.
   */
  // Not static because it references SIGNEDNESS_BOTTOM.
  private class SignednessQualifierPolymorphism extends DefaultQualifierPolymorphism {
    /**
     * Creates a {@link SignednessQualifierPolymorphism}.
     *
     * @param env the processing environment
     * @param factory the factory for the current checker
     */
    public SignednessQualifierPolymorphism(
        ProcessingEnvironment env, AnnotatedTypeFactory factory) {
      super(env, factory);
    }

    /**
     * Combines the two annotations. If they are comparable, return the lub. If they are
     * incomparable, return @SignednessBottom.
     *
     * @param polyQual the polymorphic qualifier
     * @param a1 the first annotation to compare
     * @param a2 the second annotation to compare
     * @return the lub, unless the annotations are incomparable
     */
    @Override
    protected AnnotationMirror combine(
        AnnotationMirror polyQual, AnnotationMirror a1, AnnotationMirror a2) {
      if (a1 == null) {
        return a2;
      } else if (a2 == null) {
        return a1;
      } else if (AnnotationUtils.areSame(a1, a2)) {
        return a1;
      } else if (qualHierarchy.isSubtypeQualifiersOnly(a1, a2)) {
        return a2;
      } else if (qualHierarchy.isSubtypeQualifiersOnly(a2, a1)) {
        return a1;
      } else
        // The two annotations are incomparable
        // TODO: Issue a warning at the proper code location.
        // TODO: Returning bottom leads to obscure error messages.  It would probably be
        // better to issue a warning in this method, then return lub as usual.
        return SIGNEDNESS_BOTTOM;
    }
  }

  @Override
  protected QualifierPolymorphism createQualifierPolymorphism() {
    return new SignednessQualifierPolymorphism(processingEnv, this);
  }
}
