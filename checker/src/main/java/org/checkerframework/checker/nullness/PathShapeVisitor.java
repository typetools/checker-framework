package org.checkerframework.checker.nullness;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.List;
import javax.lang.model.type.TypeKind;
import org.checkerframework.checker.compilermsgs.qual.CompilerMessageKey;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.TreeUtils;

/**
 * The visitor for the Path Shape Checker. It skips classes that do not use paths, and it treats the
 * type argument of the result of {@code Stream.toList()} as covariant.
 *
 * @checker_framework.manual #path-shape-checker Path Shape Checker
 */
public class PathShapeVisitor extends BaseTypeVisitor<PathShapeAnnotatedTypeFactory> {

  /**
   * Creates a new PathShapeVisitor.
   *
   * @param checker the checker that uses this visitor
   */
  public PathShapeVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  /**
   * Returns true if the given class is a top-level class that does not use paths. In such a class,
   * the Path Shape Checker never issues a warning and never refines a type, so checking the class
   * would only waste time. A nested class is checked if its top-level class is.
   *
   * @param tree a class
   * @return true if the Path Shape Checker should skip {@code tree}
   */
  @Override
  protected boolean shouldSkipClass(ClassTree tree) {
    return root.getTypeDecls().contains(tree) && !atypeFactory.usesPaths(tree);
  }

  /**
   * {@inheritDoc}
   *
   * <p>{@code Stream.toList()} returns an unmodifiable list, so no element whose path shape is
   * unknown can be inserted into it. Therefore, it is sound to treat its type argument as
   * covariant. For example, {@code Files.list(dir).toList()} has type {@code List<@HasName Path>},
   * and it may be returned from a method whose return type is {@code List<Path>}.
   */
  @Override
  protected boolean commonAssignmentCheck(
      AnnotatedTypeMirror varType,
      ExpressionTree valueExpTree,
      @CompilerMessageKey String errorKey,
      Object... extraArgs) {
    if (!(TreeUtils.withoutParens(valueExpTree) instanceof MethodInvocationTree invocation)
        || !atypeFactory.isStreamToList(invocation)
        || varType.getKind() != TypeKind.DECLARED
        || shouldSkipUses(valueExpTree)) {
      return super.commonAssignmentCheck(varType, valueExpTree, errorKey, extraArgs);
    }
    if (!validateTypeOf(valueExpTree)) {
      return true;
    }
    AnnotatedTypeMirror valueType = atypeFactory.getAnnotatedType(valueExpTree);
    if (valueType.getKind() == TypeKind.DECLARED) {
      AnnotatedDeclaredType varDeclaredType = (AnnotatedDeclaredType) varType;
      AnnotatedDeclaredType valueAsSuper =
          AnnotatedTypes.castedAsSuper(
              atypeFactory, (AnnotatedDeclaredType) valueType, varDeclaredType);
      useCovariantTypeArguments(valueAsSuper, varDeclaredType);
      valueType = valueAsSuper;
    }
    return commonAssignmentCheck(varType, valueType, valueExpTree, errorKey, extraArgs);
  }

  /**
   * Replaces each type argument qualifier of {@code valueType} by the corresponding one of {@code
   * varType}, if it is a subtype. Afterward, checking that {@code valueType} is a subtype of {@code
   * varType} treats their type arguments as covariant.
   *
   * @param valueType the type of an unmodifiable collection, viewed as {@code varType}; is
   *     side-effected by this method
   * @param varType the type to which the value is assigned
   */
  private void useCovariantTypeArguments(
      AnnotatedDeclaredType valueType, AnnotatedDeclaredType varType) {
    List<AnnotatedTypeMirror> valueTypeArgs = valueType.getTypeArguments();
    List<AnnotatedTypeMirror> varTypeArgs = varType.getTypeArguments();
    if (valueTypeArgs.size() != varTypeArgs.size()) {
      // One of the types is raw.
      return;
    }
    for (int i = 0; i < valueTypeArgs.size(); i++) {
      AnnotatedTypeMirror valueTypeArg = valueTypeArgs.get(i);
      AnnotatedTypeMirror varTypeArg = varTypeArgs.get(i);
      if (valueTypeArg.getKind() != TypeKind.DECLARED
          || varTypeArg.getKind() != TypeKind.DECLARED) {
        continue;
      }
      AnnotationMirrorSet valueQualifiers = valueTypeArg.getPrimaryAnnotations();
      AnnotationMirrorSet varQualifiers = varTypeArg.getPrimaryAnnotations();
      if (valueQualifiers.size() == varQualifiers.size()
          && atypeFactory
              .getQualifierHierarchy()
              .isSubtypeShallow(
                  valueQualifiers,
                  valueTypeArg.getUnderlyingType(),
                  varQualifiers,
                  varTypeArg.getUnderlyingType())) {
        valueTypeArg.replaceAnnotations(varQualifiers);
      }
    }
  }
}
