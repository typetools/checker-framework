public class NPE2Test {
    public void testNPE() {
        SupplierDefs.Supplier<String> s = new SupplierDefs.NullSupplier();
        boolean b = s.get().equals("");
    }
}
