package humanika.rafeki.james.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import reactor.core.publisher.Mono;
import java.util.Optional;
/**
 * A simple interface defining our slash command class contract.
 *  a getName() method to provide the case-sensitive name of the command.
 *  and a handle() method which will house all the logic for processing each command.
 */
public abstract class SlashCommand {

    public abstract String getName();
    public String getJson() {
        return getName() + ".json";
    }

    public abstract Mono<Void> handle(ChatInputInteractionEvent event);

    protected boolean isEphemeral(ChatInputInteractionEvent event) {
        Optional<String> maybeHide = getString(event, "hidden");
        return maybeHide.isPresent() && maybeHide.get().equals("hide");
    }

    protected String getStringOrDefault(ChatInputInteractionEvent event, String name, String def) {
        Optional<String> result = getString(event, name);
        return result.isPresent() ? result.get() : def;
    }

    protected Optional<String> getString(ChatInputInteractionEvent event, String name) {
        return event.getOption(name)
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString);
    }
}
