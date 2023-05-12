import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.Unsigned;

public final class IrrelevantAnnotationsTest {

  // :: error: (anno.on.irrelevant)
  @Signed Boolean b1;

  // :: error: (anno.on.irrelevant)
  @Unsigned Boolean b2;

  @Signed Object o1;

  @Unsigned Object o2;
}
