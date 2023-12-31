package humanika.rafeki.james.listeners;

import humanika.rafeki.james.commands.*;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.object.command.Interaction;
import discord4j.common.util.Snowflake;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import humanika.rafeki.james.GlobalCommandRegistrar;
import humanika.rafeki.james.James;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;

public class SlashCommandListener {
    //An array list of classes that implement the SlashCommand interface
    private final static List<SlashCommand> commands = new ArrayList<>();
    private final static List<String> commandJson = new ArrayList<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(James.class);

    static {
        //We register our commands here when the class is initialized
        commands.add(new RantCommand());
        commands.add(new PingCommand());
        commands.add(new IndokorathCommand());
        commands.add(new KorathCommand());
        commands.add(new PhrasesCommand());
        commands.add(new NewsCommand());
        commands.add(new SayCommand());
        commands.add(new CRConvertCommand());
        commands.add(new SwizzleImageCommand());
        commands.add(new SwizzleCommand());
        commands.add(new LookupCommand());
        for(SlashCommand command : commands)
            commandJson.add(command.getJson());
    }

    public static void registerCommands(GatewayDiscordClient gateway) throws IOException {
        new GlobalCommandRegistrar(gateway.getRestClient()).registerCommands(commandJson);
    }

    private static boolean buildLogMessage(String what, String name, Interaction interaction, StringBuilder message) {
        message.append(what).append(" ").append(name).append(" id=").append(interaction.getId().asLong());

        Optional<Snowflake> guildId = interaction.getGuildId();
        if(guildId.isPresent())
            message.append(" server=").append(guildId.get().asLong());

        Snowflake channelId = interaction.getChannelId();
        message.append(" channel=").append(channelId.asLong());

        Optional<Member> maybeMember = interaction.getMember();
        boolean isBot = false;
        if(maybeMember.isPresent()) {
            Member member = maybeMember.get();
            if(member.isBot())
                isBot = true;
            message.append(" member=").append(member.getId().asLong());
        }

        return isBot;
    }

    public static Mono<Void> handleChatCommand(ChatInputInteractionEvent event) {
        StringBuilder message = new StringBuilder();
        String commandName = event.getCommandName();
        boolean isBot = buildLogMessage("command", commandName, event.getInteraction(), message);

        if(isBot)
            message.append(" REJECT: bot");
        else
            message.append(" ACCEPT");

        LOGGER.info(message.toString());

        if(isBot)
            return Mono.empty();

        // Convert our array list to a flux that we can iterate through
        return Flux.fromIterable(commands)
            //Filter out all commands that don't match the name of the command this event is for
            .filter(command -> command.getName().equals(commandName))
            // Get the first (and only) item in the flux that matches our filter
            .next()
            //have our command class handle all the logic related to its specific command.
            .flatMap(command -> command.handleChatCommand(event));
    }

    public static Mono<Void> handleButtonInteraction(ButtonInteractionEvent event) {
        StringBuilder message = new StringBuilder();

        String[] split = event.getCustomId().split(":", 2);
        String commandName = split[0];

        boolean isBot = buildLogMessage("button", commandName, event.getInteraction(), message);

        if(isBot)
            message.append(" REJECT: bot");
        else
            message.append(" ACCEPT");

        LOGGER.info(message.toString());

        if(isBot)
            return Mono.empty();

        Mono<Void> deferEdit = event.deferEdit();
        // Convert our array list to a flux that we can iterate through
        Mono<Void> buttonInteraction = Flux.fromIterable(commands)
            //Filter out all commands that don't match the name of the command this event is for
            .filter(command -> command.getName().equals(commandName))
            // Get the first (and only) item in the flux that matches our filter
            .next()
            //have our command class handle all the logic related to its specific command.
            .flatMap(command -> command.handleButtonInteraction(event));
        return deferEdit.concatWith(buttonInteraction).then();
    }
}
