package humanika.rafeki.james.commands;

import java.util.Optional;
import java.time.Duration;
import java.net.URI;
import java.util.List;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
// import discord4j.core.event.domain.interaction.InteractionApplicationCommandCallbackReplyMono;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Mono;
import discord4j.gateway.ShardInfo;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.gateway.GatewayClient;

import humanika.rafeki.james.James;

public class CRConvertCommand extends PrimitiveSlashCommand {
    @Override
    public String getName() {
        return "crconvert";
    }

    @Override
    public Mono<Void> handleChatCommand() {
        if(!event.getInteraction().getGuildId().isPresent())
            return handleDirectMessage();

	Optional<PrimitiveSlashSubcommand> subcommand = findSubcommand();
        if(!subcommand.isPresent())
            // Should never get here.
            return event.reply("You must use the cr or points subcommands.");

        return subcommand.get().handleChatCommand();
    }

    @Override
    public Optional<PrimitiveSlashSubcommand> findSubcommand() {
        Optional<List<ApplicationCommandInteractionOption>> cr = getSubcommandOptions("cr");
        if(cr.isPresent())
            return Optional.of(new CRConvertValueSubcommand().forChatEvent(cr.get(), event));
        Optional<List<ApplicationCommandInteractionOption>> points = getSubcommandOptions("points");
        if(points.isPresent())
            return Optional.of(new CRConvertPointsSubcommand().forChatEvent(points.get(), event));
        return Optional.empty();
    }
}
