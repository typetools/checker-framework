public class NPE2Test {
    public void testNPE() {
        SupplierDefs.Supplier<String> s = new SupplierDefs.NullSupplier();
        boolean b = s.get().equals("");
    }

    public void testNPE2() {
        SupplierDefs.MyInterface<String> s = new SupplierDefs.NullInterface();
        boolean b = s.getT().equals("");
    }

    public void testNPE3() {
        SupplierDefs.MyInterface<String> s = new SupplierDefs.NullSupplierMyInterface();
        boolean b = s.getT().equals("");

        SupplierDefs.Supplier<String> s2 = new SupplierDefs.NullSupplierMyInterface();
        boolean b2 = s2.get().equals("");
    }
}
