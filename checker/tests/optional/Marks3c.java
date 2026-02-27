import java.util.List;
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

  String customerNameByID1(Optional<Customer> opt) {
    // better: return opt.map(Customer::getName).orElse("UNKNOWN");
    // :: warning: (prefer.map.and.orelse)
    return opt.isPresent() ? opt.get().getName() : "UNKNOWN";
  }

  String customerNameByID2(List<Customer> custList, int custID) {
    Optional<Customer> opt = custList.stream().filter(c -> c.getID() == custID).findFirst();
    // better: return opt.map(Customer::getName).orElse("UNKNOWN");
    // :: warning: (prefer.map.and.orelse)
    return opt.isPresent() ? opt.get().getName() : "UNKNOWN";
  }

  void main(Optional<Customer> optCustomer) {
    // :: warning: (prefer.map)
    if (optCustomer.isPresent()) {
      Customer c = identity(optCustomer.get());
    } else {
    }

    if (optCustomer.isPresent()) {
      Customer c = identity(optCustomer.get());
    } else {
      System.out.println("hello world");
    }

    // :: warning: (prefer.map)
    if (!optCustomer.isPresent()) {
    } else {
      Customer c = identity(optCustomer.get());
    }
  }

  void m1() {
    if (getOptCustomerImpure().isPresent()) {
      // No "prefer.map.and.orelse" warning because calling `getOptCustomerImpure()` once
      // instead of twice may not be semantics-preserving.
      Customer c = identity(getOptCustomerImpure().get());
    } else {
    }
  }

  void m2() {
    // :: warning: (prefer.map)
    if (getOptCustomerPure().isPresent()) {
      Customer c = identity(getOptCustomerPure().get());
    } else {
    }
  }
}
