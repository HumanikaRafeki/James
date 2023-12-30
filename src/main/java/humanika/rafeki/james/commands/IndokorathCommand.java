package humanika.rafeki.james.commands;

import java.util.Optional;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.User;
import reactor.core.publisher.Mono;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.EmbedCreateFields;

import humanika.rafeki.james.James;
import humanika.rafeki.james.Utils;
import humanika.rafeki.james.utils.KorathCipher;

public class IndokorathCommand extends SlashCommand {
    private static final int MAX_RESPONSE_LENGTH = 4000;
    private static String FOOTER_MESSAGE = "This tool only aids translation. You must massage the words for readability. Sometimes the cipher will produce obscene or offensive terms. Words with standard translations, like human/Humani, won't be correct.";
    private static String TITLE = "Korath Cipher";

    @Override
    public String getName() {
        return "indokorath";
    }

    @Override
    public Mono<Void> handleChatCommand(ChatInputInteractionEvent event) {
        if(!event.getInteraction().getGuildId().isPresent())
            return handleDirectMessage(event);
        String text = getStringOrDefault(event, "text", "").replaceAll("[@\t\n *<>|]+", " ").strip();
        boolean ephemeral = isEphemeral(event);

        EmbedCreateSpec creator = EmbedCreateSpec.create().withTitle(TITLE)
            .withFooter(EmbedCreateFields.Footer.of(FOOTER_MESSAGE, null));

        String mention = ephemeral ? null : event.getInteraction().getUser().getMention();
        if(mention != null && mention.length() > 0)
            creator = creator.withDescription(mention + " said:\n");

        KorathCipher results = Utils.applyKorathCipher(text);

        creator = creator.withFields(
                     EmbedCreateFields.Field.of("Indonesian", results.getIndonesian(), true),
                     EmbedCreateFields.Field.of("Exile", results.getExile(), true),
                     EmbedCreateFields.Field.of("Efreti", results.getEfreti(), true));

        return event.reply().withEmbeds(creator).withEphemeral(ephemeral);
    }
}
