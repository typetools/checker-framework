import org.checkerframework.checker.confidential.qual.PolyConfidential;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class PuritySubstring {

  @SuppressWarnings({
    "allcheckers:purity", // local StringBuilder
    "lock", // local StringBuilder
    "confidential" // JDK is not yet annotated
  })
  @SideEffectFree
  @Override
  public @PolyConfidential String toString(@PolyConfidential PuritySubstring this) {
    @PolyConfidential StringBuilder sb = new @PolyConfidential StringBuilder();
    sb.append("hello");
    return sb.toString();
  }
}
