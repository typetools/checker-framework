package org.checkerframework.framework.util.typeinference;

import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.*;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.type.TypeHierarchy;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.*;
import javax.lang.model.util.Types;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedType;
import java.util.*;
import java.util.Map.Entry;

/**
 *  A class used to perform greatest lower bound calculations
 *
 */
public class GlbUtil {

    private static boolean isSubtypeInHierarchies(final AnnotatedTypeMirror subtype, final AnnotatedTypeMirror supertype,
                                                  final Set<AnnotationMirror> hierarchies, AnnotatedTypeFactory typeFactory) {
        final TypeHierarchy typeHierarchy = typeFactory.getTypeHierarchy();
        for (AnnotationMirror top : hierarchies) {
            if (!typeHierarchy.isSubtype(subtype, supertype, top)) {
                return false;
            }
        }

        return true;
    }

    public static AnnotatedTypeMirror hackGlb(final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2,
                                   final Set<AnnotationMirror> hierarchies, final AnnotatedTypeFactory typeFactory) {
        if (isSubtypeInHierarchies(type1, type2, hierarchies, typeFactory)) {
            return type1;
        }

        if (isSubtypeInHierarchies(type2, type1, hierarchies, typeFactory)) {
            return type2;
        }

        return null;
    }

    public static boolean isInterface(final AnnotatedTypeMirror type) {
        if (type.getKind() == TypeKind.DECLARED) {
            return ((DeclaredType) type.getUnderlyingType()).asElement().getKind().isInterface();

        } else if (type.getKind() == TypeKind.TYPEVAR) {
            return isInterface(((AnnotatedTypeVariable) type).getUpperBound());

        } else if (type.getKind() == TypeKind.WILDCARD) {
            return isInterface(((AnnotatedWildcardType) type).getExtendsBound());

        } else if (type.getKind() == TypeKind.INTERSECTION) {
            //the first bound of an intersection type is either a class or an interface
            //all other bounds are interfaces.  Therefore, if the first one is an interface
            //they all are
            TypeMirror firstBound = ((IntersectionType)type.getUnderlyingType()).getBounds().get(0);
            return ((DeclaredType)firstBound).asElement().getKind().isInterface();
        }

        return false;
    }

    public static AnnotatedTypeMirror glbAll(final Map<AnnotatedTypeMirror, Set<AnnotationMirror>> typeMirrors, final AnnotatedTypeFactory typeFactory) {
        final QualifierHierarchy qualifierHierarchy = typeFactory.getQualifierHierarchy();
        if (typeMirrors.isEmpty()) {
            return null;
        }

        //what to do when the GLB is an unannotated type variable

        Map<AnnotationMirror, AnnotationMirror> glbPrimaries = AnnotationUtils.createAnnotationMap();
        for(Entry<AnnotatedTypeMirror, Set<AnnotationMirror>> tmEntry : typeMirrors.entrySet()) {
            final Set<AnnotationMirror> typeAnnoHierarchies = tmEntry.getValue();
            final AnnotatedTypeMirror type = tmEntry.getKey();

            //TODO: GLB TYPE VARS
            for (AnnotationMirror top : typeAnnoHierarchies) {
                final AnnotationMirror typeAnno = type.getEffectiveAnnotationInHierarchy(top);
                final AnnotationMirror currentAnno = glbPrimaries.get(top);
                if (typeAnno != null && currentAnno != null) {
                    glbPrimaries.put(top, qualifierHierarchy.greatestLowerBound(currentAnno, typeAnno));
                } else if (typeAnno != null) {
                    glbPrimaries.put(top, typeAnno);
                }
            }
        }

        final List<AnnotatedTypeMirror> glbTypes = new ArrayList<>();

        final Set<AnnotationMirror> values = new HashSet<>(glbPrimaries.values());
        for (AnnotatedTypeMirror type : typeMirrors.keySet()) {
            if (type.getKind() != TypeKind.TYPEVAR
             || !qualifierHierarchy.isSubtype(type.getEffectiveAnnotations(), values)) {
                final AnnotatedTypeMirror copy = type.deepCopy();
                copy.replaceAnnotations(values);
                glbTypes.add(copy);
            } else {
                //if the annotations came from the upper bound of this typevar
                //we do NOT want to place them as primary annotations (and destroy the
                //type vars lower bound)
               glbTypes.add(type);
            }
        }

        final TypeHierarchy typeHierarchy = typeFactory.getTypeHierarchy();

        sortForGlb(glbTypes, typeFactory);
        //TODO: we might a GLB type that is a TYPE VARIABLE or WILDCARD that is actually below the
        //type resulting from adding a primary, if so, use that instead
        AnnotatedTypeMirror glbType = glbTypes.get(0);
        int index = 1;
        while (index < glbTypes.size()) {
            //avoid using null if possible, since constraints form the lower bound will often have NULL types
            if (glbType.getKind() != TypeKind.NULL) {
                glbType = glbTypes.get(index);
            }
            index += 1;
        }

        boolean incomparable = false;
        for(final AnnotatedTypeMirror type : glbTypes) {
            if (!incomparable && !typeHierarchy.isSubtype(glbType, type) && type.getKind() != TypeKind.NULL) {
                incomparable = true;
            }
        }

        if (incomparable) {
            return createBottom(typeFactory, glbType.getEffectiveAnnotations());
        }

        return glbType;
    }

    private static AnnotatedNullType createBottom(final AnnotatedTypeFactory typeFactory,
                                           final Set<? extends AnnotationMirror> annos) {
        final AnnotatedNullType nullType = (AnnotatedNullType)
                typeFactory.toAnnotatedType(typeFactory.getProcessingEnv().getTypeUtils().getNullType(), false);
        nullType.addAnnotations(annos);
        return nullType;
    }

    public static void sortForGlb(final List<? extends AnnotatedTypeMirror> typeMirrors, final AnnotatedTypeFactory typeFactory) {
        final QualifierHierarchy qualifierHierarchy = typeFactory.getQualifierHierarchy();
        final Types types = typeFactory.getProcessingEnv().getTypeUtils();

        typeMirrors.sort(new Comparator<AnnotatedTypeMirror>() {
            @Override
            public int compare(AnnotatedTypeMirror type1, AnnotatedTypeMirror type2) {
                final TypeMirror underlyingType1 = type1.getUnderlyingType();
                final TypeMirror underlyingType2 = type2.getUnderlyingType();

                if (types.isSameType(underlyingType1, underlyingType2)) {
                    return compareAnnotations(qualifierHierarchy, type1, type2);
                }

                if (types.isSubtype(underlyingType1, underlyingType2)) {
                    return 1;
                }

                //if they're incomparable or type2 is a subtype of type1
                return -1;
            }

            private int compareAnnotations(final QualifierHierarchy qualHierarchy, final AnnotatedTypeMirror type1, final AnnotatedTypeMirror type2) {
                if (AnnotationUtils.areSame(type1.getAnnotations(), type2.getAnnotations())) {
                    return 0;
                }

                if (qualHierarchy.isSubtype(type1.getAnnotations(), type2.getAnnotations())) {
                    return 1;

                } else {
                    return -1;
                }
            }
        });
    }
}
