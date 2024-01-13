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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import reactor.core.publisher.Mono;

public abstract class PrimitiveCommand implements Cloneable, InteractionEventHandler {
    /** Subcommand options from the original event. Will be null in responses. */
    private List<ApplicationCommandInteractionOption> options = null;

    protected EventDataProvider data = null;

    /** Chat event currently being processed. Will be null in responses. */
    private ChatInputInteractionEvent chatEvent;

    /** Button event currently being processed. Will be null otherwise. */
    private ButtonInteractionEvent buttonEvent;

    @Override
    public boolean shouldDefer() {
        return false;
    }

    @Override
    public String getFullName() {
        return getName();
    }

    @Override
    public Optional<String> getJson() {
        return Optional.of(getName() + ".json");
    }

    @Override
    public Mono<Void> handleChatCommand() {
        return Mono.empty();
    }

    @Override
    public Mono<Void> handleButtonInteraction() {
        return Mono.empty();
    }

    @Override
    public EventDataProvider getData() {
        return data;
    }

    @Override
    public InteractionEventHandler withChatEvent(ChatInputInteractionEvent chatEvent) {
        try {
            PrimitiveCommand result = (PrimitiveCommand)clone();
            result.data = new ChatEventDataProvider(chatEvent);
            result.chatEvent = chatEvent;
            return result;
        } catch(CloneNotSupportedException cnse) {
            // Should never happen.
            return null;
        }
    }

    @Override
    public InteractionEventHandler withChatOptions(List<ApplicationCommandInteractionOption> options, ChatInputInteractionEvent chatEvent) {
        try {
            PrimitiveCommand result = (PrimitiveCommand)clone();
            result.options = options;
            result.data = new SubcommandDataProvider(options, chatEvent);
            result.chatEvent = chatEvent;
            return result;
        } catch(CloneNotSupportedException cnse) {
            // Should never happen.
            return null;
        }
    }

    @Override
    public InteractionEventHandler withButtonEvent(ButtonInteractionEvent buttonEvent) {
        try {
            PrimitiveCommand result = (PrimitiveCommand)clone();
            result.data = new ButtonEventDataProvider(buttonEvent);
            result.buttonEvent = buttonEvent;
            return result;
        } catch(CloneNotSupportedException cnse) {
            // Should never happen.
            return null;
        }
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    protected ChatInputInteractionEvent getChatEvent() {
        return chatEvent;
    }

    protected ButtonInteractionEvent getButtonEvent() {
        return buttonEvent;
    }

    protected List<ApplicationCommandInteractionOption> getOptions() {
        return options;
    }

    public Optional<InteractionEventHandler> findSubcommand() {
        return Optional.empty();
    }
}
