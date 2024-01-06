package humanika.rafeki.james.commands;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Attachment;
import java.util.List;
import java.util.Optional;
import reactor.core.publisher.Mono;

public class SubcommandDataProvider implements EventDataProvider {
    private DeferrableInteractionEvent event;
    List<ApplicationCommandInteractionOption> options;

    SubcommandDataProvider(List<ApplicationCommandInteractionOption> options, DeferrableInteractionEvent event) {
        this.options = options;
        this.event = event;
    }

    public Interaction getInteraction() {
        return event.getInteraction();
    }

    public DeferrableInteractionEvent getEvent() {
        return event;
    }

    public boolean isEphemeral() {
        return getStringOrDefault("hidden", "show").equals("hide");
    }

    public Optional<List<ApplicationCommandInteractionOption>> getSubcommandOptions(String name) {
        for(ApplicationCommandInteractionOption option : options)
            if(option.getName().equals(name))
                return Optional.of(option.getOptions());
        return Optional.empty();
    }

    public String getStringOrDefault(String name, String def) {
        Optional<String> result = getString(name);
        return result.isPresent() ? result.get() : def;
    }

    public Optional<String> getString(String name) {
        for(ApplicationCommandInteractionOption option : options)
            if(option.getName().equals(name))
                return option.getValue().flatMap(value -> Optional.of(value.asString()) );
        return Optional.empty();
    }

    public Optional<Long> getLong(String name) {
        for(ApplicationCommandInteractionOption option : options)
            if(option.getName().equals(name))
                return option.getValue().flatMap(value -> Optional.of(value.asLong()) );
        return Optional.empty();
    }

    public long getLongOrDefault(String name, long def) {
        Optional<Long> result = getLong(name);
        return result.isPresent() ? result.get() : def;
    }

    public Optional<Boolean> getBoolean(String name) {
        for(ApplicationCommandInteractionOption option : options)
            if(option.getName().equals(name))
                return option.getValue().flatMap(value -> Optional.of(value.asBoolean()));
        return Optional.empty();
    }

    public Boolean getBooleanOrDefault(String name, Boolean def) {
        Optional<Boolean> result = getBoolean(name);
        return result.isPresent() ? result.get() : def;
    }

    public Optional<Attachment> getAttachment(String name) {
        for(ApplicationCommandInteractionOption option : options)
            if(option.getName().equals(name))
                return option.getValue().flatMap(value -> Optional.of(value.asAttachment()) );
        return Optional.empty();
    }
}
