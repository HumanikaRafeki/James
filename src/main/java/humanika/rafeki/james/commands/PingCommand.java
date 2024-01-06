package humanika.rafeki.james.commands;

import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.gateway.GatewayClient;
import discord4j.gateway.ShardInfo;
import humanika.rafeki.james.James;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import reactor.core.publisher.Mono;

public class PingCommand extends PrimitiveCommand {
    @Override
    public String getName() {
        return "ping";
    }

    @Override
    public Mono<Void> handleChatCommand() {
        EmbedCreateSpec creator = EmbedCreateSpec.create()
            .withDescription(getPing())
            .withFooter(EmbedCreateFields.Footer.of(getCommentary(), null));

        creator = creator.withTitle("Pong");

        return getChatEvent().reply().withEmbeds(creator).withEphemeral(data.isEphemeral());
    }

    private String getCommentary() {
        String babble = James.getState().jamesPhrase("JAMES::ping");
        return babble!=null ? babble : "*no commentary*";
    }

    private String getPing() {
        long interactionTime = data.getInteraction().getId().getTimestamp().toEpochMilli();
        long interactionAge = Instant.now().toEpochMilli() - interactionTime;
        double latencyAgeDouble = interactionAge;
        double interactionBPM = Math.round(60.0 / Math.max(latencyAgeDouble / 1000, 1e-9));
        String latencyMessage = String.format("Communication latency is %.1f ms (%.1f BPM)\n",
                                              latencyAgeDouble, interactionBPM);

        ShardInfo shard = getChatEvent().getShardInfo();
        int shardId = shard.getIndex();
        GatewayDiscordClient discord = getChatEvent().getClient();
        Optional<GatewayClient> optionalGateway = discord.getGatewayClient(shardId);
        String pingMessage = "*cannot find gateway*";

        if(optionalGateway.isPresent()) {
            GatewayClient gateway = optionalGateway.get();
            Duration responseTime = gateway.getResponseTime();
            double seconds = responseTime.getSeconds();
            double nano = responseTime.getNano();
            double milliping = seconds*1e3 + nano*1e-6; // milliseconds
            if(milliping > 0) {
                double bpm = Math.round(60.0 / Math.max(milliping / 1000, 1e-9));
                pingMessage = String.format("Last heartbeat took %.1f ms (%.1f BPM).", milliping, bpm);
            }
        }
        return latencyMessage + pingMessage;
    }
}
