// A suggested or inferred purity annotation outlives the command line that produced it, so
// -AsuggestPureMethods must not suggest an annotation that holds only because of an -Aassume*
// option.  This file is checked with -AassumeSideEffectFree, -AassumeDeterministic, and
// -AassumePureGetters, none of which may affect the suggestions below.

public class SuggestionsIgnoreAssumptions {

  int field = 0;

  // :: warning: [purity.more.pure]
  int getField() {
    return field;
  }

  void impure() {
    field++;
  }

  /** Not suggested as pure: {@code impure()} has no purity annotation. */
  int callUnannotated() {
    impure();
    return field;
  }

  /** Not suggested as pure: {@code getField()} is pure only under -AassumePureGetters. */
  int callGetter() {
    return getField();
  }

  // :: warning: [purity.more.pure]
  int noCalls() {
    return 1;
  }
}
