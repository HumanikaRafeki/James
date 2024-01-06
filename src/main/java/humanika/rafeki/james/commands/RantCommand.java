package humanika.rafeki.james.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import humanika.rafeki.james.James;
import reactor.core.publisher.Mono;

public class RantCommand extends PrimitiveSlashCommand {
    @Override
    public String getName() {
        return "rant";
    }

    @Override
    public Mono<Void> handleChatCommand() {
        String babble = James.getState().jamesPhrase("JAMES::rant");
        return event.reply(babble!=null ? babble : "I have nothing to say to you.").withEphemeral(isEphemeral());
    }
}
