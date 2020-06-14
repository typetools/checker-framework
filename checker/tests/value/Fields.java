import javax.swing.plaf.BorderUIResource;
import org.checkerframework.common.value.qual.*;

class Fields {

    static final int field = 1;

    public void innerClassFields() {
        @IntVal({9}) int x = java.util.zip.Deflater.BEST_COMPRESSION;
        @IntVal({4}) int a = BorderUIResource.TitledBorderUIResource.ABOVE_BOTTOM;
        // :: error: (assignment.type.incompatible)
        @IntVal({0}) int b = BorderUIResource.TitledBorderUIResource.ABOVE_BOTTOM;
    }

    public void inClassFields() {
        @IntVal({1}) int a = field;
        // :: error: (assignment.type.incompatible)
        @IntVal({0}) int b = field;
    }

    public void otherClassFields() {
        @IntVal({56319}) char x = Character.MAX_HIGH_SURROGATE;
        @IntVal({16}) byte y = Character.FORMAT;

        @BoolVal({false}) boolean a = Boolean.FALSE;
        // :: error: (assignment.type.incompatible)
        a = Boolean.TRUE;

        @IntVal({4}) int b = java.util.Calendar.MAY;
        // :: error: (assignment.type.incompatible)
        b = java.util.Calendar.APRIL;

        @IntVal({9}) int c = java.util.zip.Deflater.BEST_COMPRESSION;
        // :: error: (assignment.type.incompatible)
        c = java.util.zip.Deflater.BEST_SPEED;

        @IntVal({1024}) int d = java.awt.GridBagConstraints.ABOVE_BASELINE;
        // :: error: (assignment.type.incompatible)
        d = java.awt.GridBagConstraints.LAST_LINE_END;
    }

    void innerFieldTest() {
        @StringVal("section_number") String a = InnerStaticClass.INNER_STATIC_FIELD;

        // :: error: (assignment.type.incompatible)
        @StringVal("") String b = InnerStaticClass.INNER_STATIC_FIELD;
    }

    static final int fieldDeclAtBottom = 1;

    public static class InnerStaticClass {
        public static final String INNER_STATIC_FIELD = "section_number";
    }
}
