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

import humanika.rafeki.james.James;

/**
 * A simple interface defining our slash command class contract.
 *  a getName() method to provide the case-sensitive name of the command.
 *  and a handleChatCommand() method which will house all the logic for processing each command.
 */
public abstract class SlashCommand {

    public abstract String getName();
    public String getJson() {
        return getName() + ".json";
    }

    public Mono<Void> handleChatCommand(ChatInputInteractionEvent event) {
        return Mono.empty();
    }

    public Mono<Void> handleButtonInteraction(ButtonInteractionEvent event) {
        return Mono.empty();
    }

    protected Optional<List<ApplicationCommandInteractionOption>> getSubcommand(ChatInputInteractionEvent event, String name) {
        Optional<ApplicationCommandInteractionOption> subcommand = event.getOption(name);
        Optional<List<ApplicationCommandInteractionOption>> result;
        if(!subcommand.isPresent())
            result = Optional.empty();
        else
            result = Optional.of(subcommand.get().getOptions());
        return result;
    }


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

    protected Optional<Long> getLong(ChatInputInteractionEvent event, String name) {
        return event.getOption(name)
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asLong);
    }

    protected long getLongOrDefault(ChatInputInteractionEvent event, String name, long def) {
        Optional<Long> result = getLong(event, name);
        return result.isPresent() ? result.get() : def;
    }

    protected Optional<Attachment> getAttachment(ChatInputInteractionEvent event, String name) {
        return event.getOption(name)
            .flatMap(ApplicationCommandInteractionOption::getValue)
            .map(ApplicationCommandInteractionOptionValue::asAttachment);
    }



    protected boolean isEphemeral(List<ApplicationCommandInteractionOption> options) {
        return getStringOrDefault(options, "hidden", "show").equals("hide");
    }

    protected String getStringOrDefault(List<ApplicationCommandInteractionOption> options, String name, String def) {
        Optional<String> result = getString(options, name);
        return result.isPresent() ? result.get() : def;
    }

    protected Optional<String> getString(List<ApplicationCommandInteractionOption> options, String name) {
        for(ApplicationCommandInteractionOption option : options)
            if(option.getName().equals(name))
                return option.getValue().flatMap(value -> Optional.of(value.asString()) );
        return Optional.empty();
    }

    protected Optional<Long> getLong(List<ApplicationCommandInteractionOption> options, String name) {
        for(ApplicationCommandInteractionOption option : options)
            if(option.getName().equals(name))
                return option.getValue().flatMap(value -> Optional.of(value.asLong()) );
        return Optional.empty();
    }

    protected long getLongOrDefault(List<ApplicationCommandInteractionOption> options, String name, long def) {
        Optional<Long> result = getLong(options, name);
        return result.isPresent() ? result.get() : def;
    }

    protected Optional<Attachment> getAttachment(List<ApplicationCommandInteractionOption> options, String name) {
        for(ApplicationCommandInteractionOption option : options)
            if(option.getName().equals(name))
                return option.getValue().flatMap(value -> Optional.of(value.asAttachment()) );
        return Optional.empty();
    }



    public Mono<Void> handleDirectMessage(ChatInputInteractionEvent event) {
        String babble = James.getState().jamesPhrase("JAMES::ping");
        EmbedCreateSpec creator = EmbedCreateSpec.create()
            .withDescription("This command is unavailable in direct messages.")
            .withTitle("Not in Direct Messages");
        if(babble != null)
            creator = creator.withFooter(EmbedCreateFields.Footer.of(babble, null));
        return event.reply().withEmbeds(creator);
    }
}
