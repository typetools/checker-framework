import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;
import java.util.stream.Collector;

@SuppressWarnings({"unchecked", "all"})
public class Bug14 {
  private static final Collector<Object, ?, ImmutableList<Object>> TO_IMMUTABLE_LIST =
      Collector.of(
          ImmutableList::<Object>builder,
          ImmutableList.Builder::add,
          ImmutableList.Builder::combine,
          ImmutableList.Builder::build);

  public abstract static class ImmutableList<E> extends ImmutableCollection<E>
      implements List<E>, RandomAccess {
    public static final class Builder<E> {

      public Builder<E> add(E element) {
        return this;
      }

      public Builder<E> add(E... elements) {
        return this;
      }

      public Builder<E> addAll(Iterator<? extends E> elements) {
        return this;
      }

      public ImmutableList<E> build() {
        throw new RuntimeException();
      }

      Builder<E> combine(Builder<E> builder) {
        return this;
      }
    }

    public static <E> Builder<E> builder() {
      return new Builder<E>();
    }
  }

  public abstract static class ImmutableCollection<E> extends AbstractCollection<E>
      implements Serializable {}
}
