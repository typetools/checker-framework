import java.util.EnumSet;
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

class Issue6110 {
  enum TestEnum {
    ONE,
    @Untainted TWO
  }

  static void test(Enum<@Untainted TestEnum> o) {

    @Tainted TestEnum e = TestEnum.ONE;
    o.compareTo(TestEnum.ONE);
    o.compareTo(TestEnum.TWO);

    EnumSet<@Tainted TestEnum> s1 = EnumSet.of(TestEnum.ONE);
    // :: error: (assignment)
    EnumSet<@Untainted TestEnum> s2 = EnumSet.of(TestEnum.ONE);
    EnumSet<@Untainted TestEnum> s3 = EnumSet.of(TestEnum.TWO);
  }
}
