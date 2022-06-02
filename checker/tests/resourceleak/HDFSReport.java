class Handler extends Thread {
    boolean running;

    static class Call {
        boolean isResponseDeferred() {
            return true;
        }
    }

    @Override
    public void run() {
        while (running) {
            Call call = null;
            try {
                if (running) {
                    continue;
                }
            } catch (Exception e) {
            } finally {
                String s = call.isResponseDeferred() ? ", deferred" : "";
            }
        }
    }
}
