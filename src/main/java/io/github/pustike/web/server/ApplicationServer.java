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

/**
 * The Application Server.
 */
public interface ApplicationServer {
    /**
     * Start the Application Server.
     * @param serverPort the port to use
     */
    void start(int serverPort);

    /**
     * Stop the Application Server.
     * @param serverPort the port to use
     */
    void stop(int serverPort);
}
