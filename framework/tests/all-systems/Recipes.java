import java.util.stream.Stream;

public class Recipes {

  public static boolean flag = false;

  void method(Stream<Customer> customers) {
    customers.flatMap(customer -> flag ? Stream.empty() : customer.getOrders());
  }

  public static class Customer {

    public Stream<Order> getOrders() {
      throw new RuntimeException();
    }
  }

  public static class Order {}
}
