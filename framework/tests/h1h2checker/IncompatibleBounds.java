package h1h2checker;

import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.testchecker.h1h2checker.quals.*;

/**
 * This test is solely to ensure that if bounds in type parameters and wildcards are invalid then
 * they are reported as such using a "bound" error.
 *
 * <p>A valid bound is one with LOWER_BOUND annotations that subtypes of UPPER_BOUND annotations.
 */

// set the defaults in the H2 hierarchy such that do not report errors in this test
@DefaultQualifier(
    value = H2Top.class,
    locations = {TypeUseLocation.UPPER_BOUND})
@DefaultQualifier(
    value = H2Bot.class,
    locations = {TypeUseLocation.LOWER_BOUND})
public class IncompatibleBounds {

  // The bounds below are valid
  class TopToBottom<@H1Bot T extends @H1Top Object> {}

  class TopToH1S1<@H1S1 TT extends @H1Top Object> {}

  class H1S1ToBot<@H1Bot TTT extends @H1S1 Object> {}

  class H1S1ToH1S1<@H1S1 TTTT extends @H1S1 Object> {}

  class ValidContext {
    TopToBottom<@H1Bot ? extends @H1Top Object> topToBot;
    TopToH1S1<@H1S1 ? extends @H1Top Object> topToH1S1;
    H1S1ToBot<@H1Bot ? extends @H1S1 Object> h1S1ToBot;
    H1S1ToH1S1<@H1S1 ? extends @H1S1 Object> h1S1ToH1S1;
  }

  // invalid combinations
  // :: error: (bound)
  class BottomToTop<@H1Top U extends @H1Bot Object> {}
  // :: error: (bound)
  class H1S1ToTop<@H1Top UU extends @H1S1 Object> {}
  // :: error: (bound)
  class BottomToH1S1<@H1S1 UUU extends @H1Bot Object> {}
  // :: error: (bound)
  class H1S2ToH1S1<@H1S1 UUUU extends @H1S2 Object> {}

  class InvalidContext {
    // :: error: (bound)
    BottomToTop<@H1Top ? extends @H1Bot Object> bottomToTop;
    // :: error: (bound)
    H1S1ToTop<@H1Top ? extends @H1S1 Object> h1S1ToTop;
    // :: error: (bound)
    BottomToH1S1<@H1S1 ? extends @H1Bot Object> bottomToH1S1;
    // :: error: (bound)
    H1S2ToH1S1<@H1S1 ? extends @H1S2 Object> h1S2ToH1S1;
  }
}
