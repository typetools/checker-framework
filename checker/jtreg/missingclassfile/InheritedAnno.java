import lib.SubOfQualParam;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

/**
 * Pins down that the {@code @Inherited} declaration annotation on lib.QualParam is still found,
 * even though reading lib.QualParam's supertype fails. See Issue8055.java.
 */
public class InheritedAnno {

  void m(@Untainted SubOfQualParam u) {
    // Assigning @Untainted to @Tainted is an error only because SubOfQualParam inherits
    // @HasQualifierParameter from lib.QualParam, which makes the qualifier invariant.
    @Tainted SubOfQualParam t = u;
  }
}
