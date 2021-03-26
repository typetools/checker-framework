public class Issue2739<E extends Object & Issue2739.EppEnum> {
  public interface EppEnum {
    String getXmlName();
  }

  void method(E e) {
    String s = e.getXmlName();
  }
}
