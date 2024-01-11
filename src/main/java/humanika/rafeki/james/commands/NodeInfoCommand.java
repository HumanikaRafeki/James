package humanika.rafeki.james.commands;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.Embed;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.entity.Message;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.EmbedCreateSpec;
import humanika.rafeki.james.James;
import humanika.rafeki.james.data.NodeInfo;
import humanika.rafeki.james.data.SearchResult;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BooleanSupplier;
import me.mcofficer.esparser.DataNode;
import reactor.core.publisher.Mono;

public abstract class NodeInfoCommand extends PrimitiveCommand {
    protected final static int DISPLAY_COUNT = 14;
    protected final static int QUERY_COUNT = DISPLAY_COUNT + 5;
    protected final static int PRIMARY_COUNT = 6;
    protected final static int MAX_CHARS_PER_FIELD = 1000;
    protected final static int MAX_BUTTON_LABEL_LENGTH = 60;

    @Override
    public String getName() {
        return "show";
    }

    public void describeSearch(StringBuilder builder, Optional<String> maybeType, String query) {
        builder.append("## Search Results\nFor `");
        builder.append(query.replaceAll("`", "'")).append('`');
        if(maybeType.isPresent())
            builder.append(" of type `").append(maybeType.get().replaceAll("`", "'")).append('`');
    }

    public void getButtonFlags(StringBuilder builder) {
        if(data.isEphemeral())
            builder.append('E');
    }

    @Override
    public Mono<Void> handleChatCommand() {
        Optional<InteractionEventHandler> subcommand = findSubcommand();
        boolean ephemeral;
        if(subcommand.isPresent()) {
            if(!(subcommand.get() instanceof NodeInfoSubcommand))
                return subcommand.get().handleChatCommand();
            ephemeral = subcommand.get().getData().isEphemeral();
        } else
            ephemeral = data.isEphemeral();
        Optional<String> maybeType = getType();
        Optional<String> maybeQuery = getQuery();
        if(!maybeQuery.isPresent())
            return getChatEvent().reply().withContent("Provide a query for the search!").withEphemeral(true);

        String query = maybeQuery.get().toLowerCase().replaceAll("[^0-9a-zA-Z-]+", " ").strip();
        if(query.length() < 1)
            return getChatEvent().reply().withContent("Provide a query for the search!").withEphemeral(true);

        StringBuilder builder = new StringBuilder();
        describeSearch(builder, maybeType, query);

        List<SearchResult> matches = getMatches(query, maybeType);
        if(matches.size() < 1) {
            builder.append("\n*No matches.*");
            return getChatEvent().reply().withContent(builder.toString()).withEphemeral(ephemeral);
        }

        List<String> listItem = new ArrayList<>();
        List<String> buttonId = new ArrayList<>();
        getResponse(matches, listItem, buttonId, ephemeral, subcommand);
        if(listItem.size() < 1) {
            builder.append("\n*No matches.*");
            return getChatEvent().reply().withContent(builder.toString()).withEphemeral(ephemeral);
        }

        int count = DISPLAY_COUNT;
        if(count > listItem.size())
            count = listItem.size();
        int width = 3;
        int height = (count + 1 + 1) / width;
        List<ActionRow> rows = new ArrayList<>();
        List<Button> buttons = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for(int i = 0; seen.size() < count && i < buttonId.size(); i++) {
            if(seen.contains(buttonId.get(i)))
                continue;

            seen.add(buttonId.get(i));
                
            String label = listItem.get(i);
            if(label.length() > MAX_BUTTON_LABEL_LENGTH)
                label = label.substring(0, MAX_BUTTON_LABEL_LENGTH - 3) + "...";
            if(seen.size() < PRIMARY_COUNT)
                buttons.add(Button.primary(buttonId.get(i), label));
            else
                buttons.add(Button.secondary(buttonId.get(i), label));
                
            if(buttons.size() >= width) {
                rows.add(ActionRow.of(buttons));
                buttons.clear();
            }
        }
        buttons.add(Button.success(getFullName() + ":X:close:close", "close"));
        if(buttons.size() > 0)
            rows.add(ActionRow.of(buttons));

        return getChatEvent().reply().withContent(builder.toString())
                 .withEphemeral(ephemeral).withComponents(rows);
    }

    @Override
    public Mono<Void> handleButtonInteraction() {
        String[] split = getButtonEvent().getCustomId().split(":", 4);
        if(split.length < 4) {
            getButtonEvent().editReply().withContent("Something got mixed up! The button had an invalid id.");
            return Mono.empty();
        }

        String type = split[0];
        String[] names = split[0].split(" ");
        String[] expect = getFullName().split(" ");
        boolean wrongCommand = names.length < expect.length;
        if(!wrongCommand) {
            for(int i = 0; i < expect.length; i++)
                if(!expect[i].equals(names[i])) {
                    wrongCommand = true;
                    break;
                }
        }

        String flags = split[1];
        String hash = split[2];
        String query = split[3];
        boolean ephemeral = flags.indexOf('E') >= 0;
        if(hash.equals("delete") || hash.equals("close"))
            getButtonEvent().deleteReply().subscribe();
        else if(hash.equals("done"))
            getButtonEvent().editReply().withComponents().subscribe();
        else if(!wrongCommand) {
            Optional<InteractionEventHandler> subcommand = subcommandFor(names);
            Optional<SearchResult> found = James.getState().dummyResultWithHash(hash);
            if(found.isPresent()) {
                return generateResult(found.get(), ephemeral, (PrimitiveCommand)subcommand.orElse(null));
            } else {
                return getButtonEvent().editReply()
                    .withEmbeds(EmbedCreateSpec.create().withTitle("No Match")
                        .withDescription("Query beginning with \"" + query
                                       + "\" comes from an out-of-date search. Please try again."))
                    .then();
            }
        } else
            return getButtonEvent().editReply().withEmbeds(EmbedCreateSpec.create().withTitle("Error")
                 .withDescription("Something got mixed up! The button had an invalid id.")).then();
        return Mono.empty();
    }

    protected abstract Mono<Void> generateResult(SearchResult found, boolean ephemeral, PrimitiveCommand subcommand);

    protected abstract List<SearchResult> getMatches(String query, Optional<String> type);

    protected Optional<String> getType() {
        return data.getString("type");
    }

    protected Optional<String> getQuery() {
        return data.getString("query");
    }

    protected String[] getImageAndThumbnail(NodeInfo info, boolean canRecurse) {
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

        if(image == null && thumbnail == null) {
            Iterator<String> iter = info.getImageIterator();
            if(iter.hasNext())
                image = iter.next();
        } else if(info.getType().equals("ship")) {
            // Ships look better with their shipyard image ("thumbnail" in data files) as the big one and top view as the thumbnail
            String swap = image;
            image = thumbnail;
            thumbnail = swap;
        }

        String[] result = { image, thumbnail };
        return result;
    }

    protected void getResponse(List<SearchResult> matches, List<String> listItem, List<String> buttonId, boolean ephemeral, Optional<InteractionEventHandler> subcommand) {
        StringBuilder builder = new StringBuilder(100);
        String buttonDataName = null;

        if(subcommand.isPresent() && subcommand.get() instanceof NodeInfoSubcommand)
            buttonDataName = ((NodeInfoSubcommand)subcommand.get()).getButtonDataName();
        else
            buttonDataName = getFullName();

        int i = 0;
        for(SearchResult result : matches) {
            i++;
            if(i > 1)
                builder.delete(0, builder.length());

            builder.append(result.getBestType()).append(' ').append(result.getName());
            String built = builder.toString();
            listItem.add(built);

            builder.delete(0, builder.length());
            builder.append(buttonDataName).append(':');
            getButtonFlags(builder);
            builder.append(":");
            builder.append(result.getHashString()).append(":").append(built);
            if(builder.length() > 95)
                builder.delete(95, builder.length());
            buttonId.add(builder.toString());
        }
    }

    public Optional<InteractionEventHandler> findSubcommand() {
        return Optional.empty();
    }

    protected Optional<InteractionEventHandler> subcommandFor(String[] names) {
        return Optional.empty();
    }
}
