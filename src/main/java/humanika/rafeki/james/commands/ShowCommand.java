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

public class ShowCommand extends NodeInfoCommand {

    private ShowSubcommand subcommand = null;

    protected Optional<String> getType() {
        return subcommand.getString("type");
    }

    protected Optional<String> getQuery() {
        return subcommand.getString("query");
    }

    @Override
    public String getFullName() {
        if(subcommand == null)
            return "show";
        else
            return "show " + subcommand.getFullName();
    }

    @Override
    protected Mono<Void> generateResult(List<NodeInfo> found, boolean ephemeral, PrimitiveSlashSubcommand subcommand) {
        return( ((ShowSubcommand)subcommand).generateResult(found, ephemeral) );
    }

    @Override
    protected Optional<List<NodeInfo>> getMatches(String query, Optional<String> maybeType) {
        if(maybeType.isPresent()) {
            final String type = maybeType.get();
            if(type.equals("variant"))
                return James.getState().fuzzyMatchNodeNames(query, QUERY_COUNT, info -> info.isShipVariant());
            else
                return James.getState().fuzzyMatchNodeNames(query, QUERY_COUNT, info -> info.getType().equals(type) && !info.isShipVariant());
        } else
            return James.getState().fuzzyMatchNodeNames(query, QUERY_COUNT, info -> !info.isShipVariant());
    }

    @Override
    protected Optional<PrimitiveSlashSubcommand> subcommandFor(String[] names) {
        if(names.length != 2) {
            return Optional.empty();
        }
        if(names[1].equals("data")) {
            subcommand = (ShowSubcommand)(new ShowSubcommand().showing(true, false).forButtonEvent(buttonEvent));
            return Optional.of(subcommand);
        }
        else if(names[1].equals("image")) {
            subcommand = (ShowSubcommand)(new ShowSubcommand().showing(false, true).forButtonEvent(buttonEvent));
            return Optional.of(subcommand);
        } else if(names[1].equals("both")) {
            subcommand = (ShowSubcommand)(new ShowSubcommand().showing(true, true).forButtonEvent(buttonEvent));
            return Optional.of(subcommand);
        }
        return Optional.empty();
    }

    @Override
    public Optional<PrimitiveSlashSubcommand> findSubcommand() {
        Optional<List<ApplicationCommandInteractionOption>> sub;

        sub = getSubcommandOptions("data");
        if(sub.isPresent()) {
            subcommand = (ShowSubcommand)(new ShowSubcommand().showing(true, false).forChatEvent(sub.get(), event));
            return Optional.of(subcommand);
        }

        sub = getSubcommandOptions("image");
        if(sub.isPresent()) {
            subcommand = (ShowSubcommand)(new ShowSubcommand().showing(false, true).forChatEvent(sub.get(), event));
            return Optional.of(subcommand);
        }

        sub = getSubcommandOptions("both");
        if(sub.isPresent()) {
            subcommand = (ShowSubcommand)(new ShowSubcommand().showing(true, true).forChatEvent(sub.get(), event));
            return Optional.of(subcommand);
        }

        return Optional.empty();
    }
}
