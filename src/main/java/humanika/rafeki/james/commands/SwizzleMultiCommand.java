package humanika.rafeki.james.commands;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.gateway.GatewayClient;
import discord4j.gateway.ShardInfo;
import humanika.rafeki.james.James;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import reactor.core.publisher.Mono;

public class SwizzleMultiCommand extends PrimitiveCommand {
    @Override
    public String getName() {
        return "swizzle";
    }

    @Override
    public Optional<String> getJson() {
        return Optional.of("swizzle-subcommands.json");
    }

    @Override
    public Mono<Void> handleChatCommand() {
	Optional<InteractionEventHandler> subcommand = findSubcommand();
        if(!subcommand.isPresent())
            // Should never get here.
            return getChatEvent().reply("You must use the image or search subcommands.");

        return subcommand.get().handleChatCommand();
    }

    @Override
    public Mono<Void> handleButtonInteraction() {
        return new SwizzleSearchSubcommand().withButtonEvent(getButtonEvent()).handleButtonInteraction();
    }

    @Override
    public Optional<InteractionEventHandler> findSubcommand() {
        Optional<List<ApplicationCommandInteractionOption>> options;
        options = data.getSubcommandOptions("search");
        if(options.isPresent())
            return Optional.of(new SwizzleSearchSubcommand().withChatOptions(options.get(), getChatEvent()));
        options = data.getSubcommandOptions("image");
        if(options.isPresent())
            return Optional.of(new SwizzleImageSubcommand().withChatOptions(options.get(), getChatEvent()));
        return Optional.empty();
    }
}
