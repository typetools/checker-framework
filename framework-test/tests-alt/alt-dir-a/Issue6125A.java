import org.checkerframework.common.value.qual.StringVal;

public class Issue6125A {
  // :: error: (assignment)
  @StringVal("hello") String s = "goodbye";
}
