package humanika.rafeki.james.commands;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import java.util.List;
import java.util.Optional;
import reactor.core.publisher.Mono;

public interface InteractionEventHandler {
    public boolean shouldDefer();
    public String getName();
    public String getFullName();
    public Optional<String> getJson();
    public Mono<Void> handleChatCommand();
    public Mono<Void> handleButtonInteraction();
    public EventDataProvider getData();
    public InteractionEventHandler withChatEvent(ChatInputInteractionEvent event);
    public InteractionEventHandler withButtonEvent(ButtonInteractionEvent event);
    public InteractionEventHandler withChatOptions(List<ApplicationCommandInteractionOption> options, ChatInputInteractionEvent event);
};
