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
package io.github.pustike.web.servlet;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Static resource servlet.
 * REVIEW: Content-Range support, Resource Cache, use builder pattern to create resource instances.
 */
public abstract class StaticResourceServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final long DEFAULT_EXPIRE_TIME_IN_MILLIS = 0; // TimeUnit.DAYS.toMillis(30);
    private static final long ONE_SECOND_IN_MILLIS = TimeUnit.SECONDS.toMillis(1);
    private static final String ETAG_HEADER = "W/\"%s-%s\"";
    private static final int DEFAULT_STREAM_BUFFER_SIZE = 102400;
    private static final String CONTENT_DISPOSITION_HEADER = "inline;filename=\"%1$s\"; filename*=UTF-8''%1$s";
    private static final String ERROR_UNSUPPORTED_ENCODING = "UTF-8 is apparently not supported on this platform.";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doRequest(request, response, false);
    }

    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doRequest(request, response, true);
    }

    private void doRequest(HttpServletRequest request, HttpServletResponse response, boolean head) throws IOException {
        response.reset();
        StaticResource resource;
        try {
            resource = getStaticResource(request);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        if (resource == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String fileName = encodeURL(resource.getFileName());
        String eTag = String.format(ETAG_HEADER, fileName, resource.getLastModified());
        if (preconditionFailed(request, resource, eTag)) {
            response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
            return;
        }
        if (notModified(request, resource, eTag)) {
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }
        setCacheHeaders(response, resource, eTag);
        setContentHeaders(request, response, resource);
        if (head) {
            return;
        }
        writeContent(response, resource);
    }

    /**
     * Returns the static resource associated with the given HTTP servlet request. This returns <code>null</code> when
     * the resource does actually not exist. The servlet will then return a HTTP 404 error.
     * @param request The involved HTTP servlet request.
     * @return The static resource associated with the given HTTP servlet request.
     * @throws IllegalArgumentException When the request is mangled in such way that it's not recognizable as a valid
     *                                  static resource request. The servlet will then return a HTTP 400 error.
     */
    protected abstract StaticResource getStaticResource(HttpServletRequest request) throws IllegalArgumentException;

    /**
     * Returns true if it's a conditional request which must return 412.
     */
    private boolean preconditionFailed(HttpServletRequest request, StaticResource resource, String eTag) {
        String match = request.getHeader("If-Match");
        long unmodified = request.getDateHeader("If-Unmodified-Since");
        return (match != null) ? !matches(match, eTag) :
                (unmodified != -1 && modified(unmodified, resource.getLastModified()));
    }

    private boolean notModified(HttpServletRequest request, StaticResource resource, String eTag) {
        String ifNoneMatch = request.getHeader("If-None-Match");
        if (ifNoneMatch != null) {
            String[] matches = ifNoneMatch.split("\\s*,\\s*");
            Arrays.sort(matches);
            return (Arrays.binarySearch(matches, eTag) > -1 || Arrays.binarySearch(matches, "*") > -1);
        } else {
            long ifModifiedSince = request.getDateHeader("If-Modified-Since");
            // That second is because the header is in seconds, not millis.
            return (ifModifiedSince + ONE_SECOND_IN_MILLIS > resource.getLastModified());
        }
    }

    /**
     * Set the cache headers.
     *
     * <p>If the <code>expires</code> argument is larger than 0 seconds, then the following headers will be set:
     * <ul>
     * <li><code>Cache-Control: public,max-age=[expiration time in seconds],must-revalidate</code></li>
     * <li><code>Expires: [expiration date of now plus expiration time in seconds]</code></li>
     * </ul>
     * <p>Else the method will delegate to {@link #setNoCacheHeaders(HttpServletResponse)}.
     * @param response The HTTP servlet response to set the headers on
     * @param resource The static resource
     * @param eTag     The ETag
     */
    private void setCacheHeaders(HttpServletResponse response, StaticResource resource, String eTag) {
        response.setHeader("ETag", eTag);
        response.setDateHeader("Last-Modified", resource.getLastModified());
        long expires = DEFAULT_EXPIRE_TIME_IN_MILLIS;
        if (expires > 0) {
            response.setHeader("Cache-Control", "public,max-age=" + expires + ",must-revalidate");
            response.setDateHeader("Expires", System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(expires));
            response.setHeader("Pragma", ""); // Explicitly set pragma to prevent container from overriding it.
        } else {
            setNoCacheHeaders(response);
        }
    }

    /**
     * Set the no-cache headers.
     *
     * <p>The following headers will be set:
     * <ul>
     * <li><code>Cache-Control: no-cache,no-store,must-revalidate</code></li>
     * <li><code>Expires: [expiration date of 0]</code></li>
     * <li><code>Pragma: no-cache</code></li>
     * </ul>
     * @param response The HTTP servlet response to set the headers on
     */
    private void setNoCacheHeaders(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache,no-store,must-revalidate");
        response.setDateHeader("Expires", 0);
        response.setHeader("Pragma", "no-cache"); // Backwards compatibility for HTTP 1.0.
    }

    /**
     * Set content headers.
     *
     * <p>The following headers will be set:
     * <ul>
     * <li><code>Content-Type:</code> delegates the fileName to {@link jakarta.servlet.ServletContext#getMimeType(String)}
     * with a fallback default value of <code>application/octet-stream</code>.</li>
     * <li><code>Content-Disposition: </code></li>
     * <li><code>Content-Length: </code></li>
     * </ul>
     * @param request  The HTTP servlet request
     * @param response The HTTP servlet response to set the headers on
     * @param resource The static resource
     */
    private void setContentHeaders(HttpServletRequest request, HttpServletResponse response, StaticResource resource) {
        String fileName = resource.getFileName();
        response.setContentType(Optional.of(request.getServletContext().getMimeType(fileName))
                .orElse("application/octet-stream"));
        response.setHeader("Content-Disposition", String.format(CONTENT_DISPOSITION_HEADER, encodeURI(fileName)));
        if (resource.getContentLength() != -1) {
            response.setContentLengthLong(resource.getContentLength());
        }
    }

    private void writeContent(HttpServletResponse response, StaticResource resource) throws IOException {
        try (ReadableByteChannel inputChannel = Channels.newChannel(resource.getInputStream());
             WritableByteChannel outputChannel = Channels.newChannel(response.getOutputStream())) {
            ByteBuffer buffer = ByteBuffer.allocateDirect(DEFAULT_STREAM_BUFFER_SIZE);
            long size = 0;
            while (inputChannel.read(buffer) != -1) {
                buffer.flip();
                size += outputChannel.write(buffer);
                buffer.clear();
            }
            if (resource.getContentLength() == -1 && !response.isCommitted()) {
                response.setContentLengthLong(size);
            }
        }
    }

    /**
     * Returns true if the given match header matches the given ETag value.
     */
    private static boolean matches(String matchHeader, String eTag) {
        String[] matchValues = matchHeader.split("\\s*,\\s*");
        Arrays.sort(matchValues);
        return Arrays.binarySearch(matchValues, eTag) > -1
                || Arrays.binarySearch(matchValues, "*") > -1;
    }

    /**
     * Returns true if the given modified header is older than the given last modified value.
     */
    private static boolean modified(long modifiedHeader, long lastModified) {
        // That second is because the header is in seconds, not millis.
        return (modifiedHeader + ONE_SECOND_IN_MILLIS <= lastModified);
    }

    /**
     * URI-encode the given string using UTF-8. URIs (paths and filenames) have different encoding rules as compared to
     * URL query string parameters. {@link URLEncoder} is actually only for www (HTML) form based query string parameter
     * values (as used when a webbrowser submits a HTML form). URI encoding has a lot in common with URL encoding, but
     * the space has to be %20 and some chars doesn't necessarily need to be encoded.
     * @param string The string to be URI-encoded using UTF-8.
     * @return The given string, URI-encoded using UTF-8, or <code>null</code> if <code>null</code> was given.
     * @throws UnsupportedOperationException When this platform does not support UTF-8.
     */
    private static String encodeURI(String string) throws UnsupportedOperationException {
        if (string == null) {
            return null;
        }
        return encodeURL(string).replace("+", "%20").replace("%21", "!").replace("%27", "'")
                .replace("%28", "(").replace("%29", ")").replace("%7E", "~");
    }

    /**
     * URL-encode the given string using UTF-8.
     * @param string The string to be URL-encoded using UTF-8
     * @return The given string, URL-encoded using UTF-8, or <code>null</code> if <code>null</code> was given
     * @throws UnsupportedOperationException When this platform does not support UTF-8
     */
    private static String encodeURL(String string) throws UnsupportedOperationException {
        if (string == null) {
            return null;
        }
        try {
            return URLEncoder.encode(string, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new UnsupportedOperationException(ERROR_UNSUPPORTED_ENCODING, e);
        }
    }
}
