// Test case for https://github.com/typetools/checker-framework/issues/5245

import java.util.List;

class Issue5245<E> {
  final Issue5245<List<String>> repro = new Issue5245<>(List.of());

  <V extends E> Issue5245(V unknownObj) {}
}
