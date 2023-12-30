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

public class ShowCommand extends SlashCommand {
    protected final static int QUERY_COUNT = 14;
    protected final static int PRIMARY_COUNT = 6;
    protected final static int MAX_CHARS_PER_FIELD = 1000;

    @Override
    public String getName() {
        return "show";
    }

    @Override
    public Mono<Void> handleChatCommand(ChatInputInteractionEvent event) {
        if(!event.getInteraction().getGuildId().isPresent())
            return handleDirectMessage(event);

        Optional<String> maybeQuery = getString(event, "query");
        if(!maybeQuery.isPresent())
            return event.reply().withContent("Provide a query for the search!").withEphemeral(true);

        String query = maybeQuery.get().replace("\\s+", " ").strip().toLowerCase();
        if(query.length() < 1)
            return event.reply().withContent("Provide a query for the search!").withEphemeral(true);

        EmbedCreateSpec embed = EmbedCreateSpec.create().withTitle("Matches for \"" + query + '"');

        Optional<List<NodeInfo>> maybeMatches = getMatches(query);
        if(!maybeMatches.isPresent())
            return event.reply().withEmbeds(embed).withEphemeral(isEphemeral(event));

        List<NodeInfo> matches = maybeMatches.get();
        if(matches.size() < 1)
            return event.reply().withEmbeds(embed).withEphemeral(isEphemeral(event));

        boolean ephemeral = isEphemeral(event);

        List<String> listItem = new ArrayList<>();
        List<String> buttonText = new ArrayList<>();
        List<String> buttonId = new ArrayList<>();
        getResponse(matches, listItem, buttonText, buttonId, ephemeral);
        if(listItem.size() < 1)
            return event.reply().withEmbeds(embed).withEphemeral(isEphemeral(event));

        int count = QUERY_COUNT;
        int width = 3;
        int height = (count + 1 + 2) / width;
        ActionRow rows[] = new ActionRow[height];
        Button buttons[] = new Button[width];
        for(int i = 0; i <= count; i++) {
            if(i == count) {
                buttons[i % width] = Button.success(getName() + ":X:close:close", "close");
                rows[i / width] = ActionRow.of(Arrays.copyOfRange(buttons, 0, (i % width) + 1));
            } else {
                if(i < PRIMARY_COUNT)
                    buttons[i % width] = Button.primary(buttonId.get(i), listItem.get(i));
                else
                    buttons[i % width] = Button.secondary(buttonId.get(i), listItem.get(i));
                if((i + 1) % width == 0)
                    rows[i / width] = ActionRow.of(buttons);
            }
        }

        return event.reply().withContent("## Search Results\nFor `" + query.replaceAll("`", "'") + '`')
            .withEphemeral(ephemeral).withComponents(rows);
    }

    public Mono<Void> handleButtonInteraction(ButtonInteractionEvent event) {
        String[] split = event.getCustomId().split(":", 4);
        if(split.length < 4) {
            event.editReply().withContent("Something got mixed up! The button had an invalid id.");
            return Mono.empty();
        }

        event.deferEdit().block();

        String type = split[0];
        String flags = split[1];
        String hash = split[2];
        String query = split[3];
        boolean ephemeral = flags.indexOf('E') >= 0;
        if(hash.equals("close"))
            event.deleteReply().subscribe();
        else if(split[0].equals(getName())) {
            Optional<List<NodeInfo>> found = James.getState().nodesWithHash(hash);
            if(found.isPresent() && found.get().size() > 0)
                generateResult(event, found.get(), ephemeral);
            else {
                event.editReply()
                    .withEmbeds(EmbedCreateSpec.create().withTitle("No Match")
                        .withDescription("Query beginning with \"" + query
                                       + "\" comes from an out-of-date search. Please try again."))
                    .subscribe();
            }
        } else {
            event.editReply().withContent("Something got mixed up! The button had an invalid id.").subscribe();
        }
        return Mono.empty();
    }

    protected void generateResult(ButtonInteractionEvent event, List<NodeInfo> found, boolean ephemeral) {
        List<EmbedCreateSpec> embeds = new ArrayList<>();
        StringBuilder builder = new StringBuilder(100);
        String before = James.getConfig().endlessSkyData;
        String after = James.getConfig().endlessSkyDataQuery;
        AddParagraphFields fieldAdder = new AddParagraphFields(4);
        for(NodeInfo info : found) {
            List<EmbedCreateFields.Field> fields = new ArrayList<>();

            Optional<List<DataNode>> descriptions = info.getDescription();
            if(descriptions.isPresent())
                fieldAdder.add("Description", fields, descriptions.get());

            fieldAdder.setMaxFields(5);
            Optional<List<DataNode>> spaceport = info.getSpaceport();
            if(spaceport.isPresent())
                fieldAdder.add("Spaceport", fields, spaceport.get());

            if(fields.size() < 1)
                fields.add(EmbedCreateFields.Field.of("Description", "*no description*", false));

            EmbedCreateSpec embed = EmbedCreateSpec.create().withFields(fields)
                .withTitle(info.getBestType() + ' ' + info.getName());

            String[] imageAndThumbnail = getImageAndThumbnail(info);
            if(imageAndThumbnail[0] != null)
                embed = embed.withImage(imageAndThumbnail[0]);
            if(imageAndThumbnail[1] != null)
                embed = embed.withThumbnail(imageAndThumbnail[1]);
            embeds.add(embed);
        }
        Mono<Message> newReply = event.getReply().flatMap(reply -> event.editReply().withEmbeds(embeds));
        newReply.block();
    }

    protected String[] getImageAndThumbnail(NodeInfo info) {
        Optional<String> maybeThumbnail = info.getBestThumbnail();
        Optional<String> maybeImage = info.getBestImage();
        String image = null;
        String thumbnail = null;
        if(maybeImage.isPresent())
            image = maybeImage.get();
        if(maybeThumbnail.isPresent())
            thumbnail = maybeThumbnail.get();
        if(image == thumbnail)
            thumbnail = null;

        if(image != null) {
            maybeImage = James.getState().getImageRawUrl(image);
            image = maybeImage.orElse(null);
        }
        if(thumbnail != null) {
            maybeThumbnail = James.getState().getImageRawUrl(thumbnail);
            thumbnail = maybeThumbnail.orElse(null);
        }

        if(image == null && thumbnail != null) {
            image = thumbnail;
            thumbnail = null;
        }

        String[] result = { image, thumbnail };
        return result;
    }

    protected Optional<List<NodeInfo>> getMatches(String query) {
        return James.getState().fuzzyMatchNodeNames(query, QUERY_COUNT, info -> !info.isShipVariant() && (info.hasDescription() || info.hasSpaceport() || info.hasImage()) );
    }

    protected class AddParagraphFields {
        private int fieldsSent = 0;
        private int paragraphsInField = 0;
        private StringBuilder builder = new StringBuilder(100);
        private int maxFields;

        public AddParagraphFields(int maxFields) {
            this.maxFields = maxFields;
        }

        public void setMaxFields(int maxFields) {
            this.maxFields = maxFields;
        }

        public int getMaxFields() {
            return maxFields;
        }

        public void add(String title, List<EmbedCreateFields.Field> fields, List<DataNode> paragraphs) {
            paragraphsInField = 0;
            builder.delete(0, builder.length());

            for(DataNode node : paragraphs) {
                if(node.size() < 1)
                    continue;
                
                String paragraph = node.token(1);
                int maxFieldSize = MAX_CHARS_PER_FIELD;
                if(fieldsSent == 0)
                    maxFieldSize -= title.length() + 1;
                
                if(builder.length() + 1 + paragraph.length() > maxFieldSize) {
                    if(!addField(title, fields))
                        break;
                    if(paragraph.length() > maxFieldSize) {
                        builder.append(paragraph.substring(0, maxFieldSize - 3)).append("...");
                        if(!addField(title, fields))
                            break;
                        continue;
                    }
                }
                if(paragraphsInField > 0)
                    builder.append('\n');
                builder.append(paragraph);
                paragraphsInField++;
                if(builder.length() > 0 && fieldsSent < maxFields)
                    addField(title, fields);
            }
        }

        private boolean addField(String title, List<EmbedCreateFields.Field> fields) {
            if(fieldsSent == 0)
                fields.add(EmbedCreateFields.Field.of(title, builder.toString(), false));
            else
                fields.add(EmbedCreateFields.Field.of("", builder.toString(), false));
            fieldsSent++;
            paragraphsInField = 0;
            builder.delete(0, builder.length());
            return fieldsSent < maxFields;
        }
    }

    protected void getResponse(List<NodeInfo> matches, List<String> listItem, List<String> buttonText, List<String> buttonId, boolean ephemeral) {
        StringBuilder builder = new StringBuilder(100);
        int i = 0;
        for(NodeInfo node : matches) {
            i++;
            if(i > 1)
                builder.delete(0, builder.length());

            builder.append(node.getBestType()).append(' ').append(node.getName());
            String built = builder.toString();
            listItem.add(built);

            buttonText.add(String.valueOf(i));

            builder.delete(0, builder.length());
            builder.append(ephemeral ? getName() + ":E:" : getName() + ":-:");
            builder.append(node.getHashString()).append(":").append(built);
            if(builder.length() > 95)
                builder.delete(95, builder.length());
            buttonId.add(builder.toString());
        }
    }
}
