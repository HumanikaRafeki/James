package humanika.rafeki.james.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Attachment;
import java.util.List;
import java.util.Optional;
import reactor.core.publisher.Mono;

public class ChatEventDataProvider implements EventDataProvider {
    private ChatInputInteractionEvent event;

    ChatEventDataProvider(ChatInputInteractionEvent event) {
        this.event = event;
    }

    public Interaction getInteraction() {
        return getEvent().getInteraction();
    }

    public DeferrableInteractionEvent getEvent() {
        return event;
    }

    public boolean isEphemeral() {
        Optional<String> maybeHide = getString("hidden");
        return maybeHide.isPresent() && maybeHide.get().equals("hide");
    }

    public Optional<List<ApplicationCommandInteractionOption>> getSubcommandOptions(String name) {
        Optional<ApplicationCommandInteractionOption> subcommand = event.getOption(name);
        Optional<List<ApplicationCommandInteractionOption>> result;
        if(!subcommand.isPresent())
            result = Optional.empty();
        else
            result = Optional.of(subcommand.get().getOptions());
        return result;
    }

    public String getStringOrDefault(String name, String def) {
        Optional<String> result = getString(name);
        return result.isPresent() ? result.get() : def;
    }

    public Optional<String> getString(String name) {
        return event.getOption(name)
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asString);
    }

    public Optional<Long> getLong(String name) {
        return event.getOption(name)
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asLong);
    }

    public long getLongOrDefault(String name, long def) {
        Optional<Long> result = getLong(name);
        return result.isPresent() ? result.get() : def;
    }

    public Optional<Boolean> getBoolean(String name) {
        return event.getOption(name)
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asBoolean);
    }

    public Boolean getBooleanOrDefault(String name, Boolean def) {
        Optional<Boolean> result = getBoolean(name);
        return result.isPresent() ? result.get() : def;
    }

    public Optional<Attachment> getAttachment(String name) {
        return event.getOption(name)
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asAttachment);
    }
}
