package com.lcoprobe.acbs.loader;

import com.lcoprobe.acbs.domain.Server;
import com.lcoprobe.acbs.repository.ServerRepository;
import com.lcoprobe.acbs.utils.Constants;
import com.lcoprobe.acbs.wrapper.LCOEchoWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimerTask;

/**
 * @author Osvaldo Martini
 */
// tag::code[]
@Component
public class DatabaseLoader implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseLoader.class);

    private final ServerRepository serverRepository;

    @Autowired
    LCOEchoWrapper lcoEchoWrapper;

    @Autowired
    public DatabaseLoader(ServerRepository serverRepository) {
        this.serverRepository = serverRepository;
    }

    /**
     * Database Loader and Fire the Lansa Connect
     *
     * @param args arguments from the main application call
     */
    @Override
    public void run(String... args) throws Exception {
        try {
            lcoEchoWrapper.loadConfig(args);
        } catch (Exception e) {
            logger.error("Error: ".concat(e.getMessage()));
        }
        if (lcoEchoWrapper.getProperties() != null) {
            List<Server> servers = new ArrayList<>();
            serverRepository.findAll().forEach(servers::add);
            Optional<Server> server = servers.stream().findFirst();
            if (!server.isPresent()) {
                server = Optional.of(
                        this.serverRepository.save(new Server(lcoEchoWrapper.getProperties().getProperty(Constants.PROBE_SERVER_NAME),
                                lcoEchoWrapper.getProperties().getProperty("lcoProbe.host"),
                                lcoEchoWrapper.getProperties().getProperty(Constants.PROBE_USER_NAME),
                                lcoEchoWrapper.getProperties().getProperty(Constants.PROBE_PASSWORD),
                                Long.parseLong(lcoEchoWrapper.getProperties().getProperty(Constants.PROBE_TIMEOUT)),
                                lcoEchoWrapper.getProperties().getProperty(Constants.PROBE_ENVIRONMENT),
                                "pinging Server")));
            }
            //It will create at Least One Server on the List of Servers
            server.get().setEnvironment(lcoEchoWrapper.getProperties().getProperty(Constants.PROBE_ENVIRONMENT));
            server.get().setServerName(lcoEchoWrapper.getProperties().getProperty(Constants.PROBE_SERVER_NAME));
            server.get().setHost(lcoEchoWrapper.getProperties().getProperty(Constants.PROBE_HOST));
            server.get().setUserName(lcoEchoWrapper.getProperties().getProperty(Constants.PROBE_USER_NAME));
            server.get().setPassword(lcoEchoWrapper.getProperties().getProperty(Constants.PROBE_PASSWORD));
            server.get().setTimeout(Long.parseLong(lcoEchoWrapper.getProperties().getProperty(Constants.PROBE_TIMEOUT)));
            serverRepository.save(server.get());

            serverDataLoad();
        }
    }

    /**
     * DataLoad method executes the lcoEcho() and updates de entity
     */
    public void serverDataLoad() {
        new java.util.Timer().schedule(new TimerTask() {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

            @Override
            public void run() {
                LocalDateTime now = LocalDateTime.now();
                serverRepository.findAll().forEach(server -> {
                    server.setDescription("last update " + dtf.format(now));

                    lcoEchoWrapper.setHost(server.getHost());
                    lcoEchoWrapper.setUser(server.getUserName());
                    lcoEchoWrapper.setPswd(server.getPassword());
                    lcoEchoWrapper.setTimeout(server.getTimeout());

                    String result = lcoEchoWrapper.lcoEcho();

                    server.setStatus(result);

                    serverRepository.save(server);
                });
            }
        }, 1000 * 5L, 1000 * 5L);
    }
}
// end::code[]