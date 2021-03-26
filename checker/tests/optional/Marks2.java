import java.util.List;
import java.util.Optional;

/**
 * Test case for rule #2: "Never use Optional.get() unless you can prove that the Optional is
 * present."
 */
public class Marks2 {

  class Customer {
    int getID() {
      return 42;
    }

    String getName() {
      return "Fozzy Bear";
    }
  }

  String customerNameByID(List<Customer> custList, int custID) {
    Optional<Customer> opt = custList.stream().filter(c -> c.getID() == custID).findFirst();
    // :: error: (method.invocation.invalid)
    return opt.get().getName();
  }
}
