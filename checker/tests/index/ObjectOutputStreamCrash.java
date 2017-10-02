class ObjectOutputStreamCrash {

    void sideEffectingMethod() {}

    void writeFields(Object[] objVals) {
        for (int i = 0; i < objVals.length; i++) {
            try {
                sideEffectingMethod();
            } finally {
                sideEffectingMethod();
            }
        }
    }
}
