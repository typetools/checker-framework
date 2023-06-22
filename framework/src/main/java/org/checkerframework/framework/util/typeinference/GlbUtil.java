package org.checkerframework.framework.util.typeinference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedNullType;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.TypeHierarchy;
import org.checkerframework.javacutil.AnnotationMirrorMap;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;

/** A class used to determine the greatest lower bounds for a set of AnnotatedTypeMirrors. */
public class GlbUtil {

  /**
   * Returns the greatest lower bound of the given {@code TypeMirror}s. If any of the type mirrors
   * are incomparable, Returns an AnnotatedNullType that contains the greatest lower bounds of the
   * primary annotations of typeMirrors.
   *
   * <p>Note: This method can be improved for wildcards and type variables.
   *
   * @param typeMirrors the types to glb
   * @param typeFactory the type factory
   * @return the greatest lower bound of typeMirrors
   */
  public static @Nullable AnnotatedTypeMirror glbAll(
      Map<AnnotatedTypeMirror, AnnotationMirrorSet> typeMirrors, AnnotatedTypeFactory typeFactory) {
    QualifierHierarchy qualHierarchy = typeFactory.getQualifierHierarchy();
    if (typeMirrors.isEmpty()) {
      return null;
    }

    // dtermine the greatest lower bounds for the primary annotations
    AnnotationMirrorMap<AnnotationMirror> glbPrimaries = new AnnotationMirrorMap<>();
    for (Map.Entry<AnnotatedTypeMirror, AnnotationMirrorSet> tmEntry : typeMirrors.entrySet()) {
      AnnotationMirrorSet typeAnnoHierarchies = tmEntry.getValue();
      AnnotatedTypeMirror type = tmEntry.getKey();

      for (AnnotationMirror top : typeAnnoHierarchies) {
        // TODO: When all of the typeMirrors are either wildcards or type variables than the
        // greatest lower bound should involve handling the bounds individually rather than
        // using the effective annotation.  We are doing this for expediency.
        AnnotationMirror typeAnno = type.getEffectiveAnnotationInHierarchy(top);
        AnnotationMirror currentAnno = glbPrimaries.get(top);
        if (typeAnno != null && currentAnno != null) {
          glbPrimaries.put(
              top, qualHierarchy.greatestLowerBoundQualifiersOnly(currentAnno, typeAnno));
        } else if (typeAnno != null) {
          glbPrimaries.put(top, typeAnno);
        }
      }
    }

    List<AnnotatedTypeMirror> glbTypes = new ArrayList<>();

    // create a copy of all of the types and apply the glb primary annotation
    AnnotationMirrorSet values = new AnnotationMirrorSet(glbPrimaries.values());
    for (AnnotatedTypeMirror atm : typeMirrors.keySet()) {
      if (atm.getKind() != TypeKind.TYPEVAR
          || !qualHierarchy.isSubtypeShallowEffective(atm, values)) {
        AnnotatedTypeMirror copy = atm.deepCopy();
        copy.replaceAnnotations(values);
        glbTypes.add(copy);

      } else {
        // if the annotations came from the upper bound of this typevar
        // we do NOT want to place them as primary annotations (and destroy the
        // type vars lower bound)
        glbTypes.add(atm);
      }
    }

    TypeHierarchy typeHierarchy = typeFactory.getTypeHierarchy();

    // sort placing supertypes first
    sortForGlb(glbTypes, typeFactory);

    // find the lowest type in the list that is not an AnnotatedNullType
    AnnotatedTypeMirror glbType = glbTypes.get(0);
    int index = 1;
    while (index < glbTypes.size()) {
      // avoid using null if possible, since constraints form the lower bound will often have
      // NULL types
      if (glbType.getKind() != TypeKind.NULL) {
        glbType = glbTypes.get(index);
      }
      index += 1;
    }

    // if the lowest type is a subtype of all glbTypes then it is the GLB, otherwise there are
    // two types in glbTypes that are incomparable and we need to use bottom (AnnotatedNullType)
    boolean incomparable = false;
    for (AnnotatedTypeMirror type : glbTypes) {
      if (!incomparable
          && type.getKind() != TypeKind.NULL
          && (!TypesUtils.isErasedSubtype(
                  glbType.getUnderlyingType(),
                  type.getUnderlyingType(),
                  typeFactory.getChecker().getTypeUtils())
              || !typeHierarchy.isSubtype(glbType, type))) {
        incomparable = true;
      }
    }

    // we had two incomparable types in glbTypes
    if (incomparable) {
      return createBottom(typeFactory, glbType.getEffectiveAnnotations());
    }

    return glbType;
  }

  /** Returns an AnnotatedNullType with the given annotations as primaries. */
  private static AnnotatedNullType createBottom(
      AnnotatedTypeFactory typeFactory, Set<? extends AnnotationMirror> annos) {
    return typeFactory.getAnnotatedNullType(annos);
  }

  /**
   * Sort the list of type mirrors, placing supertypes first and subtypes last.
   *
   * <p>E.g. the list: {@code ArrayList<String>, List<String>, AbstractList<String>} becomes: {@code
   * List<String>, AbstractList<String>, ArrayList<String>}
   *
   * @param typeMirrors the list to sort in place
   * @param typeFactory the type factory
   */
  public static void sortForGlb(
      List<? extends AnnotatedTypeMirror> typeMirrors, AnnotatedTypeFactory typeFactory) {
    Collections.sort(typeMirrors, new GlbSortComparator(typeFactory));
  }

  /** A comparator for {@link #sortForGlb}. */
  private static final class GlbSortComparator implements Comparator<AnnotatedTypeMirror> {

    /** The qualifier hierarchy. */
    private final QualifierHierarchy qualHierarchy;

    /** The type utiliites. */
    private final Types types;

    /**
     * Creates a new GlbSortComparator.
     *
     * @param typeFactory the type factory
     */
    public GlbSortComparator(AnnotatedTypeFactory typeFactory) {
      qualHierarchy = typeFactory.getQualifierHierarchy();
      types = typeFactory.getProcessingEnv().getTypeUtils();
    }

    @Override
    public int compare(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2) {
      TypeMirror underlyingType1 = type1.getUnderlyingType();
      TypeMirror underlyingType2 = type2.getUnderlyingType();

      if (types.isSameType(underlyingType1, underlyingType2)) {
        return compareAnnotations(type1, type2);
      } else if (types.isSubtype(underlyingType1, underlyingType2)) {
        return 1;
      } else {
        // if they're incomparable or type2 is a subtype of type1
        return -1;
      }
    }

    /**
     * Returns -1, 0, or 1 depending on whether anno1 is a supertype, same as, or a subtype of
     * annos2.
     *
     * @param type1 a type whose annotations to compare
     * @param type2 a type whose annotations to compare
     * @return the comparison of type1 and type2
     */
    private int compareAnnotations(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2) {
      AnnotationMirrorSet annos1 = type1.getPrimaryAnnotations();
      AnnotationMirrorSet annos2 = type2.getPrimaryAnnotations();
      if (AnnotationUtils.areSame(annos1, annos2)) {
        return 0;
      }
      TypeMirror tm1 = type1.getUnderlyingType();
      TypeMirror tm2 = type2.getUnderlyingType();
      if (qualHierarchy.isSubtypeShallow(annos1, tm1, annos2, tm2)) {
        return 1;
      } else {
        return -1;
      }
    }
  }
}
