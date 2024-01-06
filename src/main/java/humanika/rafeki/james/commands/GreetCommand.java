package humanika.rafeki.james.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class GreetCommand extends PrimitiveCommand {
    @Override
    public String getName() {
        return "greet";
    }

    @Override
    public Mono<Void> handleChatCommand() {
        String name = data.getString("name").get();

        Optional<String> maybeReason = data.getString("reason");
        String reason = maybeReason.isPresent() ? maybeReason.get() : "*unspecified*";

        //Reply to the slash command, with the name the user supplied
        return  getChatEvent().reply()
            .withEphemeral(true)
            .withContent("Hello, " + name + ". I acknowledge your reason: " + reason);
    }
}
