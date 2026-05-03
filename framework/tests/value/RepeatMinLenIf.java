import org.checkerframework.common.value.qual.EnsuresMinLenIf;

public class RepeatMinLenIf {

  protected String a;
  protected String b;
  protected String c;

  public boolean func1() {
    a = "checker";
    c = "framework";
    b = "opensource";
    return true;
  }

  @EnsuresMinLenIf(
      expression = {"a", "b"},
      targetValue = 6,
      result = true)
  @EnsuresMinLenIf(expression = "c", targetValue = 4, result = true)
  public boolean client1() {
    return withCondPostconditionsFunc1();
  }

  @EnsuresMinLenIf.List({
    @EnsuresMinLenIf(expression = "a", targetValue = 6, result = true),
    @EnsuresMinLenIf(expression = "b", targetValue = 6, result = true)
  })
  @EnsuresMinLenIf(expression = "c", targetValue = 4, result = true)
  public boolean client2() {
    return withCondPostconditionFunc1();
  }

  @EnsuresMinLenIf(
      expression = {"a", "b"},
      targetValue = 6,
      result = true)
  @EnsuresMinLenIf(expression = "c", targetValue = 4, result = true)
  public boolean withCondPostconditionsFunc1() {
    a = "checker";
    c = "framework";
    b = "opensource";
    return true;
  }

  @EnsuresMinLenIf.List({
    @EnsuresMinLenIf(expression = "a", targetValue = 6, result = true),
    @EnsuresMinLenIf(expression = "b", targetValue = 6, result = true)
  })
  @EnsuresMinLenIf(expression = "c", targetValue = 4, result = true)
  public boolean withCondPostconditionFunc1() {
    a = "checker";
    c = "framework";
    b = "opensource";
    return true;
  }
}
