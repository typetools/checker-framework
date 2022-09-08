// Test case for https://github.com/typetools/checker-framework/issues/5245

// @skip-test until the bug is fixed

import java.util.List;

class CFRepro<E> {
  final CFRepro<List<String>> repro = new CFRepro<>(List.of());

  <V extends E> CFRepro(V unknownObj) {}
}
