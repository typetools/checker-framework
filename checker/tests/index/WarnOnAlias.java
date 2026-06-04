import org.checkerframework.checker.signedness.qual.SignedPositive;

public class WarnOnAlias {
  // :: error: (anno.on.irrelevant)
  @SignedPositive String s;
}
