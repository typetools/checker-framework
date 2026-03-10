package open.crash;

import java.util.stream.Stream;

@SuppressWarnings("all") // Just check for crashes.
public class Issue7079 {

  public final <Q> void updateColumns(Stream<ColumnName<Q>> stream) {
    Stream<String> columns1 = stream.map(ColumnName::name);
  }

  public interface ColumnName<T> {
    String name();
  }
}
