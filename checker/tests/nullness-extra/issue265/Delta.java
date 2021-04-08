import java.util.List;

public class Delta<E> {
  List<E> field;

  Delta(List<E> field) {
    this.field = ImmutableList.copyOf(field);
  }
}
