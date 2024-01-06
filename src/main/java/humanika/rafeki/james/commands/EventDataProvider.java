package humanika.rafeki.james.commands;

import discord4j.core.event.domain.interaction.ButtonInteractionEvent;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.Interaction;
import discord4j.core.object.entity.Attachment;
import java.util.List;
import java.util.Optional;
import reactor.core.publisher.Mono;

public interface EventDataProvider {
    public Interaction getInteraction();
    public DeferrableInteractionEvent getEvent();
    public boolean isEphemeral();
    public Optional<List<ApplicationCommandInteractionOption>> getSubcommandOptions(String name);
    public String getStringOrDefault(String name, String def);
    public Optional<String> getString(String name);
    public Optional<Long> getLong(String name);
    public long getLongOrDefault(String name, long def);
    public Optional<Boolean> getBoolean(String name);
    public Boolean getBooleanOrDefault(String name, Boolean def);
    public Optional<Attachment> getAttachment(String name);
}
