// @skip-test Fails, but commented out to avoid breaking the build
public class GenericCast<T> {

  @SuppressWarnings("unchecked")
  T invalid_t = (T) new Object();

  T current = invalid_t;

}
