package org.checkerframework.framework.util;

import java.util.Map;
import org.checkerframework.common.basetype.BaseTypeChecker;

/**
 * Perform purity checking only.
 *
 * @checker_framework.manual #type-refinement-purity Side effects, determinism, purity, and
 *     flow-sensitive analysis
 */
public class PurityChecker extends BaseTypeChecker {
  // Other than forcing -AcheckPurityAnnotations, there is no implementation here.
  // It uses functionality from BaseTypeChecker, which itself calls
  // dataflow's purity implementation.

  /** Creates a PurityChecker. */
  public PurityChecker() {}

  /**
   * {@inheritDoc}
   *
   * <p>Checking purity annotations is the entire purpose of this checker, so this implementation
   * behaves as if {@code -AcheckPurityAnnotations} was supplied on the command line.
   */
  @Override
  public Map<String, String> getOptions() {
    Map<String, String> options = super.getOptions();
    if (!options.containsKey("checkPurityAnnotations")) {
      options.put("checkPurityAnnotations", "true");
    }
    return options;
  }
}
