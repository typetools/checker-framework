// Based on a false positive in hdfs

import org.checkerframework.checker.calledmethods.qual.*;
import org.checkerframework.checker.mustcall.qual.*;
import org.checkerframework.common.returnsreceiver.qual.*;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;

import javax.net.ssl.*;

class HdfsReport3 {
    private StringBuffer nonObligationTest(int id) {
        final StringWriter out = new StringWriter();
        dumpTreeRecursively(new PrintWriter(out, true), new StringBuilder(), id);
        return out.getBuffer();
    }

    public void dumpTreeRecursively(PrintWriter out, StringBuilder prefix, int snapshotId) {}

    // StringBuilder doesn't implement closeable
    private final StringBuilder sb = new StringBuilder();
    private final Formatter formatter = new Formatter(sb);
}
