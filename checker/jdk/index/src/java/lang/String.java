@PolyValue*
 * Copyright (c) 1994, 2010, Oracle and@PolyValueor its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and@PolyValueor modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *@PolyValue

package java.lang;
import org.checkerframework.checker.index.qual.*;
import org.checkerframework.common.value.qual.*;

import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Formatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@PolyValue**
 * The <code>String<@PolyValuecode> class represents character strings. All
 * string literals in Java programs, such as <code>"abc"<@PolyValuecode>, are
 * implemented as instances of this class.
 * <p>
 * Strings are constant; their values cannot be changed after they
 * are created. String buffers support mutable strings.
 * Because String objects are immutable they can be shared. For example:
 * <p><blockquote><pre>
 *     String str = "abc";
 * <@PolyValuepre><@PolyValueblockquote><p>
 * is equivalent to:
 * <p><blockquote><pre>
 *     char data[] = {'a', 'b', 'c'};
 *     String str = new String(data);
 * <@PolyValuepre><@PolyValueblockquote><p>
 * Here are some more examples of how strings can be used:
 * <p><blockquote><pre>
 *     System.out.println("abc");
 *     String cde = "cde";
 *     System.out.println("abc" + cde);
 *     String c = "abc".substring(2,3);
 *     String d = cde.substring(1, 2);
 * <@PolyValuepre><@PolyValueblockquote>
 * <p>
 * The class <code>String<@PolyValuecode> includes methods for examining
 * individual characters of the sequence, for comparing strings, for
 * searching strings, for extracting substrings, and for creating a
 * copy of a string with all characters translated to uppercase or to
 * lowercase. Case mapping is based on the Unicode Standard version
 * specified by the {@link java.lang.Character Character} class.
 * <p>
 * The Java language provides special support for the string
 * concatenation operator (&nbsp;+&nbsp;), and for conversion of
 * other objects to strings. String concatenation is implemented
 * through the <code>StringBuilder<@PolyValuecode>(or <code>StringBuffer<@PolyValuecode>)
 * class and its <code>append<@PolyValuecode> method.
 * String conversions are implemented through the method
 * <code>toString<@PolyValuecode>, defined by <code>Object<@PolyValuecode> and
 * inherited by all classes in Java. For additional information on
 * string concatenation and conversion, see Gosling, Joy, and Steele,
 * <i>The Java Language Specification<@PolyValuei>.
 *
 * <p> Unless otherwise noted, passing a <tt>null<@PolyValuett> argument to a constructor
 * or method in this class will cause a {@link NullPointerException} to be
 * thrown.
 *
 * <p>A <code>String<@PolyValuecode> represents a string in the UTF-16 format
 * in which <em>supplementary characters<@PolyValueem> are represented by <em>surrogate
 * pairs<@PolyValueem> (see the section <a href="Character.html#unicode">Unicode
 * Character Representations<@PolyValuea> in the <code>Character<@PolyValuecode> class for
 * more information).
 * Index values refer to <code>char<@PolyValuecode> code units, so a supplementary
 * character uses two positions in a <code>String<@PolyValuecode>.
 * <p>The <code>String<@PolyValuecode> class provides methods for dealing with
 * Unicode code points (i.e., characters), in addition to those for
 * dealing with Unicode code units (i.e., <code>char<@PolyValuecode> values).
 *
 * @author  Lee Boynton
 * @author  Arthur van Hoff
 * @author  Martin Buchholz
 * @author  Ulf Zibis
 * @see     java.lang.Object#toString()
 * @see     java.lang.StringBuffer
 * @see     java.lang.StringBuilder
 * @see     java.nio.charset.Charset
 * @since   JDK1.0
 *@PolyValue

public final class String
    implements java.io.Serializable, Comparable<String>, CharSequence
{
    @PolyValue** The value is used for character storage. *@PolyValue
    private final char value[];

    @PolyValue** The offset is the first index of the storage that is used. *@PolyValue
    private final int offset;

    @PolyValue** The count is the number of characters in the String. *@PolyValue
    private final int count;

    @PolyValue** Cache the hash code for the string *@PolyValue
    private int hash; @PolyValue Default to 0

    @PolyValue** use serialVersionUID from JDK 1.0.2 for interoperability *@PolyValue
    private static final long serialVersionUID = -6849794470754667710L;

    @PolyValue**
     * Class String is special cased within the Serialization Stream Protocol.
     *
     * A String instance is written initially into an ObjectOutputStream in the
     * following format:
     * <pre>
     *      <code>TC_STRING<@PolyValuecode> (utf String)
     * <@PolyValuepre>
     * The String is written by method <code>DataOutput.writeUTF<@PolyValuecode>.
     * A new handle is generated to  refer to all future references to the
     * string instance within the stream.
     *@PolyValue
    private static final ObjectStreamField[] serialPersistentFields =
        new ObjectStreamField[0];

    @PolyValue**
     * Initializes a newly created {@code String} object so that it represents
     * an empty character sequence.  Note that use of this constructor is
     * unnecessary since Strings are immutable.
     *@PolyValue
    public String() {
        this.offset = 0;
        this.count = 0;
        this.value = new char[0];
    }

    @PolyValue**
     * Initializes a newly created {@code String} object so that it represents
     * the same sequence of characters as the argument; in other words, the
     * newly created string is a copy of the argument string. Unless an
     * explicit copy of {@code original} is needed, use of this constructor is
     * unnecessary since Strings are immutable.
     *
     * @param  original
     *         A {@code String}
     *@PolyValue
    public @PolyValue*!PolyMinLen*@PolyValue @PolySameLen String(@PolyValue*!PolyMinLen*@PolyValue @PolySameLen String original) {
        int size = original.count;
        char[] originalValue = original.value;
        char[] v;
        if (originalValue.length > size) {
            @PolyValue The array representing the String is bigger than the new
            @PolyValue String itself.  Perhaps this constructor is being called
            @PolyValue in order to trim the baggage, so make a copy of the array.
            int off = original.offset;
            v = Arrays.copyOfRange(originalValue, off, off+size);
        } else {
            @PolyValue The array representing the String is the same
            @PolyValue size as the String, so no point in making a copy.
            v = originalValue;
        }
        this.offset = 0;
        this.count = size;
        this.value = v;
    }

    @PolyValue**
     * Allocates a new {@code String} so that it represents the sequence of
     * characters currently contained in the character array argument. The
     * contents of the character array are copied; subsequent modification of
     * the character array does not affect the newly created string.
     *
     * @param  value
     *         The initial value of the string
     *@PolyValue
    public @PolyValue*!PolyMinLen*@PolyValue @PolySameLen String(char @PolyValue*!PolyMinLen*@PolyValue @PolySameLen [] value) {
        int size = value.length;
        this.offset = 0;
        this.count = size;
        this.value = Arrays.copyOf(value, size);
    }

    @PolyValue**
     * Allocates a new {@code String} that contains characters from a subarray
     * of the character array argument. The {@code offset} argument is the
     * index of the first character of the subarray and the {@code count}
     * argument specifies the length of the subarray. The contents of the
     * subarray are copied; subsequent modification of the character array does
     * not affect the newly created string.
     *
     * @param  value
     *         Array that is the source of characters
     *
     * @param  offset
     *         The initial offset
     *
     * @param  count
     *         The length
     *
     * @throws  IndexOutOfBoundsException
     *          If the {@code offset} and {@code count} arguments index
     *          characters outside the bounds of the {@code value} array
     *@PolyValue
    public String(char value[], @IndexFor("#1") int offset, @IndexOrHigh("#1") int count) {
        if (offset < 0) {
            throw new StringIndexOutOfBoundsException(offset);
        }
        if (count < 0) {
            throw new StringIndexOutOfBoundsException(count);
        }
        @PolyValue Note: offset or count might be near -1>>>1.
        if (offset > value.length - count) {
            throw new StringIndexOutOfBoundsException(offset + count);
        }
        this.offset = 0;
        this.count = count;
        this.value = Arrays.copyOfRange(value, offset, offset+count);
    }

    @PolyValue**
     * Allocates a new {@code String} that contains characters from a subarray
     * of the <a href="Character.html#unicode">Unicode code point<@PolyValuea> array
     * argument.  The {@code offset} argument is the index of the first code
     * point of the subarray and the {@code count} argument specifies the
     * length of the subarray.  The contents of the subarray are converted to
     * {@code char}s; subsequent modification of the {@code int} array does not
     * affect the newly created string.
     *
     * @param  codePoints
     *         Array that is the source of Unicode code points
     *
     * @param  offset
     *         The initial offset
     *
     * @param  count
     *         The length
     *
     * @throws  IllegalArgumentException
     *          If any invalid Unicode code point is found in {@code
     *          codePoints}
     *
     * @throws  IndexOutOfBoundsException
     *          If the {@code offset} and {@code count} arguments index
     *          characters outside the bounds of the {@code codePoints} array
     *
     * @since  1.5
     *@PolyValue
    public String(int[] codePoints, @IndexFor("#1") int offset, @IndexOrHigh("#1") int count) {
        if (offset < 0) {
            throw new StringIndexOutOfBoundsException(offset);
        }
        if (count < 0) {
            throw new StringIndexOutOfBoundsException(count);
        }
        @PolyValue Note: offset or count might be near -1>>>1.
        if (offset > codePoints.length - count) {
            throw new StringIndexOutOfBoundsException(offset + count);
        }

        final int end = offset + count;

        @PolyValue Pass 1: Compute precise size of char[]
        int n = count;
        for (int i = offset; i < end; i++) {
            int c = codePoints[i];
            if (Character.isBmpCodePoint(c))
                continue;
            else if (Character.isValidCodePoint(c))
                n++;
            else throw new IllegalArgumentException(Integer.toString(c));
        }

        @PolyValue Pass 2: Allocate and fill in char[]
        final char[] v = new char[n];

        for (int i = offset, j = 0; i < end; i++, j++) {
            int c = codePoints[i];
            if (Character.isBmpCodePoint(c))
                v[j] = (char) c;
            else
                Character.toSurrogates(c, v, j++);
        }

        this.value  = v;
        this.count  = n;
        this.offset = 0;
    }

    @PolyValue**
     * Allocates a new {@code String} constructed from a subarray of an array
     * of 8-bit integer values.
     *
     * <p> The {@code offset} argument is the index of the first byte of the
     * subarray, and the {@code count} argument specifies the length of the
     * subarray.
     *
     * <p> Each {@code byte} in the subarray is converted to a {@code char} as
     * specified in the method above.
     *
     * @deprecated This method does not properly convert bytes into characters.
     * As of JDK&nbsp;1.1, the preferred way to do this is via the
     * {@code String} constructors that take a {@link
     * java.nio.charset.Charset}, charset name, or that use the platform's
     * default charset.
     *
     * @param  ascii
     *         The bytes to be converted to characters
     *
     * @param  hibyte
     *         The top 8 bits of each 16-bit Unicode code unit
     *
     * @param  offset
     *         The initial offset
     * @param  count
     *         The length
     *
     * @throws  IndexOutOfBoundsException
     *          If the {@code offset} or {@code count} argument is invalid
     *
     * @see  #String(byte[], int)
     * @see  #String(byte[], int, int, java.lang.String)
     * @see  #String(byte[], int, int, java.nio.charset.Charset)
     * @see  #String(byte[], int, int)
     * @see  #String(byte[], java.lang.String)
     * @see  #String(byte[], java.nio.charset.Charset)
     * @see  #String(byte[])
     *@PolyValue
    @Deprecated
    public String(byte ascii[], int hibyte, @IndexFor("#1") int offset, @IndexOrHigh("#1") int count) {
        checkBounds(ascii, offset, count);
        char value[] = new char[count];

        if (hibyte == 0) {
            for (int i = count ; i-- > 0 ;) {
                value[i] = (char) (ascii[i + offset] & 0xff);
            }
        } else {
            hibyte <<= 8;
            for (int i = count ; i-- > 0 ;) {
                value[i] = (char) (hibyte | (ascii[i + offset] & 0xff));
            }
        }
        this.offset = 0;
        this.count = count;
        this.value = value;
    }

    @PolyValue**
     * Allocates a new {@code String} containing characters constructed from
     * an array of 8-bit integer values. Each character <i>c<@PolyValuei>in the
     * resulting string is constructed from the corresponding component
     * <i>b<@PolyValuei> in the byte array such that:
     *
     * <blockquote><pre>
     *     <b><i>c<@PolyValuei><@PolyValueb> == (char)(((hibyte &amp; 0xff) &lt;&lt; 8)
     *                         | (<b><i>b<@PolyValuei><@PolyValueb> &amp; 0xff))
     * <@PolyValuepre><@PolyValueblockquote>
     *
     * @deprecated  This method does not properly convert bytes into
     * characters.  As of JDK&nbsp;1.1, the preferred way to do this is via the
     * {@code String} constructors that take a {@link
     * java.nio.charset.Charset}, charset name, or that use the platform's
     * default charset.
     *
     * @param  ascii
     *         The bytes to be converted to characters
     *
     * @param  hibyte
     *         The top 8 bits of each 16-bit Unicode code unit
     *
     * @see  #String(byte[], int, int, java.lang.String)
     * @see  #String(byte[], int, int, java.nio.charset.Charset)
     * @see  #String(byte[], int, int)
     * @see  #String(byte[], java.lang.String)
     * @see  #String(byte[], java.nio.charset.Charset)
     * @see  #String(byte[])
     *@PolyValue
    @Deprecated
    public @PolyValue*!PolyMinLen*@PolyValue @PolySameLen String(byte @PolyValue*!PolyMinLen*@PolyValue @PolySameLen [] ascii, int hibyte) {
        this(ascii, hibyte, 0, ascii.length);
    }

    @PolyValue* Common private utility method used to bounds check the byte array
     * and requested offset & length values used by the String(byte[],..)
     * constructors.
     *@PolyValue
    private static void checkBounds(byte[] bytes, int offset, int length) {
        if (length < 0)
            throw new StringIndexOutOfBoundsException(length);
        if (offset < 0)
            throw new StringIndexOutOfBoundsException(offset);
        if (offset > bytes.length - length)
            throw new StringIndexOutOfBoundsException(offset + length);
    }

    @PolyValue**
     * Constructs a new {@code String} by decoding the specified subarray of
     * bytes using the specified charset.  The length of the new {@code String}
     * is a function of the charset, and hence may not be equal to the length
     * of the subarray.
     *
     * <p> The behavior of this constructor when the given bytes are not valid
     * in the given charset is unspecified.  The {@link
     * java.nio.charset.CharsetDecoder} class should be used when more control
     * over the decoding process is required.
     *
     * @param  bytes
     *         The bytes to be decoded into characters
     *
     * @param  offset
     *         The index of the first byte to decode
     *
     * @param  length
     *         The number of bytes to decode

     * @param  charsetName
     *         The name of a supported {@linkplain java.nio.charset.Charset
     *         charset}
     *
     * @throws  UnsupportedEncodingException
     *          If the named charset is not supported
     *
     * @throws  IndexOutOfBoundsException
     *          If the {@code offset} and {@code length} arguments index
     *          characters outside the bounds of the {@code bytes} array
     *
     * @since  JDK1.1
     *@PolyValue
    public String(byte bytes[], @IndexFor("#1") int offset, @IndexOrHigh("#1") int length, String charsetName)
        throws UnsupportedEncodingException
    {
        if (charsetName == null)
            throw new NullPointerException("charsetName");
        checkBounds(bytes, offset, length);
        char[] v = StringCoding.decode(charsetName, bytes, offset, length);
        this.offset = 0;
        this.count = v.length;
        this.value = v;
    }

    @PolyValue**
     * Constructs a new {@code String} by decoding the specified subarray of
     * bytes using the specified {@linkplain java.nio.charset.Charset charset}.
     * The length of the new {@code String} is a function of the charset, and
     * hence may not be equal to the length of the subarray.
     *
     * <p> This method always replaces malformed-input and unmappable-character
     * sequences with this charset's default replacement string.  The {@link
     * java.nio.charset.CharsetDecoder} class should be used when more control
     * over the decoding process is required.
     *
     * @param  bytes
     *         The bytes to be decoded into characters
     *
     * @param  offset
     *         The index of the first byte to decode
     *
     * @param  length
     *         The number of bytes to decode
     *
     * @param  charset
     *         The {@linkplain java.nio.charset.Charset charset} to be used to
     *         decode the {@code bytes}
     *
     * @throws  IndexOutOfBoundsException
     *          If the {@code offset} and {@code length} arguments index
     *          characters outside the bounds of the {@code bytes} array
     *
     * @since  1.6
     *@PolyValue
    public String(byte bytes[], @IndexFor("#1") int offset, @IndexOrHigh("#1") int length, Charset charset) {
        if (charset == null)
            throw new NullPointerException("charset");
        checkBounds(bytes, offset, length);
        char[] v = StringCoding.decode(charset, bytes, offset, length);
        this.offset = 0;
        this.count = v.length;
        this.value = v;
    }

    @PolyValue**
     * Constructs a new {@code String} by decoding the specified array of bytes
     * using the specified {@linkplain java.nio.charset.Charset charset}.  The
     * length of the new {@code String} is a function of the charset, and hence
     * may not be equal to the length of the byte array.
     *
     * <p> The behavior of this constructor when the given bytes are not valid
     * in the given charset is unspecified.  The {@link
     * java.nio.charset.CharsetDecoder} class should be used when more control
     * over the decoding process is required.
     *
     * @param  bytes
     *         The bytes to be decoded into characters
     *
     * @param  charsetName
     *         The name of a supported {@linkplain java.nio.charset.Charset
     *         charset}
     *
     * @throws  UnsupportedEncodingException
     *          If the named charset is not supported
     *
     * @since  JDK1.1
     *@PolyValue
    public String(byte bytes[], String charsetName)
        throws UnsupportedEncodingException
    {
        this(bytes, 0, bytes.length, charsetName);
    }

    @PolyValue**
     * Constructs a new {@code String} by decoding the specified array of
     * bytes using the specified {@linkplain java.nio.charset.Charset charset}.
     * The length of the new {@code String} is a function of the charset, and
     * hence may not be equal to the length of the byte array.
     *
     * <p> This method always replaces malformed-input and unmappable-character
     * sequences with this charset's default replacement string.  The {@link
     * java.nio.charset.CharsetDecoder} class should be used when more control
     * over the decoding process is required.
     *
     * @param  bytes
     *         The bytes to be decoded into characters
     *
     * @param  charset
     *         The {@linkplain java.nio.charset.Charset charset} to be used to
     *         decode the {@code bytes}
     *
     * @since  1.6
     *@PolyValue
    public String(byte bytes[], Charset charset) {
        this(bytes, 0, bytes.length, charset);
    }

    @PolyValue**
     * Constructs a new {@code String} by decoding the specified subarray of
     * bytes using the platform's default charset.  The length of the new
     * {@code String} is a function of the charset, and hence may not be equal
     * to the length of the subarray.
     *
     * <p> The behavior of this constructor when the given bytes are not valid
     * in the default charset is unspecified.  The {@link
     * java.nio.charset.CharsetDecoder} class should be used when more control
     * over the decoding process is required.
     *
     * @param  bytes
     *         The bytes to be decoded into characters
     *
     * @param  offset
     *         The index of the first byte to decode
     *
     * @param  length
     *         The number of bytes to decode
     *
     * @throws  IndexOutOfBoundsException
     *          If the {@code offset} and the {@code length} arguments index
     *          characters outside the bounds of the {@code bytes} array
     *
     * @since  JDK1.1
     *@PolyValue
    public String(byte bytes[], @IndexFor("#1") int offset, @IndexOrHigh("#1") int length) {
        checkBounds(bytes, offset, length);
        char[] v  = StringCoding.decode(bytes, offset, length);
        this.offset = 0;
        this.count = v.length;
        this.value = v;
    }

    @PolyValue**
     * Constructs a new {@code String} by decoding the specified array of bytes
     * using the platform's default charset.  The length of the new {@code
     * String} is a function of the charset, and hence may not be equal to the
     * length of the byte array.
     *
     * <p> The behavior of this constructor when the given bytes are not valid
     * in the default charset is unspecified.  The {@link
     * java.nio.charset.CharsetDecoder} class should be used when more control
     * over the decoding process is required.
     *
     * @param  bytes
     *         The bytes to be decoded into characters
     *
     * @since  JDK1.1
     *@PolyValue
    public String(byte bytes[]) {
        this(bytes, 0, bytes.length);
    }

    @PolyValue**
     * Allocates a new string that contains the sequence of characters
     * currently contained in the string buffer argument. The contents of the
     * string buffer are copied; subsequent modification of the string buffer
     * does not affect the newly created string.
     *
     * @param  buffer
     *         A {@code StringBuffer}
     *@PolyValue
    public String(StringBuffer buffer) {
        String result = buffer.toString();
        this.value = result.value;
        this.count = result.count;
        this.offset = result.offset;
    }

    @PolyValue**
     * Allocates a new string that contains the sequence of characters
     * currently contained in the string builder argument. The contents of the
     * string builder are copied; subsequent modification of the string builder
     * does not affect the newly created string.
     *
     * <p> This constructor is provided to ease migration to {@code
     * StringBuilder}. Obtaining a string from a string builder via the {@code
     * toString} method is likely to run faster and is generally preferred.
     *
     * @param   builder
     *          A {@code StringBuilder}
     *
     * @since  1.5
     *@PolyValue
    public String(StringBuilder builder) {
        String result = builder.toString();
        this.value = result.value;
        this.count = result.count;
        this.offset = result.offset;
    }


    @PolyValue Package private constructor which shares value array for speed.
    String(int offset, int count, char value[]) {
        this.value = value;
        this.offset = offset;
        this.count = count;
    }

    @PolyValue**
     * Returns the length of this string.
     * The length is equal to the number of <a href="Character.html#unicode">Unicode
     * code units<@PolyValuea> in the string.
     *
     * @return  the length of the sequence of characters represented by this
     *          object.
     *@PolyValue
    public @NonNegative @PolyValue*!IndexOrHigh("this")*@PolyValue int length() {
        return count;
    }

    @PolyValue**
     * Returns <tt>true<@PolyValuett> if, and only if, {@link #length()} is <tt>0<@PolyValuett>.
     *
     * @return <tt>true<@PolyValuett> if {@link #length()} is <tt>0<@PolyValuett>, otherwise
     * <tt>false<@PolyValuett>
     *
     * @since 1.6
     *@PolyValue
    public boolean isEmpty() {
        return count == 0;
    }

    @PolyValue**
     * Returns the <code>char<@PolyValuecode> value at the
     * specified index. An index ranges from <code>0<@PolyValuecode> to
     * <code>length() - 1<@PolyValuecode>. The first <code>char<@PolyValuecode> value of the sequence
     * is at index <code>0<@PolyValuecode>, the next at index <code>1<@PolyValuecode>,
     * and so on, as for array indexing.
     *
     * <p>If the <code>char<@PolyValuecode> value specified by the index is a
     * <a href="Character.html#unicode">surrogate<@PolyValuea>, the surrogate
     * value is returned.
     *
     * @param      index   the index of the <code>char<@PolyValuecode> value.
     * @return     the <code>char<@PolyValuecode> value at the specified index of this string.
     *             The first <code>char<@PolyValuecode> value is at index <code>0<@PolyValuecode>.
     * @exception  IndexOutOfBoundsException  if the <code>index<@PolyValuecode>
     *             argument is negative or not less than the length of this
     *             string.
     *@PolyValue
    public char charAt(@PolyValue*!IndexFor("this")*@PolyValue int index) {
        if ((index < 0) || (index >= count)) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return value[index + offset];
    }

    @PolyValue**
     * Returns the character (Unicode code point) at the specified
     * index. The index refers to <code>char<@PolyValuecode> values
     * (Unicode code units) and ranges from <code>0<@PolyValuecode> to
     * {@link #length()}<code> - 1<@PolyValuecode>.
     *
     * <p> If the <code>char<@PolyValuecode> value specified at the given index
     * is in the high-surrogate range, the following index is less
     * than the length of this <code>String<@PolyValuecode>, and the
     * <code>char<@PolyValuecode> value at the following index is in the
     * low-surrogate range, then the supplementary code point
     * corresponding to this surrogate pair is returned. Otherwise,
     * the <code>char<@PolyValuecode> value at the given index is returned.
     *
     * @param      index the index to the <code>char<@PolyValuecode> values
     * @return     the code point value of the character at the
     *             <code>index<@PolyValuecode>
     * @exception  IndexOutOfBoundsException  if the <code>index<@PolyValuecode>
     *             argument is negative or not less than the length of this
     *             string.
     * @since      1.5
     *@PolyValue
    public int codePointAt(@PolyValue*!IndexFor("this")*@PolyValue int index) {
        if ((index < 0) || (index >= count)) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return Character.codePointAtImpl(value, offset + index, offset + count);
    }

    @PolyValue**
     * Returns the character (Unicode code point) before the specified
     * index. The index refers to <code>char<@PolyValuecode> values
     * (Unicode code units) and ranges from <code>1<@PolyValuecode> to {@link
     * CharSequence#length() length}.
     *
     * <p> If the <code>char<@PolyValuecode> value at <code>(index - 1)<@PolyValuecode>
     * is in the low-surrogate range, <code>(index - 2)<@PolyValuecode> is not
     * negative, and the <code>char<@PolyValuecode> value at <code>(index -
     * 2)<@PolyValuecode> is in the high-surrogate range, then the
     * supplementary code point value of the surrogate pair is
     * returned. If the <code>char<@PolyValuecode> value at <code>index -
     * 1<@PolyValuecode> is an unpaired low-surrogate or a high-surrogate, the
     * surrogate value is returned.
     *
     * @param     index the index following the code point that should be returned
     * @return    the Unicode code point value before the given index.
     * @exception IndexOutOfBoundsException if the <code>index<@PolyValuecode>
     *            argument is less than 1 or greater than the length
     *            of this string.
     * @since     1.5
     *@PolyValue
    public int codePointBefore(@PolyValue*!IndexFor("this")*@PolyValue int index) {
        int i = index - 1;
        if ((i < 0) || (i >= count)) {
            throw new StringIndexOutOfBoundsException(index);
        }
        return Character.codePointBeforeImpl(value, offset + index, offset);
    }

    @PolyValue**
     * Returns the number of Unicode code points in the specified text
     * range of this <code>String<@PolyValuecode>. The text range begins at the
     * specified <code>beginIndex<@PolyValuecode> and extends to the
     * <code>char<@PolyValuecode> at index <code>endIndex - 1<@PolyValuecode>. Thus the
     * length (in <code>char<@PolyValuecode>s) of the text range is
     * <code>endIndex-beginIndex<@PolyValuecode>. Unpaired surrogates within
     * the text range count as one code point each.
     *
     * @param beginIndex the index to the first <code>char<@PolyValuecode> of
     * the text range.
     * @param endIndex the index after the last <code>char<@PolyValuecode> of
     * the text range.
     * @return the number of Unicode code points in the specified text
     * range
     * @exception IndexOutOfBoundsException if the
     * <code>beginIndex<@PolyValuecode> is negative, or <code>endIndex<@PolyValuecode>
     * is larger than the length of this <code>String<@PolyValuecode>, or
     * <code>beginIndex<@PolyValuecode> is larger than <code>endIndex<@PolyValuecode>.
     * @since  1.5
     *@PolyValue
    public int codePointCount(@PolyValue*!IndexFor("this")*@PolyValue int beginIndex, @PolyValue*!IndexOrHigh("this")*@PolyValue int endIndex) {
        if (beginIndex < 0 || endIndex > count || beginIndex > endIndex) {
            throw new IndexOutOfBoundsException();
        }
        return Character.codePointCountImpl(value, offset+beginIndex, endIndex-beginIndex);
    }

    @PolyValue**
     * Returns the index within this <code>String<@PolyValuecode> that is
     * offset from the given <code>index<@PolyValuecode> by
     * <code>codePointOffset<@PolyValuecode> code points. Unpaired surrogates
     * within the text range given by <code>index<@PolyValuecode> and
     * <code>codePointOffset<@PolyValuecode> count as one code point each.
     *
     * @param index the index to be offset
     * @param codePointOffset the offset in code points
     * @return the index within this <code>String<@PolyValuecode>
     * @exception IndexOutOfBoundsException if <code>index<@PolyValuecode>
     *   is negative or larger then the length of this
     *   <code>String<@PolyValuecode>, or if <code>codePointOffset<@PolyValuecode> is positive
     *   and the substring starting with <code>index<@PolyValuecode> has fewer
     *   than <code>codePointOffset<@PolyValuecode> code points,
     *   or if <code>codePointOffset<@PolyValuecode> is negative and the substring
     *   before <code>index<@PolyValuecode> has fewer than the absolute value
     *   of <code>codePointOffset<@PolyValuecode> code points.
     * @since 1.5
     *@PolyValue
    public @PolyValue*!IndexFor("this")*@PolyValue int offsetByCodePoints(@PolyValue*!IndexFor("this")*@PolyValue int index, int codePointOffset) {
        if (index < 0 || index > count) {
            throw new IndexOutOfBoundsException();
        }
        return Character.offsetByCodePointsImpl(value, offset, count,
                                                offset+index, codePointOffset) - offset;
    }

    @PolyValue**
     * Copy characters from this string into dst starting at dstBegin.
     * This method doesn't perform any range checking.
     *@PolyValue
    void getChars(char dst[], int dstBegin) {
        System.arraycopy(value, offset, dst, dstBegin, count);
    }

    @PolyValue**
     * Copies characters from this string into the destination character
     * array.
     * <p>
     * The first character to be copied is at index <code>srcBegin<@PolyValuecode>;
     * the last character to be copied is at index <code>srcEnd-1<@PolyValuecode>
     * (thus the total number of characters to be copied is
     * <code>srcEnd-srcBegin<@PolyValuecode>). The characters are copied into the
     * subarray of <code>dst<@PolyValuecode> starting at index <code>dstBegin<@PolyValuecode>
     * and ending at index:
     * <p><blockquote><pre>
     *     dstbegin + (srcEnd-srcBegin) - 1
     * <@PolyValuepre><@PolyValueblockquote>
     *
     * @param      srcBegin   index of the first character in the string
     *                        to copy.
     * @param      srcEnd     index after the last character in the string
     *                        to copy.
     * @param      dst        the destination array.
     * @param      dstBegin   the start offset in the destination array.
     * @exception IndexOutOfBoundsException If any of the following
     *            is true:
     *            <ul><li><code>srcBegin<@PolyValuecode> is negative.
     *            <li><code>srcBegin<@PolyValuecode> is greater than <code>srcEnd<@PolyValuecode>
     *            <li><code>srcEnd<@PolyValuecode> is greater than the length of this
     *                string
     *            <li><code>dstBegin<@PolyValuecode> is negative
     *            <li><code>dstBegin+(srcEnd-srcBegin)<@PolyValuecode> is larger than
     *                <code>dst.length<@PolyValuecode><@PolyValueul>
     *@PolyValue
    public void getChars(@PolyValue*!IndexFor("this")*@PolyValue int srcBegin, @PolyValue*!IndexOrHigh("this")*@PolyValue int srcEnd, char dst[], @IndexFor("#3") int dstBegin) {
        if (srcBegin < 0) {
            throw new StringIndexOutOfBoundsException(srcBegin);
        }
        if (srcEnd > count) {
            throw new StringIndexOutOfBoundsException(srcEnd);
        }
        if (srcBegin > srcEnd) {
            throw new StringIndexOutOfBoundsException(srcEnd - srcBegin);
        }
        System.arraycopy(value, offset + srcBegin, dst, dstBegin,
             srcEnd - srcBegin);
    }

    @PolyValue**
     * Copies characters from this string into the destination byte array. Each
     * byte receives the 8 low-order bits of the corresponding character. The
     * eight high-order bits of each character are not copied and do not
     * participate in the transfer in any way.
     *
     * <p> The first character to be copied is at index {@code srcBegin}; the
     * last character to be copied is at index {@code srcEnd-1}.  The total
     * number of characters to be copied is {@code srcEnd-srcBegin}. The
     * characters, converted to bytes, are copied into the subarray of {@code
     * dst} starting at index {@code dstBegin} and ending at index:
     *
     * <blockquote><pre>
     *     dstbegin + (srcEnd-srcBegin) - 1
     * <@PolyValuepre><@PolyValueblockquote>
     *
     * @deprecated  This method does not properly convert characters into
     * bytes.  As of JDK&nbsp;1.1, the preferred way to do this is via the
     * {@link #getBytes()} method, which uses the platform's default charset.
     *
     * @param  srcBegin
     *         Index of the first character in the string to copy
     *
     * @param  srcEnd
     *         Index after the last character in the string to copy
     *
     * @param  dst
     *         The destination array
     *
     * @param  dstBegin
     *         The start offset in the destination array
     *
     * @throws  IndexOutOfBoundsException
     *          If any of the following is true:
     *          <ul>
     *            <li> {@code srcBegin} is negative
     *            <li> {@code srcBegin} is greater than {@code srcEnd}
     *            <li> {@code srcEnd} is greater than the length of this String
     *            <li> {@code dstBegin} is negative
     *            <li> {@code dstBegin+(srcEnd-srcBegin)} is larger than {@code
     *                 dst.length}
     *          <@PolyValueul>
     *@PolyValue
    @Deprecated
    public void getBytes(@PolyValue*!IndexFor("this")*@PolyValue int srcBegin, @PolyValue*!IndexOrHigh("#1")*@PolyValue int srcEnd, byte dst[], @IndexFor("#3") int dstBegin) {
        if (srcBegin < 0) {
            throw new StringIndexOutOfBoundsException(srcBegin);
        }
        if (srcEnd > count) {
            throw new StringIndexOutOfBoundsException(srcEnd);
        }
        if (srcBegin > srcEnd) {
            throw new StringIndexOutOfBoundsException(srcEnd - srcBegin);
        }
        int j = dstBegin;
        int n = offset + srcEnd;
        int i = offset + srcBegin;
        char[] val = value;   @PolyValue* avoid getfield opcode *@PolyValue

        while (i < n) {
            dst[j++] = (byte)val[i++];
        }
    }

    @PolyValue**
     * Encodes this {@code String} into a sequence of bytes using the named
     * charset, storing the result into a new byte array.
     *
     * <p> The behavior of this method when this string cannot be encoded in
     * the given charset is unspecified.  The {@link
     * java.nio.charset.CharsetEncoder} class should be used when more control
     * over the encoding process is required.
     *
     * @param  charsetName
     *         The name of a supported {@linkplain java.nio.charset.Charset
     *         charset}
     *
     * @return  The resultant byte array
     *
     * @throws  UnsupportedEncodingException
     *          If the named charset is not supported
     *
     * @since  JDK1.1
     *@PolyValue
    public byte[] getBytes(String charsetName)
        throws UnsupportedEncodingException
    {
        if (charsetName == null) throw new NullPointerException();
        return StringCoding.encode(charsetName, value, offset, count);
    }

    @PolyValue**
     * Encodes this {@code String} into a sequence of bytes using the given
     * {@linkplain java.nio.charset.Charset charset}, storing the result into a
     * new byte array.
     *
     * <p> This method always replaces malformed-input and unmappable-character
     * sequences with this charset's default replacement byte array.  The
     * {@link java.nio.charset.CharsetEncoder} class should be used when more
     * control over the encoding process is required.
     *
     * @param  charset
     *         The {@linkplain java.nio.charset.Charset} to be used to encode
     *         the {@code String}
     *
     * @return  The resultant byte array
     *
     * @since  1.6
     *@PolyValue
    public byte[] getBytes(Charset charset) {
        if (charset == null) throw new NullPointerException();
        return StringCoding.encode(charset, value, offset, count);
    }

    @PolyValue**
     * Encodes this {@code String} into a sequence of bytes using the
     * platform's default charset, storing the result into a new byte array.
     *
     * <p> The behavior of this method when this string cannot be encoded in
     * the default charset is unspecified.  The {@link
     * java.nio.charset.CharsetEncoder} class should be used when more control
     * over the encoding process is required.
     *
     * @return  The resultant byte array
     *
     * @since      JDK1.1
     *@PolyValue
    public byte[] getBytes() {
        return StringCoding.encode(value, offset, count);
    }

    @PolyValue**
     * Compares this string to the specified object.  The result is {@code
     * true} if and only if the argument is not {@code null} and is a {@code
     * String} object that represents the same sequence of characters as this
     * object.
     *
     * @param  anObject
     *         The object to compare this {@code String} against
     *
     * @return  {@code true} if the given object represents a {@code String}
     *          equivalent to this string, {@code false} otherwise
     *
     * @see  #compareTo(String)
     * @see  #equalsIgnoreCase(String)
     *@PolyValue
    public boolean equals(Object anObject) {
        if (this == anObject) {
            return true;
        }
        if (anObject instanceof String) {
            String anotherString = (String)anObject;
            int n = count;
            if (n == anotherString.count) {
                char v1[] = value;
                char v2[] = anotherString.value;
                int i = offset;
                int j = anotherString.offset;
                while (n-- != 0) {
                    if (v1[i++] != v2[j++])
                        return false;
                }
                return true;
            }
        }
        return false;
    }

    @PolyValue**
     * Compares this string to the specified {@code StringBuffer}.  The result
     * is {@code true} if and only if this {@code String} represents the same
     * sequence of characters as the specified {@code StringBuffer}.
     *
     * @param  sb
     *         The {@code StringBuffer} to compare this {@code String} against
     *
     * @return  {@code true} if this {@code String} represents the same
     *          sequence of characters as the specified {@code StringBuffer},
     *          {@code false} otherwise
     *
     * @since  1.4
     *@PolyValue
    public boolean contentEquals(StringBuffer sb) {
        synchronized(sb) {
            return contentEquals((CharSequence)sb);
        }
    }

    @PolyValue**
     * Compares this string to the specified {@code CharSequence}.  The result
     * is {@code true} if and only if this {@code String} represents the same
     * sequence of char values as the specified sequence.
     *
     * @param  cs
     *         The sequence to compare this {@code String} against
     *
     * @return  {@code true} if this {@code String} represents the same
     *          sequence of char values as the specified sequence, {@code
     *          false} otherwise
     *
     * @since  1.5
     *@PolyValue
    public boolean contentEquals(CharSequence cs) {
        if (count != cs.length())
            return false;
        @PolyValue Argument is a StringBuffer, StringBuilder
        if (cs instanceof AbstractStringBuilder) {
            char v1[] = value;
            char v2[] = ((AbstractStringBuilder)cs).getValue();
            int i = offset;
            int j = 0;
            int n = count;
            while (n-- != 0) {
                if (v1[i++] != v2[j++])
                    return false;
            }
            return true;
        }
        @PolyValue Argument is a String
        if (cs.equals(this))
            return true;
        @PolyValue Argument is a generic CharSequence
        char v1[] = value;
        int i = offset;
        int j = 0;
        int n = count;
        while (n-- != 0) {
            if (v1[i++] != cs.charAt(j++))
                return false;
        }
        return true;
    }

    @PolyValue**
     * Compares this {@code String} to another {@code String}, ignoring case
     * considerations.  Two strings are considered equal ignoring case if they
     * are of the same length and corresponding characters in the two strings
     * are equal ignoring case.
     *
     * <p> Two characters {@code c1} and {@code c2} are considered the same
     * ignoring case if at least one of the following is true:
     * <ul>
     *   <li> The two characters are the same (as compared by the
     *        {@code ==} operator)
     *   <li> Applying the method {@link
     *        java.lang.Character#toUpperCase(char)} to each character
     *        produces the same result
     *   <li> Applying the method {@link
     *        java.lang.Character#toLowerCase(char)} to each character
     *        produces the same result
     * <@PolyValueul>
     *
     * @param  anotherString
     *         The {@code String} to compare this {@code String} against
     *
     * @return  {@code true} if the argument is not {@code null} and it
     *          represents an equivalent {@code String} ignoring case; {@code
     *          false} otherwise
     *
     * @see  #equals(Object)
     *@PolyValue
    public boolean equalsIgnoreCase(String anotherString) {
        return (this == anotherString) ? true :
               (anotherString != null) && (anotherString.count == count) &&
               regionMatches(true, 0, anotherString, 0, count);
    }

    @PolyValue**
     * Compares two strings lexicographically.
     * The comparison is based on the Unicode value of each character in
     * the strings. The character sequence represented by this
     * <code>String<@PolyValuecode> object is compared lexicographically to the
     * character sequence represented by the argument string. The result is
     * a negative integer if this <code>String<@PolyValuecode> object
     * lexicographically precedes the argument string. The result is a
     * positive integer if this <code>String<@PolyValuecode> object lexicographically
     * follows the argument string. The result is zero if the strings
     * are equal; <code>compareTo<@PolyValuecode> returns <code>0<@PolyValuecode> exactly when
     * the {@link #equals(Object)} method would return <code>true<@PolyValuecode>.
     * <p>
     * This is the definition of lexicographic ordering. If two strings are
     * different, then either they have different characters at some index
     * that is a valid index for both strings, or their lengths are different,
     * or both. If they have different characters at one or more index
     * positions, let <i>k<@PolyValuei> be the smallest such index; then the string
     * whose character at position <i>k<@PolyValuei> has the smaller value, as
     * determined by using the &lt; operator, lexicographically precedes the
     * other string. In this case, <code>compareTo<@PolyValuecode> returns the
     * difference of the two character values at position <code>k<@PolyValuecode> in
     * the two string -- that is, the value:
     * <blockquote><pre>
     * this.charAt(k)-anotherString.charAt(k)
     * <@PolyValuepre><@PolyValueblockquote>
     * If there is no index position at which they differ, then the shorter
     * string lexicographically precedes the longer string. In this case,
     * <code>compareTo<@PolyValuecode> returns the difference of the lengths of the
     * strings -- that is, the value:
     * <blockquote><pre>
     * this.length()-anotherString.length()
     * <@PolyValuepre><@PolyValueblockquote>
     *
     * @param   anotherString   the <code>String<@PolyValuecode> to be compared.
     * @return  the value <code>0<@PolyValuecode> if the argument string is equal to
     *          this string; a value less than <code>0<@PolyValuecode> if this string
     *          is lexicographically less than the string argument; and a
     *          value greater than <code>0<@PolyValuecode> if this string is
     *          lexicographically greater than the string argument.
     *@PolyValue
    public int compareTo(String anotherString) {
        int len1 = count;
        int len2 = anotherString.count;
        int n = Math.min(len1, len2);
        char v1[] = value;
        char v2[] = anotherString.value;
        int i = offset;
        int j = anotherString.offset;

        if (i == j) {
            int k = i;
            int lim = n + i;
            while (k < lim) {
                char c1 = v1[k];
                char c2 = v2[k];
                if (c1 != c2) {
                    return c1 - c2;
                }
                k++;
            }
        } else {
            while (n-- != 0) {
                char c1 = v1[i++];
                char c2 = v2[j++];
                if (c1 != c2) {
                    return c1 - c2;
                }
            }
        }
        return len1 - len2;
    }

    @PolyValue**
     * A Comparator that orders <code>String<@PolyValuecode> objects as by
     * <code>compareToIgnoreCase<@PolyValuecode>. This comparator is serializable.
     * <p>
     * Note that this Comparator does <em>not<@PolyValueem> take locale into account,
     * and will result in an unsatisfactory ordering for certain locales.
     * The java.text package provides <em>Collators<@PolyValueem> to allow
     * locale-sensitive ordering.
     *
     * @see     java.text.Collator#compare(String, String)
     * @since   1.2
     *@PolyValue
    public static final Comparator<String> CASE_INSENSITIVE_ORDER
                                         = new CaseInsensitiveComparator();
    private static class CaseInsensitiveComparator
                         implements Comparator<String>, java.io.Serializable {
        @PolyValue use serialVersionUID from JDK 1.2.2 for interoperability
        private static final long serialVersionUID = 8575799808933029326L;

        public int compare(String s1, String s2) {
            int n1 = s1.length();
            int n2 = s2.length();
            int min = Math.min(n1, n2);
            for (int i = 0; i < min; i++) {
                char c1 = s1.charAt(i);
                char c2 = s2.charAt(i);
                if (c1 != c2) {
                    c1 = Character.toUpperCase(c1);
                    c2 = Character.toUpperCase(c2);
                    if (c1 != c2) {
                        c1 = Character.toLowerCase(c1);
                        c2 = Character.toLowerCase(c2);
                        if (c1 != c2) {
                            @PolyValue No overflow because of numeric promotion
                            return c1 - c2;
                        }
                    }
                }
            }
            return n1 - n2;
        }
    }

    @PolyValue**
     * Compares two strings lexicographically, ignoring case
     * differences. This method returns an integer whose sign is that of
     * calling <code>compareTo<@PolyValuecode> with normalized versions of the strings
     * where case differences have been eliminated by calling
     * <code>Character.toLowerCase(Character.toUpperCase(character))<@PolyValuecode> on
     * each character.
     * <p>
     * Note that this method does <em>not<@PolyValueem> take locale into account,
     * and will result in an unsatisfactory ordering for certain locales.
     * The java.text package provides <em>collators<@PolyValueem> to allow
     * locale-sensitive ordering.
     *
     * @param   str   the <code>String<@PolyValuecode> to be compared.
     * @return  a negative integer, zero, or a positive integer as the
     *          specified String is greater than, equal to, or less
     *          than this String, ignoring case considerations.
     * @see     java.text.Collator#compare(String, String)
     * @since   1.2
     *@PolyValue
    public int compareToIgnoreCase(String str) {
        return CASE_INSENSITIVE_ORDER.compare(this, str);
    }

    @PolyValue**
     * Tests if two string regions are equal.
     * <p>
     * A substring of this <tt>String<@PolyValuett> object is compared to a substring
     * of the argument other. The result is true if these substrings
     * represent identical character sequences. The substring of this
     * <tt>String<@PolyValuett> object to be compared begins at index <tt>toffset<@PolyValuett>
     * and has length <tt>len<@PolyValuett>. The substring of other to be compared
     * begins at index <tt>ooffset<@PolyValuett> and has length <tt>len<@PolyValuett>. The
     * result is <tt>false<@PolyValuett> if and only if at least one of the following
     * is true:
     * <ul><li><tt>toffset<@PolyValuett> is negative.
     * <li><tt>ooffset<@PolyValuett> is negative.
     * <li><tt>toffset+len<@PolyValuett> is greater than the length of this
     * <tt>String<@PolyValuett> object.
     * <li><tt>ooffset+len<@PolyValuett> is greater than the length of the other
     * argument.
     * <li>There is some nonnegative integer <i>k<@PolyValuei> less than <tt>len<@PolyValuett>
     * such that:
     * <tt>this.charAt(toffset+<i>k<@PolyValuei>)&nbsp;!=&nbsp;other.charAt(ooffset+<i>k<@PolyValuei>)<@PolyValuett>
     * <@PolyValueul>
     *
     * @param   toffset   the starting offset of the subregion in this string.
     * @param   other     the string argument.
     * @param   ooffset   the starting offset of the subregion in the string
     *                    argument.
     * @param   len       the number of characters to compare.
     * @return  <code>true<@PolyValuecode> if the specified subregion of this string
     *          exactly matches the specified subregion of the string argument;
     *          <code>false<@PolyValuecode> otherwise.
     *@PolyValue
    public boolean regionMatches(@PolyValue*!IndexFor("this")*@PolyValue int toffset, String other, @IndexFor("#2") int ooffset,
                                 @PolyValue*!IndexOrHigh({"this","#2"})*@PolyValue
				 @IndexOrHigh("#2") int len) {
        char ta[] = value;
        int to = offset + toffset;
        char pa[] = other.value;
        int po = other.offset + ooffset;
        @PolyValue Note: toffset, ooffset, or len might be near -1>>>1.
        if ((ooffset < 0) || (toffset < 0) || (toffset > (long)count - len)
            || (ooffset > (long)other.count - len)) {
            return false;
        }
        while (len-- > 0) {
            if (ta[to++] != pa[po++]) {
                return false;
            }
        }
        return true;
    }

    @PolyValue**
     * Tests if two string regions are equal.
     * <p>
     * A substring of this <tt>String<@PolyValuett> object is compared to a substring
     * of the argument <tt>other<@PolyValuett>. The result is <tt>true<@PolyValuett> if these
     * substrings represent character sequences that are the same, ignoring
     * case if and only if <tt>ignoreCase<@PolyValuett> is true. The substring of
     * this <tt>String<@PolyValuett> object to be compared begins at index
     * <tt>toffset<@PolyValuett> and has length <tt>len<@PolyValuett>. The substring of
     * <tt>other<@PolyValuett> to be compared begins at index <tt>ooffset<@PolyValuett> and
     * has length <tt>len<@PolyValuett>. The result is <tt>false<@PolyValuett> if and only if
     * at least one of the following is true:
     * <ul><li><tt>toffset<@PolyValuett> is negative.
     * <li><tt>ooffset<@PolyValuett> is negative.
     * <li><tt>toffset+len<@PolyValuett> is greater than the length of this
     * <tt>String<@PolyValuett> object.
     * <li><tt>ooffset+len<@PolyValuett> is greater than the length of the other
     * argument.
     * <li><tt>ignoreCase<@PolyValuett> is <tt>false<@PolyValuett> and there is some nonnegative
     * integer <i>k<@PolyValuei> less than <tt>len<@PolyValuett> such that:
     * <blockquote><pre>
     * this.charAt(toffset+k) != other.charAt(ooffset+k)
     * <@PolyValuepre><@PolyValueblockquote>
     * <li><tt>ignoreCase<@PolyValuett> is <tt>true<@PolyValuett> and there is some nonnegative
     * integer <i>k<@PolyValuei> less than <tt>len<@PolyValuett> such that:
     * <blockquote><pre>
     * Character.toLowerCase(this.charAt(toffset+k)) !=
               Character.toLowerCase(other.charAt(ooffset+k))
     * <@PolyValuepre><@PolyValueblockquote>
     * and:
     * <blockquote><pre>
     * Character.toUpperCase(this.charAt(toffset+k)) !=
     *         Character.toUpperCase(other.charAt(ooffset+k))
     * <@PolyValuepre><@PolyValueblockquote>
     * <@PolyValueul>
     *
     * @param   ignoreCase   if <code>true<@PolyValuecode>, ignore case when comparing
     *                       characters.
     * @param   toffset      the starting offset of the subregion in this
     *                       string.
     * @param   other        the string argument.
     * @param   ooffset      the starting offset of the subregion in the string
     *                       argument.
     * @param   len          the number of characters to compare.
     * @return  <code>true<@PolyValuecode> if the specified subregion of this string
     *          matches the specified subregion of the string argument;
     *          <code>false<@PolyValuecode> otherwise. Whether the matching is exact
     *          or case insensitive depends on the <code>ignoreCase<@PolyValuecode>
     *          argument.
     *@PolyValue
    public boolean regionMatches(boolean ignoreCase, @PolyValue*!IndexFor("this")*@PolyValue int toffset,
                           String other, @IndexFor("#2") int ooffset,
				 @PolyValue*!IndexOrHigh({"this","#2"})*@PolyValue
				 @IndexOrHigh("#2") int len) {
        char ta[] = value;
        int to = offset + toffset;
        char pa[] = other.value;
        int po = other.offset + ooffset;
        @PolyValue Note: toffset, ooffset, or len might be near -1>>>1.
        if ((ooffset < 0) || (toffset < 0) || (toffset > (long)count - len) ||
                (ooffset > (long)other.count - len)) {
            return false;
        }
        while (len-- > 0) {
            char c1 = ta[to++];
            char c2 = pa[po++];
            if (c1 == c2) {
                continue;
            }
            if (ignoreCase) {
                @PolyValue If characters don't match but case may be ignored,
                @PolyValue try converting both characters to uppercase.
                @PolyValue If the results match, then the comparison scan should
                @PolyValue continue.
                char u1 = Character.toUpperCase(c1);
                char u2 = Character.toUpperCase(c2);
                if (u1 == u2) {
                    continue;
                }
                @PolyValue Unfortunately, conversion to uppercase does not work properly
                @PolyValue for the Georgian alphabet, which has strange rules about case
                @PolyValue conversion.  So we need to make one last check before
                @PolyValue exiting.
                if (Character.toLowerCase(u1) == Character.toLowerCase(u2)) {
                    continue;
                }
            }
            return false;
        }
        return true;
    }

    @PolyValue**
     * Tests if the substring of this string beginning at the
     * specified index starts with the specified prefix.
     *
     * @param   prefix    the prefix.
     * @param   toffset   where to begin looking in this string.
     * @return  <code>true<@PolyValuecode> if the character sequence represented by the
     *          argument is a prefix of the substring of this object starting
     *          at index <code>toffset<@PolyValuecode>; <code>false<@PolyValuecode> otherwise.
     *          The result is <code>false<@PolyValuecode> if <code>toffset<@PolyValuecode> is
     *          negative or greater than the length of this
     *          <code>String<@PolyValuecode> object; otherwise the result is the same
     *          as the result of the expression
     *          <pre>
     *          this.substring(toffset).startsWith(prefix)
     *          <@PolyValuepre>
     *@PolyValue
    public boolean startsWith(String prefix, @PolyValue* !IndexFor("this")*@PolyValue int toffset) {
        char ta[] = value;
        int to = offset + toffset;
        char pa[] = prefix.value;
        int po = prefix.offset;
        int pc = prefix.count;
        @PolyValue Note: toffset might be near -1>>>1.
        if ((toffset < 0) || (toffset > count - pc)) {
            return false;
        }
        while (--pc >= 0) {
            if (ta[to++] != pa[po++]) {
                return false;
            }
        }
        return true;
    }

    @PolyValue**
     * Tests if this string starts with the specified prefix.
     *
     * @param   prefix   the prefix.
     * @return  <code>true<@PolyValuecode> if the character sequence represented by the
     *          argument is a prefix of the character sequence represented by
     *          this string; <code>false<@PolyValuecode> otherwise.
     *          Note also that <code>true<@PolyValuecode> will be returned if the
     *          argument is an empty string or is equal to this
     *          <code>String<@PolyValuecode> object as determined by the
     *          {@link #equals(Object)} method.
     * @since   1. 0
     *@PolyValue
    public boolean startsWith(String prefix) {
        return startsWith(prefix, 0);
    }

    @PolyValue**
     * Tests if this string ends with the specified suffix.
     *
     * @param   suffix   the suffix.
     * @return  <code>true<@PolyValuecode> if the character sequence represented by the
     *          argument is a suffix of the character sequence represented by
     *          this object; <code>false<@PolyValuecode> otherwise. Note that the
     *          result will be <code>true<@PolyValuecode> if the argument is the
     *          empty string or is equal to this <code>String<@PolyValuecode> object
     *          as determined by the {@link #equals(Object)} method.
     *@PolyValue
    public boolean endsWith(String suffix) {
        return startsWith(suffix, count - suffix.count);
    }

    @PolyValue**
     * Returns a hash code for this string. The hash code for a
     * <code>String<@PolyValuecode> object is computed as
     * <blockquote><pre>
     * s[0]*31^(n-1) + s[1]*31^(n-2) + ... + s[n-1]
     * <@PolyValuepre><@PolyValueblockquote>
     * using <code>int<@PolyValuecode> arithmetic, where <code>s[i]<@PolyValuecode> is the
     * <i>i<@PolyValuei>th character of the string, <code>n<@PolyValuecode> is the length of
     * the string, and <code>^<@PolyValuecode> indicates exponentiation.
     * (The hash value of the empty string is zero.)
     *
     * @return  a hash code value for this object.
     *@PolyValue
    public int hashCode() {
        int h = hash;
        if (h == 0 && count > 0) {
            int off = offset;
            char val[] = value;
            int len = count;

            for (int i = 0; i < len; i++) {
                h = 31*h + val[off++];
            }
            hash = h;
        }
        return h;
    }

    @PolyValue**
     * Returns the index within this string of the first occurrence of
     * the specified character. If a character with value
     * <code>ch<@PolyValuecode> occurs in the character sequence represented by
     * this <code>String<@PolyValuecode> object, then the index (in Unicode
     * code units) of the first such occurrence is returned. For
     * values of <code>ch<@PolyValuecode> in the range from 0 to 0xFFFF
     * (inclusive), this is the smallest value <i>k<@PolyValuei> such that:
     * <blockquote><pre>
     * this.charAt(<i>k<@PolyValuei>) == ch
     * <@PolyValuepre><@PolyValueblockquote>
     * is true. For other values of <code>ch<@PolyValuecode>, it is the
     * smallest value <i>k<@PolyValuei> such that:
     * <blockquote><pre>
     * this.codePointAt(<i>k<@PolyValuei>) == ch
     * <@PolyValuepre><@PolyValueblockquote>
     * is true. In either case, if no such character occurs in this
     * string, then <code>-1<@PolyValuecode> is returned.
     *
     * @param   ch   a character (Unicode code point).
     * @return  the index of the first occurrence of the character in the
     *          character sequence represented by this object, or
     *          <code>-1<@PolyValuecode> if the character does not occur.
     *@PolyValue
    public @GTENegativeOne int indexOf(int ch) {
        return indexOf(ch, 0);
    }

    @PolyValue**
     * Returns the index within this string of the first occurrence of the
     * specified character, starting the search at the specified index.
     * <p>
     * If a character with value <code>ch<@PolyValuecode> occurs in the
     * character sequence represented by this <code>String<@PolyValuecode>
     * object at an index no smaller than <code>fromIndex<@PolyValuecode>, then
     * the index of the first such occurrence is returned. For values
     * of <code>ch<@PolyValuecode> in the range from 0 to 0xFFFF (inclusive),
     * this is the smallest value <i>k<@PolyValuei> such that:
     * <blockquote><pre>
     * (this.charAt(<i>k<@PolyValuei>) == ch) && (<i>k<@PolyValuei> &gt;= fromIndex)
     * <@PolyValuepre><@PolyValueblockquote>
     * is true. For other values of <code>ch<@PolyValuecode>, it is the
     * smallest value <i>k<@PolyValuei> such that:
     * <blockquote><pre>
     * (this.codePointAt(<i>k<@PolyValuei>) == ch) && (<i>k<@PolyValuei> &gt;= fromIndex)
     * <@PolyValuepre><@PolyValueblockquote>
     * is true. In either case, if no such character occurs in this
     * string at or after position <code>fromIndex<@PolyValuecode>, then
     * <code>-1<@PolyValuecode> is returned.
     *
     * <p>
     * There is no restriction on the value of <code>fromIndex<@PolyValuecode>. If it
     * is negative, it has the same effect as if it were zero: this entire
     * string may be searched. If it is greater than the length of this
     * string, it has the same effect as if it were equal to the length of
     * this string: <code>-1<@PolyValuecode> is returned.
     *
     * <p>All indices are specified in <code>char<@PolyValuecode> values
     * (Unicode code units).
     *
     * @param   ch          a character (Unicode code point).
     * @param   fromIndex   the index to start the search from.
     * @return  the index of the first occurrence of the character in the
     *          character sequence represented by this object that is greater
     *          than or equal to <code>fromIndex<@PolyValuecode>, or <code>-1<@PolyValuecode>
     *          if the character does not occur.
     *@PolyValue
    public @GTENegativeOne int indexOf(int ch, @PolyValue*!IndexFor("this")*@PolyValue int fromIndex) {
        if (fromIndex < 0) {
            fromIndex = 0;
        } else if (fromIndex >= count) {
            @PolyValue Note: fromIndex might be near -1>>>1.
            return -1;
        }

        if (ch < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
            @PolyValue handle most cases here (ch is a BMP code point or a
            @PolyValue negative value (invalid code point))
            final char[] value = this.value;
            final int offset = this.offset;
            final int max = offset + count;
            for (int i = offset + fromIndex; i < max ; i++) {
                if (value[i] == ch) {
                    return i - offset;
                }
            }
            return -1;
        } else {
            return indexOfSupplementary(ch, fromIndex);
        }
    }

    @PolyValue**
     * Handles (rare) calls of indexOf with a supplementary character.
     *@PolyValue
    private int indexOfSupplementary(int ch, int fromIndex) {
        if (Character.isValidCodePoint(ch)) {
            final char[] value = this.value;
            final int offset = this.offset;
            final char hi = Character.highSurrogate(ch);
            final char lo = Character.lowSurrogate(ch);
            final int max = offset + count - 1;
            for (int i = offset + fromIndex; i < max; i++) {
                if (value[i] == hi && value[i+1] == lo) {
                    return i - offset;
                }
            }
        }
        return -1;
    }

    @PolyValue**
     * Returns the index within this string of the last occurrence of
     * the specified character. For values of <code>ch<@PolyValuecode> in the
     * range from 0 to 0xFFFF (inclusive), the index (in Unicode code
     * units) returned is the largest value <i>k<@PolyValuei> such that:
     * <blockquote><pre>
     * this.charAt(<i>k<@PolyValuei>) == ch
     * <@PolyValuepre><@PolyValueblockquote>
     * is true. For other values of <code>ch<@PolyValuecode>, it is the
     * largest value <i>k<@PolyValuei> such that:
     * <blockquote><pre>
     * this.codePointAt(<i>k<@PolyValuei>) == ch
     * <@PolyValuepre><@PolyValueblockquote>
     * is true.  In either case, if no such character occurs in this
     * string, then <code>-1<@PolyValuecode> is returned.  The
     * <code>String<@PolyValuecode> is searched backwards starting at the last
     * character.
     *
     * @param   ch   a character (Unicode code point).
     * @return  the index of the last occurrence of the character in the
     *          character sequence represented by this object, or
     *          <code>-1<@PolyValuecode> if the character does not occur.
     *@PolyValue
    public @GTENegativeOne int lastIndexOf(int ch) {
        return lastIndexOf(ch, count - 1);
    }

    @PolyValue**
     * Returns the index within this string of the last occurrence of
     * the specified character, searching backward starting at the
     * specified index. For values of <code>ch<@PolyValuecode> in the range
     * from 0 to 0xFFFF (inclusive), the index returned is the largest
     * value <i>k<@PolyValuei> such that:
     * <blockquote><pre>
     * (this.charAt(<i>k<@PolyValuei>) == ch) && (<i>k<@PolyValuei> &lt;= fromIndex)
     * <@PolyValuepre><@PolyValueblockquote>
     * is true. For other values of <code>ch<@PolyValuecode>, it is the
     * largest value <i>k<@PolyValuei> such that:
     * <blockquote><pre>
     * (this.codePointAt(<i>k<@PolyValuei>) == ch) && (<i>k<@PolyValuei> &lt;= fromIndex)
     * <@PolyValuepre><@PolyValueblockquote>
     * is true. In either case, if no such character occurs in this
     * string at or before position <code>fromIndex<@PolyValuecode>, then
     * <code>-1<@PolyValuecode> is returned.
     *
     * <p>All indices are specified in <code>char<@PolyValuecode> values
     * (Unicode code units).
     *
     * @param   ch          a character (Unicode code point).
     * @param   fromIndex   the index to start the search from. There is no
     *          restriction on the value of <code>fromIndex<@PolyValuecode>. If it is
     *          greater than or equal to the length of this string, it has
     *          the same effect as if it were equal to one less than the
     *          length of this string: this entire string may be searched.
     *          If it is negative, it has the same effect as if it were -1:
     *          -1 is returned.
     * @return  the index of the last occurrence of the character in the
     *          character sequence represented by this object that is less
     *          than or equal to <code>fromIndex<@PolyValuecode>, or <code>-1<@PolyValuecode>
     *          if the character does not occur before that point.
     *@PolyValue
    public @GTENegativeOne int lastIndexOf(int ch, @PolyValue*!IndexFor("this")*@PolyValue int fromIndex) {
        if (ch < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
            @PolyValue handle most cases here (ch is a BMP code point or a
            @PolyValue negative value (invalid code point))
            final char[] value = this.value;
            final int offset = this.offset;
            int i = offset + Math.min(fromIndex, count - 1);
            for (; i >= offset ; i--) {
                if (value[i] == ch) {
                    return i - offset;
                }
            }
            return -1;
        } else {
            return lastIndexOfSupplementary(ch, fromIndex);
        }
    }

    @PolyValue**
     * Handles (rare) calls of lastIndexOf with a supplementary character.
     *@PolyValue
    private int lastIndexOfSupplementary(int ch, int fromIndex) {
        if (Character.isValidCodePoint(ch)) {
            final char[] value = this.value;
            final int offset = this.offset;
            char hi = Character.highSurrogate(ch);
            char lo = Character.lowSurrogate(ch);
            int i = offset + Math.min(fromIndex, count - 2);
            for (; i >= offset; i--) {
                if (value[i] == hi && value[i+1] == lo) {
                    return i - offset;
                }
            }
        }
        return -1;
    }

    @PolyValue**
     * Returns the index within this string of the first occurrence of the
     * specified substring.
     *
     * <p>The returned index is the smallest value <i>k<@PolyValuei> for which:
     * <blockquote><pre>
     * this.startsWith(str, <i>k<@PolyValuei>)
     * <@PolyValuepre><@PolyValueblockquote>
     * If no such value of <i>k<@PolyValuei> exists, then {@code -1} is returned.
     *
     * @param   str   the substring to search for.
     * @return  the index of the first occurrence of the specified substring,
     *          or {@code -1} if there is no such occurrence.
     *@PolyValue
    public @GTENegativeOne int indexOf(String str) {
        return indexOf(str, 0);
    }

    @PolyValue**
     * Returns the index within this string of the first occurrence of the
     * specified substring, starting at the specified index.
     *
     * <p>The returned index is the smallest value <i>k<@PolyValuei> for which:
     * <blockquote><pre>
     * <i>k<@PolyValuei> &gt;= fromIndex && this.startsWith(str, <i>k<@PolyValuei>)
     * <@PolyValuepre><@PolyValueblockquote>
     * If no such value of <i>k<@PolyValuei> exists, then {@code -1} is returned.
     *
     * @param   str         the substring to search for.
     * @param   fromIndex   the index from which to start the search.
     * @return  the index of the first occurrence of the specified substring,
     *          starting at the specified index,
     *          or {@code -1} if there is no such occurrence.
     *@PolyValue
    public @GTENegativeOne int indexOf(String str, @PolyValue*!IndexFor("this")*@PolyValue int fromIndex) {
        return indexOf(value, offset, count,
                       str.value, str.offset, str.count, fromIndex);
    }

    @PolyValue**
     * Code shared by String and StringBuffer to do searches. The
     * source is the character array being searched, and the target
     * is the string being searched for.
     *
     * @param   source       the characters being searched.
     * @param   sourceOffset offset of the source string.
     * @param   sourceCount  count of the source string.
     * @param   target       the characters being searched for.
     * @param   targetOffset offset of the target string.
     * @param   targetCount  count of the target string.
     * @param   fromIndex    the index to begin searching from.
     *@PolyValue
    static int indexOf(char[] source, int sourceOffset, int sourceCount,
                       char[] target, int targetOffset, int targetCount,
                       int fromIndex) {
        if (fromIndex >= sourceCount) {
            return (targetCount == 0 ? sourceCount : -1);
        }
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        if (targetCount == 0) {
            return fromIndex;
        }

        char first  = target[targetOffset];
        int max = sourceOffset + (sourceCount - targetCount);

        for (int i = sourceOffset + fromIndex; i <= max; i++) {
            @PolyValue* Look for first character. *@PolyValue
            if (source[i] != first) {
                while (++i <= max && source[i] != first);
            }

            @PolyValue* Found first character, now look at the rest of v2 *@PolyValue
            if (i <= max) {
                int j = i + 1;
                int end = j + targetCount - 1;
                for (int k = targetOffset + 1; j < end && source[j] ==
                         target[k]; j++, k++);

                if (j == end) {
                    @PolyValue* Found whole string. *@PolyValue
                    return i - sourceOffset;
                }
            }
        }
        return -1;
    }

    @PolyValue**
     * Returns the index within this string of the last occurrence of the
     * specified substring.  The last occurrence of the empty string ""
     * is considered to occur at the index value {@code this.length()}.
     *
     * <p>The returned index is the largest value <i>k<@PolyValuei> for which:
     * <blockquote><pre>
     * this.startsWith(str, <i>k<@PolyValuei>)
     * <@PolyValuepre><@PolyValueblockquote>
     * If no such value of <i>k<@PolyValuei> exists, then {@code -1} is returned.
     *
     * @param   str   the substring to search for.
     * @return  the index of the last occurrence of the specified substring,
     *          or {@code -1} if there is no such occurrence.
     *@PolyValue
    public @GTENegativeOne int lastIndexOf(String str) {
        return lastIndexOf(str, count);
    }

    @PolyValue**
     * Returns the index within this string of the last occurrence of the
     * specified substring, searching backward starting at the specified index.
     *
     * <p>The returned index is the largest value <i>k<@PolyValuei> for which:
     * <blockquote><pre>
     * <i>k<@PolyValuei> &lt;= fromIndex && this.startsWith(str, <i>k<@PolyValuei>)
     * <@PolyValuepre><@PolyValueblockquote>
     * If no such value of <i>k<@PolyValuei> exists, then {@code -1} is returned.
     *
     * @param   str         the substring to search for.
     * @param   fromIndex   the index to start the search from.
     * @return  the index of the last occurrence of the specified substring,
     *          searching backward from the specified index,
     *          or {@code -1} if there is no such occurrence.
     *@PolyValue
    public @GTENegativeOne int lastIndexOf(String str, @PolyValue*!IndexFor("this")*@PolyValue int fromIndex) {
        return lastIndexOf(value, offset, count,
                           str.value, str.offset, str.count, fromIndex);
    }

    @PolyValue**
     * Code shared by String and StringBuffer to do searches. The
     * source is the character array being searched, and the target
     * is the string being searched for.
     *
     * @param   source       the characters being searched.
     * @param   sourceOffset offset of the source string.
     * @param   sourceCount  count of the source string.
     * @param   target       the characters being searched for.
     * @param   targetOffset offset of the target string.
     * @param   targetCount  count of the target string.
     * @param   fromIndex    the index to begin searching from.
     *@PolyValue
    static int lastIndexOf(char[] source, int sourceOffset, int sourceCount,
                           char[] target, int targetOffset, int targetCount,
                           int fromIndex) {
        @PolyValue*
         * Check arguments; return immediately where possible. For
         * consistency, don't check for null str.
         *@PolyValue
        int rightIndex = sourceCount - targetCount;
        if (fromIndex < 0) {
            return -1;
        }
        if (fromIndex > rightIndex) {
            fromIndex = rightIndex;
        }
        @PolyValue* Empty string always matches. *@PolyValue
        if (targetCount == 0) {
            return fromIndex;
        }

        int strLastIndex = targetOffset + targetCount - 1;
        char strLastChar = target[strLastIndex];
        int min = sourceOffset + targetCount - 1;
        int i = min + fromIndex;

    startSearchForLastChar:
        while (true) {
            while (i >= min && source[i] != strLastChar) {
                i--;
            }
            if (i < min) {
                return -1;
            }
            int j = i - 1;
            int start = j - (targetCount - 1);
            int k = strLastIndex - 1;

            while (j > start) {
                if (source[j--] != target[k--]) {
                    i--;
                    continue startSearchForLastChar;
                }
            }
            return start - sourceOffset + 1;
        }
    }

    @PolyValue**
     * Returns a new string that is a substring of this string. The
     * substring begins with the character at the specified index and
     * extends to the end of this string. <p>
     * Examples:
     * <blockquote><pre>
     * "unhappy".substring(2) returns "happy"
     * "Harbison".substring(3) returns "bison"
     * "emptiness".substring(9) returns "" (an empty string)
     * <@PolyValuepre><@PolyValueblockquote>
     *
     * @param      beginIndex   the beginning index, inclusive.
     * @return     the specified substring.
     * @exception  IndexOutOfBoundsException  if
     *             <code>beginIndex<@PolyValuecode> is negative or larger than the
     *             length of this <code>String<@PolyValuecode> object.
     *@PolyValue
    public String substring(@PolyValue*!IndexFor("this")*@PolyValue int beginIndex) {
        return substring(beginIndex, count);
    }

    @PolyValue**
     * Returns a new string that is a substring of this string. The
     * substring begins at the specified <code>beginIndex<@PolyValuecode> and
     * extends to the character at index <code>endIndex - 1<@PolyValuecode>.
     * Thus the length of the substring is <code>endIndex-beginIndex<@PolyValuecode>.
     * <p>
     * Examples:
     * <blockquote><pre>
     * "hamburger".substring(4, 8) returns "urge"
     * "smiles".substring(1, 5) returns "mile"
     * <@PolyValuepre><@PolyValueblockquote>
     *
     * @param      beginIndex   the beginning index, inclusive.
     * @param      endIndex     the ending index, exclusive.
     * @return     the specified substring.
     * @exception  IndexOutOfBoundsException  if the
     *             <code>beginIndex<@PolyValuecode> is negative, or
     *             <code>endIndex<@PolyValuecode> is larger than the length of
     *             this <code>String<@PolyValuecode> object, or
     *             <code>beginIndex<@PolyValuecode> is larger than
     *             <code>endIndex<@PolyValuecode>.
     *@PolyValue
    public String substring(@PolyValue*!IndexFor("this")*@PolyValue int beginIndex, @PolyValue*!IndexOrHigh("this")*@PolyValue int endIndex) {
        if (beginIndex < 0) {
            throw new StringIndexOutOfBoundsException(beginIndex);
        }
        if (endIndex > count) {
            throw new StringIndexOutOfBoundsException(endIndex);
        }
        if (beginIndex > endIndex) {
            throw new StringIndexOutOfBoundsException(endIndex - beginIndex);
        }
        return ((beginIndex == 0) && (endIndex == count)) ? this :
            new String(offset + beginIndex, endIndex - beginIndex, value);
    }

    @PolyValue**
     * Returns a new character sequence that is a subsequence of this sequence.
     *
     * <p> An invocation of this method of the form
     *
     * <blockquote><pre>
     * str.subSequence(begin,&nbsp;end)<@PolyValuepre><@PolyValueblockquote>
     *
     * behaves in exactly the same way as the invocation
     *
     * <blockquote><pre>
     * str.substring(begin,&nbsp;end)<@PolyValuepre><@PolyValueblockquote>
     *
     * This method is defined so that the <tt>String<@PolyValuett> class can implement
     * the {@link CharSequence} interface. <@PolyValuep>
     *
     * @param      beginIndex   the begin index, inclusive.
     * @param      endIndex     the end index, exclusive.
     * @return     the specified subsequence.
     *
     * @throws  IndexOutOfBoundsException
     *          if <tt>beginIndex<@PolyValuett> or <tt>endIndex<@PolyValuett> are negative,
     *          if <tt>endIndex<@PolyValuett> is greater than <tt>length()<@PolyValuett>,
     *          or if <tt>beginIndex<@PolyValuett> is greater than <tt>startIndex<@PolyValuett>
     *
     * @since 1.4
     * @spec JSR-51
     *@PolyValue
    public CharSequence subSequence(@PolyValue*!IndexFor("this")*@PolyValue int beginIndex, @PolyValue*!IndexOrHigh("this")*@PolyValue int endIndex) {
        return this.substring(beginIndex, endIndex);
    }

    @PolyValue**
     * Concatenates the specified string to the end of this string.
     * <p>
     * If the length of the argument string is <code>0<@PolyValuecode>, then this
     * <code>String<@PolyValuecode> object is returned. Otherwise, a new
     * <code>String<@PolyValuecode> object is created, representing a character
     * sequence that is the concatenation of the character sequence
     * represented by this <code>String<@PolyValuecode> object and the character
     * sequence represented by the argument string.<p>
     * Examples:
     * <blockquote><pre>
     * "cares".concat("s") returns "caress"
     * "to".concat("get").concat("her") returns "together"
     * <@PolyValuepre><@PolyValueblockquote>
     *
     * @param   str   the <code>String<@PolyValuecode> that is concatenated to the end
     *                of this <code>String<@PolyValuecode>.
     * @return  a string that represents the concatenation of this object's
     *          characters followed by the string argument's characters.
     *@PolyValue
    public String concat(String str) {
        int otherLen = str.length();
        if (otherLen == 0) {
            return this;
        }
        char buf[] = new char[count + otherLen];
        getChars(0, count, buf, 0);
        str.getChars(0, otherLen, buf, count);
        return new String(0, count + otherLen, buf);
    }

    @PolyValue**
     * Returns a new string resulting from replacing all occurrences of
     * <code>oldChar<@PolyValuecode> in this string with <code>newChar<@PolyValuecode>.
     * <p>
     * If the character <code>oldChar<@PolyValuecode> does not occur in the
     * character sequence represented by this <code>String<@PolyValuecode> object,
     * then a reference to this <code>String<@PolyValuecode> object is returned.
     * Otherwise, a new <code>String<@PolyValuecode> object is created that
     * represents a character sequence identical to the character sequence
     * represented by this <code>String<@PolyValuecode> object, except that every
     * occurrence of <code>oldChar<@PolyValuecode> is replaced by an occurrence
     * of <code>newChar<@PolyValuecode>.
     * <p>
     * Examples:
     * <blockquote><pre>
     * "mesquite in your cellar".replace('e', 'o')
     *         returns "mosquito in your collar"
     * "the war of baronets".replace('r', 'y')
     *         returns "the way of bayonets"
     * "sparring with a purple porpoise".replace('p', 't')
     *         returns "starring with a turtle tortoise"
     * "JonL".replace('q', 'x') returns "JonL" (no change)
     * <@PolyValuepre><@PolyValueblockquote>
     *
     * @param   oldChar   the old character.
     * @param   newChar   the new character.
     * @return  a string derived from this string by replacing every
     *          occurrence of <code>oldChar<@PolyValuecode> with <code>newChar<@PolyValuecode>.
     *@PolyValue
    public String replace(char oldChar, char newChar) {
        if (oldChar != newChar) {
            int len = count;
            int i = -1;
            char[] val = value; @PolyValue* avoid getfield opcode *@PolyValue
            int off = offset;   @PolyValue* avoid getfield opcode *@PolyValue

            while (++i < len) {
                if (val[off + i] == oldChar) {
                    break;
                }
            }
            if (i < len) {
                char buf[] = new char[len];
                for (int j = 0 ; j < i ; j++) {
                    buf[j] = val[off+j];
                }
                while (i < len) {
                    char c = val[off + i];
                    buf[i] = (c == oldChar) ? newChar : c;
                    i++;
                }
                return new String(0, len, buf);
            }
        }
        return this;
    }

    @PolyValue**
     * Tells whether or not this string matches the given <a
     * href="..@PolyValueutil@PolyValueregex@PolyValuePattern.html#sum">regular expression<@PolyValuea>.
     *
     * <p> An invocation of this method of the form
     * <i>str<@PolyValuei><tt>.matches(<@PolyValuett><i>regex<@PolyValuei><tt>)<@PolyValuett> yields exactly the
     * same result as the expression
     *
     * <blockquote><tt> {@link java.util.regex.Pattern}.{@link
     * java.util.regex.Pattern#matches(String,CharSequence)
     * matches}(<@PolyValuett><i>regex<@PolyValuei><tt>,<@PolyValuett> <i>str<@PolyValuei><tt>)<@PolyValuett><@PolyValueblockquote>
     *
     * @param   regex
     *          the regular expression to which this string is to be matched
     *
     * @return  <tt>true<@PolyValuett> if, and only if, this string matches the
     *          given regular expression
     *
     * @throws  PatternSyntaxException
     *          if the regular expression's syntax is invalid
     *
     * @see java.util.regex.Pattern
     *
     * @since 1.4
     * @spec JSR-51
     *@PolyValue
    public boolean matches(String regex) {
        return Pattern.matches(regex, this);
    }

    @PolyValue**
     * Returns true if and only if this string contains the specified
     * sequence of char values.
     *
     * @param s the sequence to search for
     * @return true if this string contains <code>s<@PolyValuecode>, false otherwise
     * @throws NullPointerException if <code>s<@PolyValuecode> is <code>null<@PolyValuecode>
     * @since 1.5
     *@PolyValue
    public boolean contains(CharSequence s) {
        return indexOf(s.toString()) > -1;
    }

    @PolyValue**
     * Replaces the first substring of this string that matches the given <a
     * href="..@PolyValueutil@PolyValueregex@PolyValuePattern.html#sum">regular expression<@PolyValuea> with the
     * given replacement.
     *
     * <p> An invocation of this method of the form
     * <i>str<@PolyValuei><tt>.replaceFirst(<@PolyValuett><i>regex<@PolyValuei><tt>,<@PolyValuett> <i>repl<@PolyValuei><tt>)<@PolyValuett>
     * yields exactly the same result as the expression
     *
     * <blockquote><tt>
     * {@link java.util.regex.Pattern}.{@link java.util.regex.Pattern#compile
     * compile}(<@PolyValuett><i>regex<@PolyValuei><tt>).{@link
     * java.util.regex.Pattern#matcher(java.lang.CharSequence)
     * matcher}(<@PolyValuett><i>str<@PolyValuei><tt>).{@link java.util.regex.Matcher#replaceFirst
     * replaceFirst}(<@PolyValuett><i>repl<@PolyValuei><tt>)<@PolyValuett><@PolyValueblockquote>
     *
     *<p>
     * Note that backslashes (<tt>\<@PolyValuett>) and dollar signs (<tt>$<@PolyValuett>) in the
     * replacement string may cause the results to be different than if it were
     * being treated as a literal replacement string; see
     * {@link java.util.regex.Matcher#replaceFirst}.
     * Use {@link java.util.regex.Matcher#quoteReplacement} to suppress the special
     * meaning of these characters, if desired.
     *
     * @param   regex
     *          the regular expression to which this string is to be matched
     * @param   replacement
     *          the string to be substituted for the first match
     *
     * @return  The resulting <tt>String<@PolyValuett>
     *
     * @throws  PatternSyntaxException
     *          if the regular expression's syntax is invalid
     *
     * @see java.util.regex.Pattern
     *
     * @since 1.4
     * @spec JSR-51
     *@PolyValue
    public String replaceFirst(String regex, String replacement) {
        return Pattern.compile(regex).matcher(this).replaceFirst(replacement);
    }

    @PolyValue**
     * Replaces each substring of this string that matches the given <a
     * href="..@PolyValueutil@PolyValueregex@PolyValuePattern.html#sum">regular expression<@PolyValuea> with the
     * given replacement.
     *
     * <p> An invocation of this method of the form
     * <i>str<@PolyValuei><tt>.replaceAll(<@PolyValuett><i>regex<@PolyValuei><tt>,<@PolyValuett> <i>repl<@PolyValuei><tt>)<@PolyValuett>
     * yields exactly the same result as the expression
     *
     * <blockquote><tt>
     * {@link java.util.regex.Pattern}.{@link java.util.regex.Pattern#compile
     * compile}(<@PolyValuett><i>regex<@PolyValuei><tt>).{@link
     * java.util.regex.Pattern#matcher(java.lang.CharSequence)
     * matcher}(<@PolyValuett><i>str<@PolyValuei><tt>).{@link java.util.regex.Matcher#replaceAll
     * replaceAll}(<@PolyValuett><i>repl<@PolyValuei><tt>)<@PolyValuett><@PolyValueblockquote>
     *
     *<p>
     * Note that backslashes (<tt>\<@PolyValuett>) and dollar signs (<tt>$<@PolyValuett>) in the
     * replacement string may cause the results to be different than if it were
     * being treated as a literal replacement string; see
     * {@link java.util.regex.Matcher#replaceAll Matcher.replaceAll}.
     * Use {@link java.util.regex.Matcher#quoteReplacement} to suppress the special
     * meaning of these characters, if desired.
     *
     * @param   regex
     *          the regular expression to which this string is to be matched
     * @param   replacement
     *          the string to be substituted for each match
     *
     * @return  The resulting <tt>String<@PolyValuett>
     *
     * @throws  PatternSyntaxException
     *          if the regular expression's syntax is invalid
     *
     * @see java.util.regex.Pattern
     *
     * @since 1.4
     * @spec JSR-51
     *@PolyValue
    public String replaceAll(String regex, String replacement) {
        return Pattern.compile(regex).matcher(this).replaceAll(replacement);
    }

    @PolyValue**
     * Replaces each substring of this string that matches the literal target
     * sequence with the specified literal replacement sequence. The
     * replacement proceeds from the beginning of the string to the end, for
     * example, replacing "aa" with "b" in the string "aaa" will result in
     * "ba" rather than "ab".
     *
     * @param  target The sequence of char values to be replaced
     * @param  replacement The replacement sequence of char values
     * @return  The resulting string
     * @throws NullPointerException if <code>target<@PolyValuecode> or
     *         <code>replacement<@PolyValuecode> is <code>null<@PolyValuecode>.
     * @since 1.5
     *@PolyValue
    public String replace(CharSequence target, CharSequence replacement) {
        return Pattern.compile(target.toString(), Pattern.LITERAL).matcher(
            this).replaceAll(Matcher.quoteReplacement(replacement.toString()));
    }

    @PolyValue**
     * Splits this string around matches of the given
     * <a href="..@PolyValueutil@PolyValueregex@PolyValuePattern.html#sum">regular expression<@PolyValuea>.
     *
     * <p> The array returned by this method contains each substring of this
     * string that is terminated by another substring that matches the given
     * expression or is terminated by the end of the string.  The substrings in
     * the array are in the order in which they occur in this string.  If the
     * expression does not match any part of the input then the resulting array
     * has just one element, namely this string.
     *
     * <p> The <tt>limit<@PolyValuett> parameter controls the number of times the
     * pattern is applied and therefore affects the length of the resulting
     * array.  If the limit <i>n<@PolyValuei> is greater than zero then the pattern
     * will be applied at most <i>n<@PolyValuei>&nbsp;-&nbsp;1 times, the array's
     * length will be no greater than <i>n<@PolyValuei>, and the array's last entry
     * will contain all input beyond the last matched delimiter.  If <i>n<@PolyValuei>
     * is non-positive then the pattern will be applied as many times as
     * possible and the array can have any length.  If <i>n<@PolyValuei> is zero then
     * the pattern will be applied as many times as possible, the array can
     * have any length, and trailing empty strings will be discarded.
     *
     * <p> The string <tt>"boo:and:foo"<@PolyValuett>, for example, yields the
     * following results with these parameters:
     *
     * <blockquote><table cellpadding=1 cellspacing=0 summary="Split example showing regex, limit, and result">
     * <tr>
     *     <th>Regex<@PolyValueth>
     *     <th>Limit<@PolyValueth>
     *     <th>Result<@PolyValueth>
     * <@PolyValuetr>
     * <tr><td align=center>:<@PolyValuetd>
     *     <td align=center>2<@PolyValuetd>
     *     <td><tt>{ "boo", "and:foo" }<@PolyValuett><@PolyValuetd><@PolyValuetr>
     * <tr><td align=center>:<@PolyValuetd>
     *     <td align=center>5<@PolyValuetd>
     *     <td><tt>{ "boo", "and", "foo" }<@PolyValuett><@PolyValuetd><@PolyValuetr>
     * <tr><td align=center>:<@PolyValuetd>
     *     <td align=center>-2<@PolyValuetd>
     *     <td><tt>{ "boo", "and", "foo" }<@PolyValuett><@PolyValuetd><@PolyValuetr>
     * <tr><td align=center>o<@PolyValuetd>
     *     <td align=center>5<@PolyValuetd>
     *     <td><tt>{ "b", "", ":and:f", "", "" }<@PolyValuett><@PolyValuetd><@PolyValuetr>
     * <tr><td align=center>o<@PolyValuetd>
     *     <td align=center>-2<@PolyValuetd>
     *     <td><tt>{ "b", "", ":and:f", "", "" }<@PolyValuett><@PolyValuetd><@PolyValuetr>
     * <tr><td align=center>o<@PolyValuetd>
     *     <td align=center>0<@PolyValuetd>
     *     <td><tt>{ "b", "", ":and:f" }<@PolyValuett><@PolyValuetd><@PolyValuetr>
     * <@PolyValuetable><@PolyValueblockquote>
     *
     * <p> An invocation of this method of the form
     * <i>str.<@PolyValuei><tt>split(<@PolyValuett><i>regex<@PolyValuei><tt>,<@PolyValuett>&nbsp;<i>n<@PolyValuei><tt>)<@PolyValuett>
     * yields the same result as the expression
     *
     * <blockquote>
     * {@link java.util.regex.Pattern}.{@link java.util.regex.Pattern#compile
     * compile}<tt>(<@PolyValuett><i>regex<@PolyValuei><tt>)<@PolyValuett>.{@link
     * java.util.regex.Pattern#split(java.lang.CharSequence,int)
     * split}<tt>(<@PolyValuett><i>str<@PolyValuei><tt>,<@PolyValuett>&nbsp;<i>n<@PolyValuei><tt>)<@PolyValuett>
     * <@PolyValueblockquote>
     *
     *
     * @param  regex
     *         the delimiting regular expression
     *
     * @param  limit
     *         the result threshold, as described above
     *
     * @return  the array of strings computed by splitting this string
     *          around matches of the given regular expression
     *
     * @throws  PatternSyntaxException
     *          if the regular expression's syntax is invalid
     *
     * @see java.util.regex.Pattern
     *
     * @since 1.4
     * @spec JSR-51
     *@PolyValue
    public String @MinLen(1) [] split(String regex, int limit) {
        @PolyValue* fastpath if the regex is a
           (1)one-char String and this character is not one of the
              RegEx's meta characters ".$|()[{^?*+\\", or
           (2)two-char String and the first char is the backslash and
              the second is not the ascii digit or ascii letter.
        *@PolyValue
        char ch = 0;
        if (((regex.count == 1 &&
             ".$|()[{^?*+\\".indexOf(ch = regex.charAt(0)) == -1) ||
             (regex.length() == 2 &&
              regex.charAt(0) == '\\' &&
              (((ch = regex.charAt(1))-'0')|('9'-ch)) < 0 &&
              ((ch-'a')|('z'-ch)) < 0 &&
              ((ch-'A')|('Z'-ch)) < 0)) &&
            (ch < Character.MIN_HIGH_SURROGATE ||
             ch > Character.MAX_LOW_SURROGATE))
        {
            int off = 0;
            int next = 0;
            boolean limited = limit > 0;
            ArrayList<String> list = new ArrayList<>();
            while ((next = indexOf(ch, off)) != -1) {
                if (!limited || list.size() < limit - 1) {
                    list.add(substring(off, next));
                    off = next + 1;
                } else {    @PolyValue last one
                    @PolyValueassert (list.size() == limit - 1);
                    list.add(substring(off, count));
                    off = count;
                    break;
                }
            }
            @PolyValue If no match was found, return this
            if (off == 0)
                return new String[] { this };

            @PolyValue Add remaining segment
            if (!limited || list.size() < limit)
                list.add(substring(off, count));

            @PolyValue Construct result
            int resultSize = list.size();
            if (limit == 0)
                while (resultSize > 0 && list.get(resultSize-1).length() == 0)
                    resultSize--;
            String[] result = new String[resultSize];
            return list.subList(0, resultSize).toArray(result);
        }
        return Pattern.compile(regex).split(this, limit);
    }

    @PolyValue**
     * Splits this string around matches of the given <a
     * href="..@PolyValueutil@PolyValueregex@PolyValuePattern.html#sum">regular expression<@PolyValuea>.
     *
     * <p> This method works as if by invoking the two-argument {@link
     * #split(String, int) split} method with the given expression and a limit
     * argument of zero.  Trailing empty strings are therefore not included in
     * the resulting array.
     *
     * <p> The string <tt>"boo:and:foo"<@PolyValuett>, for example, yields the following
     * results with these expressions:
     *
     * <blockquote><table cellpadding=1 cellspacing=0 summary="Split examples showing regex and result">
     * <tr>
     *  <th>Regex<@PolyValueth>
     *  <th>Result<@PolyValueth>
     * <@PolyValuetr>
     * <tr><td align=center>:<@PolyValuetd>
     *     <td><tt>{ "boo", "and", "foo" }<@PolyValuett><@PolyValuetd><@PolyValuetr>
     * <tr><td align=center>o<@PolyValuetd>
     *     <td><tt>{ "b", "", ":and:f" }<@PolyValuett><@PolyValuetd><@PolyValuetr>
     * <@PolyValuetable><@PolyValueblockquote>
     *
     *
     * @param  regex
     *         the delimiting regular expression
     *
     * @return  the array of strings computed by splitting this string
     *          around matches of the given regular expression
     *
     * @throws  PatternSyntaxException
     *          if the regular expression's syntax is invalid
     *
     * @see java.util.regex.Pattern
     *
     * @since 1.4
     * @spec JSR-51
     *@PolyValue
    public String @MinLen(1) [] split(String regex) {
        return split(regex, 0);
    }

    @PolyValue**
     * Converts all of the characters in this <code>String<@PolyValuecode> to lower
     * case using the rules of the given <code>Locale<@PolyValuecode>.  Case mapping is based
     * on the Unicode Standard version specified by the {@link java.lang.Character Character}
     * class. Since case mappings are not always 1:1 char mappings, the resulting
     * <code>String<@PolyValuecode> may be a different length than the original <code>String<@PolyValuecode>.
     * <p>
     * Examples of lowercase  mappings are in the following table:
     * <table border="1" summary="Lowercase mapping examples showing language code of locale, upper case, lower case, and description">
     * <tr>
     *   <th>Language Code of Locale<@PolyValueth>
     *   <th>Upper Case<@PolyValueth>
     *   <th>Lower Case<@PolyValueth>
     *   <th>Description<@PolyValueth>
     * <@PolyValuetr>
     * <tr>
     *   <td>tr (Turkish)<@PolyValuetd>
     *   <td>&#92;u0130<@PolyValuetd>
     *   <td>&#92;u0069<@PolyValuetd>
     *   <td>capital letter I with dot above -&gt; small letter i<@PolyValuetd>
     * <@PolyValuetr>
     * <tr>
     *   <td>tr (Turkish)<@PolyValuetd>
     *   <td>&#92;u0049<@PolyValuetd>
     *   <td>&#92;u0131<@PolyValuetd>
     *   <td>capital letter I -&gt; small letter dotless i <@PolyValuetd>
     * <@PolyValuetr>
     * <tr>
     *   <td>(all)<@PolyValuetd>
     *   <td>French Fries<@PolyValuetd>
     *   <td>french fries<@PolyValuetd>
     *   <td>lowercased all chars in String<@PolyValuetd>
     * <@PolyValuetr>
     * <tr>
     *   <td>(all)<@PolyValuetd>
     *   <td><img src="doc-files@PolyValuecapiota.gif" alt="capiota"><img src="doc-files@PolyValuecapchi.gif" alt="capchi">
     *       <img src="doc-files@PolyValuecaptheta.gif" alt="captheta"><img src="doc-files@PolyValuecapupsil.gif" alt="capupsil">
     *       <img src="doc-files@PolyValuecapsigma.gif" alt="capsigma"><@PolyValuetd>
     *   <td><img src="doc-files@PolyValueiota.gif" alt="iota"><img src="doc-files@PolyValuechi.gif" alt="chi">
     *       <img src="doc-files@PolyValuetheta.gif" alt="theta"><img src="doc-files@PolyValueupsilon.gif" alt="upsilon">
     *       <img src="doc-files@PolyValuesigma1.gif" alt="sigma"><@PolyValuetd>
     *   <td>lowercased all chars in String<@PolyValuetd>
     * <@PolyValuetr>
     * <@PolyValuetable>
     *
     * @param locale use the case transformation rules for this locale
     * @return the <code>String<@PolyValuecode>, converted to lowercase.
     * @see     java.lang.String#toLowerCase()
     * @see     java.lang.String#toUpperCase()
     * @see     java.lang.String#toUpperCase(Locale)
     * @since   1.1
     *@PolyValue
    public String toLowerCase(Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }

        int     firstUpper;

        @PolyValue* Now check if there are any characters that need to be changed. *@PolyValue
        scan: {
            for (firstUpper = 0 ; firstUpper < count; ) {
                char c = value[offset+firstUpper];
                if ((c >= Character.MIN_HIGH_SURROGATE) &&
                    (c <= Character.MAX_HIGH_SURROGATE)) {
                    int supplChar = codePointAt(firstUpper);
                    if (supplChar != Character.toLowerCase(supplChar)) {
                        break scan;
                    }
                    firstUpper += Character.charCount(supplChar);
                } else {
                    if (c != Character.toLowerCase(c)) {
                        break scan;
                    }
                    firstUpper++;
                }
            }
            return this;
        }

        char[]  result = new char[count];
        int     resultOffset = 0;  @PolyValue* result may grow, so i+resultOffset
                                    * is the write location in result *@PolyValue

        @PolyValue* Just copy the first few lowerCase characters. *@PolyValue
        System.arraycopy(value, offset, result, 0, firstUpper);

        String lang = locale.getLanguage();
        boolean localeDependent =
            (lang == "tr" || lang == "az" || lang == "lt");
        char[] lowerCharArray;
        int lowerChar;
        int srcChar;
        int srcCount;
        for (int i = firstUpper; i < count; i += srcCount) {
            srcChar = (int)value[offset+i];
            if ((char)srcChar >= Character.MIN_HIGH_SURROGATE &&
                (char)srcChar <= Character.MAX_HIGH_SURROGATE) {
                srcChar = codePointAt(i);
                srcCount = Character.charCount(srcChar);
            } else {
                srcCount = 1;
            }
            if (localeDependent || srcChar == '\u03A3') { @PolyValue GREEK CAPITAL LETTER SIGMA
                lowerChar = ConditionalSpecialCasing.toLowerCaseEx(this, i, locale);
            } else if (srcChar == '\u0130') { @PolyValue LATIN CAPITAL LETTER I DOT
                lowerChar = Character.ERROR;
            } else {
                lowerChar = Character.toLowerCase(srcChar);
            }
            if ((lowerChar == Character.ERROR) ||
                (lowerChar >= Character.MIN_SUPPLEMENTARY_CODE_POINT)) {
                if (lowerChar == Character.ERROR) {
                     if (!localeDependent && srcChar == '\u0130') {
                         lowerCharArray =
                             ConditionalSpecialCasing.toLowerCaseCharArray(this, i, Locale.ENGLISH);
                     } else {
                        lowerCharArray =
                            ConditionalSpecialCasing.toLowerCaseCharArray(this, i, locale);
                     }
                } else if (srcCount == 2) {
                    resultOffset += Character.toChars(lowerChar, result, i + resultOffset) - srcCount;
                    continue;
                } else {
                    lowerCharArray = Character.toChars(lowerChar);
                }

                @PolyValue* Grow result if needed *@PolyValue
                int mapLen = lowerCharArray.length;
                if (mapLen > srcCount) {
                    char[] result2 = new char[result.length + mapLen - srcCount];
                    System.arraycopy(result, 0, result2, 0,
                        i + resultOffset);
                    result = result2;
                }
                for (int x=0; x<mapLen; ++x) {
                    result[i+resultOffset+x] = lowerCharArray[x];
                }
                resultOffset += (mapLen - srcCount);
            } else {
                result[i+resultOffset] = (char)lowerChar;
            }
        }
        return new String(0, count+resultOffset, result);
    }

    @PolyValue**
     * Converts all of the characters in this <code>String<@PolyValuecode> to lower
     * case using the rules of the default locale. This is equivalent to calling
     * <code>toLowerCase(Locale.getDefault())<@PolyValuecode>.
     * <p>
     * <b>Note:<@PolyValueb> This method is locale sensitive, and may produce unexpected
     * results if used for strings that are intended to be interpreted locale
     * independently.
     * Examples are programming language identifiers, protocol keys, and HTML
     * tags.
     * For instance, <code>"TITLE".toLowerCase()<@PolyValuecode> in a Turkish locale
     * returns <code>"t\u005Cu0131tle"<@PolyValuecode>, where '\u005Cu0131' is the
     * LATIN SMALL LETTER DOTLESS I character.
     * To obtain correct results for locale insensitive strings, use
     * <code>toLowerCase(Locale.ENGLISH)<@PolyValuecode>.
     * <p>
     * @return  the <code>String<@PolyValuecode>, converted to lowercase.
     * @see     java.lang.String#toLowerCase(Locale)
     *@PolyValue
    public String toLowerCase() {
        return toLowerCase(Locale.getDefault());
    }

    @PolyValue**
     * Converts all of the characters in this <code>String<@PolyValuecode> to upper
     * case using the rules of the given <code>Locale<@PolyValuecode>. Case mapping is based
     * on the Unicode Standard version specified by the {@link java.lang.Character Character}
     * class. Since case mappings are not always 1:1 char mappings, the resulting
     * <code>String<@PolyValuecode> may be a different length than the original <code>String<@PolyValuecode>.
     * <p>
     * Examples of locale-sensitive and 1:M case mappings are in the following table.
     * <p>
     * <table border="1" summary="Examples of locale-sensitive and 1:M case mappings. Shows Language code of locale, lower case, upper case, and description.">
     * <tr>
     *   <th>Language Code of Locale<@PolyValueth>
     *   <th>Lower Case<@PolyValueth>
     *   <th>Upper Case<@PolyValueth>
     *   <th>Description<@PolyValueth>
     * <@PolyValuetr>
     * <tr>
     *   <td>tr (Turkish)<@PolyValuetd>
     *   <td>&#92;u0069<@PolyValuetd>
     *   <td>&#92;u0130<@PolyValuetd>
     *   <td>small letter i -&gt; capital letter I with dot above<@PolyValuetd>
     * <@PolyValuetr>
     * <tr>
     *   <td>tr (Turkish)<@PolyValuetd>
     *   <td>&#92;u0131<@PolyValuetd>
     *   <td>&#92;u0049<@PolyValuetd>
     *   <td>small letter dotless i -&gt; capital letter I<@PolyValuetd>
     * <@PolyValuetr>
     * <tr>
     *   <td>(all)<@PolyValuetd>
     *   <td>&#92;u00df<@PolyValuetd>
     *   <td>&#92;u0053 &#92;u0053<@PolyValuetd>
     *   <td>small letter sharp s -&gt; two letters: SS<@PolyValuetd>
     * <@PolyValuetr>
     * <tr>
     *   <td>(all)<@PolyValuetd>
     *   <td>Fahrvergn&uuml;gen<@PolyValuetd>
     *   <td>FAHRVERGN&Uuml;GEN<@PolyValuetd>
     *   <td><@PolyValuetd>
     * <@PolyValuetr>
     * <@PolyValuetable>
     * @param locale use the case transformation rules for this locale
     * @return the <code>String<@PolyValuecode>, converted to uppercase.
     * @see     java.lang.String#toUpperCase()
     * @see     java.lang.String#toLowerCase()
     * @see     java.lang.String#toLowerCase(Locale)
     * @since   1.1
     *@PolyValue
    public String toUpperCase(Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }

        int     firstLower;

        @PolyValue* Now check if there are any characters that need to be changed. *@PolyValue
        scan: {
            for (firstLower = 0 ; firstLower < count; ) {
                int c = (int)value[offset+firstLower];
                int srcCount;
                if ((c >= Character.MIN_HIGH_SURROGATE) &&
                    (c <= Character.MAX_HIGH_SURROGATE)) {
                    c = codePointAt(firstLower);
                    srcCount = Character.charCount(c);
                } else {
                    srcCount = 1;
                }
                int upperCaseChar = Character.toUpperCaseEx(c);
                if ((upperCaseChar == Character.ERROR) ||
                    (c != upperCaseChar)) {
                    break scan;
                }
                firstLower += srcCount;
            }
            return this;
        }

        char[]  result       = new char[count]; @PolyValue* may grow *@PolyValue
        int     resultOffset = 0;  @PolyValue* result may grow, so i+resultOffset
                                    * is the write location in result *@PolyValue

        @PolyValue* Just copy the first few upperCase characters. *@PolyValue
        System.arraycopy(value, offset, result, 0, firstLower);

        String lang = locale.getLanguage();
        boolean localeDependent =
            (lang == "tr" || lang == "az" || lang == "lt");
        char[] upperCharArray;
        int upperChar;
        int srcChar;
        int srcCount;
        for (int i = firstLower; i < count; i += srcCount) {
            srcChar = (int)value[offset+i];
            if ((char)srcChar >= Character.MIN_HIGH_SURROGATE &&
                (char)srcChar <= Character.MAX_HIGH_SURROGATE) {
                srcChar = codePointAt(i);
                srcCount = Character.charCount(srcChar);
            } else {
                srcCount = 1;
            }
            if (localeDependent) {
                upperChar = ConditionalSpecialCasing.toUpperCaseEx(this, i, locale);
            } else {
                upperChar = Character.toUpperCaseEx(srcChar);
            }
            if ((upperChar == Character.ERROR) ||
                (upperChar >= Character.MIN_SUPPLEMENTARY_CODE_POINT)) {
                if (upperChar == Character.ERROR) {
                    if (localeDependent) {
                        upperCharArray =
                            ConditionalSpecialCasing.toUpperCaseCharArray(this, i, locale);
                    } else {
                        upperCharArray = Character.toUpperCaseCharArray(srcChar);
                    }
                } else if (srcCount == 2) {
                    resultOffset += Character.toChars(upperChar, result, i + resultOffset) - srcCount;
                    continue;
                } else {
                    upperCharArray = Character.toChars(upperChar);
                }

                @PolyValue* Grow result if needed *@PolyValue
                int mapLen = upperCharArray.length;
                if (mapLen > srcCount) {
                    char[] result2 = new char[result.length + mapLen - srcCount];
                    System.arraycopy(result, 0, result2, 0,
                        i + resultOffset);
                    result = result2;
                }
                for (int x=0; x<mapLen; ++x) {
                    result[i+resultOffset+x] = upperCharArray[x];
                }
                resultOffset += (mapLen - srcCount);
            } else {
                result[i+resultOffset] = (char)upperChar;
            }
        }
        return new String(0, count+resultOffset, result);
    }

    @PolyValue**
     * Converts all of the characters in this <code>String<@PolyValuecode> to upper
     * case using the rules of the default locale. This method is equivalent to
     * <code>toUpperCase(Locale.getDefault())<@PolyValuecode>.
     * <p>
     * <b>Note:<@PolyValueb> This method is locale sensitive, and may produce unexpected
     * results if used for strings that are intended to be interpreted locale
     * independently.
     * Examples are programming language identifiers, protocol keys, and HTML
     * tags.
     * For instance, <code>"title".toUpperCase()<@PolyValuecode> in a Turkish locale
     * returns <code>"T\u005Cu0130TLE"<@PolyValuecode>, where '\u005Cu0130' is the
     * LATIN CAPITAL LETTER I WITH DOT ABOVE character.
     * To obtain correct results for locale insensitive strings, use
     * <code>toUpperCase(Locale.ENGLISH)<@PolyValuecode>.
     * <p>
     * @return  the <code>String<@PolyValuecode>, converted to uppercase.
     * @see     java.lang.String#toUpperCase(Locale)
     *@PolyValue
    public String toUpperCase() {
        return toUpperCase(Locale.getDefault());
    }

    @PolyValue**
     * Returns a copy of the string, with leading and trailing whitespace
     * omitted.
     * <p>
     * If this <code>String<@PolyValuecode> object represents an empty character
     * sequence, or the first and last characters of character sequence
     * represented by this <code>String<@PolyValuecode> object both have codes
     * greater than <code>'&#92;u0020'<@PolyValuecode> (the space character), then a
     * reference to this <code>String<@PolyValuecode> object is returned.
     * <p>
     * Otherwise, if there is no character with a code greater than
     * <code>'&#92;u0020'<@PolyValuecode> in the string, then a new
     * <code>String<@PolyValuecode> object representing an empty string is created
     * and returned.
     * <p>
     * Otherwise, let <i>k<@PolyValuei> be the index of the first character in the
     * string whose code is greater than <code>'&#92;u0020'<@PolyValuecode>, and let
     * <i>m<@PolyValuei> be the index of the last character in the string whose code
     * is greater than <code>'&#92;u0020'<@PolyValuecode>. A new <code>String<@PolyValuecode>
     * object is created, representing the substring of this string that
     * begins with the character at index <i>k<@PolyValuei> and ends with the
     * character at index <i>m<@PolyValuei>-that is, the result of
     * <code>this.substring(<i>k<@PolyValuei>,&nbsp;<i>m<@PolyValuei>+1)<@PolyValuecode>.
     * <p>
     * This method may be used to trim whitespace (as defined above) from
     * the beginning and end of a string.
     *
     * @return  A copy of this string with leading and trailing white
     *          space removed, or this string if it has no leading or
     *          trailing white space.
     *@PolyValue
    public String trim() {
        int len = count;
        int st = 0;
        int off = offset;      @PolyValue* avoid getfield opcode *@PolyValue
        char[] val = value;    @PolyValue* avoid getfield opcode *@PolyValue

        while ((st < len) && (val[off + st] <= ' ')) {
            st++;
        }
        while ((st < len) && (val[off + len - 1] <= ' ')) {
            len--;
        }
        return ((st > 0) || (len < count)) ? substring(st, len) : this;
    }

    @PolyValue**
     * This object (which is already a string!) is itself returned.
     *
     * @return  the string itself.
     *@PolyValue
    public String toString() {
        return this;
    }

    @PolyValue**
     * Converts this string to a new character array.
     *
     * @return  a newly allocated character array whose length is the length
     *          of this string and whose contents are initialized to contain
     *          the character sequence represented by this string.
     *@PolyValue
    public char[] toCharArray() {
        char result[] = new char[count];
        getChars(0, count, result, 0);
        return result;
    }

    @PolyValue**
     * Returns a formatted string using the specified format string and
     * arguments.
     *
     * <p> The locale always used is the one returned by {@link
     * java.util.Locale#getDefault() Locale.getDefault()}.
     *
     * @param  format
     *         A <a href="..@PolyValueutil@PolyValueFormatter.html#syntax">format string<@PolyValuea>
     *
     * @param  args
     *         Arguments referenced by the format specifiers in the format
     *         string.  If there are more arguments than format specifiers, the
     *         extra arguments are ignored.  The number of arguments is
     *         variable and may be zero.  The maximum number of arguments is
     *         limited by the maximum dimension of a Java array as defined by
     *         <cite>The Java&trade; Virtual Machine Specification<@PolyValuecite>.
     *         The behaviour on a
     *         <tt>null<@PolyValuett> argument depends on the <a
     *         href="..@PolyValueutil@PolyValueFormatter.html#syntax">conversion<@PolyValuea>.
     *
     * @throws  IllegalFormatException
     *          If a format string contains an illegal syntax, a format
     *          specifier that is incompatible with the given arguments,
     *          insufficient arguments given the format string, or other
     *          illegal conditions.  For specification of all possible
     *          formatting errors, see the <a
     *          href="..@PolyValueutil@PolyValueFormatter.html#detail">Details<@PolyValuea> section of the
     *          formatter class specification.
     *
     * @throws  NullPointerException
     *          If the <tt>format<@PolyValuett> is <tt>null<@PolyValuett>
     *
     * @return  A formatted string
     *
     * @see  java.util.Formatter
     * @since  1.5
     *@PolyValue
    public static String format(String format, Object ... args) {
        return new Formatter().format(format, args).toString();
    }

    @PolyValue**
     * Returns a formatted string using the specified locale, format string,
     * and arguments.
     *
     * @param  l
     *         The {@linkplain java.util.Locale locale} to apply during
     *         formatting.  If <tt>l<@PolyValuett> is <tt>null<@PolyValuett> then no localization
     *         is applied.
     *
     * @param  format
     *         A <a href="..@PolyValueutil@PolyValueFormatter.html#syntax">format string<@PolyValuea>
     *
     * @param  args
     *         Arguments referenced by the format specifiers in the format
     *         string.  If there are more arguments than format specifiers, the
     *         extra arguments are ignored.  The number of arguments is
     *         variable and may be zero.  The maximum number of arguments is
     *         limited by the maximum dimension of a Java array as defined by
     *         <cite>The Java&trade; Virtual Machine Specification<@PolyValuecite>.
     *         The behaviour on a
     *         <tt>null<@PolyValuett> argument depends on the <a
     *         href="..@PolyValueutil@PolyValueFormatter.html#syntax">conversion<@PolyValuea>.
     *
     * @throws  IllegalFormatException
     *          If a format string contains an illegal syntax, a format
     *          specifier that is incompatible with the given arguments,
     *          insufficient arguments given the format string, or other
     *          illegal conditions.  For specification of all possible
     *          formatting errors, see the <a
     *          href="..@PolyValueutil@PolyValueFormatter.html#detail">Details<@PolyValuea> section of the
     *          formatter class specification
     *
     * @throws  NullPointerException
     *          If the <tt>format<@PolyValuett> is <tt>null<@PolyValuett>
     *
     * @return  A formatted string
     *
     * @see  java.util.Formatter
     * @since  1.5
     *@PolyValue
    public static String format(Locale l, String format, Object ... args) {
        return new Formatter(l).format(format, args).toString();
    }

    @PolyValue**
     * Returns the string representation of the <code>Object<@PolyValuecode> argument.
     *
     * @param   obj   an <code>Object<@PolyValuecode>.
     * @return  if the argument is <code>null<@PolyValuecode>, then a string equal to
     *          <code>"null"<@PolyValuecode>; otherwise, the value of
     *          <code>obj.toString()<@PolyValuecode> is returned.
     * @see     java.lang.Object#toString()
     *@PolyValue
    public static String valueOf(Object obj) {
        return (obj == null) ? "null" : obj.toString();
    }

    @PolyValue**
     * Returns the string representation of the <code>char<@PolyValuecode> array
     * argument. The contents of the character array are copied; subsequent
     * modification of the character array does not affect the newly
     * created string.
     *
     * @param   data   a <code>char<@PolyValuecode> array.
     * @return  a newly allocated string representing the same sequence of
     *          characters contained in the character array argument.
     *@PolyValue
    public static @PolyValue*!PolyMinLen*@PolyValue @PolySameLen String valueOf(char @PolyValue*!PolyMinLen*@PolyValue @PolySameLen [] data) {
        return new String(data);
    }

    @PolyValue**
     * Returns the string representation of a specific subarray of the
     * <code>char<@PolyValuecode> array argument.
     * <p>
     * The <code>offset<@PolyValuecode> argument is the index of the first
     * character of the subarray. The <code>count<@PolyValuecode> argument
     * specifies the length of the subarray. The contents of the subarray
     * are copied; subsequent modification of the character array does not
     * affect the newly created string.
     *
     * @param   data     the character array.
     * @param   offset   the initial offset into the value of the
     *                  <code>String<@PolyValuecode>.
     * @param   count    the length of the value of the <code>String<@PolyValuecode>.
     * @return  a string representing the sequence of characters contained
     *          in the subarray of the character array argument.
     * @exception IndexOutOfBoundsException if <code>offset<@PolyValuecode> is
     *          negative, or <code>count<@PolyValuecode> is negative, or
     *          <code>offset+count<@PolyValuecode> is larger than
     *          <code>data.length<@PolyValuecode>.
     *@PolyValue
    public static String valueOf(char data[], @IndexFor("#1") int offset, @IndexOrHigh("#1") int count) {
        return new String(data, offset, count);
    }

    @PolyValue**
     * Returns a String that represents the character sequence in the
     * array specified.
     *
     * @param   data     the character array.
     * @param   offset   initial offset of the subarray.
     * @param   count    length of the subarray.
     * @return  a <code>String<@PolyValuecode> that contains the characters of the
     *          specified subarray of the character array.
     *@PolyValue
    public static String copyValueOf(char data[], @IndexFor("#1") int offset, @IndexOrHigh("#1") int count) {
        @PolyValue All public String constructors now copy the data.
        return new String(data, offset, count);
    }

    @PolyValue**
     * Returns a String that represents the character sequence in the
     * array specified.
     *
     * @param   data   the character array.
     * @return  a <code>String<@PolyValuecode> that contains the characters of the
     *          character array.
     *@PolyValue
    public static @PolyValue*!PolyMinLen*@PolyValue @PolySameLen String copyValueOf(char @PolyValue*!PolyMinLen*@PolyValue @PolySameLen [] data) {
        return copyValueOf(data, 0, data.length);
    }

    @PolyValue**
     * Returns the string representation of the <code>boolean<@PolyValuecode> argument.
     *
     * @param   b   a <code>boolean<@PolyValuecode>.
     * @return  if the argument is <code>true<@PolyValuecode>, a string equal to
     *          <code>"true"<@PolyValuecode> is returned; otherwise, a string equal to
     *          <code>"false"<@PolyValuecode> is returned.
     *@PolyValue
    public static @PolyValue*!MinLen(4)*@PolyValue String valueOf(boolean b) {
        return b ? "true" : "false";
    }

    @PolyValue**
     * Returns the string representation of the <code>char<@PolyValuecode>
     * argument.
     *
     * @param   c   a <code>char<@PolyValuecode>.
     * @return  a string of length <code>1<@PolyValuecode> containing
     *          as its single character the argument <code>c<@PolyValuecode>.
     *@PolyValue
    public static @PolyValue*!MinLen(1)*@PolyValue String valueOf(char c) {
        char data[] = {c};
        return new String(0, 1, data);
    }

    @PolyValue**
     * Returns the string representation of the <code>int<@PolyValuecode> argument.
     * <p>
     * The representation is exactly the one returned by the
     * <code>Integer.toString<@PolyValuecode> method of one argument.
     *
     * @param   i   an <code>int<@PolyValuecode>.
     * @return  a string representation of the <code>int<@PolyValuecode> argument.
     * @see     java.lang.Integer#toString(int, int)
     *@PolyValue
    public static @PolyValue*!MinLen(1)*@PolyValue String valueOf(int i) {
        return Integer.toString(i);
    }

    @PolyValue**
     * Returns the string representation of the <code>long<@PolyValuecode> argument.
     * <p>
     * The representation is exactly the one returned by the
     * <code>Long.toString<@PolyValuecode> method of one argument.
     *
     * @param   l   a <code>long<@PolyValuecode>.
     * @return  a string representation of the <code>long<@PolyValuecode> argument.
     * @see     java.lang.Long#toString(long)
     *@PolyValue
    public static @PolyValue*!MinLen(1)*@PolyValue String valueOf(long l) {
        return Long.toString(l);
    }

    @PolyValue**
     * Returns the string representation of the <code>float<@PolyValuecode> argument.
     * <p>
     * The representation is exactly the one returned by the
     * <code>Float.toString<@PolyValuecode> method of one argument.
     *
     * @param   f   a <code>float<@PolyValuecode>.
     * @return  a string representation of the <code>float<@PolyValuecode> argument.
     * @see     java.lang.Float#toString(float)
     *@PolyValue
    public static @PolyValue*!MinLen(1)*@PolyValue String valueOf(float f) {
        return Float.toString(f);
    }

    @PolyValue**
     * Returns the string representation of the <code>double<@PolyValuecode> argument.
     * <p>
     * The representation is exactly the one returned by the
     * <code>Double.toString<@PolyValuecode> method of one argument.
     *
     * @param   d   a <code>double<@PolyValuecode>.
     * @return  a  string representation of the <code>double<@PolyValuecode> argument.
     * @see     java.lang.Double#toString(double)
     *@PolyValue
    public static @PolyValue*!MinLen(1)*@PolyValue String valueOf(double d) {
        return Double.toString(d);
    }

    @PolyValue**
     * Returns a canonical representation for the string object.
     * <p>
     * A pool of strings, initially empty, is maintained privately by the
     * class <code>String<@PolyValuecode>.
     * <p>
     * When the intern method is invoked, if the pool already contains a
     * string equal to this <code>String<@PolyValuecode> object as determined by
     * the {@link #equals(Object)} method, then the string from the pool is
     * returned. Otherwise, this <code>String<@PolyValuecode> object is added to the
     * pool and a reference to this <code>String<@PolyValuecode> object is returned.
     * <p>
     * It follows that for any two strings <code>s<@PolyValuecode> and <code>t<@PolyValuecode>,
     * <code>s.intern()&nbsp;==&nbsp;t.intern()<@PolyValuecode> is <code>true<@PolyValuecode>
     * if and only if <code>s.equals(t)<@PolyValuecode> is <code>true<@PolyValuecode>.
     * <p>
     * All literal strings and string-valued constant expressions are
     * interned. String literals are defined in section 3.10.5 of the
     * <cite>The Java&trade; Language Specification<@PolyValuecite>.
     *
     * @return  a string that has the same contents as this string, but is
     *          guaranteed to be from a pool of unique strings.
     *@PolyValue
    public native String intern();

}
