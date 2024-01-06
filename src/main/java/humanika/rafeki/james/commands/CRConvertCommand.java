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

public class CRConvertCommand extends PrimitiveCommand {
    @Override
    public String getName() {
        return "crconvert";
    }

    @Override
    public Mono<Void> handleChatCommand() {
	Optional<InteractionEventHandler> subcommand = findSubcommand();
        if(!subcommand.isPresent())
            // Should never get here.
            return getChatEvent().reply("You must use the cr or points subcommands.");

        return subcommand.get().handleChatCommand();
    }

    @Override
    public Optional<InteractionEventHandler> findSubcommand() {
        Optional<List<ApplicationCommandInteractionOption>> cr = data.getSubcommandOptions("cr");
        if(cr.isPresent())
            return Optional.of(new CRConvertValueSubcommand().withChatOptions(cr.get(), getChatEvent()));
        Optional<List<ApplicationCommandInteractionOption>> points = data.getSubcommandOptions("points");
        if(points.isPresent())
            return Optional.of(new CRConvertPointsSubcommand().withChatOptions(points.get(), getChatEvent()));
        return Optional.empty();
    }
}
