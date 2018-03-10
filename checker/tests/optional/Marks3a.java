import java.util.List;
import java.util.Optional;

/**
 * Test case for rule #3: "Prefer alternative APIs over Optional.isPresent() and Optional.get()."
 */
public class Marks3a {

    class Customer {
        int getID() {
            return 42;
        }

        String getName() {
            return "Fozzy Bear";
        }
    }

    String customerNameByID_acceptable(List<Customer> custList, int custID) {
        Optional<Customer> opt = custList.stream().filter(c -> c.getID() == custID).findFirst();

        // :: warning: (prefer.map.and.orelse)
        return opt.isPresent() ? opt.get().getName() : "UNKNOWN";
    }

    String customerNameByID_better(List<Customer> custList, int custID) {
        Optional<Customer> opt = custList.stream().filter(c -> c.getID() == custID).findFirst();

        return opt.map(Customer::getName).orElse("UNKNOWN");
    }
}
