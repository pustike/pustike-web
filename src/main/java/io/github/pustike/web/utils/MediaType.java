/*
 * Copyright (c) 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.pustike.web.utils;

import java.nio.charset.Charset;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An <a href="http://tools.ietf.org/html/rfc2045">RFC 2045</a> Media Type, appropriate to describe
 * the content type of an HTTP request or response body.
 * <p>
 *  Source: This code has been borrowed from
 *  <a href="https://github.com/square/okhttp/blob/okhttp_3.14.x/okhttp/src/main/java/okhttp3/MediaType.java">Square's OkHttp</a>.
 * </p>
 */
public final class MediaType {
    private static final String TOKEN = "([a-zA-Z0-9-!#$%&'*+.^_`{|}~]+)";
    private static final String QUOTED = "\"([^\"]*)\"";
    private static final Pattern TYPE_SUBTYPE = Pattern.compile(TOKEN + "/" + TOKEN);
    private static final Pattern PARAMETER = Pattern.compile(
            ";\\s*(?:" + TOKEN + "=(?:" + TOKEN + "|" + QUOTED + "))?");

    private final String mediaType;
    private final String type;
    private final String subtype;
    private final String charset;

    private MediaType(String mediaType, String type, String subtype, String charset) {
        this.mediaType = mediaType;
        this.type = type;
        this.subtype = subtype;
        this.charset = charset;
    }

    /**
     * Returns a media type for {@code string}.
     *
     * @throws IllegalArgumentException if {@code string} is not a well-formed media type.
     */
    public static MediaType get(String string) {
        Matcher typeSubtype = TYPE_SUBTYPE.matcher(string);
        if (!typeSubtype.lookingAt()) {
            throw new IllegalArgumentException("No subtype found for: \"" + string + '"');
        }
        String type = typeSubtype.group(1).toLowerCase(Locale.US);
        String subtype = typeSubtype.group(2).toLowerCase(Locale.US);

        String charset = null;
        Matcher parameter = PARAMETER.matcher(string);
        for (int s = typeSubtype.end(); s < string.length(); s = parameter.end()) {
            parameter.region(s, string.length());
            if (!parameter.lookingAt()) {
                throw new IllegalArgumentException("Parameter is not formatted correctly: \""
                        + string.substring(s) + "\" for: \"" + string + '"');
            }

            String name = parameter.group(1);
            if (name == null || !name.equalsIgnoreCase("charset")) continue;
            String charsetParameter;
            String token = parameter.group(2);
            if (token != null) {
                // If the token is 'single-quoted' it's invalid! But we're lenient and strip the quotes.
                charsetParameter = (token.startsWith("'") && token.endsWith("'") && token.length() > 2)
                        ? token.substring(1, token.length() - 1)
                        : token;
            } else {
                // Value is "double-quoted". That's valid and our regex group already strips the quotes.
                charsetParameter = parameter.group(3);
            }
            if (charset != null && !charsetParameter.equalsIgnoreCase(charset)) {
                throw new IllegalArgumentException("Multiple charsets defined: \""
                        + charset + "\" and: \"" + charsetParameter + "\" for: \"" + string + '"');
            }
            charset = charsetParameter;
        }

        return new MediaType(string, type, subtype, charset);
    }

    /**
     * Returns a media type for {@code string}, or null if {@code string} is not a well-formed media
     * type.
     */
    public static MediaType parse(String string) {
        try {
            return get(string);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * Returns the high-level media type, such as "text", "image", "audio", "video", or
     * "application".
     */
    public String type() {
        return type;
    }

    /**
     * Returns a specific media subtype, such as "plain" or "png", "mpeg", "mp4" or "xml".
     */
    public String subtype() {
        return subtype;
    }

    /**
     * Returns the charset of this media type, or null if this media type doesn't specify a charset.
     */
    public Charset charset() {
        return charset(null);
    }

    /**
     * Returns the charset of this media type, or {@code defaultValue} if either this media type
     * doesn't specify a charset, of it its charset is unsupported by the current runtime.
     */
    public Charset charset(Charset defaultValue) {
        try {
            return charset != null ? Charset.forName(charset) : defaultValue;
        } catch (IllegalArgumentException e) {
            return defaultValue; // This charset is invalid or unsupported. Give up.
        }
    }

    /**
     * Returns the encoded media type, like "text/plain; charset=utf-8", appropriate for use in a
     * Content-Type header.
     */
    @Override
    public String toString() {
        return mediaType;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof MediaType && ((MediaType) other).mediaType.equals(mediaType);
    }

    @Override
    public int hashCode() {
        return mediaType.hashCode();
    }

    // ----------
    private static final String WILDCARD_TYPE = "*";

    /**
     * Indicates whether the {@linkplain #type() type} is the wildcard character <code>&#42;</code> or not.
     * @return whether the type is a wildcard
     */
    public boolean isWildcardType() {
        return WILDCARD_TYPE.equals(type());
    }

    /**
     * Indicates whether the {@linkplain #subtype() subtype} is the wildcard character <code>&#42;</code>
     * or the wildcard character followed by a suffix (e.g. <code>&#42;+xml</code>).
     * @return whether the subtype is a wildcard
     */
    public boolean isWildcardSubtype() {
        return WILDCARD_TYPE.equals(subtype()) || subtype().startsWith("*+");
    }

    /**
     * Indicate whether this {@code MediaType} includes the given media type.
     * <p>For instance, {@code text/*} includes {@code text/plain} and {@code text/html},
     * and {@code application/*+xml} includes {@code application/soap+xml}, etc. This method is <b>not</b> symmetric.
     * @param other the reference media type with which to compare
     * @return {@code true} if this media type includes the given media type; {@code false} otherwise
     */
    public boolean includes(MediaType other) {
        if (other == null) {
            return false;
        }
        if (this.isWildcardType()) {
            // */* includes anything
            return true;
        } else if (type().equals(other.type())) {
            if (subtype().equals(other.subtype())) {
                return true;
            }
            if (this.isWildcardSubtype()) {
                // wildcard with suffix, e.g. application/*+xml
                int thisPlusIdx = subtype().indexOf('+');
                if (thisPlusIdx == -1) {
                    return true;
                } else {
                    // application/*+xml includes application/soap+xml
                    int otherPlusIdx = other.subtype().indexOf('+');
                    if (otherPlusIdx != -1) {
                        String thisSubtypeNoSuffix = subtype().substring(0, thisPlusIdx);
                        String thisSubtypeSuffix = subtype().substring(thisPlusIdx + 1);
                        String otherSubtypeSuffix = other.subtype().substring(otherPlusIdx + 1);
                        if (thisSubtypeSuffix.equals(otherSubtypeSuffix) && WILDCARD_TYPE.equals(thisSubtypeNoSuffix)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
