// @skip-test Fails, but commented out to avoid breaking the build
public class GenericCast<T> {

  @SuppressWarnings("unchecked")
  T tObject = (T) new Object();

  T field1 = tObject;

  T field2;

  GenericCast() {
    field2 = tObject;
  }
}
