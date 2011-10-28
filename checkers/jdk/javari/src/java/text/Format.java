package java.text;
import checkers.javari.quals.*;

import java.io.Serializable;

public abstract class Format implements Serializable, Cloneable {
    private static final long serialVersionUID = -299282585814624189L;

    protected Format() {
        throw new RuntimeException("skeleton method");
    }

    public final String format (Object obj) {
        throw new RuntimeException("skeleton method");
    }

    public abstract StringBuffer format(Object obj,
                    StringBuffer toAppendTo,
                    FieldPosition pos);

    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        throw new RuntimeException("skeleton method");
    }

    public abstract Object parseObject (String source, ParsePosition pos);

    public Object parseObject(String source) throws ParseException {
        throw new RuntimeException("skeleton method");
    }

    public Object clone() {
        throw new RuntimeException("skeleton method");
    }

    public static class Field extends AttributedCharacterIterator.Attribute {
        private static final long serialVersionUID = 276966692217360283L;

        protected Field(String name) {
            super(name); // Why is this needed???
            throw new RuntimeException("skeleton method");
        }
    }

    interface FieldDelegate {
        public void formatted(Format.Field attr, Object value, int start,
                              int end, StringBuffer buffer);

        public void formatted(int fieldID, Format.Field attr, Object value,
                              int start, int end, StringBuffer buffer);
    }

}
