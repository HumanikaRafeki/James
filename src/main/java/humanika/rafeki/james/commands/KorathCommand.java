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
import java.io.IOException;
import java.util.Optional;
import reactor.core.publisher.Mono;

public class KorathCommand extends PrimitiveSlashCommand {
    private static final int MAX_RESPONSE_LENGTH = 4000;
    private static String FOOTER_MESSAGE = "This tool only aids translation. You must massage the words for readability. Sometimes the cipher will produce obscene or offensive terms. Words with standard translations, like human/Humani, won't be correct.";
    private static String TITLE = "Korath Encoding";

    @Override
    public String getName() {
        return "korath";
    }

    @Override
    public Mono<Void> handleChatCommand() {
        String text = data.getStringOrDefault("text", "").replaceAll("[@\t\n *<>|]+", " ").strip();
        boolean ephemeral = data.isEphemeral();

        EmbedCreateSpec creator = EmbedCreateSpec.create().withTitle(TITLE)
            .withFooter(EmbedCreateFields.Footer.of(FOOTER_MESSAGE, null));

        String mention = ephemeral ? null : getChatEvent().getInteraction().getUser().getMention();
        if(mention != null && mention.length() > 0)
            creator = creator.withDescription(mention + " said:\n");

        try {
            KorathCipher results = Utils.translateToKorath(text);
            creator = creator.withFields(
                     EmbedCreateFields.Field.of("English", results.getEnglish(), true),
                     EmbedCreateFields.Field.of("Indonesian", results.getIndonesian(), true),
                     EmbedCreateFields.Field.of("Exile", results.getExile(), true),
                     EmbedCreateFields.Field.of("Efreti", results.getEfreti(), true));
        } catch(IOException ioe) {
            creator = creator.withFields(
                     EmbedCreateFields.Field.of("English", text, false),
                     EmbedCreateFields.Field.of("Error", "Network error contacting translation server.", false));
        }

        return getChatEvent().reply().withEmbeds(creator).withEphemeral(ephemeral);
    }
}
