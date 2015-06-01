public class FooConflict implements IFooSafe, IFooUI {
    //:: warning: (override.effect.warning.inheritance)
    @Override public void foo() { }
}
