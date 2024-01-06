package humanika.rafeki.james.commands;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import java.util.Optional;
import reactor.core.publisher.Mono;

public interface InteractionEventHandler {
    public abstract String getName();
    public String getFullName();
    public Optional<String> getJson();
    public Mono<Void> handleChatCommand();
    public Mono<Void> handleButtonInteraction();

    public InteractionEventHandler withChatEvent(ChatInputInteractionEvent event);
    public InteractionEventHandler withButtonEvent(ButtonInteractionEvent event);
};
