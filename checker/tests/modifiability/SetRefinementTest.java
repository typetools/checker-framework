import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.modifiability.qual.Modifiable;

// Tests that @DoesNotUnrefineReceiver on List.set() preserves the @Modifiable refinement of
// the receiver expression across different receiver kinds: field access, local variable,
// array access, and `this`.

public class SetRefinementTest {

  // Flow-refinement behaves as if this assignment is at the beginning of every constructor body.
  private List<String> items = new ArrayList<>();

  /** Creates a new SetRefinementTest. */
  public SetRefinementTest() {}

  // ==========================================================
  // Field access as receiver
  // ==========================================================

  public SetRefinementTest(List<String> other) {
    // items is @Modifiable from the field initializer.
    this.items.addAll(other);
  }

  public SetRefinementTest(List<String> other, int dummy) {
    this.items.set(0, other.get(0));
    this.items.addAll(other);
  }

  public SetRefinementTest(List<String> other, boolean dummy) {
    this.items.set(0, other.get(0));
    this.items.set(0, other.get(0));
    this.items.addAll(other);
  }

  public SetRefinementTest(List<String> other, float dummy) {
    this.items.get(0);
    this.items.set(0, other.get(0));
    this.items.addAll(other);
  }

  // Negative field case: in a regular method, items has its declared type @MaybeModifiable.
  public void updateItemsLikeConstructor(List<String> other, int dummy) {
    // :: error: [method.invocation]
    this.items.set(0, other.get(0));
    // :: error: [method.invocation]
    this.items.addAll(other);
  }

  // ==========================================================
  // Local variable as receiver
  //
  // In this checker (sideEffectsUnrefineAliases = false), local variable refinements are never
  // cleared by side effects, so the behavior is correct regardless of @DoesNotUnrefineReceiver.
  // These tests document the expected semantics.
  // ==========================================================

  public void localVarReceiver(List<String> other) {
    List<String> local = new ArrayList<>();
    local.set(0, other.get(0));
    local.addAll(other);
  }

  public void localVarReceiverMultipleSets(List<String> other) {
    List<String> local = new ArrayList<>();
    local.set(0, other.get(0));
    local.set(0, other.get(0));
    local.addAll(other);
  }

  // ==========================================================
  // Array access as receiver
  //
  // Array access refinements ARE always cleared by side effects (unlike local variables), so
  // @DoesNotUnrefineReceiver makes an observable difference here.
  // ==========================================================

  // Positive: set() preserves the @Modifiable refinement of the array element.
  @SuppressWarnings("unchecked")
  public void arrayAccessReceiver(List<String> other) {
    List<String>[] arr = new ArrayList[1];
    arr[0] = new ArrayList<>();
    arr[0].set(0, other.get(0));
    arr[0].addAll(other);
  }

  // Positive: add() also preserves the @Modifiable refinement of the array element.
  // Collection.add() is annotated @DoesNotUnrefineReceiver("modifiability"), and that declaration
  // annotation is inherited by overriding methods such as List.add().
  @SuppressWarnings("unchecked")
  public void arrayAccessReceiverSideEffect(List<String> other) {
    List<String>[] arr = new ArrayList[1];
    arr[0] = new ArrayList<>(); // arr[0] is @Modifiable
    arr[0].add("x"); // side effect with @DoesNotUnrefineReceiver; refinement of arr[0] preserved
    arr[0].addAll(other);
  }

  // ==========================================================
  // `this` as receiver (when the class is itself a List)
  //
  // In this checker (sideEffectsUnrefineAliases = false), thisValue is never cleared by side
  // effects, so the behavior is correct regardless of @DoesNotUnrefineReceiver.
  // ==========================================================

  static class ThisListReceiver extends ArrayList<String> {

    public void update(@Modifiable ThisListReceiver this, List<String> other) {
      this.set(0, other.get(0));
      this.addAll(other);
      this.addAll(other);
    }

    public void updateMultipleSets(@Modifiable ThisListReceiver this, List<String> other) {
      this.set(0, other.get(0));
      this.set(0, other.get(0));
      this.addAll(other);
    }
  }
}
