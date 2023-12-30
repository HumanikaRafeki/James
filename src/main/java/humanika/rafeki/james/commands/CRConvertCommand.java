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

public class CRConvertCommand extends SlashCommand {
    @Override
    public String getName() {
        return "crconvert";
    }

    @Override
    public Mono<Void> handleChatCommand(ChatInputInteractionEvent event) {
        if(!event.getInteraction().getGuildId().isPresent())
            return handleDirectMessage(event);

        Optional<List<ApplicationCommandInteractionOption>> cr = getSubcommand(event, "cr");
        Optional<List<ApplicationCommandInteractionOption>> points = getSubcommand(event, "points");

        if(cr.isPresent())
            return handleCr(event, cr.get());
        else if(points.isPresent())
            return handlePoints(event, points.get());
        // Should never get here.
        return event.reply("You must use the cr or points subcommands.");
    }

    private Mono<Void> handleCr(ChatInputInteractionEvent event, List<ApplicationCommandInteractionOption> options) {
        long value = getLongOrDefault(options, "value", 0);
        return event.reply(String.format("Combat points %s gives a rating of %s.",
                                         value, getRatingFromPoints(value)))
            .withEphemeral(isEphemeral(options));
    }

    private Mono<Void> handlePoints(ChatInputInteractionEvent event, List<ApplicationCommandInteractionOption> options) {
        long value = getLong(options, "value").get();
        return event.reply(String.format("Combat rating %s requires %s combat points.",
                                         value, getPointsFromRating(value)))
            .withEphemeral(isEphemeral(options));
    }

    private long getRatingFromPoints(long points) {
        return points > 0 ? (long)Math.log(points) : 0;
    }

    private long getPointsFromRating(long rating) {
        return rating > 0 ? (long)Math.ceil(Math.exp(rating)) : 0;
    }
}
