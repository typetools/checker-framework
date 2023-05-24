package org.checkerframework.common.value;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeCastTree;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.checker.formatter.qual.FormatMethod;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.value.qual.IntRangeFromGTENegativeOne;
import org.checkerframework.common.value.qual.IntRangeFromNonNegative;
import org.checkerframework.common.value.qual.IntRangeFromPositive;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.StaticallyExecutable;
import org.checkerframework.common.value.util.NumberUtils;
import org.checkerframework.common.value.util.Range;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.visitor.AnnotatedTypeScanner;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.ElementUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeKindUtils;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.CollectionsPlume;

/** Visitor for the Constant Value type system. */
public class ValueVisitor extends BaseTypeVisitor<ValueAnnotatedTypeFactory> {

  public ValueVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  /**
   * ValueVisitor overrides this method so that it does not have to check variables annotated with
   * the {@link IntRangeFromPositive} annotation, the {@link IntRangeFromNonNegative} annotation, or
   * the {@link IntRangeFromGTENegativeOne} annotation. This annotation is only introduced by the
   * Index Checker's lower bound annotations. It is safe to defer checking of these values to the
   * Index Checker because this is only introduced for explicitly-written {@code
   * org.checkerframework.checker.index.qual.Positive}, explicitly-written {@code
   * org.checkerframework.checker.index.qual.NonNegative}, and explicitly-written {@code
   * org.checkerframework.checker.index.qual.GTENegativeOne} annotations, which must be checked by
   * the Lower Bound Checker.
   *
   * @param varType the annotated type of the lvalue (usually a variable)
   * @param valueExp the AST node for the rvalue (the new value)
   * @param errorKey the error message key to use if the check fails
   * @param extraArgs arguments to the error message key, before "found" and "expected" types
   * @return true if the check succeeds, false if an error message was issued
   */
  @Override
  protected boolean commonAssignmentCheck(
      AnnotatedTypeMirror varType,
      ExpressionTree valueExp,
      @CompilerMessageKey String errorKey,
      Object... extraArgs) {

    replaceSpecialIntRangeAnnotations(varType);
    return super.commonAssignmentCheck(varType, valueExp, errorKey, extraArgs);
  }

  @Override
  @FormatMethod
  protected boolean commonAssignmentCheck(
      AnnotatedTypeMirror varType,
      AnnotatedTypeMirror valueType,
      Tree valueTree,
      @CompilerMessageKey String errorKey,
      Object... extraArgs) {

    replaceSpecialIntRangeAnnotations(varType);

    if (valueType.getKind() == TypeKind.CHAR
        && valueType.hasAnnotation(getTypeFactory().UNKNOWNVAL)) {
      valueType.addAnnotation(getTypeFactory().createIntRangeAnnotation(Range.CHAR_EVERYTHING));
    }

    return super.commonAssignmentCheck(varType, valueType, valueTree, errorKey, extraArgs);
  }

  /**
   * Return types for methods that are annotated with {@code @IntRangeFromX} annotations need to be
   * replaced with {@code @UnknownVal}. See the documentation on {@link
   * #commonAssignmentCheck(AnnotatedTypeMirror, ExpressionTree, String, Object[])
   * commonAssignmentCheck}.
   *
   * <p>A separate override is necessary because checkOverride doesn't actually use the
   * commonAssignmentCheck.
   */
  @Override
  protected boolean checkOverride(
      MethodTree overriderTree,
      AnnotatedTypeMirror.AnnotatedExecutableType overrider,
      AnnotatedTypeMirror.AnnotatedDeclaredType overridingType,
      AnnotatedTypeMirror.AnnotatedExecutableType overridden,
      AnnotatedTypeMirror.AnnotatedDeclaredType overriddenType) {

    replaceSpecialIntRangeAnnotations(overrider);
    replaceSpecialIntRangeAnnotations(overridden);

    return super.checkOverride(
        overriderTree, overrider, overridingType, overridden, overriddenType);
  }

  /**
   * Replaces any {@code IntRangeFromX} annotations with {@code @UnknownVal}. This is used to
   * prevent these annotations from being required on the left hand side of assignments.
   *
   * @param varType an annotated type mirror that may contain IntRangeFromX annotations, which will
   *     be used on the lhs of an assignment or pseudo-assignment
   */
  private void replaceSpecialIntRangeAnnotations(AnnotatedTypeMirror varType) {
    AnnotatedTypeScanner<Void, Void> replaceSpecialIntRangeAnnotations =
        new AnnotatedTypeScanner<Void, Void>() {
          @Override
          protected Void scan(AnnotatedTypeMirror type, Void p) {
            if (type.hasAnnotation(IntRangeFromPositive.class)
                || type.hasAnnotation(IntRangeFromNonNegative.class)
                || type.hasAnnotation(IntRangeFromGTENegativeOne.class)) {
              type.replaceAnnotation(atypeFactory.UNKNOWNVAL);
            }
            return super.scan(type, p);
          }

          @Override
          public Void visitDeclared(AnnotatedDeclaredType type, Void p) {
            // Don't call super so that the type arguments are not visited.
            if (type.getEnclosingType() != null) {
              scan(type.getEnclosingType(), p);
            }

            return null;
          }
        };
    replaceSpecialIntRangeAnnotations.visit(varType);
  }

  @Override
  protected ValueAnnotatedTypeFactory createTypeFactory() {
    return new ValueAnnotatedTypeFactory(checker);
  }

  /**
   * Warns about malformed constant-value annotations.
   *
   * <p>Issues an error if any @IntRange annotation has its 'from' value greater than 'to' value.
   *
   * <p>Issues an error if any constant-value annotation has no arguments.
   *
   * <p>Issues a warning if any constant-value annotation has &gt; MAX_VALUES arguments.
   *
   * <p>Issues a warning if any @ArrayLen/@ArrayLenRange annotations contain a negative array
   * length.
   *
   * <p>Issues a warning if any {@literal @}MatchesRegex or {@literal @}DoesNotMatchRegex annotation
   * contains an invalid regular expression.
   */
  /* Implementation note: the ValueTypeAnnotator replaces such invalid annotations with valid ones.
   * Therefore, the usual validation in #validateType cannot perform this validation.
   * These warnings cannot be issued in the ValueAnnotatedTypeFactory, because the conversions
   * might happen multiple times.
   * On the other hand, not all validations can happen here, because only the annotations are
   * available, not the full types.
   * Therefore, some validation is still done in #validateType below.
   */
  @Override
  public Void visitAnnotation(AnnotationTree tree, Void p) {
    List<? extends ExpressionTree> args = tree.getArguments();

    if (args.isEmpty()) {
      // Nothing to do if there are no annotation arguments.
      return super.visitAnnotation(tree, p);
    }

    AnnotationMirror anno = TreeUtils.annotationFromAnnotationTree(tree);
    switch (AnnotationUtils.annotationName(anno)) {
      case ValueAnnotatedTypeFactory.INTRANGE_NAME:
        // If there are 2 arguments, issue an error if from.greater.than.to.
        // If there are fewer than 2 arguments, we needn't worry about this problem because
        // the other argument will be defaulted to Long.MIN_VALUE or Long.MAX_VALUE
        // accordingly.
        if (args.size() == 2) {
          long from = getTypeFactory().getIntRangeFromValue(anno);
          long to = getTypeFactory().getIntRangeToValue(anno);
          if (from > to) {
            checker.reportError(tree, "from.greater.than.to");
            return null;
          }
        }
        break;
      case ValueAnnotatedTypeFactory.ARRAYLEN_NAME:
      case ValueAnnotatedTypeFactory.BOOLVAL_NAME:
      case ValueAnnotatedTypeFactory.DOUBLEVAL_NAME:
      case ValueAnnotatedTypeFactory.INTVAL_NAME:
      case ValueAnnotatedTypeFactory.STRINGVAL_NAME:
        @SuppressWarnings("deprecation") // concrete annotation class is not known
        List<Object> values =
            AnnotationUtils.getElementValueArray(anno, "value", Object.class, false);

        if (values.isEmpty()) {
          checker.reportWarning(tree, "no.values.given");
          return null;
        } else if (values.size() > ValueAnnotatedTypeFactory.MAX_VALUES) {
          checker.reportWarning(
              tree,
              (AnnotationUtils.areSameByName(anno, ValueAnnotatedTypeFactory.INTVAL_NAME)
                  ? "too.many.values.given.int"
                  : "too.many.values.given"),
              ValueAnnotatedTypeFactory.MAX_VALUES);
          return null;
        } else if (AnnotationUtils.areSameByName(anno, ValueAnnotatedTypeFactory.ARRAYLEN_NAME)) {
          List<Integer> arrayLens = getTypeFactory().getArrayLength(anno);
          if (Collections.min(arrayLens) < 0) {
            checker.reportWarning(tree, "negative.arraylen", Collections.min(arrayLens));
            return null;
          }
        }
        break;
      case ValueAnnotatedTypeFactory.ARRAYLENRANGE_NAME:
        long from = getTypeFactory().getArrayLenRangeFromValue(anno);
        long to = getTypeFactory().getArrayLenRangeToValue(anno);
        if (from > to) {
          checker.reportError(tree, "from.greater.than.to");
          return null;
        } else if (from < 0) {
          checker.reportWarning(tree, "negative.arraylen", from);
          return null;
        }
        break;
      case ValueAnnotatedTypeFactory.MATCHES_REGEX_NAME:
        List<String> matchesRegexes =
            AnnotationUtils.getElementValueArray(
                anno, atypeFactory.matchesRegexValueElement, String.class);
        for (String regex : matchesRegexes) {
          try {
            Pattern.compile(regex);
          } catch (PatternSyntaxException pse) {
            checker.reportWarning(tree, "invalid.matches.regex", pse.getMessage());
          }
        }
        break;
      case ValueAnnotatedTypeFactory.DOES_NOT_MATCH_REGEX_NAME:
        List<String> doesNotMatchRegexes =
            AnnotationUtils.getElementValueArray(
                anno, atypeFactory.doesNotMatchRegexValueElement, String.class);
        for (String regex : doesNotMatchRegexes) {
          try {
            Pattern.compile(regex);
          } catch (PatternSyntaxException pse) {
            checker.reportWarning(tree, "invalid.doesnotmatch.regex", pse.getMessage());
          }
        }
        break;
      default:
        // Do nothing.
    }

    return super.visitAnnotation(tree, p);
  }

  @Override
  public Void visitTypeCast(TypeCastTree tree, Void p) {
    if (tree.getExpression().getKind() == Tree.Kind.NULL_LITERAL) {
      return null;
    }

    AnnotatedTypeMirror castType = atypeFactory.getAnnotatedType(tree);
    AnnotationMirror castAnno = castType.getAnnotationInHierarchy(atypeFactory.UNKNOWNVAL);
    AnnotationMirror exprAnno =
        atypeFactory
            .getAnnotatedType(tree.getExpression())
            .getAnnotationInHierarchy(atypeFactory.UNKNOWNVAL);

    // It is always legal to cast to an IntRange type that includes all values
    // of the underlying type. Do not warn about such casts.
    // I.e. do not warn if an @IntRange(...) int is casted
    // to a @IntRange(from = Byte.MIN_VALUE, to = Byte.MAX_VALUE byte).
    if (castAnno != null
        && exprAnno != null
        && atypeFactory.isIntRange(castAnno)
        && atypeFactory.isIntRange(exprAnno)) {
      Range castRange = atypeFactory.getRange(castAnno);
      TypeKind castTypeKind = castType.getKind();
      if (castTypeKind == TypeKind.BYTE && castRange.isByteEverything()) {
        return p;
      }
      if (castTypeKind == TypeKind.CHAR && castRange.isCharEverything()) {
        return p;
      }
      if (castTypeKind == TypeKind.SHORT && castRange.isShortEverything()) {
        return p;
      }
      if (castTypeKind == TypeKind.INT && castRange.isIntEverything()) {
        return p;
      }
      if (castTypeKind == TypeKind.LONG && castRange.isLongEverything()) {
        return p;
      }
      if (Range.ignoreOverflow) {
        // Range.ignoreOverflow is only set if this checker is ignoring overflow.
        // In that case, do not warn if the range of the expression encompasses
        // the whole type being casted to (i.e. the warning is actually about overflow).
        Range exprRange = atypeFactory.getRange(exprAnno);
        if (castTypeKind == TypeKind.BYTE
            || castTypeKind == TypeKind.CHAR
            || castTypeKind == TypeKind.SHORT
            || castTypeKind == TypeKind.INT) {
          exprRange = NumberUtils.castRange(castType.getUnderlyingType(), exprRange);
        }
        if (castRange.equals(exprRange)) {
          return p;
        }
      }
    }
    return super.visitTypeCast(tree, p);
  }

  // At this point, types are like: (@IntVal(-1) byte, @IntVal(255) int) and knowledge of signedness
  // is gone.  So, use castType's underlying type to infer correctness of the cast.  This method
  // returns true for (@IntVal(-1), @IntVal(255)) if the underlying type is `byte`, but not for any
  // other underlying type.
  @Override
  protected boolean isTypeCastSafe(AnnotatedTypeMirror castType, AnnotatedTypeMirror exprType) {
    TypeKind castTypeKind = TypeKindUtils.primitiveOrBoxedToTypeKind(castType.getUnderlyingType());
    TypeKind exprTypeKind = TypeKindUtils.primitiveOrBoxedToTypeKind(exprType.getUnderlyingType());
    if (castTypeKind != null
        && exprTypeKind != null
        && TypeKindUtils.isIntegral(castTypeKind)
        && TypeKindUtils.isIntegral(exprTypeKind)) {
      AnnotationMirrorSet castAnnos = castType.getAnnotations();
      AnnotationMirrorSet exprAnnos = exprType.getAnnotations();
      if (castAnnos.equals(exprAnnos)) {
        return true;
      }
      assert castAnnos.size() == 1;
      assert exprAnnos.size() == 1;
      AnnotationMirror castAnno = castAnnos.first();
      AnnotationMirror exprAnno = exprAnnos.first();
      boolean castAnnoIsIntVal = atypeFactory.areSameByClass(castAnno, IntVal.class);
      boolean exprAnnoIsIntVal = atypeFactory.areSameByClass(exprAnno, IntVal.class);
      if (castAnnoIsIntVal && exprAnnoIsIntVal) {
        List<Long> castValues = atypeFactory.getIntValues(castAnno);
        List<Long> exprValues = atypeFactory.getIntValues(exprAnno);
        if (castValues.size() == 1 && exprValues.size() == 1) {
          // Special-case singleton sets for speed.
          switch (castTypeKind) {
            case BYTE:
              return castValues.get(0).byteValue() == exprValues.get(0).byteValue();
            case INT:
              return castValues.get(0).intValue() == exprValues.get(0).intValue();
            case SHORT:
              return castValues.get(0).shortValue() == exprValues.get(0).shortValue();
            default:
              return castValues.get(0).longValue() == exprValues.get(0).longValue();
          }
        } else {
          switch (castTypeKind) {
            case BYTE:
              {
                TreeSet<Byte> castValuesTree =
                    new TreeSet<Byte>(CollectionsPlume.mapList(Number::byteValue, castValues));
                TreeSet<Byte> exprValuesTree =
                    new TreeSet<Byte>(CollectionsPlume.mapList(Number::byteValue, exprValues));
                return CollectionsPlume.sortedSetContainsAll(castValuesTree, exprValuesTree);
              }
            case INT:
              {
                TreeSet<Integer> castValuesTree =
                    new TreeSet<Integer>(CollectionsPlume.mapList(Number::intValue, castValues));
                TreeSet<Integer> exprValuesTree =
                    new TreeSet<Integer>(CollectionsPlume.mapList(Number::intValue, exprValues));
                return CollectionsPlume.sortedSetContainsAll(castValuesTree, exprValuesTree);
              }
            case SHORT:
              {
                TreeSet<Short> castValuesTree =
                    new TreeSet<Short>(CollectionsPlume.mapList(Number::shortValue, castValues));
                TreeSet<Short> exprValuesTree =
                    new TreeSet<Short>(CollectionsPlume.mapList(Number::shortValue, exprValues));
                return CollectionsPlume.sortedSetContainsAll(castValuesTree, exprValuesTree);
              }
            default:
              {
                TreeSet<Long> castValuesTree = new TreeSet<>(castValues);
                TreeSet<Long> exprValuesTree = new TreeSet<>(exprValues);
                return CollectionsPlume.sortedSetContainsAll(castValuesTree, exprValuesTree);
              }
          }
        }
      }
    }

    return super.isTypeCastSafe(castType, exprType);
  }

  /**
   * Overridden to issue errors at the appropriate place if an {@code IntRange} or {@code
   * ArrayLenRange} annotation has {@code from > to}. {@code from > to} either indicates a user
   * error when writing an annotation or an error in the checker's implementation, as {@code from}
   * should always be {@code <= to}. Note that additional checks are performed in {@link
   * #visitAnnotation(AnnotationTree, Void)}.
   *
   * @see #visitAnnotation(AnnotationTree, Void)
   */
  @Override
  public boolean validateType(Tree tree, AnnotatedTypeMirror type) {
    replaceSpecialIntRangeAnnotations(type);
    if (!super.validateType(tree, type)) {
      return false;
    }

    AnnotationMirror anno = type.getAnnotationInHierarchy(atypeFactory.UNKNOWNVAL);
    if (anno == null) {
      return false;
    }

    if (AnnotationUtils.areSameByName(anno, ValueAnnotatedTypeFactory.INTRANGE_NAME)) {
      if (TypesUtils.isIntegralPrimitiveOrBoxed(type.getUnderlyingType())) {
        long from = atypeFactory.getFromValueFromIntRange(type);
        long to = atypeFactory.getToValueFromIntRange(type);
        if (from > to) {
          checker.reportError(tree, "from.greater.than.to");
          return false;
        }
      } else {
        TypeMirror utype = type.getUnderlyingType();
        if (!TypesUtils.isObject(utype)
            && !TypesUtils.isDeclaredOfName(utype, "java.lang.Number")
            && !TypesUtils.isFloatingPoint(utype)) {
          checker.reportError(tree, "annotation.intrange.on.noninteger");
          return false;
        }
      }
    } else if (AnnotationUtils.areSameByName(anno, ValueAnnotatedTypeFactory.ARRAYLENRANGE_NAME)) {
      long from = getTypeFactory().getArrayLenRangeFromValue(anno);
      long to = getTypeFactory().getArrayLenRangeToValue(anno);
      if (from > to) {
        checker.reportError(tree, "from.greater.than.to");
        return false;
      }
    }

    return true;
  }

  /**
   * Returns true if an expression of the given type can be a compile-time constant value.
   *
   * @param tm a type
   * @return true if an expression of the given type can be a compile-time constant value
   */
  private boolean canBeConstant(TypeMirror tm) {
    return TypesUtils.isPrimitive(tm)
        || TypesUtils.isBoxedPrimitive(tm)
        || TypesUtils.isString(tm)
        || (tm.getKind() == TypeKind.ARRAY && canBeConstant(((ArrayType) tm).getComponentType()));
  }

  @Override
  public Void visitMethod(MethodTree tree, Void p) {
    super.visitMethod(tree, p);

    ExecutableElement method = TreeUtils.elementFromDeclaration(tree);
    if (atypeFactory.getDeclAnnotation(method, StaticallyExecutable.class) != null) {
      // The method is annotated as @StaticallyExecutable.
      if (atypeFactory.getDeclAnnotation(method, Pure.class) == null) {
        checker.reportWarning(tree, "statically.executable.not.pure");
      }
      TypeMirror returnType = method.getReturnType();
      if (returnType.getKind() != TypeKind.VOID && !canBeConstant(returnType)) {
        checker.reportError(tree, "statically.executable.nonconstant.return.type", returnType);
      }

      // Ways to determine the receiver type.
      // 1. This definition of receiverType is null when receiver is implicit and method has
      //    class com.sun.tools.javac.code.Symbol$MethodSymbol.  WHY?
      //        TypeMirror receiverType = method.getReceiverType();
      //    The same is true of TreeUtils.elementFromDeclaration(tree).getReceiverType()
      //    which seems to conflict with ExecutableType's documentation.
      // 2. Can't use the tree, because the receiver might not be explicit.
      // 3. Check whether method is static and use the declaring class.  Doesn't handle all
      //    cases, but handles the most common ones.
      TypeMirror receiverType = method.getReceiverType();
      // If the method is static, issue no warning.  This is incorrect in the case of a
      // constructor or a static method in an inner class.
      if (!ElementUtils.isStatic(method)) {
        receiverType = ElementUtils.getType(ElementUtils.enclosingTypeElement(method));
      }
      if (receiverType != null
          && receiverType.getKind() != TypeKind.NONE
          && !canBeConstant(receiverType)) {
        checker.reportError(
            tree,
            "statically.executable.nonconstant.parameter.type",
            "this (the receiver)",
            returnType);
      }

      for (VariableElement param : method.getParameters()) {
        TypeMirror paramType = param.asType();
        if (paramType.getKind() != TypeKind.NONE && !canBeConstant(paramType)) {
          checker.reportError(
              tree,
              "statically.executable.nonconstant.parameter.type",
              param.getSimpleName().toString(),
              returnType);
        }
      }
    }
    return null;
  }
}
