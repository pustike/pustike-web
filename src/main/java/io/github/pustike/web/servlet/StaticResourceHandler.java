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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.jar.JarEntry;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Http Request Handler that serves static resources.
 */
public class StaticResourceHandler extends StaticResourceServlet {
    private static final Logger logger = System.getLogger(StaticResourceHandler.class.getName());
    private static final String welcomeFile = "/index.html";
    /** A list of forbidden paths used when looking for a resource. */
    private final List<String> forbiddenPathList;

    /**
     * Default Constructor.
     */
    public StaticResourceHandler() {
        forbiddenPathList = List.of("/WEB-INF/", "/META-INF/");
    }

    @Override
    protected StaticResource getStaticResource(HttpServletRequest request) throws IllegalArgumentException {
        String relativePath = getRelativePath(request);
        relativePath = "/".equals(relativePath) ? welcomeFile : relativePath;
        if (forbiddenPathList.stream().anyMatch(relativePath::startsWith)) {
            return null;
        }
        try {
            URL resourceUrl = request.getServletContext().getResource(relativePath);
            if (resourceUrl != null) {
                String resourcePath = resourceUrl.toExternalForm();
                if (resourcePath.startsWith("file:")) {
                    return getFileResource(resourceUrl, relativePath);
                } else if (resourcePath.startsWith("jar:")) {
                    return getJarResource(resourceUrl);
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "couldn't find file for the path: " + relativePath, e);
        }
        return null;// disallow, send 404
    }

    private String getRelativePath(HttpServletRequest request) {
        String relativePath = request.getPathInfo();// is null when servlet is mapped to "/" instead "/*"
        return relativePath == null ? request.getServletPath() : relativePath;
    }

    private StaticResource getFileResource(URL resourceUrl, String relativePath) throws URISyntaxException {
        File file = new File(resourceUrl.toURI());
        if (!file.isFile()) {
            logger.log(Level.WARNING, "resource for the path is not a file: " + relativePath);
            return null;
        }
        return new StaticResource(file.getName(), file.lastModified(), file.length(),
                () -> createInputStream(file));
    }

    private StaticResource getJarResource(URL resourceUrl) throws IOException {
        JarURLConnection urlConnection = (JarURLConnection) resourceUrl.openConnection();
        final JarEntry jarEntry = urlConnection.getJarEntry();
        if (jarEntry == null || jarEntry.isDirectory()) {
            logger.log(Level.WARNING, "resource for the path is not valid jarEntry: " + jarEntry);
            return null;
        }
        return new StaticResource(jarEntry.getName(), jarEntry.getTime(), jarEntry.getSize(),
                () -> createInputStream(urlConnection));
    }

    private static InputStream createInputStream(Object source) {
        try {
            if (source instanceof File) {
                return new FileInputStream((File) source);
            } else if (source instanceof JarURLConnection) {
                return ((JarURLConnection) source).getInputStream();
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "error when creating the inputStream!", e);
        }
        return null;
    }
}
