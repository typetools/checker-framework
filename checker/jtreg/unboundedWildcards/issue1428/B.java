import java.util.List;

interface B {
  List<L<?>> getItems();
}

interface V {}

interface L<T extends V> {}
