package humanika.rafeki.james.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import reactor.core.publisher.Mono;

import java.util.Optional;

public class GreetCommand extends SlashCommand {
    @Override
    public String getName() {
        return "greet";
    }

    @Override
    public Mono<Void> handleChatCommand() {
        String name = event.getOption("name")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString)
            .get(); //This is warning us that we didn't check if its present, we can ignore this on required options

        Optional<String> maybeReason = event.getOption("reason")
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString); //This is warning us that we didn't check if its present, we can ignore this on required options

        String reason = maybeReason.isPresent() ? maybeReason.get() : "*unspecified*";

        //Reply to the slash command, with the name the user supplied
        return  event.reply()
            .withEphemeral(true)
            .withContent("Hello, " + name + ". I acknowledge your reason: " + reason);
    }
}
