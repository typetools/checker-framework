import java.util.Optional;

/**
 * Test case for rule #3: "Prefer alternative APIs over Optional.isPresent() and Optional.get()."
 */
@SuppressWarnings("optional.parameter")
public class Marks3b {

    class Task {}

    class Executor {
        void runTask(Task t) {}
    }

    Executor executor = new Executor();

    void bad(Optional<Task> oTask) {
        // :: warning: (prefer.ifpresent)
        if (oTask.isPresent()) {
            executor.runTask(oTask.get());
        }
    }

    void better(Optional<Task> oTask) {
        // no warning; better code is possible but has nothing to do with Optional
        oTask.ifPresent(task -> executor.runTask(task));
    }

    void best(Optional<Task> oTask) {
        oTask.ifPresent(executor::runTask);
    }
}
