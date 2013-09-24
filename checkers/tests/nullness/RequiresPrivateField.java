import checkers.nullness.quals.*;

// @skip-test Test case for future feature:  @RequiresNonNull is permitted to access a private field that is annotated with @SpecPublic

public class RequiresPrivateField {

  @RequiresNonNull("PptCombined.assemblies")
  public void testFindIntermediateBlocks1() {
    // no body
  }

}


class PptCombined {

  @SpecPublic
  private static @MonotonicNonNull String assemblies = null;

}
