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

import java.util.List;

import io.github.pustike.inject.bind.Module;

/**
 * Provides a list of modules to be configured for this web application. It can be provided by implementing
 * the {@link io.github.pustike.web.server.WebApplication} which is used to start the application server.
 * Or an implementation of this interface can be defined as {@link DispatcherServlet}'s init parameter
 * by name 'configurer'.
 */
public interface WebModuleConfigurer {
    /**
     * Return a list of modules to application injector
     * @return list of modules
     */
    List<Module> getModules();
}
