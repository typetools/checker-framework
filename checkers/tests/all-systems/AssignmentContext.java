import checkers.nullness.quals.Nullable;

class AssignmentContext {
    
    void foo(String @Nullable [] a) {}
    
    void t1(boolean b) {
        String[] s = b ? new String[] {""} : null;
    }
    
    void t2(boolean b) {
        foo(b ? new String[] {""} : null);
    }
    
    String @Nullable [] t3(boolean b) {
        return b ? new String[] {""} : null;
    }
    
    void t4(boolean b) {
        String[] s = null;
        s = b ? new String[] {""} : null;
    }

    void assignToCast(String[][] currentSample) {
        // This statement used to cause a null pointer exception.
        ((String [])currentSample[3])[4] = currentSample[3][4];
    }

}
