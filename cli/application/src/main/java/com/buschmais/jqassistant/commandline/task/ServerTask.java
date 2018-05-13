package com.buschmais.jqassistant.commandline.task;

import java.io.IOException;
import java.util.List;

import com.buschmais.jqassistant.commandline.CliExecutionException;
import com.buschmais.jqassistant.core.store.api.Store;
import com.buschmais.jqassistant.core.store.impl.EmbeddedGraphStore;
import com.buschmais.jqassistant.neo4j.backend.bootstrap.EmbeddedNeo4jServer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author jn4, Kontext E GmbH, 23.01.14
 */
public class ServerTask extends AbstractStoreTask {

    public static final String CMDLINE_OPTION_SERVERADDRESS = "serverAddress";
    public static final String CMDLINE_OPTION_SERVERPORT = "serverPort";
    public static final String CMDLINE_OPTION_DAEMON = "daemon";

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerTask.class);

    private String serverAddress;
    private int serverPort;
    private Boolean runAsDaemon;

    @Override
    protected void executeTask(final Store store) throws CliExecutionException {
        EmbeddedGraphStore embeddedGraphStore = (EmbeddedGraphStore) store;
        EmbeddedNeo4jServer server = embeddedGraphStore.getServer();
        server.start(serverAddress, serverPort);
        LOGGER.info("Running server");
        if (runAsDaemon) {
            // let the neo4j daemon do the job
            LOGGER.info("Running server. Use <Ctrl-C> to stop server.");
        } else {
            LOGGER.info("Press <Enter> to finish.");
            try {
                System.in.read();
            } catch (IOException e) {
                throw new CliExecutionException("Cannot read from console.", e);
            } finally {
                server.stop();
            }
        }
    }

    @Override
    public void addTaskOptions(final List<Option> options) {
        options.add(OptionBuilder.withArgName(CMDLINE_OPTION_SERVERADDRESS).withDescription("The binding address of the server.").hasArgs()
                .create(CMDLINE_OPTION_SERVERADDRESS));
        options.add(OptionBuilder.withArgName(CMDLINE_OPTION_SERVERPORT).withDescription("The binding port of the server.").hasArgs()
                .create(CMDLINE_OPTION_SERVERPORT));
        options.add(OptionBuilder.withArgName(CMDLINE_OPTION_DAEMON).withDescription("Do not wait for <Enter> on standard input to stop the server.")
                .create(CMDLINE_OPTION_DAEMON));
    }

    @Override
    public void withOptions(CommandLine options) {
        serverAddress = getOptionValue(options, CMDLINE_OPTION_SERVERADDRESS, EmbeddedNeo4jServer.DEFAULT_ADDRESS);
        serverPort = Integer.valueOf(getOptionValue(options, CMDLINE_OPTION_SERVERPORT, Integer.toString(EmbeddedNeo4jServer.DEFAULT_PORT)));
        runAsDaemon = options.hasOption(CMDLINE_OPTION_DAEMON);
    }
}
