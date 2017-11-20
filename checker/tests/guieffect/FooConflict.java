public class FooConflict implements IFooSafe, IFooUI {
    @Override
    // :: warning: (override.effect.warning.inheritance)
    public void foo() {}
}
