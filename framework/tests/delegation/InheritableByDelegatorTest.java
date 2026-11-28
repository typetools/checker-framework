// Tests that the Delegation Checker verifies each method that is annotated with
// @InheritableByDelegator: the method uses its receiver only to call methods that are overridable
// or that are themselves annotated with @InheritableByDelegator.

import java.util.List;
import java.util.function.IntSupplier;
import org.checkerframework.common.delegation.qual.*;

public class InheritableByDelegatorTest {

  private int field;

  public int size() {
    return field;
  }

  protected int protectedSize() {
    return field;
  }

  int packagePrivateSize() {
    return field;
  }

  private int privateSize() {
    return field;
  }

  public final int finalSize() {
    return field;
  }

  @InheritableByDelegator
  private boolean annotatedPrivateIsEmpty() {
    return size() == 0;
  }

  public static int count(InheritableByDelegatorTest t) {
    return 0;
  }

  @InheritableByDelegator
  public boolean callsOverridable() {
    return size() == 0 && this.size() == 0 && protectedSize() == 0; // OK
  }

  @InheritableByDelegator
  public boolean callsAnnotated() {
    return annotatedPrivateIsEmpty(); // OK
  }

  @InheritableByDelegator
  public int usesParametersAndStaticMethods(List<Integer> list, int x) {
    return list.size() + x + count(null); // OK
  }

  @InheritableByDelegator
  public int callsOverridableInLambda() {
    IntSupplier s = () -> size(); // OK
    return s.getAsInt();
  }

  @InheritableByDelegator
  public Object createsStaticNested() {
    return new StaticNested(); // OK
  }

  @InheritableByDelegator
  public int readsField() {
    // :: warning: [invalid.inheritable.by.delegator]
    return field;
  }

  @InheritableByDelegator
  public int readsFieldExplicitly() {
    // :: warning: [invalid.inheritable.by.delegator]
    return this.field;
  }

  @InheritableByDelegator
  public int readsFieldQualified() {
    // :: warning: [invalid.inheritable.by.delegator]
    return InheritableByDelegatorTest.this.field;
  }

  @InheritableByDelegator
  public int readsFieldInLambda() {
    // :: warning: [invalid.inheritable.by.delegator]
    IntSupplier s = () -> field;
    return s.getAsInt();
  }

  @InheritableByDelegator
  public int callsPackagePrivate() {
    // :: warning: [invalid.inheritable.by.delegator]
    return packagePrivateSize();
  }

  @InheritableByDelegator
  public int callsPrivate() {
    // :: warning: [invalid.inheritable.by.delegator]
    return privateSize();
  }

  @InheritableByDelegator
  public int callsFinal() {
    // :: warning: [invalid.inheritable.by.delegator]
    return finalSize();
  }

  @InheritableByDelegator
  public int passesThis() {
    // :: warning: [invalid.inheritable.by.delegator]
    return count(this);
  }

  @InheritableByDelegator
  public InheritableByDelegatorTest returnsThis() {
    // :: warning: [invalid.inheritable.by.delegator]
    return this;
  }

  @InheritableByDelegator
  public Object createsInner() {
    // :: warning: [invalid.inheritable.by.delegator]
    return new Inner();
  }

  @InheritableByDelegator
  public Object anonymousClassReadsField() {
    return new Object() {
      int ownField;

      int get() {
        // Only the use of `field` refers to the receiver of anonymousClassReadsField().
        // :: warning: [invalid.inheritable.by.delegator]
        return field + ownField + this.ownField;
      }
    };
  }

  @Override
  @InheritableByDelegator
  public String toString() {
    // :: warning: [invalid.inheritable.by.delegator]
    return super.toString();
  }

  class Inner {}

  static class StaticNested {}
}

class InheritableByDelegatorSubclass extends InheritableByDelegatorTest {

  @InheritableByDelegator
  public boolean callsAnnotatedSuperMethod() {
    return super.callsOverridable(); // OK
  }

  @InheritableByDelegator
  public int callsUnannotatedSuperMethod() {
    // :: warning: [invalid.inheritable.by.delegator]
    return super.size();
  }
}
