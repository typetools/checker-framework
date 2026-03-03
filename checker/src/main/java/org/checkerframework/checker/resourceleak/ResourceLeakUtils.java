package org.checkerframework.checker.resourceleak;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
        || referenceChecker instanceof MustCallChecker) {
      return getResourceLeakChecker(referenceChecker.getParentChecker());
    } else {
      throw new TypeSystemError(
          "Bad argument to ResourceLeakUtils#getResourceLeakChecker(): "
              + (referenceChecker == null ? "null" : referenceChecker.getClass().getSimpleName()));
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
}
