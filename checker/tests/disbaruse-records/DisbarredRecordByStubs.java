record DisbarredRecordByStubs(String barred, String fine) {

    DisbarredRecordByStubs {
        // :: error: (disbar.use)
        int x = barred.length();
    }

    void invalid() {
        // :: error: (disbar.use)
        barred();
        // :: error: (disbar.use)
        int x = barred.length();
    }

    void valid() {
        fine();
        int x = fine.length();
    }
}
