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

    public Locale getLocale(@ReadOnly MessageFormat this) {
        throw new RuntimeException("skeleton method");
    }

    public void applyPattern(String pattern) {
        throw new RuntimeException("skeleton method");
    }

    public String toPattern(@ReadOnly MessageFormat this) {
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

    public Format[] getFormatsByArgumentIndex(@ReadOnly MessageFormat this) {
        throw new RuntimeException("skeleton method");
    }

    public Format[] getFormats(@ReadOnly MessageFormat this) {
        throw new RuntimeException("skeleton method");
    }

    public final StringBuffer format(@ReadOnly MessageFormat this, @ReadOnly Object[] arguments, StringBuffer result,
                                     FieldPosition pos)
    {
        throw new RuntimeException("skeleton method");
    }

    public static String format(String pattern, @ReadOnly Object ... arguments) {
        throw new RuntimeException("skeleton method");
    }

    public final StringBuffer format(@ReadOnly MessageFormat this, @ReadOnly Object arguments, StringBuffer result,
                                     FieldPosition pos)
    {
        throw new RuntimeException("skeleton method");
    }

    public AttributedCharacterIterator formatToCharacterIterator(@ReadOnly MessageFormat this, @ReadOnly Object arguments) {
        throw new RuntimeException("skeleton method");
    }

    public Object[] parse(@ReadOnly MessageFormat this, String source, ParsePosition pos) {
        throw new RuntimeException("skeleton method");
    }

    public Object[] parse(@ReadOnly MessageFormat this, String source) throws ParseException {
        throw new RuntimeException("skeleton method");
    }

    public Object parseObject(@ReadOnly MessageFormat this, String source, ParsePosition pos) {
        throw new RuntimeException("skeleton method");
    }

    public Object clone(@ReadOnly MessageFormat this) {
        throw new RuntimeException("skeleton method");
    }

    public boolean equals(@ReadOnly MessageFormat this, Object obj) {
        throw new RuntimeException("skeleton method");
    }

    public int hashCode(@ReadOnly MessageFormat this) {
        throw new RuntimeException("skeleton method");
    }

    public static class Field extends Format.Field {
        private static final long serialVersionUID = 7899943957617360810L;

        protected Field(String name) {
            super(name); // why is this needed to compile?
            throw new RuntimeException("skeleton method");
        }

        protected Object readResolve(@ReadOnly Field this) throws InvalidObjectException {
            throw new RuntimeException("skeleton method");
        }

        public final static Field ARGUMENT;
    }
}
