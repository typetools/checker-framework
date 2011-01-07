package java.text;
import checkers.javari.quals.*;

import java.io.InvalidObjectException;
import java.util.Locale;

public class MessageFormat extends Format {
    private static final long serialVersionUID = 6479157306784022952L;

    public MessageFormat(String pattern) {
        throw new RuntimeException("skeleton method");
    }

    public MessageFormat(String pattern, Locale locale) {
        throw new RuntimeException("skeleton method");
    }

    public void setLocale(Locale locale) {
        throw new RuntimeException("skeleton method");
    }

    public Locale getLocale() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public void applyPattern(String pattern) {
        throw new RuntimeException("skeleton method");
    }

    public String toPattern() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public void setFormatsByArgumentIndex(Format[] newFormats) {
        throw new RuntimeException("skeleton method");
    }

    public void setFormats(Format[] newFormats) {
        throw new RuntimeException("skeleton method");
    }

    public void setFormatByArgumentIndex(int argumentIndex, Format newFormat) {
        throw new RuntimeException("skeleton method");
    }

    public void setFormat(int formatElementIndex, Format newFormat) {
        throw new RuntimeException("skeleton method");
    }

    public Format[] getFormatsByArgumentIndex() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public Format[] getFormats() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public final StringBuffer format(@ReadOnly Object[] arguments, StringBuffer result,
                                     FieldPosition pos) @ReadOnly
    {
        throw new RuntimeException("skeleton method");
    }

    public static String format(String pattern, @ReadOnly Object ... arguments) {
        throw new RuntimeException("skeleton method");
    }

    public final StringBuffer format(@ReadOnly Object arguments, StringBuffer result,
                                     FieldPosition pos) @ReadOnly
    {
        throw new RuntimeException("skeleton method");
    }

    public AttributedCharacterIterator formatToCharacterIterator(@ReadOnly Object arguments) @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public Object[] parse(String source, ParsePosition pos) @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public Object[] parse(String source) @ReadOnly throws ParseException {
        throw new RuntimeException("skeleton method");
    }

    public Object parseObject(String source, ParsePosition pos) @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public Object clone() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public boolean equals(Object obj) @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public int hashCode() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public static class Field extends Format.Field {
        private static final long serialVersionUID = 7899943957617360810L;

        protected Field(String name) {
            super(name); // why is this needed to compile?
            throw new RuntimeException("skeleton method");
        }

        protected Object readResolve() @ReadOnly throws InvalidObjectException {
            throw new RuntimeException("skeleton method");
        }

        public final static Field ARGUMENT;
    }
}
