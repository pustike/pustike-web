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

import java.util.Objects;

/**
 * The Application Server Service managing the start and stop events.
 */
public class ServerService {
    private static final System.Logger logger = System.getLogger(ServerService.class.getName());
    private static final int DEFAULT_PORT = 8080;

    /**
     * The server's main method. {@code pustike start} or {@code pustike stop}
     * @param applicationServer the application server instance
     * @param args the command [start/stop] and an optional port argument [-port=8080].
     */
    public static void run(ApplicationServer applicationServer, String[] args) {
        Objects.requireNonNull(args, "command arguments are not provided");
        // read args and configure the application server
        final String START = "start", STOP = "stop";
        final String multipleCommandsErrorMsg = "Only one command can be executed at a time!";
        String commandToExecute = null, portString = null;
        for (String argument : args) {
            if (START.equals(argument)) {
                if (commandToExecute != null) {
                    throw new RuntimeException(multipleCommandsErrorMsg);
                }
                commandToExecute = START;
            } else if (STOP.equals(argument)) {
                if (commandToExecute != null) {
                    throw new RuntimeException(multipleCommandsErrorMsg);
                }
                commandToExecute = STOP;
            } else if (argument.startsWith("-port=")) {
                portString = argument.replaceAll("-port=", "");
            }
        }
        // show help message if no commands are provided
        if (commandToExecute == null) {
            printConsoleHelpMessage();
            return;
        }
        int serverPort = getServerPort(portString);// default port to use
        // now execute the given command
        if (START.equals(commandToExecute)) {
            applicationServer.start(serverPort);// start the server
        } else { // it is a STOP command
            applicationServer.stop(serverPort);// stop the server
        }
    }

    private static int getServerPort(String portString) {
        if (portString != null && !portString.isEmpty()) {
            try {
                return Integer.parseInt(portString);
            } catch (Exception e) {
                // ignored
            }
        }
        return DEFAULT_PORT;
    }

    private static void printConsoleHelpMessage() {
        String nl = System.lineSeparator();
        System.out.println("Usage: [options] <command>");
        String options = "Options:" + nl +
                " -port           " + "Port number to use (default: 8080)" + nl;
        System.out.println(options);
        String commands = "Commands:" + nl +
                " start           " + "Start the application server" + nl +
                " stop            " + "Stop the application server";
        System.out.println(commands);
    }
}
