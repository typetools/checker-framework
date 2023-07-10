package issue6076;

import java.util.Collection;
import java.util.Set;

public class Issue6076 {

  public SSTableReaderLoadingBuilder<BtiTableReader, BtiTableReader.Builder> loadingBuilder(
      Descriptor descriptor, TableMetadataRef tableMetadataRef, Set<Component> components) {
    return new BtiTableReaderLoadingBuilder(
        new SSTable.Builder<>(descriptor)
            .setTableMetadataRef(tableMetadataRef)
            .setComponents(components));
  }

  public static class Component {}

  public static class Descriptor {}

  public static final class TableMetadataRef {}

  public static class BtiTableReaderLoadingBuilder
      extends SortedTableReaderLoadingBuilder<BtiTableReader, BtiTableReader.Builder> {
    public BtiTableReaderLoadingBuilder(SSTable.Builder<?, ?> builder) {}
  }

  public static class BtiTableReader extends SSTableReaderWithFilter {
    public static class Builder extends SSTableReaderWithFilter.Builder<BtiTableReader, Builder> {}
  }

  public abstract static class SSTableReaderWithFilter extends SSTableReader {
    public abstract static class Builder<R extends SSTableReaderWithFilter, B extends Builder<R, B>>
        extends SSTableReader.Builder<R, B> {}
  }

  @SuppressWarnings("all") // Just check for crashes.
  public abstract static class SSTable {
    public static class Builder<S extends SSTable, B extends Builder<S, B>> {
      public Builder() {}

      public Builder(Descriptor descriptor) {}

      @SuppressWarnings("unchecked")
      public B setTableMetadataRef(TableMetadataRef ref) {
        return (B) this;
      }

      @SuppressWarnings("unchecked")
      public B setComponents(Collection<Component> components) {
        return (B) this;
      }
    }
  }

  public abstract static class SSTableReaderLoadingBuilder<
      R extends SSTableReader, B extends SSTableReader.Builder<R, B>> {}

  public abstract static class SortedTableReaderLoadingBuilder<
          R extends SSTableReader, B extends SSTableReader.Builder<R, B>>
      extends SSTableReaderLoadingBuilder<R, B> {}

  public abstract static class SSTableReader extends SSTable {
    public abstract static class Builder<R extends SSTableReader, B extends Builder<R, B>>
        extends SSTable.Builder<R, B> {}
  }
}
