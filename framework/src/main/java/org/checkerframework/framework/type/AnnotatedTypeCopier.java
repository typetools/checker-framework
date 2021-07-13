package org.checkerframework.framework.type;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNoType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedUnionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.visitor.AnnotatedTypeVisitor;
import org.plumelib.util.CollectionsPlume;

/**
 * AnnotatedTypeCopier is a visitor that deep copies an AnnotatedTypeMirror exactly, including any
 * lazily initialized fields. That is, if a field has already been initialized, it will be
 * initialized in the copied type.
 *
 * <p>When making copies, a map of encountered {@literal references => copied} types is maintained.
 * This ensures that, if a reference appears in multiple locations in the original type, a
 * corresponding copy of the original type appears in the same locations in the output copy. This
 * ensures that the recursive loops in the input type are preserved in its output copy (see
 * makeOrReturnCopy)
 *
 * <p>In general, AnnotatedTypeMirrors should be copied via AnnotatedTypeMirror#deepCopy and
 * AnnotatedTypeMirror#shallowCopy. AnnotatedTypeMirror#deepCopy makes use of AnnotatedTypeCopier
 * under the covers. However, this visitor and its subclasses can be invoked as follows:
 *
 * <pre>{@code new AnnotatedTypeCopier().visit(myTypeVar);}</pre>
 *
 * Note: There are methods that may require a copy of a type mirror with slight changes. It is
 * intended that this class can be overridden for these cases.
 *
 * @see org.checkerframework.framework.type.TypeVariableSubstitutor
 * @see org.checkerframework.framework.type.AnnotatedTypeCopierWithReplacement
 */
public class AnnotatedTypeCopier
    implements AnnotatedTypeVisitor<
        AnnotatedTypeMirror, IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror>> {

  /**
   * This is a hack to handle the curious behavior of substitution on an AnnotatedExecutableType.
   *
   * @see org.checkerframework.framework.type.TypeVariableSubstitutor It is poor form to include
   *     such a flag on the base class for exclusive use in a subclass but it is the least bad
   *     option in this case.
   */
  protected boolean visitingExecutableTypeParam = false;

  /**
   * See {@link #AnnotatedTypeCopier(boolean)}.
   *
   * @see #AnnotatedTypeCopier(boolean)
   */
  protected final boolean copyAnnotations;

  /**
   * Creates an AnnotatedTypeCopier that may or may not copyAnnotations By default
   * AnnotatedTypeCopier provides two major properties in its copies:
   *
   * <ol>
   *   <li>Structure preservation -- the exact structure of the original AnnotatedTypeMirror is
   *       preserved in the copy including all component types.
   *   <li>Annotation preservation -- All of the annotations from the original AnnotatedTypeMirror
   *       and its components have been copied to the new type.
   * </ol>
   *
   * If copyAnnotations is set to false, the second property, annotation preservation, is removed.
   * This is useful for cases in which the user may want to copy the structure of a type exactly but
   * NOT its annotations.
   */
  public AnnotatedTypeCopier(final boolean copyAnnotations) {
    this.copyAnnotations = copyAnnotations;
  }

  /**
   * Creates an AnnotatedTypeCopier that copies both the structure and annotations of the source
   * AnnotatedTypeMirror.
   *
   * @see #AnnotatedTypeCopier(boolean)
   */
  public AnnotatedTypeCopier() {
    this(true);
  }

  @Override
  public AnnotatedTypeMirror visit(AnnotatedTypeMirror type) {
    return type.accept(this, new IdentityHashMap<>());
  }

  @Override
  public AnnotatedTypeMirror visit(
      AnnotatedTypeMirror type,
      IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
    return type.accept(this, originalToCopy);
  }

  @Override
  public AnnotatedTypeMirror visitDeclared(
      AnnotatedDeclaredType original,
      IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
    if (originalToCopy.containsKey(original)) {
      return originalToCopy.get(original);
    }

    final AnnotatedDeclaredType copy = makeOrReturnCopy(original, originalToCopy);

    if (original.isUnderlyingTypeRaw()) {
      copy.setIsUnderlyingTypeRaw();
    }

    if (original.enclosingType != null) {
      copy.enclosingType = (AnnotatedDeclaredType) visit(original.enclosingType, originalToCopy);
    }

    if (original.typeArgs != null) {
      final List<AnnotatedTypeMirror> copyTypeArgs =
          CollectionsPlume.mapList(
              (AnnotatedTypeMirror typeArg) -> visit(typeArg, originalToCopy),
              original.getTypeArguments());
      copy.setTypeArguments(copyTypeArgs);
    }

    return copy;
  }

  @Override
  public AnnotatedTypeMirror visitIntersection(
      AnnotatedIntersectionType original,
      IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
    if (originalToCopy.containsKey(original)) {
      return originalToCopy.get(original);
    }

    final AnnotatedIntersectionType copy = makeOrReturnCopy(original, originalToCopy);

    if (original.bounds != null) {
      List<AnnotatedTypeMirror> copySupertypes =
          CollectionsPlume.mapList(
              (AnnotatedTypeMirror bound) -> visit(bound, originalToCopy), original.bounds);
      copy.bounds = Collections.unmodifiableList(copySupertypes);
    }

    return copy;
  }

  @Override
  public AnnotatedTypeMirror visitUnion(
      AnnotatedUnionType original,
      IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
    if (originalToCopy.containsKey(original)) {
      return originalToCopy.get(original);
    }

    final AnnotatedUnionType copy = makeOrReturnCopy(original, originalToCopy);

    if (original.alternatives != null) {
      final List<AnnotatedDeclaredType> copyAlternatives =
          CollectionsPlume.mapList(
              (AnnotatedDeclaredType supertype) ->
                  (AnnotatedDeclaredType) visit(supertype, originalToCopy),
              original.alternatives);
      copy.alternatives = Collections.unmodifiableList(copyAlternatives);
    }

    return copy;
  }

  @Override
  public AnnotatedTypeMirror visitExecutable(
      AnnotatedExecutableType original,
      IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
    if (originalToCopy.containsKey(original)) {
      return originalToCopy.get(original);
    }

    final AnnotatedExecutableType copy = makeOrReturnCopy(original, originalToCopy);

    copy.setElement(original.getElement());

    if (original.receiverType != null) {
      copy.receiverType = (AnnotatedDeclaredType) visit(original.receiverType, originalToCopy);
    }

    for (final AnnotatedTypeMirror param : original.paramTypes) {
      copy.paramTypes.add(visit(param, originalToCopy));
    }

    for (final AnnotatedTypeMirror thrown : original.throwsTypes) {
      copy.throwsTypes.add(visit(thrown, originalToCopy));
    }

    copy.returnType = visit(original.returnType, originalToCopy);

    for (final AnnotatedTypeVariable typeVariable : original.typeVarTypes) {
      // This field is needed to identify exactly when the declaration of an executable's
      // type parameter is visited.  When subtypes of this class visit the type parameter's
      // component types, they will likely set visitingExecutableTypeParam to false.
      // Therefore, we set this variable on each iteration of the loop.
      // See TypeVariableSubstitutor.Visitor.visitTypeVariable for an example of this.
      visitingExecutableTypeParam = true;
      copy.typeVarTypes.add((AnnotatedTypeVariable) visit(typeVariable, originalToCopy));
    }
    visitingExecutableTypeParam = false;

    return copy;
  }

  @Override
  public AnnotatedTypeMirror visitArray(
      AnnotatedArrayType original,
      IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
    if (originalToCopy.containsKey(original)) {
      return originalToCopy.get(original);
    }

    final AnnotatedArrayType copy = makeOrReturnCopy(original, originalToCopy);

    copy.setComponentType(visit(original.getComponentType(), originalToCopy));

    return copy;
  }

  @Override
  public AnnotatedTypeMirror visitTypeVariable(
      AnnotatedTypeVariable original,
      IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
    if (originalToCopy.containsKey(original)) {
      return originalToCopy.get(original);
    }

    final AnnotatedTypeVariable copy = makeOrReturnCopy(original, originalToCopy);

    if (original.getUpperBoundField() != null) {
      copy.setUpperBound(visit(original.getUpperBoundField(), originalToCopy));
    }

    if (original.getLowerBoundField() != null) {
      copy.setLowerBound(visit(original.getLowerBoundField(), originalToCopy));
    }

    return copy;
  }

  @Override
  public AnnotatedTypeMirror visitPrimitive(
      AnnotatedPrimitiveType original,
      IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
    return makeOrReturnCopy(original, originalToCopy);
  }

  @Override
  public AnnotatedTypeMirror visitNoType(
      AnnotatedNoType original,
      IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
    return makeOrReturnCopy(original, originalToCopy);
  }

  @Override
  public AnnotatedTypeMirror visitNull(
      AnnotatedNullType original,
      IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
    return makeOrReturnCopy(original, originalToCopy);
  }

  @Override
  public AnnotatedTypeMirror visitWildcard(
      AnnotatedWildcardType original,
      IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
    if (originalToCopy.containsKey(original)) {
      return originalToCopy.get(original);
    }

    final AnnotatedWildcardType copy = makeOrReturnCopy(original, originalToCopy);

    if (original.isUninferredTypeArgument()) {
      copy.setUninferredTypeArgument();
    }

    if (original.getExtendsBoundField() != null) {
      copy.setExtendsBound(visit(original.getExtendsBoundField(), originalToCopy).asUse());
    }

    if (original.getSuperBoundField() != null) {
      copy.setSuperBound(visit(original.getSuperBoundField(), originalToCopy).asUse());
    }

    copy.setTypeVariable(original.getTypeVariable());

    return copy;
  }

  /**
   * For any given object in the type being copied, we only want to generate one copy of that
   * object. When that object is encountered again, using the previously generated copy will
   * preserve the structure of the original AnnotatedTypeMirror.
   *
   * <p>makeOrReturnCopy first checks to see if an object has been encountered before. If so, it
   * returns the previously generated duplicate of that object if not, it creates a duplicate of the
   * object and stores it in the history, originalToCopy
   *
   * @param original a reference to a type to copy
   * @param originalToCopy a mapping of previously encountered references to the copies made for
   *     those references
   * @param <T> the type of original copy, this is a shortcut to avoid having to insert casts all
   *     over the visitor
   * @return a copy of original
   */
  @SuppressWarnings("unchecked")
  protected <T extends AnnotatedTypeMirror> T makeOrReturnCopy(
      T original, IdentityHashMap<AnnotatedTypeMirror, AnnotatedTypeMirror> originalToCopy) {
    if (originalToCopy.containsKey(original)) {
      return (T) originalToCopy.get(original);
    }

    final T copy = makeCopy(original);
    originalToCopy.put(original, copy);

    return copy;
  }

  @SuppressWarnings("unchecked")
  protected <T extends AnnotatedTypeMirror> T makeCopy(T original) {

    final T copy =
        (T)
            AnnotatedTypeMirror.createType(
                original.getUnderlyingType(), original.atypeFactory, original.isDeclaration());
    maybeCopyPrimaryAnnotations(original, copy);

    return copy;
  }

  /**
   * This method is called in any location in which a primary annotation would be copied from source
   * to dest. Note, this method obeys the copyAnnotations field. Subclasses of AnnotatedTypeCopier
   * can use this method to customize annotations before copying.
   *
   * @param source the type whose primary annotations are being copied
   * @param dest a copy of source that should receive its primary annotations
   */
  protected void maybeCopyPrimaryAnnotations(
      final AnnotatedTypeMirror source, final AnnotatedTypeMirror dest) {
    if (copyAnnotations) {
      dest.addAnnotations(source.getAnnotationsField());
    }
  }
}
