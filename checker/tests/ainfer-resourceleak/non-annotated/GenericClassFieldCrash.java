/**
 * This test case targets the scenario where a generic field's @MustCall obligation might be
 * unknown, ensuring the framework does not erroneously infer @Owning annotations or throw
 * AssertionErrors when processing such cases.
 */
class Generic<T> {
  public T data;

  public Generic(T data) {
    this.data = data;
  }
}

public class GenericClassFieldCrash {
  private void onPacket(Generic foo) {
    String.format("socket received: data '%s'", foo.data);
  }
}
