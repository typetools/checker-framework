package org.checkerframework.checker.resourceleak;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.checker.mustcall.MustCallNoCreatesMustCallForChecker;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.MustCall;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.rlccalledmethods.RLCCalledMethodsChecker;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.dataflow.cfg.node.MethodAccessNode;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * Collection of static utility functions related to the various (sub-) checkers within the
 * ResourceLeakChecker.
 */
public class ResourceLeakUtils {

  /** List of checker names associated with the Resource Leak Checker. */
  public static List<String> rlcCheckers =
      new ArrayList<>(
          Arrays.asList(
              ResourceLeakChecker.class.getCanonicalName(),
              RLCCalledMethodsChecker.class.getCanonicalName(),
              MustCallChecker.class.getCanonicalName(),
              MustCallNoCreatesMustCallForChecker.class.getCanonicalName()));

  /**
   * Returns the type factory corresponding to the desired checker class within the
   * ResourceLeakChecker given a checker part of the ResourceLeakChecker.
   *
   * @param targetClass the desired checker class
   * @param referenceChecker the current checker
   * @return the type factory of the desired class
   */
  public static @NonNull AnnotatedTypeFactory getTypeFactory(
      Class<? extends SourceChecker> targetClass, SourceChecker referenceChecker) {
    BaseTypeChecker targetChecker = (BaseTypeChecker) getChecker(targetClass, referenceChecker);
    return targetChecker.getTypeFactory();
  }

  /**
   * Returns the type factory corresponding to the desired checker class within the
   * ResourceLeakChecker given a type factory part of the ResourceLeakChecker.
   *
   * @param targetClass the desired checker class
   * @param referenceAtf the current atf
   * @return the type factory of the desired class
   */
  public static @NonNull AnnotatedTypeFactory getTypeFactory(
      Class<? extends SourceChecker> targetClass, AnnotatedTypeFactory referenceAtf) {
    if (!rlcCheckers.contains(targetClass.getCanonicalName())) {
      throw new IllegalArgumentException(
          "Argument targetClass to ResourceLeakUtils#getChecker(targetClass, referenceChecker) expected to be an RLC checker but is "
              + targetClass.getCanonicalName());
    }
    return ((BaseTypeChecker) getChecker(targetClass, referenceAtf.getChecker())).getTypeFactory();
  }

  /**
   * Returns the checker of the desired class within the ResourceLeakChecker given a type factory
   * part of the ResourceLeakChecker.
   *
   * @param targetClass the desired checker class
   * @param referenceAtf the current atf
   * @return the checker of the desired class
   */
  public static @NonNull SourceChecker getChecker(
      Class<? extends SourceChecker> targetClass, AnnotatedTypeFactory referenceAtf) {
    if (!rlcCheckers.contains(targetClass.getCanonicalName())) {
      throw new IllegalArgumentException(
          "Argument targetClass to ResourceLeakUtils#getChecker(targetClass, referenceChecker) expected to be an RLC checker but is "
              + targetClass.getCanonicalName());
    }
    return getChecker(targetClass, referenceAtf.getChecker());
  }

  public static @NonNull ResourceLeakChecker getResourceLeakChecker(
      AnnotatedTypeFactory referenceAtf) {
    if (referenceAtf == null) {
      throw new IllegalArgumentException("Argument referenceAtf cannot be null");
    } else {
      return getResourceLeakChecker(referenceAtf.getChecker());
    }
  }

  public static @NonNull ResourceLeakChecker getResourceLeakChecker(
      SourceChecker referenceChecker) {
    if (referenceChecker == null) {
      throw new IllegalArgumentException("Argument referenceChecker cannot be null");
    }

    String className = referenceChecker.getClass().getSimpleName();
    if ("ResourceLeakChecker".equals(className)) {
      return (ResourceLeakChecker) referenceChecker;
    } else if ("RLCCalledMethodsChecker".equals(className)
        || "MustCallChecker".equals(className)
        || "MustCallNoCreatesMustCallForChecker".equals(className)) {
      return getResourceLeakChecker(referenceChecker.getParentChecker());
    } else {
      throw new IllegalArgumentException(
          "Argument referenceChecker to ResourceLeakUtils#getResourceLeakChecker(referenceChecker) expected to be an RLC checker but is "
              + className);
    }
  }

  public static @NonNull RLCCalledMethodsChecker getRLCCalledMethodsChecker(
      AnnotatedTypeFactory referenceAtf) {
    if (referenceAtf == null) {
      throw new IllegalArgumentException("Argument referenceAtf cannot be null");
    } else {
      return getRLCCalledMethodsChecker(referenceAtf.getChecker());
    }
  }

  public static @NonNull RLCCalledMethodsChecker getRLCCalledMethodsChecker(
      SourceChecker referenceChecker) {
    if (referenceChecker == null) {
      throw new IllegalArgumentException("Argument referenceChecker cannot be null");
    }

    String className = referenceChecker.getClass().getSimpleName();
    if ("RLCCalledMethodsChecker".equals(className)) {
      return (RLCCalledMethodsChecker) referenceChecker;
    } else if ("ResourceLeakChecker".equals(className)) {
      return getRLCCalledMethodsChecker(
          referenceChecker.getSubchecker(RLCCalledMethodsChecker.class));
    } else if ("MustCallChecker".equals(className)
        || "MustCallNoCreatesMustCallForChecker".equals(className)) {
      return getRLCCalledMethodsChecker(referenceChecker.getParentChecker());
    } else {
      throw new IllegalArgumentException(
          "Argument referenceChecker to ResourceLeakUtils#getRLCCalledMethodsChecker(referenceChecker) expected to be an RLC checker but is "
              + className);
    }
  }

  /**
   * Returns the checker of the desired class given a checker part of the RLC. Both the targetClass
   * and reference checker must be checkers from the RLC ecosystem, as defined by {@code
   * this.rlcCheckers}.
   *
   * @param targetClass the desired checker class
   * @param referenceChecker the current checker
   * @return the checker of the desired class
   * @throws IllegalArgumentException when either of the arguments is not one of the RLC checkers
   */
  public static @NonNull SourceChecker getChecker(
      Class<? extends SourceChecker> targetClass, SourceChecker referenceChecker) {
    if (!rlcCheckers.contains(targetClass.getCanonicalName())) {
      throw new IllegalArgumentException(
          "Argument targetClass to ResourceLeakUtils#getChecker(targetClass, referenceChecker) expected to be an RLC checker but is "
              + targetClass.getCanonicalName());
    }
    Class<?> refClass = referenceChecker.getClass();
    if (refClass == targetClass) {
      // base case -- we found the desired checker
      return referenceChecker;
    } else if (refClass == MustCallChecker.class) {
      return getChecker(targetClass, referenceChecker.getParentChecker());
    } else if (refClass == ResourceLeakChecker.class) {
      return getChecker(targetClass, referenceChecker.getSubchecker(RLCCalledMethodsChecker.class));
    } else if (refClass == RLCCalledMethodsChecker.class) {
      if (targetClass == MustCallChecker.class) {
        MustCallChecker mcc = referenceChecker.getSubchecker(MustCallChecker.class);
        return mcc != null
            ? mcc
            : referenceChecker.getSubchecker(MustCallNoCreatesMustCallForChecker.class);
      } else {
        return getChecker(targetClass, referenceChecker.getParentChecker());
      }
    } else {
      throw new IllegalArgumentException(
          "Argument referenceChecker to ResourceLeakUtils#getChecker(targetClass, referenceChecker) expected to be an RLC checker but is "
              + refClass.getCanonicalName());
    }
  }

  /**
   * Returns the list of mustcall obligations for the given {@code TypeMirror} upper bound (either
   * the type variable itself if it is concrete or the upper bound if its a wildcard or generic).
   *
   * <p>If the type variable has no upper bound, for instance if it is a wildcard with no extends
   * clause the method returns null
   *
   * @param type the {@code TypeMirror}
   * @param mcAtf the {@code MustCallAnnotatedTypeFactory} to get the {@code MustCall} type
   * @return the list of mustcall obligations for the upper bound of {@code type} or null if the
   *     upper bound is null.
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
    TypeElement typeElement = TypesUtils.getTypeElement(type);
    AnnotationMirror imcAnnotation =
        mcAtf.getDeclAnnotation(typeElement, InheritableMustCall.class);
    AnnotationMirror mcAnnotation = mcAtf.getDeclAnnotation(typeElement, MustCall.class);
    Set<String> mcValues = new HashSet<>();
    if (mcAnnotation != null) {
      mcValues.addAll(
          AnnotationUtils.getElementValueArray(
              mcAnnotation, mcAtf.getMustCallValueElement(), String.class));
    }
    if (imcAnnotation != null) {
      mcValues.addAll(
          AnnotationUtils.getElementValueArray(
              imcAnnotation, mcAtf.getInheritableMustCallValueElement(), String.class));
    }
    return new ArrayList<>(mcValues);
  }

  /**
   * Returns whether the given {@link TypeMirror} is an instance of a collection (subclass). This is
   * determined by getting the class of the TypeMirror and checking whether it is assignable from
   * Collection.
   *
   * @param type the TypeMirror
   * @return whether type is an instance of a collection (subclass)
   */
  public static boolean isCollection(TypeMirror type) {
    if (type == null) return false;
    Class<?> elementRawType = TypesUtils.getClassFromType(type);
    if (elementRawType == null) return false;
    return Collection.class.isAssignableFrom(elementRawType);
  }

  /**
   * Returns whether the given Element is a java.util.Collection type by checking whether the raw
   * type of the element is assignable from java.util.Collection. Returns false if element is null,
   * or has no valid type.
   *
   * @param element the element
   * @param atf an AnnotatedTypeFactory to get the annotated type of the element
   * @return whether the given element is a Java.util.Collection type
   */
  public static boolean isCollection(Element element, AnnotatedTypeFactory atf) {
    if (element == null) return false;
    AnnotatedTypeMirror elementTypeMirror = atf.getAnnotatedType(element).getErased();
    if (elementTypeMirror == null || elementTypeMirror.getUnderlyingType() == null) return false;
    return isCollection(elementTypeMirror.getUnderlyingType());
  }

  /**
   * Returns whether the given Tree is a java.util.Collection type by checking whether the raw type
   * of the element is assignable from java.util.Collection. Returns false if tree is null, or has
   * no valid type.
   *
   * @param tree the tree
   * @param atf an AnnotatedTypeFactory to get the annotated type of the element
   * @return whether the given Tree is a Java.util.Collection type
   */
  public static boolean isCollection(Tree tree, AnnotatedTypeFactory atf) {
    if (tree == null) return false;
    Element element = TreeUtils.elementFromTree(tree);
    return isCollection(element, atf);
  }

  /**
   * Returns whether the given {@link TypeMirror} is an instance of Iterator (subtype). This is
   * determined by getting the class of the TypeMirror and checking whether it is assignable from
   * Iterator.
   *
   * @param type the TypeMirror
   * @return whether type is an instance of Iterator
   */
  public static boolean isIterator(TypeMirror type) {
    if (type == null) return false;
    Class<?> elementRawType = TypesUtils.getClassFromType(type);
    if (elementRawType == null) return false;
    return Iterator.class.isAssignableFrom(elementRawType);
  }

  /**
   * Returns whether the given Element is a java.util.Iterator type by checking whether the raw type
   * of the element is assignable from java.util.Iterator. Returns false if element is null, or has
   * no valid type.
   *
   * @param element the element
   * @param atf an AnnotatedTypeFactory to get the annotated type of the element
   * @return whether the given element is a Java.util.Iterator type
   */
  public static boolean isIterator(Element element, AnnotatedTypeFactory atf) {
    if (element == null) return false;
    AnnotatedTypeMirror elementTypeMirror = atf.getAnnotatedType(element).getErased();
    if (elementTypeMirror == null || elementTypeMirror.getUnderlyingType() == null) return false;
    return isIterator(elementTypeMirror.getUnderlyingType());
  }

  /**
   * Returns whether the given Tree is a java.util.Iterator type by checking whether the raw type of
   * the element is assignable from java.util.Iterator. Returns false if tree is null, or has no
   * valid type.
   *
   * @param tree the tree
   * @param atf an AnnotatedTypeFactory to get the annotated type of the element
   * @return whether the given Tree is a Java.util.Iterator type
   */
  public static boolean isIterator(Tree tree, AnnotatedTypeFactory atf) {
    if (tree == null) return false;
    if (tree instanceof MethodInvocationTree) {
      tree = ((MethodInvocationTree) tree).getMethodSelect();
    }
    Element element = TreeUtils.elementFromTree(tree);
    return isIterator(element, atf);
  }

  /**
   * Returns whether the given Node is a java.util.Iterator type by checking whether the raw type of
   * the element is assignable from java.util.Iterator. If node is a method invocation or access,
   * its return type is analyzed instead. Returns false if tree is null, or has no valid type.
   *
   * @param node the node
   * @param atf an AnnotatedTypeFactory to get the annotated type of the element
   * @return whether the given Node is a Java.util.Iterator type
   */
  public static boolean isIterator(Node node, AnnotatedTypeFactory atf) {
    if (node == null) return false;
    if (node instanceof MethodInvocationNode) {
      node = ((MethodInvocationNode) node).getTarget();
    }
    if (node instanceof MethodAccessNode) {
      return isIterator(((MethodAccessNode) node).getMethod().getReturnType());
    }
    return isIterator(node.getTree(), atf);
  }
}
