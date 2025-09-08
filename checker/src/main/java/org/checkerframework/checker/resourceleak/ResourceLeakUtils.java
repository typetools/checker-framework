package org.checkerframework.checker.resourceleak;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import org.checkerframework.checker.collectionownership.CollectionOwnershipAnnotatedTypeFactory;
import org.checkerframework.checker.collectionownership.CollectionOwnershipChecker;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.checker.mustcall.MustCallNoCreatesMustCallForChecker;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.mustcall.qual.MustCallUnknown;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.rlccalledmethods.RLCCalledMethodsAnnotatedTypeFactory;
import org.checkerframework.checker.rlccalledmethods.RLCCalledMethodsChecker;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.BugInCF;
import org.checkerframework.javacutil.TypeSystemError;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Collection of static utility functions related to the various (sub-) checkers within the
 * ResourceLeakChecker.
 */
public class ResourceLeakUtils {

  /** Do not instantiate; this class is a collection of static methods. */
  private ResourceLeakUtils() {
    throw new Error("Do not instantiate");
  }

  /** List of checker names associated with the Resource Leak Checker. */
  public static List<String> rlcCheckers =
      new ArrayList<>(
          Arrays.asList(
              ResourceLeakChecker.class.getCanonicalName(),
              CollectionOwnershipChecker.class.getCanonicalName(),
              RLCCalledMethodsChecker.class.getCanonicalName(),
              MustCallChecker.class.getCanonicalName(),
              MustCallNoCreatesMustCallForChecker.class.getCanonicalName()));

  /**
   * Given a type factory that is part of the resource leak checker hierarchy, returns the {@link
   * ResourceLeakChecker} in the checker hierarchy.
   *
   * @param referenceAtf the type factory to retrieve the {@link ResourceLeakChecker} from; must be
   *     part of the Resource Leak hierarchy
   * @return the {@link ResourceLeakChecker} in the checker hierarchy
   */
  public static ResourceLeakChecker getResourceLeakChecker(AnnotatedTypeFactory referenceAtf) {
    return getResourceLeakChecker(referenceAtf.getChecker());
  }

  /**
   * Given a checker that is part of the resource leak checker hierarchy, returns the {@link
   * ResourceLeakChecker} in the checker hierarchy.
   *
   * @param referenceChecker the checker to retrieve the {@link ResourceLeakChecker} from; must be
   *     part of the Resource Leak hierarchy
   * @return the {@link ResourceLeakChecker} in the checker hierarchy
   */
  public static ResourceLeakChecker getResourceLeakChecker(SourceChecker referenceChecker) {
    if (referenceChecker instanceof ResourceLeakChecker) {
      return (ResourceLeakChecker) referenceChecker;
    } else if (referenceChecker instanceof RLCCalledMethodsChecker
        || referenceChecker instanceof CollectionOwnershipChecker
        || referenceChecker instanceof MustCallChecker) {
      return getResourceLeakChecker(referenceChecker.getParentChecker());
    } else {
      throw new TypeSystemError(
          "Bad argument to ResourceLeakUtils#getResourceLeakChecker(): "
              + (referenceChecker == null ? "null" : referenceChecker.getClass().getSimpleName()));
    }
  }

  /**
   * Given a type factory part of the resource leak ecosystem, returns the {@link
   * MustCallAnnotatedTypeFactory} in the checker hierarchy.
   *
   * @param referenceAtf the type factory to retrieve the {@link MustCallAnnotatedTypeFactory} from
   * @return the {@link MustCallAnnotatedTypeFactory} in the checker hierarchy
   */
  public static MustCallAnnotatedTypeFactory getMustCallAnnotatedTypeFactory(
      AnnotatedTypeFactory referenceAtf) {
    if (referenceAtf == null) {
      throw new IllegalArgumentException("Argument referenceAtf cannot be null");
    } else {
      return getMustCallAnnotatedTypeFactory(referenceAtf.getChecker());
    }
  }

  /**
   * Given a checker part of the resource leak ecosystem, returns the {@link
   * MustCallAnnotatedTypeFactory} in the checker hierarchy.
   *
   * @param referenceChecker the checker to retrieve the {@link MustCallAnnotatedTypeFactory} from
   * @return the {@link MustCallAnnotatedTypeFactory} in the checker hierarchy
   */
  public static MustCallAnnotatedTypeFactory getMustCallAnnotatedTypeFactory(
      SourceChecker referenceChecker) {
    if (referenceChecker == null) {
      throw new IllegalArgumentException("Argument referenceChecker cannot be null");
    }

    String className = referenceChecker.getClass().getSimpleName();
    if ("MustCallChecker".equals(className)
        || "MustCallNoCreatesMustCallForChecker".equals(className)) {
      return (MustCallAnnotatedTypeFactory) ((MustCallChecker) referenceChecker).getTypeFactory();
    } else if ("CollectionOwnershipChecker".equals(className)) {
      return getMustCallAnnotatedTypeFactory(
          referenceChecker.getSubchecker(RLCCalledMethodsChecker.class));
    } else if ("RLCCalledMethodsChecker".equals(className)) {
      MustCallChecker mcc = referenceChecker.getSubchecker(MustCallChecker.class);
      return getMustCallAnnotatedTypeFactory(
          mcc != null
              ? mcc
              : referenceChecker.getSubchecker(MustCallNoCreatesMustCallForChecker.class));
    } else if ("ResourceLeakChecker".equals(className)) {
      return getMustCallAnnotatedTypeFactory(
          referenceChecker.getSubchecker(CollectionOwnershipChecker.class));
    } else {
      throw new IllegalArgumentException(
          "Argument referenceChecker to"
              + " ResourceLeakUtils#getMustCallAnnotatedTypeFactory(referenceChecker) expected to"
              + " be an RLC checker but is "
              + className);
    }
  }

  /**
   * Given a type factory that is part of the resource leak checker hierarchy, returns the {@link
   * RLCCalledMethodsChecker} in the checker hierarchy.
   *
   * @param referenceAtf the type factory to retrieve the {@link RLCCalledMethodsChecker} from; must
   *     be part of the Resource Leak hierarchy
   * @return the {@link RLCCalledMethodsChecker} in the checker hierarchy
   */
  public static RLCCalledMethodsChecker getRLCCalledMethodsChecker(
      AnnotatedTypeFactory referenceAtf) {
    return getRLCCalledMethodsChecker(referenceAtf.getChecker());
  }

  /**
   * Given a checker that is part of the resource leak checker hierarchy, returns the {@link
   * RLCCalledMethodsChecker} in the checker hierarchy.
   *
   * @param referenceChecker the checker to retrieve the {@link RLCCalledMethodsChecker} from; must
   *     be part of the Resource Leak hierarchy
   * @return the {@link RLCCalledMethodsChecker} in the checker hierarchy
   */
  public static RLCCalledMethodsChecker getRLCCalledMethodsChecker(SourceChecker referenceChecker) {
    if (referenceChecker instanceof RLCCalledMethodsChecker) {
      return (RLCCalledMethodsChecker) referenceChecker;
    } else if (referenceChecker instanceof ResourceLeakChecker) {
      return getRLCCalledMethodsChecker(
          referenceChecker.getSubchecker(CollectionOwnershipChecker.class));
    } else if (referenceChecker instanceof CollectionOwnershipChecker) {
      return getRLCCalledMethodsChecker(
          referenceChecker.getSubchecker(RLCCalledMethodsChecker.class));
    } else if (referenceChecker instanceof MustCallChecker) {
      return getRLCCalledMethodsChecker(referenceChecker.getParentChecker());
    } else {
      throw new TypeSystemError(
          "Bad argument to"
              + " ResourceLeakUtils#getRLCCalledMethodsChecker(): "
              + (referenceChecker == null ? "null" : referenceChecker.getClass().getSimpleName()));
    }
  }

  /**
   * Given a type factory that is part of the resource leak checker hierarchy, returns the {@link
   * RLCCalledMethodsAnnotatedTypeFactory} in the checker hierarchy.
   *
   * @param referenceAtf the type factory to retrieve the {@link
   *     RLCCalledMethodsAnnotatedTypeFactory} from; must be part of the Resource Leak hierarchy
   * @return the {@link RLCCalledMethodsAnnotatedTypeFactory} in the checker hierarchy
   */
  public static RLCCalledMethodsAnnotatedTypeFactory getRLCCalledMethodsAnnotatedTypeFactory(
      AnnotatedTypeFactory referenceAtf) {
    return getRLCCalledMethodsAnnotatedTypeFactory(referenceAtf.getChecker());
  }

  /**
   * Given a checker that is part of the resource leak checker hierarchy, returns the {@link
   * RLCCalledMethodsAnnotatedTypeFactory} in the hierarchy.
   *
   * @param referenceChecker the checker to retrieve the {@link
   *     RLCCalledMethodsAnnotatedTypeFactory} from; must be part of the Resource Leak hierarchy
   * @return the {@link RLCCalledMethodsAnnotatedTypeFactory} in the hierarchy
   */
  public static RLCCalledMethodsAnnotatedTypeFactory getRLCCalledMethodsAnnotatedTypeFactory(
      SourceChecker referenceChecker) {
    return (RLCCalledMethodsAnnotatedTypeFactory)
        getRLCCalledMethodsChecker(referenceChecker).getTypeFactory();
  }

  /**
   * Given a checker part of the resource leak ecosystem, returns the {@link
   * CollectionOwnershipAnnotatedTypeFactory} in the checker hierarchy.
   *
   * @param referenceChecker the checker to retrieve the {@link
   *     CollectionOwnershipAnnotatedTypeFactory} from
   * @return the {@link CollectionOwnershipAnnotatedTypeFactory} in the checker hierarchy
   */
  public static CollectionOwnershipAnnotatedTypeFactory getCollectionOwnershipAnnotatedTypeFactory(
      SourceChecker referenceChecker) {
    if (referenceChecker == null) {
      throw new IllegalArgumentException("Argument referenceChecker cannot be null");
    }

    String className = referenceChecker.getClass().getSimpleName();
    if ("CollectionOwnershipChecker".equals(className)
        || "MustCallNoCreatesMustCallForChecker".equals(className)) {
      return (CollectionOwnershipAnnotatedTypeFactory)
          ((CollectionOwnershipChecker) referenceChecker).getTypeFactory();
    } else if ("RLCCalledMethodsChecker".equals(className)) {
      return getCollectionOwnershipAnnotatedTypeFactory(referenceChecker.getParentChecker());
    } else if ("MustCallChecker".equals(className)) {
      return getCollectionOwnershipAnnotatedTypeFactory(referenceChecker.getParentChecker());
    } else if ("ResourceLeakChecker".equals(className)) {
      return getCollectionOwnershipAnnotatedTypeFactory(
          referenceChecker.getSubchecker(CollectionOwnershipChecker.class));
    } else {
      throw new IllegalArgumentException(
          "Argument referenceChecker to"
              + " ResourceLeakUtils#getCollectionOwnershipAnnotatedTypeFactory(referenceChecker)"
              + " expected to be an RLC checker but is "
              + className);
    }
  }

  /**
   * Given a type factory part of the resource leak ecosystem, returns the {@link
   * CollectionOwnershipAnnotatedTypeFactory} in the checker hierarchy.
   *
   * @param referenceAtf the type factory to retrieve the {@link
   *     CollectionOwnershipAnnotatedTypeFactory} from
   * @return the {@link CollectionOwnershipAnnotatedTypeFactory} in the checker hierarchy
   */
  public static CollectionOwnershipAnnotatedTypeFactory getCollectionOwnershipAnnotatedTypeFactory(
      AnnotatedTypeFactory referenceAtf) {
    if (referenceAtf == null) {
      throw new IllegalArgumentException("Argument referenceAtf cannot be null");
    } else {
      return getCollectionOwnershipAnnotatedTypeFactory(referenceAtf.getChecker());
    }
  }

  /**
   * Returns true if the given Element is a java.lang.Iterable or java.util.Iterator type by
   * checking whether the raw type of the element is assignable from either. Returns false if
   * element is null, or has no valid type.
   *
   * @param element the element
   * @param atf an AnnotatedTypeFactory to get the annotated type of the element
   * @return true if the given element is a Java.lang.Iterable or Java.util.Iterator type
   */
  public static boolean isCollection(Element element, AnnotatedTypeFactory atf) {
    if (element == null) {
      return false;
    }
    AnnotatedTypeMirror elementTypeMirror = atf.getAnnotatedType(element).getErased();
    if (elementTypeMirror == null || elementTypeMirror.getUnderlyingType() == null) {
      return false;
    }
    return isCollection(elementTypeMirror.getUnderlyingType());
  }

  /**
   * Returns true if the given {@link TypeMirror} is a java.lang.Iterable or java.util.Iterator
   * subclass. This is determined by getting the class of the TypeMirror and checking whether it is
   * assignable from either.
   *
   * @param type the TypeMirror
   * @return true if type is a java.lang.Iterable or java.util.Iterator
   */
  public static boolean isCollection(TypeMirror type) {
    if (type == null) {
      return false;
    }
    Class<?> elementRawType = TypesUtils.getClassFromType(type);
    if (elementRawType == null) {
      return false;
    }
    return Iterable.class.isAssignableFrom(elementRawType)
        || Iterator.class.isAssignableFrom(elementRawType)
        || Map.class.isAssignableFrom(elementRawType);
  }

  /**
   * Returns true if the given {@link TypeMirror} is a java.util.Iterator subclass. This is
   * determined by getting the class of the TypeMirror and checking whether it is assignable from
   * java.util.Iterator.
   *
   * @param type the TypeMirror
   * @return true if type is a java.util.Iterator
   */
  public static boolean isIterator(TypeMirror type) {
    if (type == null) {
      return false;
    }
    Class<?> elementRawType = TypesUtils.getClassFromType(type);
    if (elementRawType == null) {
      return false;
    }
    return Iterator.class.isAssignableFrom(elementRawType);
  }

  /**
   * Safely extract the values of a given element in an {@link AnnotationMirror}. Returns the list
   * of such values and the empty list if the element is not present.
   *
   * @param anno the annotation to extract values from
   * @param element the element to access from the annotation
   * @return the list of values of the given element in the given annotation and the empty list if
   *     it is not present
   */
  public static List<String> getValuesInAnno(AnnotationMirror anno, ExecutableElement element) {
    if (anno == null) {
      throw new BugInCF("Annotation " + anno + " must not be null.");
    } else {
      AnnotationValue av = anno.getElementValues().get(element);
      if (av == null) {
        return new ArrayList<String>();
      } else {
        return AnnotationUtils.annotationValueToList(av, String.class);
      }
    }
  }

  /**
   * Returns the list of mustcall obligations for the given {@code TypeMirror} upper bound (either
   * the type variable itself if it is concrete or the upper bound if its a wildcard or generic).
   *
   * <p>If the type variable has no upper bound, for instance if it is a wildcard with no extends
   * clause the method returns null.
   *
   * @param type the {@code TypeMirror}
   * @param mcAtf the {@code MustCallAnnotatedTypeFactory} to get the {@code MustCall} type
   * @return the list of mustcall obligations for the upper bound of {@code type} or null if the
   *     upper bound is null
   */
  public static @Nullable List<String> getMcValues(
      TypeMirror type, MustCallAnnotatedTypeFactory mcAtf) {
    if (type instanceof TypeVariable) {
      // a generic - replace with upper bound and return null if it has no upper bound
      type = ((TypeVariable) type).getUpperBound();
      if (type == null) {
        return null;
      }
    } else if (type instanceof WildcardType) {
      // a wildcard - replace with upper bound and return null if it has no upper bound
      type = ((WildcardType) type).getExtendsBound();
      if (type == null) {
        return null;
      }
    }

    AnnotationMirror manualAnno =
        AnnotationUtils.getAnnotationByClass(type.getAnnotationMirrors(), MustCall.class);
    AnnotationMirror manualMcUnkownAnno =
        AnnotationUtils.getAnnotationByClass(type.getAnnotationMirrors(), MustCallUnknown.class);
    if (manualAnno != null) {
      return getValuesInAnno(manualAnno, mcAtf.getMustCallValueElement());
    }
    if (manualMcUnkownAnno != null) {
      return Collections.singletonList(CollectionOwnershipAnnotatedTypeFactory.UNKNOWN_METHOD_NAME);
    }

    TypeElement typeElement = TypesUtils.getTypeElement(type);
    if (typeElement == null) {
      return new ArrayList<>();
    }
    AnnotationMirror imcAnnotation =
        mcAtf.getDeclAnnotation(typeElement, InheritableMustCall.class);
    AnnotationMirror mcAnnotation = mcAtf.getDeclAnnotation(typeElement, MustCall.class);
    AnnotationMirror mcUnknownAnnotation =
        mcAtf.getDeclAnnotation(typeElement, MustCallUnknown.class);
    if (mcAnnotation != null) {
      return getValuesInAnno(mcAnnotation, mcAtf.getMustCallValueElement());
    }
    if (imcAnnotation != null) {
      return getValuesInAnno(imcAnnotation, mcAtf.getInheritableMustCallValueElement());
    }
    if (mcUnknownAnnotation != null) {
      return Collections.singletonList(CollectionOwnershipAnnotatedTypeFactory.UNKNOWN_METHOD_NAME);
    }
    return new ArrayList<>();
  }

  /**
   * Returns the list of mustcall obligations for the given {@code AnnotatedTypeMirror} upper bound
   * (either the type variable itself if it is concrete or the upper bound if its a wildcard or
   * generic).
   *
   * @param type the {@code AnnotatedTypeMirror}
   * @param mcAtf the {@code MustCallAnnotatedTypeFactory} to get the {@code MustCall} type
   * @return the list of mustcall obligations for the upper bound of {@code type}
   */
  public static List<String> getMcValues(
      AnnotatedTypeMirror type, MustCallAnnotatedTypeFactory mcAtf) {
    AnnotationMirror anno = type.getEffectiveAnnotationInHierarchy(mcAtf.TOP);
    if (anno == null) {
      return Collections.emptyList();
    }
    if (AnnotationUtils.areSameByName(anno, mcAtf.TOP)) {
      return Collections.singletonList(CollectionOwnershipAnnotatedTypeFactory.UNKNOWN_METHOD_NAME);
    }
    return getValuesInAnno(anno, mcAtf.getMustCallValueElement());
  }

  /**
   * Returns true if the passed {@code TypeMirror} has a manual {@code MustCallUnknown} annotation.
   *
   * @param typeMirror the {@code TypeMirror}
   * @return true if the passed {@code TypeMirror} has a manual {@code MustCallUnknown} annotation
   */
  public static boolean hasManualMustCallUnknownAnno(TypeMirror typeMirror) {
    if (typeMirror == null) {
      return false;
    }
    for (AnnotationMirror paramAnno : typeMirror.getAnnotationMirrors()) {
      if (AnnotationUtils.areSameByName(paramAnno, MustCallUnknown.class.getCanonicalName())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns true if the passed {@code AnnotatedTypeMirror} has a manual {@code MustCallUnknown}
   * annotation.
   *
   * @param annotatedTypeMirror the {@code AnnotatedTypeMirror}
   * @param mcAtf the annotated type factory of the MustCall type system
   * @return true if the passed {@code AnnotatedTypeMirror} has a manual {@code MustCallUnknown}
   *     annotation
   */
  public static boolean hasManualMustCallUnknownAnno(
      AnnotatedTypeMirror annotatedTypeMirror, MustCallAnnotatedTypeFactory mcAtf) {
    if (annotatedTypeMirror == null) {
      return false;
    }
    AnnotationMirror manualMcAnno = annotatedTypeMirror.getPrimaryAnnotationInHierarchy(mcAtf.TOP);
    if (manualMcAnno == null) {
      return false;
    }
    if (AnnotationUtils.areSameByName(manualMcAnno, MustCallUnknown.class.getCanonicalName())) {
      return true;
    }
    return false;
  }
}
