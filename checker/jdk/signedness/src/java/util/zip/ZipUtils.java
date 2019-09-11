/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package java.util.zip;

import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static java.util.zip.ZipConstants.*;
import static java.util.zip.ZipConstants64.*;

class ZipUtils {

    // used to adjust values between Windows and java epoch
    private static final long WINDOWS_EPOCH_IN_MICROSECONDS = -11644473600000000L;

    /**
     * Converts Windows time (in microseconds, UTC/GMT) time to FileTime.
     */
    public static final FileTime winTimeToFileTime(long wtime) {
        return FileTime.from(wtime / 10 + WINDOWS_EPOCH_IN_MICROSECONDS,
                             TimeUnit.MICROSECONDS);
    }

    /**
     * Converts FileTime to Windows time.
     */
    public static final long fileTimeToWinTime(FileTime ftime) {
        return (ftime.to(TimeUnit.MICROSECONDS) - WINDOWS_EPOCH_IN_MICROSECONDS) * 10;
    }

    /**
     * Converts "standard Unix time"(in seconds, UTC/GMT) to FileTime
     */
    public static final FileTime unixTimeToFileTime(long utime) {
        return FileTime.from(utime, TimeUnit.SECONDS);
    }

    /**
     * Converts FileTime to "standard Unix time".
     */
    public static final long fileTimeToUnixTime(FileTime ftime) {
        return ftime.to(TimeUnit.SECONDS);
    }

    /**
     * Converts DOS time to Java time (number of milliseconds since epoch).
     */
    public static long dosToJavaTime(long dtime) {
        @SuppressWarnings("deprecation") // Use of date constructor.
        Date d = new Date((int)(((dtime >> 25) & 0x7f) + 80),
                          (int)(((dtime >> 21) & 0x0f) - 1),
                          (int)((dtime >> 16) & 0x1f),
                          (int)((dtime >> 11) & 0x1f),
                          (int)((dtime >> 5) & 0x3f),
                          (int)((dtime << 1) & 0x3e));
        return d.getTime();
    }

    /**
     * Converts Java time to DOS time.
     */
    @SuppressWarnings("deprecation") // Use of date methods
    public static long javaToDosTime(long time) {
        Date d = new Date(time);
        int year = d.getYear() + 1900;
        if (year < 1980) {
            return (1 << 21) | (1 << 16);
        }
        return (year - 1980) << 25 | (d.getMonth() + 1) << 21 |
               d.getDate() << 16 | d.getHours() << 11 | d.getMinutes() << 5 |
               d.getSeconds() >> 1;
    }

    /**
     * Fetches unsigned 16-bit value from byte array at specified offset.
     * The bytes are assumed to be in Intel (little-endian) byte order.
     */
    public static final int get16(byte b[], int off) {
        return Byte.toUnsignedInt(b[off]) | (Byte.toUnsignedInt(b[off+1]) << 8);
    }

    /**
     * Fetches unsigned 32-bit value from byte array at specified offset.
     * The bytes are assumed to be in Intel (little-endian) byte order.
     */
    public static final long get32(byte b[], int off) {
        return (get16(b, off) | ((long)get16(b, off+2) << 16)) & 0xffffffffL;
    }

    /**
     * Fetches signed 64-bit value from byte array at specified offset.
     * The bytes are assumed to be in Intel (little-endian) byte order.
     */
    public static final long get64(byte b[], int off) {
        return get32(b, off) | (get32(b, off+4) << 32);
    }
}
