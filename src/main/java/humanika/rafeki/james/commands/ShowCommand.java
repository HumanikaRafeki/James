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

public class ShowCommand extends SlashCommand {
    private static String[] emojiNumbers = { ":one:", ":two:", ":three:", ":four:", ":five:",
                                             ":six:", ":seven:", ":eight:", ":nine:", ":keycap_ten:" };

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

        StringBuilder content = new StringBuilder();
        for(int i = 0; i < 10 ; i++) {
            if(i != 0)
                content.append('\n');
            content.append(emojiNumbers[i]).append(' ').append(listItem.get(i));
        }
        embed = embed.withDescription(content.toString());

        ActionRow rows[] = new ActionRow[3];
        Button buttons[] = new Button[4];
        for(int i = 0; i <= 10; i++) {
            if(i == 10) {
                buttons[i % 4] = Button.success(getName() + ":X:close:close", "close");
                rows[i / 4] = ActionRow.of(Arrays.copyOfRange(buttons, 0, 3));
            } else {
                buttons[i % 4] = Button.primary(buttonId.get(i), String.valueOf(i + 1));
                if(i == 3 || i == 7)
                    rows[i / 4] = ActionRow.of(buttons);
            }
        }

        return event.reply().withEphemeral(ephemeral).withEmbeds(embed).withComponents(rows);
    }

    public Mono<Void> handleButtonInteraction(ButtonInteractionEvent event) {
        String[] split = event.getCustomId().split(":", 4);
        if(split.length < 4) {
            event.editReply().withContent("Something got mixed up! The button had an invalid id.");
            return Mono.empty();
        }

        System.out.println("Defer edit...");
        event.deferEdit().block();
        System.out.println("Deferred.");

        String type = split[0];
        String flags = split[1];
        String hash = split[2];
        String query = split[3];
        boolean ephemeral = flags.indexOf('E') >= 0;
        System.out.println("type=\""+type+"\" flags=\""+flags+"\" hash=\""+hash+"\" query=\""+query+'"');
        if(hash.equals("close"))
            event.deleteReply().subscribe();
        else if(split[0].equals(getName())) {
            Optional<List<NodeInfo>> found = James.getState().nodesWithHash(hash);
            if(found.isPresent() && found.get().size() > 0)
                generateResult(event, found.get(), ephemeral);
            else {
                System.out.println("No match for hash \""+hash+'"');
                event.editReply()
                    .withEmbeds(EmbedCreateSpec.create().withTitle("No Match")
                        .withDescription("Query beginning with \"" + query
                                       + "\" comes from an out-of-date search. Please try again."))
                    .subscribe();
            }
        } else {
            System.out.println("Name \""+split[0]+"\" != \""+getName()+'"');
            event.editReply().withContent("Something got mixed up! The button had an invalid id.").subscribe();
        }
        return Mono.empty();
    }

    protected void generateResult(ButtonInteractionEvent event, List<NodeInfo> found, boolean ephemeral) {
        List<EmbedCreateSpec> embeds = new ArrayList<>();
        StringBuilder builder = new StringBuilder(100);
        String before = James.getConfig().endlessSkyData;
        String after = James.getConfig().endlessSkyDataQuery;
        for(NodeInfo info : found) {
            List<EmbedCreateFields.Field> fields = new ArrayList<>();

            Optional<List<DataNode>> descriptions = info.getDescription();
            if(descriptions.isPresent()) {
                builder.delete(0, builder.length());
                for(DataNode node : descriptions.get())
                    if(node.size() > 1) {
                        if(builder.length() > 0)
                            builder.append('\n');
                        builder.append(node.token(1));
                    }
                fields.add(EmbedCreateFields.Field.of("Description", builder.toString(), false));
            }

            Optional<List<DataNode>> spaceport = info.getSpaceport();
            if(spaceport.isPresent()) {
                builder.delete(0, builder.length());
                for(DataNode node : spaceport.get())
                    if(node.size() > 1) {
                        if(builder.length() > 0)
                            builder.append('\n');
                        builder.append(node.token(1));
                    }
                fields.add(EmbedCreateFields.Field.of("Spaceport", builder.toString(), false));
            }

            if(fields.size() < 1)
                fields.add(EmbedCreateFields.Field.of("Description", "*no description*", false));

            Optional<String> maybeThumbnail = info.getBestThumbnail();
            Optional<String> maybeImage = info.getBestImage();
            System.out.println("maybeThumbnail="+maybeThumbnail);
            System.out.println("maybeImage="+maybeImage);
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
                if(!maybeImage.isPresent())
                    System.out.println("Image " + image + " doesn't exist.");
                image = maybeImage.orElse(null);
            }
            if(thumbnail != null) {
                maybeThumbnail = James.getState().getImageRawUrl(thumbnail);
                if(!maybeThumbnail.isPresent())
                    System.out.println("Thumbnail " + image + " doesn't exist.");
                thumbnail = maybeThumbnail.orElse(null);
            }

            if(image == null && thumbnail != null) {
                image = thumbnail;
                thumbnail = null;
            }

            EmbedCreateSpec embed = EmbedCreateSpec.create().withFields(fields)
                .withTitle(info.getBestType() + ' ' + info.getName());
            if(image != null) {
                System.out.println("image "+image);
                embed = embed.withImage(image);
            }
            if(thumbnail != null) {
                System.out.println("thumbnail "+thumbnail);
                embed = embed.withThumbnail(thumbnail);
            }
            embeds.add(embed);
        }
        Mono<Message> newReply = event.getReply().flatMap(reply -> event.editReply()
            .withEmbeds(adjustEmbeds(reply.getEmbeds(), embeds)));
        newReply.block();
    }

    protected List<EmbedCreateSpec> adjustEmbeds(List<Embed> old, List<EmbedCreateSpec> embeds) {
        if(old != null && old.size() > 0) {
            EmbedCreateSpec keep = EmbedCreateSpec.builder().from(old.get(0).getData()).build();
            embeds.add(0, keep);
        }
        return embeds;
    }

    protected Optional<List<NodeInfo>> getMatches(String query) {
        return James.getState().fuzzyMatchNodeNames(query, 10);
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
