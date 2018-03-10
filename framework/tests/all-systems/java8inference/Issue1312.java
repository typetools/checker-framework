// Test case for Issue 1312.
// https://github.com/typetools/checker-framework/issues/1312

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

class SimpleEntry1312<K, V> {
    SimpleEntry1312(K k, V v) {}
}

@SuppressWarnings("") // check for crashes
class Issue1312 {
    Map<SimpleEntry1312, List<SimpleEntry1312>> x =
            Stream.of(Stream.of(new SimpleEntry1312<>("A", "B")))
                    .flatMap(Function.identity())
                    .collect(Collectors.groupingBy(e -> e));
}
