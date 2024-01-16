import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;

public class UncheckedConversionInference {

  private static class TransposeTable<
          C extends @Nullable Object, R extends @Nullable Object, V extends @Nullable Object>
      extends AbstractTable<C, R, V> {

    private static final Function<Cell<?, ?, ?>, Cell<?, ?, ?>> TRANSPOSE_CELL =
        new Function<Cell<?, ?, ?>, Cell<?, ?, ?>>() {
          @Override
          public Cell<?, ?, ?> apply(Cell<?, ?, ?> cell) {
            throw new RuntimeException();
          }
        };
    final Table<R, C, V> original;

    TransposeTable(Table<R, C, V> original) {
      this.original = original;
    }

    @SuppressWarnings("unchecked")
    @Override
    Iterator<Cell<C, R, V>> cellIterator() {
      return transform(original.cellSet().iterator(), (Function) TRANSPOSE_CELL);
    }

    public static <F extends @Nullable Object, T extends @Nullable Object> Iterator<T> transform(
        Iterator<F> fromIterator, Function<? super F, ? extends T> function) {
      throw new RuntimeException();
    }

    @Override
    public Set<Cell<C, R, V>> cellSet() {
      throw new RuntimeException();
    }
  }

  abstract static class AbstractTable<
          R extends @Nullable Object, C extends @Nullable Object, V extends @Nullable Object>
      implements Table<R, C, V> {

    abstract Iterator<Cell<R, C, V>> cellIterator();
  }

  public interface Table<
      R extends @Nullable Object, C extends @Nullable Object, V extends @Nullable Object> {

    Set<Cell<R, C, V>> cellSet();

    interface Cell<
        R extends @Nullable Object, C extends @Nullable Object, V extends @Nullable Object> {}
  }
}
