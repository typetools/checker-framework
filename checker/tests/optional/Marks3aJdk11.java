// @below-java11-jdk-skip-test

import java.util.List;
import java.util.Optional;

/**
 * Test case for rule #3: "Prefer alternative APIs over Optional.isPresent() and Optional.get()."
 */
public class Marks3aJdk11 {

    class Customer {
        int getID() {
            return 42;
        }

        String getName() {
            return "Fozzy Bear";
        }
    }

    String customerNameByID_acceptable3(List<Customer> custList, int custID) {
        Optional<Customer> opt = custList.stream().filter(c -> c.getID() == custID).findFirst();

        // :: warning: (prefer.map.and.orelse)
        return opt.isEmpty() ? "UNKNOWN" : opt.get().getName();
    }
}
