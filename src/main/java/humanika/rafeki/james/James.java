package humanika.rafeki.james;

import okhttp3.OkHttpClient;

import humanika.rafeki.james.listeners.SlashCommandListener;
import humanika.rafeki.james.data.JamesState;
import humanika.rafeki.james.data.JamesConfig;
import humanika.rafeki.james.phrases.PhraseLimits;
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
                    bot.gateway().setSharding(ShardingStrategy.recommended())
                        .withGateway(client -> client.on(ReadyEvent.class)
                                     .doOnNext(ready -> withGatewayClient(bot, client) ))
                        .then().block();
                }
            };
        LOGGER.info("Main thread is started.");
        thread.start();
        while(thread.isAlive()) {
            try {
                thread.join(30000);
            } catch(InterruptedException ie) {
                LOGGER.info("Main thread join was interrupted.", ie);
            }
            ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
            long[] ids = tmx.findDeadlockedThreads();
            if (ids != null) {
                ThreadInfo[] infos = tmx.getThreadInfo(ids, true, true);
                System.out.println("The following threads are deadlocked:");
                for (ThreadInfo ti : infos) {
                    LOGGER.error("Deadlocked: "+ti);
                    System.out.println(ti);
                }
            } else
                LOGGER.info("All is well.");
        }
    }

    private static void withGatewayClient(DiscordClient bot, GatewayDiscordClient gateway) {
        /* Call our code to handle creating/deleting/editing our global slash commands.

        We have to hard code our list of command files since iterating over a list of files in a resource directory
         is overly complicated for such a simple demo and requires handling for both IDE and .jar packaging.

         Using SpringBoot we can avoid all of this and use their resource pattern matcher to do this for us.
         */
        try {
            SlashCommandListener.registerCommands(gateway);
        } catch (Exception e) {
            LOGGER.error("Error trying to register global slash commands", e);
        }

        //Register our slash command listener
        gateway.on(ChatInputInteractionEvent.class, SlashCommandListener::handleChatCommand)
            .mergeWith(gateway.on(ButtonInteractionEvent.class, SlashCommandListener::handleButtonInteraction))
            .then(gateway.onDisconnect())
            .block();
    }
}
