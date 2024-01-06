package humanika.rafeki.james.commands;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.ActionRow;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.spec.EmbedCreateSpec;
import humanika.rafeki.james.James;
import humanika.rafeki.james.data.NodeInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import reactor.core.publisher.Mono;
import discord4j.core.spec.EmbedCreateFields;
import me.mcofficer.esparser.DataNode;
import discord4j.core.object.Embed;
import discord4j.core.object.entity.Message;
import java.util.Arrays;
import java.util.function.BooleanSupplier;

import humanika.rafeki.james.utils.AddParagraphFields;

public class LookupCommand extends NodeInfoCommand {
    @Override
    public String getName() {
        return "lookup";
    }

    @Override
    protected Mono<Void> generateResult(List<NodeInfo> found, boolean ephemeral, PrimitiveSlashSubcommand subcommand) {
        NodeInfo chosen = found.get(found.size() - 1);
        List<EmbedCreateSpec> embeds = new ArrayList<>();
        StringBuilder builder = new StringBuilder(100);
        String before = James.getConfig().endlessSkyData;
        String after = James.getConfig().endlessSkyDataQuery;
        AddParagraphFields fieldAdder = new AddParagraphFields(4, false);

        NodeInfo info = found.get(found.size() - 1);

        List<EmbedCreateFields.Field> fields = new ArrayList<>();

        Optional<List<DataNode>> descriptions = info.getDescription();
        if(descriptions.isPresent())
            fieldAdder.add("Description", fields, descriptions.get(), MAX_CHARS_PER_FIELD);

        fieldAdder.setMaxFields(5);
        Optional<List<DataNode>> spaceport = info.getSpaceport();
        if(spaceport.isPresent())
            fieldAdder.add("Spaceport", fields, spaceport.get(), MAX_CHARS_PER_FIELD);

        if(fields.size() < 1)
            fields.add(EmbedCreateFields.Field.of("Description", "*no description*", false));

        EmbedCreateSpec embed = EmbedCreateSpec.create().withFields(fields)
            .withTitle(info.getBestType() + ' ' + info.getName());

        String[] imageAndThumbnail = getImageAndThumbnail(info, false);
        if(imageAndThumbnail[0] != null)
            embed = embed.withImage(imageAndThumbnail[0]);
        if(imageAndThumbnail[1] != null)
            embed = embed.withThumbnail(imageAndThumbnail[1]);
        embeds.add(embed);

        return buttonEvent.getReply().flatMap(reply -> buttonEvent.editReply().withEmbeds(embeds).withComponents()).then();
    }

    protected Optional<List<NodeInfo>> getMatches(String query, Optional<String> maybeType) {
        if(maybeType.isPresent()) {
            final String type = maybeType.get();
            if(type.equals("variant"))
                return James.getState().fuzzyMatchNodeNames(query, QUERY_COUNT,
                    info -> info.isShipVariant());
            else
                return James.getState().fuzzyMatchNodeNames(query, QUERY_COUNT,
                    info -> info.getType().equals(type) && !info.isShipVariant()
                        && (info.hasDescription() || info.hasSpaceport() || info.hasImage()) );
        } else
            return James.getState().fuzzyMatchNodeNames(query, QUERY_COUNT,
                info -> !info.isShipVariant() && (info.hasDescription()
                        || info.hasSpaceport() || info.hasImage()) );
    }
}
