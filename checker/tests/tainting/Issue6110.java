import java.util.EnumSet;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

class Issue6110 {
  @SuppressWarnings("assignment") // #2156
  enum TestEnum {
    ONE,
    @Untainted TWO
  }

  static void test(Enum<@Untainted TestEnum> o) {

    o.compareTo(TestEnum.ONE);
    o.compareTo(TestEnum.TWO);

    EnumSet<@Tainted TestEnum> s1 = EnumSet.of(TestEnum.ONE);
    EnumSet<@Untainted TestEnum> s2 = EnumSet.<@Untainted TestEnum>of(TestEnum.TWO);
  }
}
