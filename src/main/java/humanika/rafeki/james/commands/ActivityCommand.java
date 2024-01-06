package humanika.rafeki.james.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import humanika.rafeki.james.James;
import reactor.core.publisher.Mono;

public class ActivityCommand extends PrimitiveSlashCommand {
    @Override
    public String getName() {
        return "activity";
    }

    @Override
    public Mono<Void> handleChatCommand() {
        String babble = James.getState().jamesPhrase("JAMES::activity");
        return getChatEvent().reply(babble!=null ? babble : "I ain't no narc.").withEphemeral(data.isEphemeral());
    }
}
