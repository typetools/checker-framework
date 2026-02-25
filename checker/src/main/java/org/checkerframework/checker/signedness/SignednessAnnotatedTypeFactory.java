package org.checkerframework.checker.signedness;

import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.CompoundAssignmentTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePath;
import java.io.Serializable;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
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
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeKindUtils;
import org.checkerframework.javacutil.TypesUtils;

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
  @SuppressWarnings("this-escape")
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
   * True when the AnnotatedTypeMirror currently being computed is the left-hand side of an
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
    if (tree instanceof TypeCastTree) {
      return;
    }
    TypeMirror javaType = type.getUnderlyingType();
    TypeKind javaTypeKind = javaType.getKind();
    if (tree instanceof VariableTree) {
      return;
    }
    if (!(javaTypeKind == TypeKind.BYTE
        || javaTypeKind == TypeKind.CHAR
        || javaTypeKind == TypeKind.SHORT
        || javaTypeKind == TypeKind.INT
        || javaTypeKind == TypeKind.LONG)) {
      return;
    }
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
              && (SignednessShifts.isMaskedShiftEitherSignedness(tree, path)
                  || SignednessShifts.isCastedShiftEitherSignedness(tree, path))) {
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
