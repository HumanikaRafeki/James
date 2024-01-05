package humanika.rafeki.james;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.shard.ShardingStrategy;
import humanika.rafeki.james.data.JamesConfig;
import humanika.rafeki.james.data.JamesState;
import java.io.BufferedReader;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class James {
    private static JamesState jamesState;
    private static JamesConfig jamesConfig;
    private static final String JAMES_CONFIG_PATH = "james-config.json";
    //private static String BOT_URI = "https://github.com/HumanikaRafeki/James";
    private static final Logger LOGGER = LoggerFactory.getLogger(James.class);
    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();

    public static JamesConfig getConfig() {
        return jamesConfig;
    }

    public static JamesState getState() {
        return jamesState;
    }

    public static OkHttpClient getHttpClient() {
        return HTTP_CLIENT;
    }

    public static void main(String[] args) {
        try {
            jamesConfig = new JamesConfig(JAMES_CONFIG_PATH, LOGGER);
        } catch(Exception pe) {
            LOGGER.error("Unable to read settings from \"" + JAMES_CONFIG_PATH + '"', pe);
            System.exit(1);
        }

        String token = null;
        try(BufferedReader reader = Files.newBufferedReader(jamesConfig.botTokenFile)) {
            token = reader.readLine().strip();
        } catch(Exception ioe) {
            LOGGER.error("Unable to read bot token file from \"" + jamesConfig.botTokenFile + '"', ioe);
            System.exit(1);
        }

        //Creates the gateway client and connects to the gateway
        EventDispatcher customDispatcher = EventDispatcher.builder()
            .eventScheduler(Schedulers.boundedElastic())
            .build();
        DiscordClient bot = DiscordClient.create(token);

        jamesState = new JamesState(jamesConfig, LOGGER);
        try {
            jamesState.update(jamesConfig);
        } catch(Exception se) {
            LOGGER.error("Unable to load initial data", se);
            System.exit(1);
        }
        Thread thread = new Thread() {
                public void run() {
                    bot.gateway().setEventDispatcher(customDispatcher) // .setSharding(ShardingStrategy.recommended())
                        .withGateway(client -> client.on(ReadyEvent.class)
                                     .flatMap(ready -> returnGatewayClient(bot, client))
                                     .doOnError(error -> LOGGER.error("gateway error", error)))
                        .doOnError(error -> LOGGER.error("dispatcher error", error))
                        .block();
                        // .subscribe(null, error -> {
                        //         LOGGER.error("bot failed with error", error);
                        //     }).
                }
            };
        LOGGER.info("Main thread is started.");
        thread.start();

        final String successMessage = "success";
        final long naptime = 30000;
        final long printInterval = 1800000; // 30 minutes in milliseconds
        long lastStackDump = -printInterval * 2;
        while(thread.isAlive()) {
            try {
                thread.join(naptime);
                if(!thread.isAlive()) {
                    LOGGER.info("Control thread exited.");
                    LOGGER.error("Exiting successfully due to control thread exiting.");
                    System.exit(0);
                }
                LOGGER.info("Control thread is running.");
            } catch(InterruptedException ie) {
                LOGGER.info("Main thread join was interrupted.", ie);
            } catch(Exception e) {
                LOGGER.error("Main thread join failed.", e);
                LOGGER.error("Aborting due to main thread join failing.");
                System.exit(2);
            }

            ThreadMXBean tmx = null;
            try {
                tmx = ManagementFactory.getThreadMXBean();
                long[] deadlocked = tmx.findDeadlockedThreads();
                if(deadlocked != null && deadlocked.length > 0) {
                    ThreadInfo[] infos = tmx.getThreadInfo(deadlocked, true, true);
                    for (ThreadInfo ti : infos)
                        LOGGER.error("Deadlocked: "+ti);
                    LOGGER.error("Aborting due to deadlock.");
                    System.exit(2);
                }
                LOGGER.info("No deadlocks.");
            } catch(Exception e) {
                LOGGER.error("Deadlock detection failed with an exception.", e);
                LOGGER.error("Aborting due to deadlock detection failure.");
                System.exit(2);
            }

            try {
                ForkJoinTask<String> checkup = ForkJoinTask.adapt(() -> successMessage);
                ForkJoinPool.commonPool().execute(checkup);
                String message = checkup.get(30, TimeUnit.SECONDS);
                if(message != successMessage) {
                    LOGGER.error("ForkJoinPool test did not return \""+successMessage+'"');
                    LOGGER.error("Aborting due to ForkJoinPool test failure.");
                }
                LOGGER.info("ForkJoinTask check succeeded.");
            } catch(Exception e) {
                LOGGER.error("Cannot execute the ForkJoinPool test.", e);
                LOGGER.error("Aborting due to ForkJoinPool test failure.");
                System.exit(2);
            }

            RuntimeMXBean rmx = null;
            long now = lastStackDump;
            try {
                rmx = ManagementFactory.getRuntimeMXBean();
                now = rmx.getUptime();
            } catch(Exception e) {
                LOGGER.error("Cannot check runtime uptime", e);
                LOGGER.error("Aborting due to error checking runtime uptime.");
                System.exit(1);
            }

            if(now - lastStackDump > printInterval) {
                try {
                    LOGGER.info("Dumping stack of all running threads.");
                    ThreadInfo[] infos = tmx.dumpAllThreads(true, true);
                    for(ThreadInfo ti : infos)
                        LOGGER.info("Running: "+ti);
                    lastStackDump = now;
                    LOGGER.info("Successfully dumped stack of all running threads.");
                } catch(Exception e) {
                    LOGGER.error("Cannot dump thread stack", e);
                    LOGGER.error("Aborting due to inability to dump thread stack.");
                    System.exit(1);
                }
            }

            LOGGER.info("No errors detected.");
        }
    }

    private static Mono<Void> returnGatewayClient(DiscordClient bot, GatewayDiscordClient gateway) {
        Mono.defer(() -> Commands.registerCommands(gateway)).doOnError(e -> {
                LOGGER.error("Error trying to register global slash commands", e);
            }).block();
        
        return Mono.when(gateway.on(ChatInputInteractionEvent.class, Commands::handleChatCommand),
                         gateway.on(ButtonInteractionEvent.class, Commands::handleButtonInteraction))
            .then();
    }
}
