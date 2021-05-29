// @below-java11-jdk-skip-test

import java.util.Optional;

/**
 * Test case for rule #3: "Prefer alternative APIs over Optional.isPresent() and Optional.get()."
 */
@SuppressWarnings("optional.parameter")
public class Marks3bJdk11 {

  class Task {}

  class Executor {
    void runTask(Task t) {}
  }

  Executor executor = new Executor();

  void bad2(Optional<Task> oTask) {
    // :: warning: (prefer.ifpresent)
    if (!oTask.isEmpty()) {
      executor.runTask(oTask.get());
    }
  }

  void bad3(Optional<Task> oTask) {
    // :: warning: (prefer.ifpresent)
    if (oTask.isEmpty()) {
    } else {
      executor.runTask(oTask.get());
    }
  }
}
