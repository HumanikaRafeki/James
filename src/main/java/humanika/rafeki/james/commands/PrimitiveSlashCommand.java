package humanika.rafeki.james.commands;

import discord4j.core.object.command.Interaction;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Attachment;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import humanika.rafeki.james.James;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import reactor.core.publisher.Mono;

/**
 * A simple interface defining our slash command class contract.
 *  a getName() method to provide the case-sensitive name of the command.
 *  and a handleChatCommand() method which will house all the logic for processing each command.
 */
public abstract class PrimitiveSlashCommand implements Cloneable, InteractionEventHandler {
    /** Chat event being processed. Will be null in responses */
    private ChatInputInteractionEvent chatEvent = null;

    /** Button event being processed. Will be null in any other situation. */
    private ButtonInteractionEvent buttonEvent = null;

    protected EventDataProvider data = null;

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
    public PrimitiveSlashCommand withChatEvent(ChatInputInteractionEvent chatEvent) {
        try {
            PrimitiveSlashCommand cloned = (PrimitiveSlashCommand)clone();
            cloned.data = new ChatEventDataProvider(chatEvent);
            cloned.chatEvent = chatEvent;
            return cloned;
        } catch(CloneNotSupportedException cnse) {
            // Should never happen.
            System.out.println("Clone not supported!");
            return null;
        }
    }

    @Override
    public InteractionEventHandler withChatOptions(List<ApplicationCommandInteractionOption> options, ChatInputInteractionEvent chatEvent) {
        return withChatEvent(chatEvent);
    }

    @Override
    public PrimitiveSlashCommand withButtonEvent(ButtonInteractionEvent buttonEvent) {
        try {
            PrimitiveSlashCommand cloned = (PrimitiveSlashCommand)clone();
            cloned.data = new ButtonEventDataProvider(buttonEvent);
            cloned.buttonEvent = buttonEvent;
            return cloned;
        } catch(CloneNotSupportedException cnse) {
            // Should never happen.
            System.out.println("Clone not supported!");
            return null;
        }
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /** Full of the Subcommand being processed. Syntax: "commandname subcommand subsubcommand" */
    protected String getActiveSubcommandPath() {
        return getName();
    }

    public Optional<InteractionEventHandler> findSubcommand() {
        return Optional.empty();
    }

    protected ChatInputInteractionEvent getChatEvent() {
        return chatEvent;
    }

    protected ButtonInteractionEvent getButtonEvent() {
        return buttonEvent;
    }
}
