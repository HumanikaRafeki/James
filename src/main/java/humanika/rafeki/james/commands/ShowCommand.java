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
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import me.mcofficer.esparser.DataNode;
import reactor.core.publisher.Mono;

public class ShowCommand extends NodeInfoCommand {

    private ShowSubcommand subcommand = null;
    private boolean findImages = true;
    private boolean findData = true;

    protected Optional<String> getType() {
        return subcommand.getData().getString("type");
    }

    protected Optional<String> getQuery() {
        return subcommand.getData().getString("query");
    }

    @Override
    public String getFullName() {
        if(subcommand == null)
            return "show";
        else
            return subcommand.getFullName();
    }

    @Override
    protected Mono<Void> generateResult(SearchResult found, boolean ephemeral, PrimitiveCommand subcommand) {
        return( ((ShowSubcommand)subcommand).generateResult(found, ephemeral) );
    }

    @Override
    protected List<SearchResult> getMatches(String query, Optional<String> maybeType) {
        if(maybeType.isPresent()) {
            final String type = maybeType.get();
            if(type.equals("image")) {
                if(!findImages)
                    return James.getState().fuzzyMatchNodeNames(query, QUERY_COUNT,
                        info -> info.isShipVariant() && info.hasImage());
                else
                    return James.getState().fuzzyMatchImagePaths(query, QUERY_COUNT, name -> true);
            } else if(type.equals("variant"))
                return James.getState().fuzzyMatchNodeNames(query, QUERY_COUNT,
                    info -> info.isShipVariant() && (!findImages || info.hasImage()));
            else
                return James.getState().fuzzyMatchNodeNames(query, QUERY_COUNT,
                    info -> info.getType().equals(type) && (!findImages || info.hasImage()));
        } else if(findImages)
            return James.getState().fuzzyMatchNodesAndImages(query, QUERY_COUNT,
                info -> !findImages || info.hasImage(), name -> true);
        else
            return James.getState().fuzzyMatchNodeNames(query, QUERY_COUNT,
                info -> !findImages || info.hasImage());
    }

    protected Optional<InteractionEventHandler> subcommandFor(String[] names) {
        if(names.length != 2) {
            return Optional.empty();
        }
        if(names[1].equals("data")) {
            findImages = false;
            findData = true;
            subcommand = (ShowSubcommand)(new ShowSubcommand().showing(true, false).withButtonEvent(getButtonEvent()));
            return Optional.of(subcommand);
        }
        else if(names[1].equals("image")) {
            findImages = true;
            findData = false;
            subcommand = (ShowSubcommand)(new ShowSubcommand().showing(false, true).withButtonEvent(getButtonEvent()));
            return Optional.of(subcommand);
        } else if(names[1].equals("both")) {
            findImages = true;
            findData = true;
            subcommand = (ShowSubcommand)(new ShowSubcommand().showing(true, true).withButtonEvent(getButtonEvent()));
            return Optional.of(subcommand);
        }
        return Optional.empty();
    }

    @Override
    public Optional<InteractionEventHandler> findSubcommand() {
        Optional<List<ApplicationCommandInteractionOption>> sub;

        sub = data.getSubcommandOptions("swizzle");
        if(sub.isPresent()) {
            return Optional.of(new ShowSwizzleSubcommand().withChatOptions(sub.get(), getChatEvent()));
        }

        sub = data.getSubcommandOptions("data");
        if(sub.isPresent()) {
            subcommand = (ShowSubcommand)(new ShowSubcommand().showing(true, false).withChatOptions(sub.get(), getChatEvent()));
            return Optional.of(subcommand);
        }

        sub = data.getSubcommandOptions("image");
        if(sub.isPresent()) {
            subcommand = (ShowSubcommand)(new ShowSubcommand().showing(false, true).withChatOptions(sub.get(), getChatEvent()));
            return Optional.of(subcommand);
        }

        sub = data.getSubcommandOptions("both");
        if(sub.isPresent()) {
            subcommand = (ShowSubcommand)(new ShowSubcommand().showing(true, true).withChatOptions(sub.get(), getChatEvent()));
            return Optional.of(subcommand);
        }
        return Optional.empty();
    }
}
