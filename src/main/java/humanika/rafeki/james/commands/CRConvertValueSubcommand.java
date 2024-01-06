package humanika.rafeki.james.commands;

import reactor.core.publisher.Mono;

public class CRConvertValueSubcommand extends PrimitiveCommand {

    @Override
    public String getName() {
        return "cr";
    }

    @Override
    public String getFullName() {
        return "crconvert cr";
    }

    @Override
    public Mono<Void> handleChatCommand() {
        long value = data.getLongOrDefault("value", 0);
        return getChatEvent().reply(String.format("Combat points %s gives a rating of %s.",
                                             value, getRatingFromPoints(value)))
            .withEphemeral(data.isEphemeral());
    }

    private long getRatingFromPoints(long points) {
        return points > 0 ? (long)Math.log(points) : 0;
    }
}
