package java.text;
import checkers.javari.quals.*;

import java.util.Date;
import java.util.Locale;

public class SimpleDateFormat extends DateFormat {
    static final long serialVersionUID = 4774881970558875024L;

    public SimpleDateFormat() {
        throw new RuntimeException("skeleton method");
    }

    public SimpleDateFormat(String pattern) {
        throw new RuntimeException("skeleton method");
    }

    public SimpleDateFormat(String pattern, Locale locale) {
        throw new RuntimeException("skeleton method");
    }

    public SimpleDateFormat(String pattern, DateFormatSymbols formatSymbols) {
        throw new RuntimeException("skeleton method");
    }

    public void set2DigitYearStart(@ReadOnly Date startDate) {
        throw new RuntimeException("skeleton method");
    }

    public Date get2DigitYearStart(@ReadOnly SimpleDateFormat this) {
        throw new RuntimeException("skeleton method");
    }

    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition pos) {
        throw new RuntimeException("skeleton method");
    }

    public AttributedCharacterIterator formatToCharacterIterator(Object obj) {
        throw new RuntimeException("skeleton method");
    }

    public Date parse(String text, ParsePosition pos) {
        throw new RuntimeException("skeleton method");
    }

    public String toPattern(@ReadOnly SimpleDateFormat this) {
        throw new RuntimeException("skeleton method");
    }

    public String toLocalizedPattern(@ReadOnly SimpleDateFormat this) {
        throw new RuntimeException("skeleton method");
    }

    public void applyPattern (String pattern) {
        throw new RuntimeException("skeleton method");
    }

    public void applyLocalizedPattern(String pattern) {
        throw new RuntimeException("skeleton method");
    }

    public DateFormatSymbols getDateFormatSymbols(@ReadOnly SimpleDateFormat this) {
        throw new RuntimeException("skeleton method");
    }

    public void setDateFormatSymbols(@ReadOnly DateFormatSymbols newFormatSymbols) {
        throw new RuntimeException("skeleton method");
    }

    public Object clone(@ReadOnly SimpleDateFormat this) {
        throw new RuntimeException("skeleton method");
    }

    public int hashCode(@ReadOnly SimpleDateFormat this) {
        throw new RuntimeException("skeleton method");
    }

    public boolean equals(@ReadOnly SimpleDateFormat this, @ReadOnly Object obj) {
        throw new RuntimeException("skeleton method");
    }
}
