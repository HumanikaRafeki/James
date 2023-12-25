package com.github.HumanikaRafeki.james;

import com.github.HumanikaRafeki.james.listeners.SlashCommandListener;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.shard.ShardingStrategy;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.reactivestreams.Publisher;

import java.util.List;

public class SimpleBot {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleBot.class);

    public static void main(String[] args) {
        //Creates the gateway client and connects to the gateway
        DiscordClient bot = DiscordClient.create(System.getenv("BOT_TOKEN"));

        bot.gateway().setSharding(ShardingStrategy.recommended())
            .withGateway(client -> client.on(ReadyEvent.class)
                         .doOnNext(ready -> withGatewayClient(bot, client) ))
                         .then()
            .block();
    }

    private static void withGatewayClient(DiscordClient bot, GatewayDiscordClient gateway) {
        /* Call our code to handle creating/deleting/editing our global slash commands.

        We have to hard code our list of command files since iterating over a list of files in a resource directory
         is overly complicated for such a simple demo and requires handling for both IDE and .jar packaging.

         Using SpringBoot we can avoid all of this and use their resource pattern matcher to do this for us.
         */
        List<String> commands = List.of("greet.json", "ping.json");
        try {
            new GlobalCommandRegistrar(gateway.getRestClient()).registerCommands(commands);
        } catch (Exception e) {
            LOGGER.error("Error trying to register global slash commands", e);
        }

        //Register our slash command listener
        gateway.on(ChatInputInteractionEvent.class, SlashCommandListener::handle)
            .then(gateway.onDisconnect())
            .block(); // We use .block() as there is not another non-daemon thread and the jvm would close otherwise.
    }
}
