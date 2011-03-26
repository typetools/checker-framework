//@skip-test
public class SymbolNotFoundErrors {
    // We only expect one error message for the unknown symbol.
    // However, we receive three.
    // http://code.google.com/p/checker-framework/issues/detail?id=94
    CCC f;
    // The testing framework doesn't distinguish multiple equal
    // annotations on one line. Therefore manually inspect
    // the compilation result.
}
