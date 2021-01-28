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

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

import io.github.pustike.inject.Injector;
import io.github.pustike.inject.Injectors;

/**
 * Web ServletContextListener will ensure that the injector is created when the web application is deployed.
 */
public abstract class WebServletContextListener implements ServletContextListener {
    static final String INJECTOR_NAME = "root-injector";

    @Override
    public final void contextInitialized(ServletContextEvent servletContextEvent) {
        ServletContext servletContext = servletContextEvent.getServletContext();
        Injector injector = createInjector(servletContext);
        servletContext.setAttribute(INJECTOR_NAME, injector);
        postConstruct(servletContext, injector);
    }

    /**
     * Implement this method to create the injector with services.
     * @param servletContext the servlet context
     * @return the injector instance
     */
    protected abstract  Injector createInjector(ServletContext servletContext);

    /**
     * Called after servlet context is initialized
     * @param servletContext the servlet context
     * @param injector the injector
     */
    protected void postConstruct(ServletContext servletContext, Injector injector) {

    }

    @Override
    public final void contextDestroyed(ServletContextEvent servletContextEvent) {
        ServletContext servletContext = servletContextEvent.getServletContext();
        Injector injector = (Injector) servletContext.getAttribute(INJECTOR_NAME);
        preDestroy(servletContext, injector);
        Injectors.dispose(injector);
        servletContext.removeAttribute(INJECTOR_NAME);
    }

    /**
     * Called before servlet context is destroyed
     * @param servletContext the servlet context
     * @param injector the injector
     */
    protected void preDestroy(ServletContext servletContext, Injector injector) {

    }
}
