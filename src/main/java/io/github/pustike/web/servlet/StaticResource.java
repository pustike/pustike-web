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

import java.io.InputStream;
import java.util.function.Supplier;

/**
 * Static resource.
 */
public final class StaticResource {
    private final String fileName;
    private final long lastModified;
    private final long contentLength;
    private final Supplier<InputStream> streamSupplier;

    /**
     * Constructor with static resource properties.
     * @param fileName the file name
     * @param lastModified file's last modified time
     * @param contentLength the file content length
     * @param streamSupplier the input stream supplier
     */
    public StaticResource(String fileName, long lastModified, long contentLength,
            Supplier<InputStream> streamSupplier) {
        this.fileName = fileName;
        this.lastModified = lastModified;
        this.contentLength = contentLength;
        this.streamSupplier = streamSupplier;
    }

    /**
     * Returns the file name of the resource. This must be unique across all static resources. If any, the file
     * extension will be used to determine the content type being set. If the container doesn't recognize the extension,
     * then you can always register it as <code>&lt;mime-type&gt;</code> in <code>web.xml</code>.
     * @return The file name of the resource.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Returns the last modified timestamp of the resource in milliseconds.
     * @return The last modified timestamp of the resource in milliseconds.
     */
    public long getLastModified() {
        return lastModified;
    }

    /**
     * Returns the content length of the resource. This returns <code>-1</code> if the content length is unknown.
     * In that case, the container will automatically switch to chunked encoding if the response is already committed
     * after streaming. The file download progress may be unknown.
     * @return The content length of the resource.
     */
    public long getContentLength() {
        return contentLength;
    }

    /**
     * Returns the input stream with the content of the resource. This method will be called only once by the servlet,
     * and only when the resource actually needs to be streamed, so lazy loading is not necessary.
     * @return The input stream with the content of the resource.
     */
    public InputStream getInputStream() {
        return streamSupplier.get();
    }

    @Override
    public String toString() {
        return "StaticResource{" + "fileName='" + fileName + '\'' + ", lastModified=" + lastModified +
                ", contentLength=" + contentLength + '}';
    }
}
