package humanika.rafeki.james;

import discord4j.common.JacksonResources;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.Interaction;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import discord4j.rest.service.ApplicationService;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import humanika.rafeki.james.commands.*;

class Commands {
    private static final Logger LOGGER = LoggerFactory.getLogger(James.class);
    private static final List<SlashCommand> commands;
    private static final Map<String, SlashCommand> nameCommand;
    private static final List<String> commandJson;
    // The name of the folder the commands json is in, inside our resources folder
    private static final String commandsFolderName = "commands/";

    static {
        SlashCommand commandArray[] = {
            new CRConvertCommand(),
            new IndokorathCommand(),
            new KorathCommand(),
            new LookupCommand(),
            new NewsCommand(),
            new PhrasesCommand(),
            new PingCommand(),
            new RantCommand(),
            new SayCommand(),
            new SwizzleCommand(),
            new SwizzleImageCommand()
        };
        commands = Arrays.asList(commandArray);
        nameCommand = new HashMap();
        commandJson = new ArrayList();
        for(SlashCommand command : commandArray) {
            commandJson.add(command.getJson());
            nameCommand.put(command.getName(), command);
        }
    }

    public static Mono<Void> registerCommands(GatewayDiscordClient gateway) throws IOException {
        final RestClient restClient = gateway.getRestClient();
        //Create an ObjectMapper that supports Discord4J classes
        final JacksonResources d4jMapper = JacksonResources.create();

        // Convenience variables for the sake of easier to read code below
        final ApplicationService applicationService = restClient.getApplicationService();
        final long applicationId = restClient.getApplicationId().block();

        //Get our commands json from resources as command data
        List<ApplicationCommandRequest> requests = new ArrayList<>();
        for (String jsonFile : commandJson) {
            String jsonContent = getResourceFileAsString(commandsFolderName + jsonFile);
            System.out.println(jsonFile + " => " + jsonContent);
            requests.add(d4jMapper.getObjectMapper()
                         .readValue(jsonContent, ApplicationCommandRequest.class));
        }
        /* Bulk overwrite commands. This is now idempotent, so it is safe to use this even when only 1 command
        is changed/added/removed
        */
        return applicationService.bulkOverwriteGlobalApplicationCommand(applicationId, requests)
            .doOnNext(cmd -> LOGGER.debug("Successfully registered Global Command " + cmd.name()))
            .doOnError(e -> LOGGER.error("Failed to register global commands", e))
            .then();
    }

    private static boolean buildLogMessage(String what, String name, Interaction interaction, StringBuilder message) {
        message.append(what).append(" ").append(name).append(" id=").append(interaction.getId().asLong());

        Optional<Snowflake> guildId = interaction.getGuildId();
        if(guildId.isPresent())
            message.append(" server=").append(guildId.get().asLong());

        Snowflake channelId = interaction.getChannelId();
        message.append(" channel=").append(channelId.asLong());

        Optional<Member> maybeMember = interaction.getMember();
        boolean isBot = false;
        if(maybeMember.isPresent()) {
            Member member = maybeMember.get();
            if(member.isBot())
                isBot = true;
            message.append(" member=").append(member.getId().asLong());
        }

        return isBot;
    }

    public static Mono<Void> handleChatCommand(ChatInputInteractionEvent event) {
        StringBuilder message = new StringBuilder();
        String commandName = event.getCommandName();

        boolean isBot = buildLogMessage("command", commandName, event.getInteraction(), message);
        if(isBot) {
            message.append(" REJECT: bot");
            return Mono.empty();
        }

        SlashCommand command = nameCommand.get(commandName);
        if(command == null) {
            message.append(" REJECT: invalid command");
            LOGGER.info(message.toString());
            return Mono.empty();
        }

        message.append(" ACCEPT");
        LOGGER.info(message.toString());

        return command.withChatEvent(event).handleChatCommand();
    }

    public static Mono<Void> handleButtonInteraction(ButtonInteractionEvent event) {
        StringBuilder message = new StringBuilder();
        String[] fields = event.getCustomId().split(":", 2);
        String[] subnames = fields[0].split("\s+");
        String commandName = subnames[0];

        boolean isBot = buildLogMessage("button", commandName, event.getInteraction(), message);
        if(isBot) {
            message.append(" REJECT: bot");
            LOGGER.info(message.toString());
            return Mono.empty();
        }

        SlashCommand command = nameCommand.get(commandName);
        if(command == null) {
            message.append(" REJECT: invalid command");
            LOGGER.info(message.toString());
            return Mono.empty();
        }

        message.append(" ACCEPT");
        LOGGER.info(message.toString());

        // Tell Discord we are working on the request. Then work on the request.
        return event.deferEdit().concatWith(Mono.defer(
            command.withButtonEvent(event)::handleButtonInteraction)).then();
    }

    /**
     * Gets a specific resource file as String
     *
     * @param fileName The file path omitting "resources/"
     * @return The contents of the file as a String, otherwise throws an exception
     */
    private static String getResourceFileAsString(String fileName) throws IOException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try (InputStream resourceAsStream = classLoader.getResourceAsStream(fileName)) {
            if (resourceAsStream == null) return null;
            try (InputStreamReader inputStreamReader = new InputStreamReader(resourceAsStream);
                 BufferedReader reader = new BufferedReader(inputStreamReader)) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        }
    }
}
