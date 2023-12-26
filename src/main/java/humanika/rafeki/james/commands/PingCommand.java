package humanika.rafeki.james.commands;

import java.util.Optional;
import java.time.Duration;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import reactor.core.publisher.Mono;
import discord4j.gateway.ShardInfo;
import discord4j.core.GatewayDiscordClient;
import discord4j.gateway.GatewayClient;

import humanika.rafeki.james.James;

public class PingCommand extends SlashCommand {
    @Override
    public String getName() {
        return "ping";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        return event.reply()
            .withEphemeral(isEphemeral(event))
            .withContent(getPing(event) + '\n' + getCommentary(event));
    }

    private String getCommentary(ChatInputInteractionEvent event) {
        String babble = James.getState().jamesPhrase("JAMES::ping");
        return babble!=null ? babble : "*no commentary*";
    }

    private String getPing(ChatInputInteractionEvent event) {
        ShardInfo shard = event.getShardInfo();
        int shardId = shard.getIndex();
        GatewayDiscordClient discord = event.getClient();
        Optional<GatewayClient> optionalGateway = discord.getGatewayClient(shardId);
        String ping = "*cannot find gateway*";
        if(optionalGateway.isPresent()) {
            GatewayClient gateway = optionalGateway.get();
            Duration responseTime = gateway.getResponseTime();
            double seconds = responseTime.getSeconds();
            double nano = responseTime.getNano();
            double milliping = seconds*1e3 + nano*1e-6; // milliseconds
            if(milliping > 0) {
                double bpm = Math.round(60.0 / Math.max(milliping / 1000, 1e-9));
                ping = String.format("Last heartbeat took %.1f ms (%.1f BPM).", milliping, bpm);
            }
        }
        return ping;
    }
}
