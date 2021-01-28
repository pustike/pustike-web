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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;

public abstract class ServletUtils {
    /**
     * The HTTP {@code Content-Type} header field name.
     * @see <a href="http://tools.ietf.org/html/rfc7231#section-3.1.1.5">Section 3.1.1.5 of RFC 7231</a>
     */
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String FORM_CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final Charset UTF8_CHARSET = StandardCharsets.UTF_8;
    public static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json"); // ;charset=UTF-8

    public static String getRelativePath(HttpServletRequest servletRequest) {
        String relativePath = servletRequest.getPathInfo();// is null when servlet is mapped to "/" instead "/*"
        return relativePath == null ? servletRequest.getServletPath() : relativePath;
    }

    public static MediaType getContentType(HttpServletRequest servletRequest) {
        String contentType = servletRequest.getHeader(CONTENT_TYPE);
        return contentType != null ? MediaType.parse(contentType) : null;
    }

    public static Charset getCharset(MediaType mediaType) {
        Charset charset = mediaType == null ? UTF8_CHARSET : mediaType.charset();
        return charset == null ? UTF8_CHARSET : charset;
    }

    public static InputStream getBody(HttpServletRequest servletRequest) throws IOException {
        if (isFormPost(servletRequest)) {
            return getBodyFromServletRequestParameters(servletRequest);
        } else {
            return servletRequest.getInputStream();
        }
    }

    private static boolean isFormPost(HttpServletRequest request) {
        String contentType = request.getContentType();
        return (contentType != null && contentType.contains(FORM_CONTENT_TYPE) &&
                HttpMethod.POST.matches(request.getMethod()));
    }

    /**
     * Use {@link jakarta.servlet.ServletRequest#getParameterMap()} to reconstruct the
     * body of a form 'POST' providing a predictable outcome as opposed to reading
     * from the body, which can fail if any other code has used the ServletRequest
     * to access a parameter, thus causing the input stream to be "consumed".
     */
    private static InputStream getBodyFromServletRequestParameters(HttpServletRequest request) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
        Writer writer = new OutputStreamWriter(bos, UTF8_CHARSET);

        Map<String, String[]> form = request.getParameterMap();
        for (Iterator<String> nameIterator = form.keySet().iterator(); nameIterator.hasNext(); ) {
            String name = nameIterator.next();
            List<String> values = Arrays.asList(form.get(name));
            for (Iterator<String> valueIterator = values.iterator(); valueIterator.hasNext(); ) {
                String value = valueIterator.next();
                writer.write(URLEncoder.encode(name, UTF8_CHARSET.name()));
                if (value != null) {
                    writer.write('=');
                    writer.write(URLEncoder.encode(value, UTF8_CHARSET.name()));
                    if (valueIterator.hasNext()) {
                        writer.write('&');
                    }
                }
            }
            if (nameIterator.hasNext()) {
                writer.append('&');
            }
        }
        writer.flush();
        return new ByteArrayInputStream(bos.toByteArray());
    }

    private ServletUtils() {

    }
}
