import java.util.Optional;

/**
 * Test case for rule #3: "Prefer alternative APIs over Optional.isPresent() and Optional.get()."
 */
@SuppressWarnings("optional:parameter")
public class Marks3c {

  class Customer {
    int getID() {
      return 42;
    }

    String getName() {
      return "Fozzy Bear";
    }
  }

  Customer identity(Customer c) {
    return c;
  }

  void main(Optional<Customer> optCustomer) {
    // :: warning: (prefer.map.and.orelse)
    if (optCustomer.isPresent()) {
      Customer c = identity(optCustomer.get());
    } else {
    }

    // :: warning: (prefer.map.and.orelse)
    if (!optCustomer.isPresent()) {
    } else {
      Customer c = identity(optCustomer.get());
    }
  }
}
