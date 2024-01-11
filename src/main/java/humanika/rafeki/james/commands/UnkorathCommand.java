package humanika.rafeki.james.commands;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import humanika.rafeki.james.James;
import humanika.rafeki.james.Utils;
import humanika.rafeki.james.utils.KorathCipher;
import java.util.Optional;
import reactor.core.publisher.Mono;

public class UnkorathCommand extends PrimitiveCommand {
    private static String TITLE = "Korath Reverse Cipher";

    @Override
    public String getName() {
        return "unkorath";
    }

    @Override
    public Mono<Void> handleChatCommand() {
        String text = data.getStringOrDefault("text", "").replaceAll("[@\t\n *<>|]+", " ").strip();
        boolean ephemeral = data.isEphemeral();
        boolean efreti = data.getStringOrDefault("language", "efreti").equals("efreti");

        EmbedCreateSpec creator = EmbedCreateSpec.create().withTitle(TITLE);

        String mention = ephemeral ? null : getChatEvent().getInteraction().getUser().getMention();
        if(mention != null && mention.length() > 0)
            creator = creator.withDescription(mention + " said:\n");

        if(efreti) {
            KorathCipher results = Utils.reverseEfretiCipher(text);
            creator = creator.withFields(
                     EmbedCreateFields.Field.of("Efreti", results.getEfreti(), true),
                     EmbedCreateFields.Field.of("Cipher", results.getIndonesian(), true));
        } else {
            KorathCipher results = Utils.reverseExileCipher(text);
            creator = creator.withFields(
                     EmbedCreateFields.Field.of("Exile", results.getExile(), true),
                     EmbedCreateFields.Field.of("Cipher", results.getIndonesian(), true));
        }

        return getChatEvent().reply().withEmbeds(creator).withEphemeral(ephemeral);
    }
}
