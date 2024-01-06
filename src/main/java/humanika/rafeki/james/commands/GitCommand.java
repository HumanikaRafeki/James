package humanika.rafeki.james.commands;

import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.spec.EmbedCreateSpec;
import humanika.rafeki.james.James;
import humanika.rafeki.james.Utils;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import reactor.core.publisher.Mono;

public class GitCommand extends PrimitiveSlashCommand {

    private static Pattern VALID_HASH = Pattern.compile("\\A[a-zA-Z0-9]+\\z");

    @Override
    public String getName() {
        return "git";
    }

    @Override
    public Mono<Void> handleChatCommand() {
        Optional<List<ApplicationCommandInteractionOption>> options;

        options = data.getSubcommandOptions("issue");
        if(options.isPresent()) {
            InteractionEventHandler subcommand = new NamedSubcommand().withName("issue").withChatOptions(options.get(), getChatEvent());
            String issue = String.valueOf(subcommand.getData().getLongOrDefault("issue", 0));
            return replyUrl(false, subcommand.getData().isEphemeral(),
                            "https://github.com/endless-sky/endless-sky/issues/",
                            issue, "issue number", "## Git Issue\n", "## Invalid Git Issue\n");
        }

        options = data.getSubcommandOptions("pr");
        if(!options.isPresent())
            options = data.getSubcommandOptions("pull");
        if(options.isPresent()) {
            InteractionEventHandler subcommand = new NamedSubcommand().withName("pr").withChatOptions(options.get(), getChatEvent());
            String pull = String.valueOf(subcommand.getData().getLongOrDefault("pr", 0));
            return replyUrl(false, subcommand.getData().isEphemeral(),
                            "https://github.com/endless-sky/endless-sky/pull/",
                            pull, "pull request number", "## Git Pull Request (PR)\n", "## Invalid Pull Request\n");
        }

        options = data.getSubcommandOptions("commit");
        if(options.isPresent()) {
            InteractionEventHandler subcommand = new NamedSubcommand().withName("commit").withChatOptions(options.get(), getChatEvent());
            String hash = subcommand.getData().getStringOrDefault("hash", "1234567");
            boolean isAHash = VALID_HASH.matcher(hash).matches();
            return replyUrl(!isAHash, subcommand.getData().isEphemeral(),
                            "https://github.com/endless-sky/endless-sky/commit/",
                            hash, "commit hash", "## Git Commit Hash\n", "## Invalid Hash\n");
        }

        return Mono.empty();
    }

    private Mono<Void> replyUrl(boolean isInvalid, boolean ephemeral, String baseUrl, String theRest, String what,
                                String successTitle, String failTitle) {

        String url = baseUrl + theRest;

        StringBuilder response = new StringBuilder();

        String mention = ephemeral ? null : data.getInteraction().getUser().getMention();
        if(mention != null && mention.length() > 0)
            response.append(mention);
        else
            response.append("You");
        String cleaned = theRest.replaceAll("`", "'");
        response.append(" asked for ").append(what).append(" `").append(cleaned).append('`');

        boolean valid = !isInvalid;
        if(valid) {            
            int code = Utils.getHttpStatus(url);
            valid = code >= 200 && code < 400 || code >= 500;
        }

        if(valid)
            response.append("\nIt's here:\n").append(url).insert(0, successTitle);
        else
            response.append("\nBut that ").append(what).append(" is invalid. Sorry.").insert(0, failTitle);
        return getChatEvent().reply().withContent(response.toString()).withEphemeral(ephemeral);
    }

    private String getCommentary() {
        String babble = James.getState().jamesPhrase("JAMES::ping");
        return babble!=null ? babble : "*no commentary*";
    }
}
