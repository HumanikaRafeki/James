package humanika.rafeki.james.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.object.entity.Attachment;

import reactor.core.publisher.Mono;
import java.util.Optional;
import java.util.List;

public abstract class SlashSubcommand implements Cloneable {
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

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public SlashSubcommand forChatEvent(List<ApplicationCommandInteractionOption> options, ChatInputInteractionEvent chatEvent) {
        try {
            SlashSubcommand result = (SlashSubcommand)clone();
            result.options = options;
            result.chatEvent = chatEvent;
            result.buttonEvent = null;
            return result;
        } catch(CloneNotSupportedException cnse) {
            // Should never happen.
            return null;
        }
    }

    public SlashSubcommand forButtonEvent(ButtonInteractionEvent buttonEvent) {
        try {
            SlashSubcommand result = (SlashSubcommand)clone();
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

    protected boolean isEphemeral() {
        return getStringOrDefault("hidden", "show").equals("hide");
    }

    protected String getStringOrDefault(String name, String def) {
        Optional<String> result = getString(name);
        return result.isPresent() ? result.get() : def;
    }

    protected Optional<String> getString(String name) {
        for(ApplicationCommandInteractionOption option : options)
            if(option.getName().equals(name))
                return option.getValue().flatMap(value -> Optional.of(value.asString()) );
        return Optional.empty();
    }

    protected Optional<Long> getLong(String name) {
        for(ApplicationCommandInteractionOption option : options)
            if(option.getName().equals(name))
                return option.getValue().flatMap(value -> Optional.of(value.asLong()) );
        return Optional.empty();
    }

    protected long getLongOrDefault(String name, long def) {
        Optional<Long> result = getLong(name);
        return result.isPresent() ? result.get() : def;
    }

    protected Optional<Attachment> getAttachment(String name) {
        for(ApplicationCommandInteractionOption option : options)
            if(option.getName().equals(name))
                return option.getValue().flatMap(value -> Optional.of(value.asAttachment()) );
        return Optional.empty();
    }


}
