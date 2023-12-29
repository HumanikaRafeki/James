package humanika.rafeki.james.commands;

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

public class ShowCommand extends SlashCommand {
    @Override
    public String getName() {
        return "show";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
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

        List<String> listItem = new ArrayList<>();
        List<String> buttonText = new ArrayList<>();
        List<String> buttonId = new ArrayList<>();
        getResponse(matches, listItem, buttonText, buttonId);
        if(listItem.size() < 1)
            return event.reply().withEmbeds(embed).withEphemeral(isEphemeral(event));

        StringBuilder content = new StringBuilder();
        for(int i = 0; i < listItem.size(); i++) {
            if(i != 0)
                content.append('\n');
            content.append("- ").append(buttonText.get(i)).append(' ').append(listItem.get(i))
                .append("\n    - ").append("Button: ").append(buttonText.get(i))
                .append("\n    - ").append("Data: ").append(buttonId.get(i));
        }
        embed.withDescription(content.toString());
        return event.reply().withEphemeral(isEphemeral(event)).withEmbeds(embed);
    }

    protected Optional<List<NodeInfo>> getMatches(String query) {
        return James.getState().fuzzyMatchNodeNames(query, 10);
    }

    protected void getResponse(List<NodeInfo> matches, List<String> listItem, List<String> buttonText, List<String> buttonId) {
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
            builder.append("lookup:").append(node.getHashString()).append(built);
            buttonId.add(builder.toString());
        }
    }
}
