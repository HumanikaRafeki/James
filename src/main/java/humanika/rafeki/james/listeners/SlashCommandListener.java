package humanika.rafeki.james.listeners;

import humanika.rafeki.james.commands.GreetCommand;
import humanika.rafeki.james.commands.PingCommand;
import humanika.rafeki.james.commands.IndokorathCommand;
import humanika.rafeki.james.commands.SlashCommand;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.GatewayDiscordClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import humanika.rafeki.james.GlobalCommandRegistrar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SlashCommandListener {
    //An array list of classes that implement the SlashCommand interface
    private final static List<SlashCommand> commands = new ArrayList<>();
    private final static List<String> commandJson = new ArrayList<>();

    static {
        //We register our commands here when the class is initialized
        commands.add(new PingCommand());
        commands.add(new IndokorathCommand());
        commands.add(new GreetCommand());
        for(SlashCommand command : commands)
            commandJson.add(command.getJson());
    }

    public static void registerCommands(GatewayDiscordClient gateway) throws IOException {
        new GlobalCommandRegistrar(gateway.getRestClient()).registerCommands(commandJson);
    }

    public static Mono<Void> handle(ChatInputInteractionEvent event) {
        // Convert our array list to a flux that we can iterate through
        return Flux.fromIterable(commands)
            //Filter out all commands that don't match the name of the command this event is for
            .filter(command -> command.getName().equals(event.getCommandName()))
            // Get the first (and only) item in the flux that matches our filter
            .next()
            //have our command class handle all the logic related to its specific command.
            .flatMap(command -> command.handle(event));
    }
}
