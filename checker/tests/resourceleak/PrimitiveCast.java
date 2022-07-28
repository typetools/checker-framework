import java.util.List;

public class PrimitiveCast {

  char foo(List<?> values) {
    for (Object o : values) {
      return (char) o;
    }
    return 'A';
  }
}
