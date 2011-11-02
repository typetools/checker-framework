package java.net;
import checkers.javari.quals.*;

import java.io.InputStream;
import java.io.IOException;
import java.security.Permission;

abstract public class HttpURLConnection extends URLConnection {
    protected HttpURLConnection (URL u) {
        super(u);
        throw new RuntimeException("skeleton method");
    }

    public String getHeaderFieldKey (int n) {
        throw new RuntimeException("skeleton method");
    }

    public void setFixedLengthStreamingMode (int contentLength) {
        throw new RuntimeException("skeleton method");
    }

    public void setFixedLengthStreamingMode(long contentLength) {
        throw new RuntimeException("skeleton method");
    }

    public void setChunkedStreamingMode (int chunklen) {
        throw new RuntimeException("skeleton method");
    }

    public String getHeaderField(@ReadOnly HttpURLConnection this, int n) {
        throw new RuntimeException("skeleton method");
    }

    public static void setFollowRedirects(boolean set) {
        throw new RuntimeException("skeleton method");
    }

    public static boolean getFollowRedirects() {
        throw new RuntimeException("skeleton method");
    }

    public void setInstanceFollowRedirects(boolean followRedirects) {
        throw new RuntimeException("skeleton method");
    }

    public boolean getInstanceFollowRedirects(@ReadOnly HttpURLConnection this) {
        throw new RuntimeException("skeleton method");
    }

    public void setRequestMethod(String method) throws ProtocolException {
        throw new RuntimeException("skeleton method");
    }

    public String getRequestMethod(@ReadOnly HttpURLConnection this) {
        throw new RuntimeException("skeleton method");
    }

    public int getResponseCode() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public String getResponseMessage() throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public long getHeaderFieldDate(@ReadOnly HttpURLConnection this, String name, long Default) {
        throw new RuntimeException("skeleton method");
    }

    public abstract void disconnect();

    public abstract boolean usingProxy();

    public Permission getPermission(@ReadOnly HttpURLConnection this) throws IOException {
        throw new RuntimeException("skeleton method");
    }

    public InputStream getErrorStream(@ReadOnly HttpURLConnection this) {
        throw new RuntimeException("skeleton method");
    }

    public static final int HTTP_OK = 200;
    public static final int HTTP_CREATED = 201;
    public static final int HTTP_ACCEPTED = 202;
    public static final int HTTP_NOT_AUTHORITATIVE = 203;
    public static final int HTTP_NO_CONTENT = 204;
    public static final int HTTP_RESET = 205;
    public static final int HTTP_PARTIAL = 206;
    public static final int HTTP_MULT_CHOICE = 300;
    public static final int HTTP_MOVED_PERM = 301;
    public static final int HTTP_MOVED_TEMP = 302;
    public static final int HTTP_SEE_OTHER = 303;
    public static final int HTTP_NOT_MODIFIED = 304;
    public static final int HTTP_USE_PROXY = 305;
    public static final int HTTP_BAD_REQUEST = 400;
    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_PAYMENT_REQUIRED = 402;
    public static final int HTTP_FORBIDDEN = 403;
    public static final int HTTP_NOT_FOUND = 404;
    public static final int HTTP_BAD_METHOD = 405;
    public static final int HTTP_NOT_ACCEPTABLE = 406;
    public static final int HTTP_PROXY_AUTH = 407;
    public static final int HTTP_CLIENT_TIMEOUT = 408;
    public static final int HTTP_CONFLICT = 409;
    public static final int HTTP_GONE = 410;
    public static final int HTTP_LENGTH_REQUIRED = 411;
    public static final int HTTP_PRECON_FAILED = 412;
    public static final int HTTP_ENTITY_TOO_LARGE = 413;
    public static final int HTTP_REQ_TOO_LONG = 414;
    public static final int HTTP_UNSUPPORTED_TYPE = 415;
    @Deprecated
    public static final int HTTP_SERVER_ERROR = 500;
    public static final int HTTP_INTERNAL_ERROR = 500;
    public static final int HTTP_NOT_IMPLEMENTED = 501;
    public static final int HTTP_BAD_GATEWAY = 502;
    public static final int HTTP_UNAVAILABLE = 503;
    public static final int HTTP_GATEWAY_TIMEOUT = 504;
    public static final int HTTP_VERSION = 505;
}
