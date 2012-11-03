
class AssignmentContext {
    
    void foo(String[] a) {}
    
    void t1(boolean b) {
        String[] s = b ? new String[] {""} : null;
    }
    
    void t2(boolean b) {
        foo(b ? new String[] {""} : null);
    }
    
    String[] t3(boolean b) {
        return b ? new String[] {""} : null;
    }
    
    void t4(boolean b) {
        String[] s = null;
        s = b ? new String[] {""} : null;
    }
}
