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
package io.github.pustike.web.server;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletContextListener;

import io.github.pustike.web.servlet.WebModuleConfigurer;

/**
 * Web application configuration class used when starting the application server.
 */
public abstract class WebApplication implements WebModuleConfigurer {
    private String resourceBase;
    private ServletContextListener contextListener;
    private MultipartConfigElement multipartConfig;

    /**
     * Return the resource base to configure servlet context handler.
     * @return the resource base path
     */
    public String getResourceBase() {
        return resourceBase;
    }

    /**
     * Set the resource base to configure servlet context handler.
     * @param resourceBase the resource base path
     */
    protected void setResourceBase(String resourceBase) {
        this.resourceBase = resourceBase;
    }

    /**
     * Get the servlet context listener instance.
     * @return the server context listener
     */
    public ServletContextListener getContextListener() {
        return contextListener;
    }

    /**
     * Set the application's servlet context listener.
     * @param contextListener the servlet context listener
     */
    protected void setContextListener(ServletContextListener contextListener) {
        this.contextListener = contextListener;
    }

    /**
     * Get the multipart config element.
     * @return the multipart config element
     */
    public MultipartConfigElement getMultipartConfig() {
        return multipartConfig;
    }

    /**
     * Set the multipart config element which will be registered with the servlet.
     * @param multipartConfig the multipart config element
     */
    protected void setMultipartConfig(MultipartConfigElement multipartConfig) {
        this.multipartConfig = multipartConfig;
    }
}
