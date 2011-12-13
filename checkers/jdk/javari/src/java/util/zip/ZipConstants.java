package java.util.zip;
import checkers.javari.quals.*;

interface ZipConstants {
    static long LOCSIG = 0x04034b50L;
    static long EXTSIG = 0x08074b50L;
    static long CENSIG = 0x02014b50L;
    static long ENDSIG = 0x06054b50L;

    static final int LOCHDR = 30;
    static final int EXTHDR = 16;
    static final int CENHDR = 46;
    static final int ENDHDR = 22;

    static final int LOCVER = 4;
    static final int LOCFLG = 6;
    static final int LOCHOW = 8;
    static final int LOCTIM = 10;
    static final int LOCCRC = 14;
    static final int LOCSIZ = 18;
    static final int LOCLEN = 22;
    static final int LOCNAM = 26;
    static final int LOCEXT = 28;

    static final int EXTCRC = 4;
    static final int EXTSIZ = 8;
    static final int EXTLEN = 12;

    static final int CENVEM = 4;
    static final int CENVER = 6;
    static final int CENFLG = 8;
    static final int CENHOW = 10;
    static final int CENTIM = 12;
    static final int CENCRC = 16;
    static final int CENSIZ = 20;
    static final int CENLEN = 24;
    static final int CENNAM = 28;
    static final int CENEXT = 30;
    static final int CENCOM = 32;
    static final int CENDSK = 34;
    static final int CENATT = 36;
    static final int CENATX = 38;
    static final int CENOFF = 42;

    static final int ENDSUB = 8;
    static final int ENDTOT = 10;
    static final int ENDSIZ = 12;
    static final int ENDOFF = 16;
    static final int ENDCOM = 20;
}
