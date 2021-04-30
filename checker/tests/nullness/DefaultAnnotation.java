import java.util.Iterator;
import java.util.List;
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.checkerframework.framework.qual.TypeUseLocation;

public class DefaultAnnotation {

  public void testNoDefault() {

    String s = null;
  }

  @DefaultQualifier.List(
      @DefaultQualifier(
          value = org.checkerframework.checker.nullness.qual.NonNull.class,
          locations = {TypeUseLocation.ALL}))
  public void testDefault() {

    // :: error: (assignment)
    String s = null; // error
    List<String> lst = new List<>(); // valid
    // :: error: (argument)
    lst.add(null); // error
  }

  @DefaultQualifier(
      value = org.checkerframework.checker.nullness.qual.NonNull.class,
      locations = {TypeUseLocation.ALL})
  public class InnerDefault {

    public void testDefault() {
      // :: error: (assignment)
      String s = null; // error
      List<String> lst = new List<>(); // valid
      // :: error: (argument)
      lst.add(null); // error
      s = lst.get(0); // valid

      List<@Nullable String> nullList = new List<>(); // valid
      nullList.add(null); // valid
      // :: error: (assignment)
      s = nullList.get(0); // error
    }
  }

  @DefaultQualifier(
      value = org.checkerframework.checker.nullness.qual.NonNull.class,
      locations = {TypeUseLocation.ALL})
  public static class DefaultDefs {

    public String getNNString() {
      return "foo"; // valid
    }

    public String getNNString2() {
      // :: error: (return)
      return null; // error
    }

    public <T extends @Nullable Object> T getNull(T t) {
      // :: error: (return)
      return null; // invalid
    }

    public <T extends @NonNull Object> T getNonNull(T t) {
      // :: error: (return)
      return null; // error
    }
  }

  public class DefaultUses {

    public void test() {

      DefaultDefs d = new DefaultDefs();

      @NonNull String s = d.getNNString(); // valid
    }

    @DefaultQualifier(
        value = org.checkerframework.checker.nullness.qual.NonNull.class,
        locations = {TypeUseLocation.ALL})
    public void testDefaultArgs() {

      DefaultDefs d = new DefaultDefs();

      // :: error: (assignment)
      String s1 = d.<@Nullable String>getNull(null); // error
      String s2 = d.<String>getNonNull("foo"); // valid
      // :: error: (type.argument) :: error: (assignment)
      String s3 = d.<@Nullable String>getNonNull("foo"); // error
    }
  }

  @DefaultQualifier(value = org.checkerframework.checker.nullness.qual.NonNull.class)
  static class DefaultExtends implements Iterator<String>, Iterable<String> {

    @Override
    public boolean hasNext() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

    @Override
    public String next() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<String> iterator() {
      return this;
    }
  }

  class List<E extends @Nullable Object> {
    public E get(int i) {
      throw new RuntimeException();
    }

    public boolean add(E e) {
      throw new RuntimeException();
    }
  }

  @DefaultQualifier(value = NonNull.class)
  public void testDefaultUnqualified() {

    // :: error: (assignment)
    String s = null; // error
    List<String> lst = new List<>(); // valid
    // :: error: (argument)
    lst.add(null); // error
  }
}
