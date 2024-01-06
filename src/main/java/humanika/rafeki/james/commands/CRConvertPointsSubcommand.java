package humanika.rafeki.james.commands;

import reactor.core.publisher.Mono;

public class CRConvertPointsSubcommand extends PrimitiveSlashSubcommand {
    @Override
    public String getName() {
        return "points";
    }

    @Override
    public Mono<Void> handleChatCommand() {
        long value = data.getLongOrDefault("value", 0);
        return getChatEvent().reply(String.format("Combat rating %s requires %s combat points.",
                                         value, getPointsFromRating(value)))
            .withEphemeral(data.isEphemeral());
    }

    private long getPointsFromRating(long rating) {
        return rating > 0 ? (long)Math.ceil(Math.exp(rating)) : 0;
    }
}
