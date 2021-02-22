// Library for issue #511: https://github.com/typetools/checker-framework/issues/511

public abstract class GwiParent {
    abstract void syntaxError(Recognizer<?> recognizer);
}

abstract class ATNSimulator {}

class Recognizer<ATNInterpreter extends ATNSimulator> {}
