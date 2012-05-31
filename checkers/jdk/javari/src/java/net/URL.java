package java.net;
import checkers.javari.quals.*;

import java.io.IOException;
import java.io.InputStream;

public final class URL implements java.io.Serializable {
    static final long serialVersionUID = -7627629688361524110L;

    public URL(String protocol, String host, int port, String file) throws MalformedURLException {
        throw new RuntimeException("skeleton method"); 
    }

    public URL(String protocol, String host, String file) throws MalformedURLException {
        throw new RuntimeException("skeleton method");
    }

    public URL(String protocol, String host, int port, String file,
               @ReadOnly URLStreamHandler handler) throws MalformedURLException {
        throw new RuntimeException("skeleton method");
    }

    public URL(String spec) throws MalformedURLException {
        throw new RuntimeException("skeleton method");
    }

    public URL(URL context, String spec) throws MalformedURLException {
        throw new RuntimeException("skeleton method");
    }

    public URL(URL context, String spec, @ReadOnly URLStreamHandler handler) throws MalformedURLException {
        throw new RuntimeException("skeleton method");
    }

    public String getQuery() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public String getPath() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public String getUserInfo() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public String getAuthority() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public int getPort() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public int getDefaultPort() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public String getProtocol() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public String getHost() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public String getFile() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public String getRef() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public boolean equals(@ReadOnly Object obj) @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public synchronized int hashCode() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public boolean sameFile(@ReadOnly URL other) @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public String toString() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public String toExternalForm() @ReadOnly {
        throw new RuntimeException("skeleton method");
    }

    public URI toURI() @ReadOnly throws URISyntaxException {
        throw new RuntimeException("skeleton method");
    }

    public URLConnection openConnection() throws java.io.IOException {
        throw new RuntimeException("skeleton method");
    }

    public URLConnection openConnection(Proxy proxy) throws java.io.IOException {
        throw new RuntimeException("skeleton method");
    }

    public final InputStream openStream() throws java.io.IOException {
        throw new RuntimeException("skeleton method");
    }

    public final Object getContent() throws java.io.IOException {
        throw new RuntimeException("skeleton method");
    }

    public final Object getContent(Class[] classes) throws java.io.IOException {
        throw new RuntimeException("skeleton method");
    }

    public static void setURLStreamHandlerFactory(URLStreamHandlerFactory fac) {
        throw new RuntimeException("skeleton method");
    }
}
