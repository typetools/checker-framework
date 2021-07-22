// A test for code encountered by Narges.

import java.io.*;
import java.net.*;
import java.nio.channels.*;

class BindChannel {
    static void test(InetSocketAddress addr, boolean b) {
        try {
            // This channel is bound - so even with unconnected socket support, we need to
            // treat either this channel or the .socket() expression as must-close.
            //
            // Even though there's now a temporary in the Must Call Checker for the value that
            // has the reset method (bind) called on it below, we can't successfully translate
            // the reset expression to that temporary, since all we have is a string (from the
            // reset annotation) and so we have to go through the type factory's parsing facility,
            // which doesn't know about the temporaries and so doesn't return them. We're therefore
            // limited to issuing the reset.not.owning error below,
            // instead of the preferable required.method.not.called error on this line - as in
            // the method below, which extracts the socket into a local variable, which can be
            // parsed as an CO target.
            ServerSocketChannel httpChannel = ServerSocketChannel.open();
            // :: error: reset.not.owning
            httpChannel.socket().bind(addr);
        } catch (IOException io) {

        }
    }

    static void test_lv(InetSocketAddress addr, boolean b) {
        try {
            ServerSocketChannel httpChannel = ServerSocketChannel.open();
            // :: error: required.method.not.called
            ServerSocket httpSock = httpChannel.socket();
            httpSock.bind(addr);
        } catch (IOException io) {

        }
    }
}
