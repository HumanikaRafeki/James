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

public abstract class NodeInfoCommand extends SlashCommand {
    protected final static int QUERY_COUNT = 14;
    protected final static int PRIMARY_COUNT = 6;
    protected final static int MAX_CHARS_PER_FIELD = 1000;
    protected final static int MAX_BUTTON_LABEL_LENGTH = 60;

    @Override
    public String getName() {
        return "show";
    }

    @Override
    public Mono<Void> handleChatCommand() {
        if(!event.getInteraction().getGuildId().isPresent())
            return handleDirectMessage();

        boolean ephemeral = isEphemeral();

        Optional<String> maybeType = getType();
        Optional<String> maybeQuery = getQuery();
        if(!maybeQuery.isPresent())
            return event.reply().withContent("Provide a query for the search!").withEphemeral(true);

        String query = maybeQuery.get().replace("\\s+", " ").strip().toLowerCase();
        if(query.length() < 1)
            return event.reply().withContent("Provide a query for the search!").withEphemeral(true);

        StringBuilder builder = new StringBuilder();

        builder.append("## Search Results\nFor `");
        builder.append(query.replaceAll("`", "'")).append('`');
        if(maybeType.isPresent())
            builder.append(" of type `").append(maybeType.get().replaceAll("`", "'")).append('`');

        Optional<List<NodeInfo>> maybeMatches = getMatches(query, maybeType);
        if(!maybeMatches.isPresent()) {
            builder.append("\n*No matches.*");
            return event.reply().withContent(builder.toString()).withEphemeral(ephemeral);
        }

        List<NodeInfo> matches = maybeMatches.get();
        if(matches.size() < 1) {
            builder.append("\n*No matches.*");
            return event.reply().withContent(builder.toString()).withEphemeral(ephemeral);
        }

        List<String> listItem = new ArrayList<>();
        List<String> buttonText = new ArrayList<>();
        List<String> buttonId = new ArrayList<>();
        getResponse(matches, listItem, buttonText, buttonId, ephemeral);
        if(listItem.size() < 1) {
            builder.append("\n*No matches.*");
            return event.reply().withContent(builder.toString()).withEphemeral(ephemeral);
        }

        int count = QUERY_COUNT;
        if(count > listItem.size())
            count = listItem.size();
        int width = 3;
        int height = (count + 1 + 1) / width;
        ActionRow rows[] = new ActionRow[height];
        Button buttons[] = new Button[width];
        for(int i = 0; i <= count; i++) {
            if(i == count)
                buttons[i % width] = Button.success(getActiveSubcommandPath() + ":X:close:close", "close");
            else {
                System.out.println("Button "+i+" id = "+buttonId.get(i));
                String label = listItem.get(i);
                if(label.length() > MAX_BUTTON_LABEL_LENGTH) {
                    label = label.substring(0, MAX_BUTTON_LABEL_LENGTH - 3) + "...";
                }
                if(i < PRIMARY_COUNT)
                    buttons[i % width] = Button.primary(buttonId.get(i), label);
                else
                    buttons[i % width] = Button.secondary(buttonId.get(i), label);
            }
            if((i + 1) % width == 0)
                rows[i / width] = ActionRow.of(buttons);
        }

        return event.reply().withContent(builder.toString())
                 .withEphemeral(ephemeral).withComponents(rows);
    }

    public Mono<Void> handleButtonInteraction() {
        String[] split = buttonEvent.getCustomId().split(":", 4);
        if(split.length < 4) {
            buttonEvent.editReply().withContent("Something got mixed up! The button had an invalid id.");
            return Mono.empty();
        }

        String type = split[0];
        String flags = split[1];
        String hash = split[2];
        String query = split[3];
        boolean ephemeral = flags.indexOf('E') >= 0;
        if(hash.equals("delete") || hash.equals("close"))
            buttonEvent.deleteReply().subscribe();
        else if(hash.equals("done"))
            buttonEvent.editReply().withComponents().subscribe();
        else if(split[0].equals(getName())) {
            Optional<List<NodeInfo>> found = James.getState().nodesWithHash(hash);
            if(found.isPresent() && found.get().size() > 0)
                return generateResult(found.get(), ephemeral, null);
            else {
                return buttonEvent.editReply()
                    .withEmbeds(EmbedCreateSpec.create().withTitle("No Match")
                        .withDescription("Query beginning with \"" + query
                                       + "\" comes from an out-of-date search. Please try again."))
                    .then();
            }
        } else
            return buttonEvent.editReply().withEmbeds(EmbedCreateSpec.create().withTitle("Error")
                 .withDescription("Something got mixed up! The button had an invalid id.")).then();
        return Mono.empty();
    }

    protected abstract Mono<Void> generateResult(List<NodeInfo> found, boolean ephemeral, SlashSubcommand subcommand);

    protected abstract Optional<List<NodeInfo>> getMatches(String query, Optional<String> type);

    protected Optional<String> getType() {
        return getString("type");
    }

    protected Optional<String> getQuery() {
        return getString("query");
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
