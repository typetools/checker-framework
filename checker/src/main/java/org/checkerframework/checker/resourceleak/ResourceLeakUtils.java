package org.checkerframework.checker.resourceleak;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.checkerframework.checker.mustcall.MustCallChecker;
import org.checkerframework.checker.mustcall.MustCallNoCreatesMustCallForChecker;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.rlccalledmethods.RLCCalledMethodsChecker;
import org.checkerframework.framework.source.SourceChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;

/**
 * Collection of static utility functions related to the various (sub-) checkers within the
 * ResourceLeakChecker.
 */
public class ResourceLeakUtils {

  /** Shouldn't be instantiated, pure utility class. */
  public ResourceLeakUtils() {}

  /** List of checker names associated with the Resource Leak Checker. */
  public static List<String> rlcCheckers =
      new ArrayList<>(
          Arrays.asList(
              ResourceLeakChecker.class.getCanonicalName(),
              RLCCalledMethodsChecker.class.getCanonicalName(),
              MustCallChecker.class.getCanonicalName(),
              MustCallNoCreatesMustCallForChecker.class.getCanonicalName()));

  /**
   * Given a type factory part of the resource leak ecosystem, returns the {@link
   * ResourceLeakChecker} in the checker hierarchy.
   *
   * @param referenceAtf the type factory to retrieve the {@link ResourceLeakChecker} from
   * @return the {@link ResourceLeakChecker} in the checker hierarchy
   */
  public static @NonNull ResourceLeakChecker getResourceLeakChecker(
      AnnotatedTypeFactory referenceAtf) {
    if (referenceAtf == null) {
      throw new IllegalArgumentException("Argument referenceAtf cannot be null");
    } else {
      return getResourceLeakChecker(referenceAtf.getChecker());
    }
  }

  /**
   * Given a checker part of the resource leak ecosystem, returns the {@link ResourceLeakChecker} in
   * the checker hierarchy.
   *
   * @param referenceChecker the checker to retrieve the {@link ResourceLeakChecker} from
   * @return the {@link ResourceLeakChecker} in the checker hierarchy
   */
  public static @NonNull ResourceLeakChecker getResourceLeakChecker(
      SourceChecker referenceChecker) {
    if (referenceChecker == null) {
      throw new IllegalArgumentException("Argument referenceChecker cannot be null");
    }

    if (referenceChecker instanceof ResourceLeakChecker) {
      return (ResourceLeakChecker) referenceChecker;
    } else if (referenceChecker instanceof RLCCalledMethodsChecker
        || referenceChecker instanceof MustCallChecker) {
      return getResourceLeakChecker(referenceChecker.getParentChecker());
    } else {
      throw new IllegalArgumentException(
          "Argument referenceChecker to ResourceLeakUtils#getResourceLeakChecker(referenceChecker) expected to be an RLC checker but is "
              + referenceChecker.getClass().getSimpleName());
    }
  }

  /**
   * Given a type factory part of the resource leak ecosystem, returns the {@link
   * RLCCalledMethodsChecker} in the checker hierarchy.
   *
   * @param referenceAtf the type factory to retrieve the {@link RLCCalledMethodsChecker} from
   * @return the {@link RLCCalledMethodsChecker} in the checker hierarchy
   */
  public static @NonNull RLCCalledMethodsChecker getRLCCalledMethodsChecker(
      AnnotatedTypeFactory referenceAtf) {
    if (referenceAtf == null) {
      throw new IllegalArgumentException("Argument referenceAtf cannot be null");
    } else {
      return getRLCCalledMethodsChecker(referenceAtf.getChecker());
    }
  }

  /**
   * Given a checker part of the resource leak ecosystem, returns the {@link
   * RLCCalledMethodsChecker} in the checker hierarchy.
   *
   * @param referenceChecker the checker to retrieve the {@link RLCCalledMethodsChecker} from
   * @return the {@link RLCCalledMethodsChecker} in the checker hierarchy
   */
  public static @NonNull RLCCalledMethodsChecker getRLCCalledMethodsChecker(
      SourceChecker referenceChecker) {
    if (referenceChecker == null) {
      throw new IllegalArgumentException("Argument referenceChecker cannot be null");
    }

    if (referenceChecker instanceof RLCCalledMethodsChecker) {
      return (RLCCalledMethodsChecker) referenceChecker;
    } else if (referenceChecker instanceof ResourceLeakChecker) {
      return getRLCCalledMethodsChecker(
          referenceChecker.getSubchecker(RLCCalledMethodsChecker.class));
    } else if (referenceChecker instanceof MustCallChecker) {
      return getRLCCalledMethodsChecker(referenceChecker.getParentChecker());
    } else {
      throw new IllegalArgumentException(
          "Argument referenceChecker to ResourceLeakUtils#getRLCCalledMethodsChecker(referenceChecker) expected to be an RLC checker but is "
              + referenceChecker.getClass().getSimpleName());
    }
  }
}
