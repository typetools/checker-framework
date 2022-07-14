class LhsArrayCast {

    void populateWithSamples(Object[] currentSample) {
        int j = 22;
        int k = 42;
        ((String[]) currentSample[j])[k] = "hello";
    }
}
