package humanika.rafeki.james.commands;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Attachment;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import java.util.List;
import java.util.Optional;
import reactor.core.publisher.Mono;

public abstract class PrimitiveSlashSubcommand implements Cloneable, EventDataProvider {
    /** Subcommand options from the original event. Will be null in responses. */
    protected List<ApplicationCommandInteractionOption> options = null;

    /** Chat event currently being processed. Will be null in responses. */
    protected ChatInputInteractionEvent chatEvent;

    /** Button event currently being processed. Will be null otherwise. */
    protected ButtonInteractionEvent buttonEvent;

    public abstract String getName();

    public String getFullName() {
        return getName();
    }

    public DeferrableInteractionEvent getEvent() {
        if(chatEvent != null)
            return chatEvent;
        return buttonEvent;
    }

    public Interaction getInteraction() {
        return getEvent().getInteraction();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public PrimitiveSlashSubcommand forChatEvent(List<ApplicationCommandInteractionOption> options, ChatInputInteractionEvent chatEvent) {
        try {
            PrimitiveSlashSubcommand result = (PrimitiveSlashSubcommand)clone();
            result.options = options;
            result.chatEvent = chatEvent;
            result.buttonEvent = null;
            return result;
        } catch(CloneNotSupportedException cnse) {
            // Should never happen.
            return null;
        }
    }

    public PrimitiveSlashSubcommand forButtonEvent(ButtonInteractionEvent buttonEvent) {
        try {
            PrimitiveSlashSubcommand result = (PrimitiveSlashSubcommand)clone();
            result.options = options;
            result.chatEvent = chatEvent;
            result.buttonEvent = buttonEvent;
            return result;
        } catch(CloneNotSupportedException cnse) {
            // Should never happen.
            return null;
        }
    }

    public Mono<Void> handleChatCommand() {
        return Mono.empty();
    }

    public Mono<Void> handleButtonInteraction() {
        return Mono.empty();
    }

    public Optional<String> getJson() {
        return Optional.empty();
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
