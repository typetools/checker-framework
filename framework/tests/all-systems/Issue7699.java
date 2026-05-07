import java.util.Optional;

@SuppressWarnings("optional.parameter") // true positive.
public class Issue7699 {
  @SuppressWarnings("argument") // TODO: This is a false postive.
  <T> Optional<T> run(Optional<Object> optional, T t) {
    return optional.flatMap(o -> true ? Optional.of(t) : Optional.empty());
  }
}
