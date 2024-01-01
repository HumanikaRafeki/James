package humanika.rafeki.james.commands;

import reactor.core.publisher.Mono;

public class CRConvertValueSubcommand extends SlashSubcommand {

    @Override
    public String getName() {
        return "crconvert";
    }

    @Override
    public Mono<Void> handleChatCommand() {
        long value = getLongOrDefault("value", 0);
        return chatEvent.reply(String.format("Combat points %s gives a rating of %s.",
                                             value, getRatingFromPoints(value)))
            .withEphemeral(isEphemeral());
    }

    private long getRatingFromPoints(long points) {
        return points > 0 ? (long)Math.log(points) : 0;
    }
}
