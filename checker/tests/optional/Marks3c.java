import java.util.Optional;
import org.checkerframework.dataflow.qual.Pure;

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

  Optional<Customer> getOptCustomerImpure() {
    System.out.println("Side Effect");
    return Optional.ofNullable(new Customer());
  }

  @Pure
  Optional<Customer> getOptCustomerPure() {
    return Optional.ofNullable(new Customer());
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

  void m1() {
    if (getOptCustomerImpure().isPresent()) {
      // Fine, calling `getOptCustomerImpure()` once instead of twice may not be
      // semantics-preserving
      Customer c = identity(getOptCustomerImpure().get());
    } else {
    }
  }

  void m2() {
    // :: warning: (prefer.map.and.orelse)
    if (getOptCustomerPure().isPresent()) {
      Customer c = identity(getOptCustomerPure().get());
    } else {
    }
  }
}
