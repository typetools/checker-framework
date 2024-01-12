import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MapEntryGetFails {
  void test(Stream<List<Integer>> listStream) {
    listStream.collect(Collectors.groupingByConcurrent(l -> l.get(1))).entrySet().stream()
        .sorted(Entry.comparingByKey());
  }
}
