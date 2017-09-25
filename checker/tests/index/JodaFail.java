class Converter {}

class PeriodConverter extends Converter {}

public final class JodaFail {

    void copyInto(Converter[] converters) {}

    public void getPeriodConverters(PeriodConverter[] converters) {
        copyInto(converters);
    }
}
