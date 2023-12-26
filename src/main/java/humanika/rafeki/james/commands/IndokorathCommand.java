package humanika.rafeki.james.commands;

import java.util.Optional;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;

import humanika.rafeki.james.James;
import humanika.rafeki.james.Utils;
import humanika.rafeki.james.utils.KorathCipher;

public class IndokorathCommand extends SlashCommand {
    private static final int MAX_RESPONSE_LENGTH = 4000;
    @Override
    public String getName() {
        return "indokorath";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        String text = getStringOrDefault(event, "text", "").replaceAll("[@\t\n *<>|]+", " ").strip();
        boolean ephemeral = isEphemeral(event);
        String mention = ephemeral ? null : event.getInteraction().getUser().getMention();
        return event.reply().withEphemeral(ephemeral)
            .withContent(text.length() > 0 && text.matches(".*[a-zA-Z]+") ? cipher(text, mention) : "*no text*");
    }

    private String cipher(String indonesian, String mention) {
        KorathCipher results = Utils.applyKorathCipher(indonesian);
        StringBuffer buffer = new StringBuffer();
        buffer.append("## Indonesian\n");
        if(mention != null && mention.length() > 0)
            buffer.append(mention).append(" said:\n");
        buffer.append(results.getIndonesian())
              .append("\n## Exile\n").append(results.getExile())
              .append("\n## Efreti\n").append(results.getEfreti());
        if(buffer.length() > MAX_RESPONSE_LENGTH)
            buffer.delete(MAX_RESPONSE_LENGTH - 3, buffer.length()).append("...").trimToSize();
        return buffer.toString();
    }
}
