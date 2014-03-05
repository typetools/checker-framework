import checkers.value.quals.*;

class Fields {

    static final int field = 1;
    
    public void inClassFields(){
        @IntVal({1}) int a = field;
        //:: error: (assignment.type.incompatible)
        @IntVal({0}) int b = field;
    }

    public void otherClassFields(){
        @BoolVal({false}) boolean a = Boolean.FALSE;
        //:: error: (assignment.type.incompatible)
        a = Boolean.TRUE;


        @IntVal({4}) int b = java.util.Calendar.MAY;
        //:: error: (assignment.type.incompatible)
        b = java.util.Calendar.APRIL;

        @IntVal({9}) int c = java.util.zip.Deflater.BEST_COMPRESSION;
        //:: error: (assignment.type.incompatible)
        c = java.util.zip.Deflater.BEST_SPEED;
    }

}