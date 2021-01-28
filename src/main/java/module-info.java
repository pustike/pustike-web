/*
 * Copyright (C) 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Pustike Web provides API for creating application using RESTful Web Services with
 * a central servlet `DispatcherServlet`, having an API similar to <a href="https://github.com/eclipse-ee4j/jaxrs-api">JAX-RS API</a>.
 */
module io.github.pustike.web {
    requires transitive jetty.servlet.api;
    requires io.github.pustike.inject;
    requires io.github.pustike.json;

    requires static org.eclipse.jetty.servlet;

    exports io.github.pustike.web;
    exports io.github.pustike.web.scope;
    exports io.github.pustike.web.servlet;
    exports io.github.pustike.web.server;
}
