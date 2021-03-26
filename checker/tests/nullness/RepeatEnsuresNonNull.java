import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

public class RepeatEnsuresNonNull {

  protected @Nullable String value1;
  protected @Nullable String value2;
  protected @Nullable String value3;

  public boolean func1() {
    value1 = "value1";
    value2 = "value2";
    value3 = "value3";
    return true;
  }

  public void func2() {
    value1 = "value1";
    value2 = "value2";
    value3 = "value3";
  }

  @EnsuresNonNullIf(
      expression = {"value1", "value2"},
      result = true)
  @EnsuresNonNullIf(expression = "value3", result = true)
  public boolean client1() {
    return withcondpostconditionsfunc1();
  }

  @EnsuresNonNull("value1")
  @EnsuresNonNull(value = {"value2", "value3"})
  public void client2() {
    withpostconditionsfunc2();
  }

  @EnsuresNonNullIf.List({
    @EnsuresNonNullIf(expression = "value1", result = true),
    @EnsuresNonNullIf(expression = "value2", result = true),
  })
  @EnsuresNonNullIf(expression = "value3", result = true)
  public boolean client3() {
    return withcondpostconditionfunc1();
  }

  @EnsuresNonNull.List({@EnsuresNonNull("value1"), @EnsuresNonNull("value2")})
  @EnsuresNonNull("value3")
  public void client4() {
    withpostconditionfunc2();
  }

  @EnsuresNonNullIf(
      expression = {"value1", "value2"},
      result = true)
  @EnsuresNonNullIf(expression = "value3", result = true)
  public boolean withcondpostconditionsfunc1() {
    value1 = "value1";
    value2 = "value2";
    value3 = "value3";
    return true;
  }

  @EnsuresNonNull("value1")
  @EnsuresNonNull(value = {"value2", "value3"})
  public void withpostconditionsfunc2() {
    value1 = "value1";
    value2 = "value2";
    value3 = "value3";
  }

  @EnsuresNonNullIf.List({
    @EnsuresNonNullIf(expression = "value1", result = true),
    @EnsuresNonNullIf(expression = "value2", result = true),
  })
  @EnsuresNonNullIf(expression = "value3", result = true)
  public boolean withcondpostconditionfunc1() {
    value1 = "value1";
    value2 = "value2";
    value3 = "value3";
    return true;
  }

  @EnsuresNonNull.List({@EnsuresNonNull("value1"), @EnsuresNonNull("value2")})
  @EnsuresNonNull("value3")
  public void withpostconditionfunc2() {
    value1 = "value1";
    value2 = "value2";
    value3 = "value3";
  }
}
