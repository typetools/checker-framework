public class NPE2Test {
    public void testNPE() {
        SupplierDefs.Supplier<String> s = new SupplierDefs.NullSupplier();
        boolean b = s.get().equals("");
    }

    public void testNPE2() {
        SupplierDefs.MyInterface<String> s = new SupplierDefs.NullInterface();
        boolean b = s.getT().equals("");
    }
}
