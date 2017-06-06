package testlib.nontopdefault;

import org.checkerframework.common.basetype.BaseTypeChecker;

/* Hierarchy:
 *    NTDTop (default for local variables, implicit upper bound, and receiver)
 *    /     \
 * NTDSide  NTDMiddle (default in hierarchy, default for exceptions and resource variables)
 *   \       /
 *   NTDBottom (default for implicit and explicit lower bounds, implicit for null literal and Void.class)
 */

public class NTDChecker extends BaseTypeChecker {}
