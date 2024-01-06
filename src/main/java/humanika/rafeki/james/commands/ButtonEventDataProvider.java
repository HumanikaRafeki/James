package humanika.rafeki.james.commands;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Attachment;
import java.util.List;
import java.util.Optional;
import reactor.core.publisher.Mono;

public class ButtonEventDataProvider implements EventDataProvider {
    private ButtonInteractionEvent event;

    ButtonEventDataProvider(ButtonInteractionEvent event) {
        this.event = event;
    }

    public Interaction getInteraction() {
        return getEvent().getInteraction();
    }

    public DeferrableInteractionEvent getEvent() {
        return event;
    }

    public boolean isEphemeral() {
        return true;
    }

    public Optional<List<ApplicationCommandInteractionOption>> getSubcommandOptions(String name) {
        return Optional.empty();
    }

    public String getStringOrDefault(String name, String def) {
        return def;
    }

    public Optional<String> getString(String name) {
        return Optional.empty();
    }

    public Optional<Long> getLong(String name) {
        return Optional.empty();
    }

    public long getLongOrDefault(String name, long def) {
        return def;
    }

    public Optional<Boolean> getBoolean(String name) {
        return Optional.empty();
    }

    public Boolean getBooleanOrDefault(String name, Boolean def) {
        return def;
    }

    public Optional<Attachment> getAttachment(String name) {
        return Optional.empty();
    }
}
