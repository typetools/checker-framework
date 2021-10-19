class Issue4923 {
    interface Go {
        void go();
    }

    final Go go =
            new Go() {
                @Override
                public void go() {
                    synchronized (x) {
                    }
                }
            };
    final Object x = new Object();

    // Make sure that initializer type is compatible with declared type
    // :: error: (assignment.type.incompatible)
    final Object y = null;
}
