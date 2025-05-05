package org.checkerframework.checker.resourceleak;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.checkerframework.checker.collectionownership.CollectionOwnershipAnnotatedTypeFactory;
import org.checkerframework.checker.collectionownership.CollectionOwnershipChecker;
import org.checkerframework.checker.mustcall.MustCallAnnotatedTypeFactory;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.checker.mustcall.MustCallNoCreatesMustCallForChecker;
import org.checkerframework.checker.rlccalledmethods.RLCCalledMethodsChecker;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.javacutil.TypeSystemError;

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
              + " ResourceLeakUtils#getCollectionOwnershipAnnotatedTypeFactory(referenceChecker) expected to"
              + " be an RLC checker but is "
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
}
