package humanika.rafeki.james.commands;
import reactor.core.publisher.Mono;

public class CRConvertPointsSubcommand extends PrimitiveSlashSubcommand {
    @Override
    public String getName() {
        return "points";
    }

    @Override
    public Mono<Void> handleChatCommand() {
        if(options == null)
            System.out.println("NULL OPTIONS");
        if(chatEvent == null)
            System.out.println("NULL EVENT");
        long value = getLong("value").get();
        return chatEvent.reply(String.format("Combat rating %s requires %s combat points.",
                                         value, getPointsFromRating(value)))
            .withEphemeral(isEphemeral());
    }

    private long getPointsFromRating(long rating) {
        return rating > 0 ? (long)Math.ceil(Math.exp(rating)) : 0;
    }
}
