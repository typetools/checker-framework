import org.checkerframework.common.value.qual.StringVal;

public class Issue6125B {
  // :: error: (assignment)
  @StringVal("hello") String s = "world";
}
