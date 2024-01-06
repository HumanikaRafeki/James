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
public abstract class PrimitiveSlashCommand implements InteractionEventHandler, EventDataProvider {
    /** Chat event being processed. Will be null in responses */
    protected ChatInputInteractionEvent event = null;

    /** Button event being processed. Will be null in any other situation. */
    protected ButtonInteractionEvent buttonEvent = null;

    public DeferrableInteractionEvent getEvent() {
        if(event != null)
            return event;
        return buttonEvent;
    }

    public Interaction getInteraction() {
        return getEvent().getInteraction();
    }

    public PrimitiveSlashCommand withChatEvent(ChatInputInteractionEvent event) {
        try {
            PrimitiveSlashCommand cloned = clone();
            cloned.event = event;
            return cloned;
        } catch(CloneNotSupportedException cnse) {
            // Should never happen.
            return null;
        }
    }

    public PrimitiveSlashCommand withButtonEvent(ButtonInteractionEvent buttonEvent) {
        try {
            PrimitiveSlashCommand cloned = clone();
            cloned.buttonEvent = buttonEvent;
            return cloned;
        } catch(CloneNotSupportedException cnse) {
            // Should never happen.
            return null;
        }
    }

    @Override
    public PrimitiveSlashCommand clone() throws CloneNotSupportedException {
        try {
            PrimitiveSlashCommand cloned = this.getClass().newInstance();
            cloned.event = event;
            return cloned;
        } catch(InstantiationException ie) {
            throw new CloneNotSupportedException(ie.toString());
        } catch(IllegalAccessException ie) {
            throw new CloneNotSupportedException(ie.toString());
        }
    }

    public abstract String getName();

    public String getFullName() {
        return getName();
    }

    /** Full of the Subcommand being processed. Syntax: "commandname subcommand subsubcommand" */
    protected String getActiveSubcommandPath() {
        return getName();
    }

    public Optional<String> getJson() {
        return Optional.of(getName() + ".json");
    }

    public Mono<Void> handleChatCommand() {
        return Mono.empty();
    }

    public Mono<Void> handleButtonInteraction() {
        return Mono.empty();
    }

    public Optional<PrimitiveSlashSubcommand> findSubcommand() {
        return Optional.empty();
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

    public Mono<Void> handleDirectMessage() {
        String babble = James.getState().jamesPhrase("JAMES::ping");
        EmbedCreateSpec creator = EmbedCreateSpec.create()
            .withDescription("This command is unavailable in direct messages.")
            .withTitle("Not in Direct Messages");
        if(babble != null)
            creator = creator.withFooter(EmbedCreateFields.Footer.of(babble, null));
        return event.reply().withEmbeds(creator);
    }
}
