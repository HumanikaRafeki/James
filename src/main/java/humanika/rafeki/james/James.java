package humanika.rafeki.james;

import okhttp3.OkHttpClient;

import humanika.rafeki.james.data.JamesState;
import humanika.rafeki.james.data.JamesConfig;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.shard.ShardingStrategy;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.management.ThreadInfo;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import discord4j.core.event.EventDispatcher;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

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
        //Creates the gateway client and connects to the gateway
        EventDispatcher customDispatcher = EventDispatcher.builder()
            .eventScheduler(Schedulers.boundedElastic())
            .build();
        DiscordClient bot = DiscordClient.create(System.getenv("BOT_TOKEN"));

        try {
            jamesConfig = new JamesConfig(JAMES_CONFIG_PATH, LOGGER);
        } catch(Exception pe) {
            LOGGER.error("Unable to read settings from ", pe);
            System.exit(1);
        }

        jamesState = new JamesState(jamesConfig, LOGGER);
        try {
            jamesState.update(jamesConfig);
        } catch(Exception se) {
            LOGGER.error("Unable to load initial data", se);
            System.exit(1);
        }
        Thread thread = new Thread() {
                public void run() {
                    bot.gateway().setEventDispatcher(customDispatcher).setSharding(ShardingStrategy.recommended())
                        .withGateway(client -> client.on(ReadyEvent.class)
                                     .doOnNext(ready -> withGatewayClient(bot, client) ))
                        .then().block();
                }
            };
        LOGGER.info("Main thread is started.");
        thread.start();
        long i = 0;
        final long naptime = 30000;
        final long dumpInterval = 120;
        boolean deadlockDetected = false;
        while(thread.isAlive()) {
            i++;
            try {
                thread.join(naptime);
            } catch(InterruptedException ie) {
                LOGGER.info("Main thread join was interrupted.", ie);
            }
            if(!deadlockDetected || i % dumpInterval == 0) {
                ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
                long[] ids = tmx.findDeadlockedThreads();
                deadlockDetected = false;
                if (ids != null) {
                    deadlockDetected = true;
                    ThreadInfo[] infos = tmx.getThreadInfo(ids, true, true);
                    System.out.println("The following threads are deadlocked:");
                    for (ThreadInfo ti : infos) {
                        LOGGER.error("Deadlocked: "+ti);
                        System.out.println(ti);
                    }
                } else {
                    LOGGER.info("All is well.");
                    if(i % dumpInterval == 0) {
                        ThreadInfo[] infos = tmx.dumpAllThreads(true, true);
                        for(ThreadInfo ti : infos)
                            LOGGER.debug("Running: "+ti);
                    }
                }
            }
        }
    }

    private static void withGatewayClient(DiscordClient bot, GatewayDiscordClient gateway) {
        try {
            Commands.registerCommands(gateway).block();
        } catch (Exception e) {
            LOGGER.error("Error trying to register global slash commands", e);
        }

        //Register our slash command listener
        gateway.on(ChatInputInteractionEvent.class, Commands::handleChatCommand)
            .mergeWith(gateway.on(ButtonInteractionEvent.class, Commands::handleButtonInteraction))
            .then(gateway.onDisconnect())
            .block();
        LOGGER.error("Slash command listener ended.");
    }
}
