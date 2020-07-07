import com.sun.tools.javac.code.Symbol;

public class SymbolCompleterUsesObjectEquals {
    boolean p(Symbol.Completer c) {
        return c == Symbol.Completer.NULL_COMPLETER;
    }
}
