package humanika.rafeki.james.commands;

import humanika.rafeki.james.James;
import humanika.rafeki.james.data.CreatorTemplate;
import reactor.core.publisher.Mono;
import java.util.List;
import java.util.ArrayList;
import discord4j.core.object.component.Button;
import discord4j.core.object.component.ActionRow;

public class TemplateCommand extends PrimitiveSlashCommand {
    private final static int MAX_ROWS = 5;
    private final static int BUTTONS_PER_ROW = 4;
    private final static int MAX_TEMPLATES = BUTTONS_PER_ROW * MAX_ROWS - 1;
    private final static int MAX_BUTTONS_PER_TYPE = 10;

    @Override
    public String getName() {
        return "template";
    }

    @Override
    public Mono<Void> handleChatCommand() {
        StringBuilder response = new StringBuilder();

        response.append("template:");
        boolean ephemeral = isEphemeral();
        response.append(ephemeral ? "E:template:" : "-:template:");
        String buttonId = response.toString();

        List<ActionRow> rows = new ArrayList<>();
        List<Button> buttons = new ArrayList<>();

        response.delete(0, response.length());
        response.append("Select a template:\n");
        int buttonCounter = 0;
        for(CreatorTemplate template : James.getConfig().creatorTemplates) {
            buttonCounter++;
            if(buttonCounter >= BUTTONS_PER_ROW * 2 || buttonCounter >= MAX_BUTTONS_PER_TYPE)
                buttons.add(Button.secondary(buttonId + template.id, template.id));
            else
                buttons.add(Button.primary(buttonId + template.id, template.id));
            response.append("- *").append(template.id).append("* = ").append(template.explanation).append('\n');
            if(buttons.size() >= BUTTONS_PER_ROW) {
                rows.add(ActionRow.of(buttons));
                buttons.clear();
            }
            if(buttonCounter >= MAX_TEMPLATES)
                break;
        }

        StringBuilder close = new StringBuilder();
        close.append("template:");
        close.append(ephemeral ? "E:close:close" : "-:close:close");
        buttons.add(Button.success(close.toString(), "cancel"));

        if(buttons.size() > 0)
            rows.add(ActionRow.of(buttons));

        return event.reply(response.toString()).withEphemeral(ephemeral).withComponents(rows);
    }

    public Mono<Void> handleButtonInteraction() {
        String[] split = buttonEvent.getCustomId().split(":", 4);
        String flags = split[1];
        boolean ephemeral = flags.contains("E");
        String action = split[2];
        String id = split[3];
        if(action.equals("close"))
            return buttonEvent.deleteReply().then();
        else {
            StringBuilder response = new StringBuilder();
            String cleaned = id.replaceAll("`", "'");
            String mention = ephemeral ? null : buttonEvent.getInteraction().getUser().getMention();
            if(mention != null && mention.length() > 0)
                response.append(mention);
            else
                response.append("You");
            response.append(" asked for template ").append(" `").append(cleaned).append('`');
            String url = null;
            for(CreatorTemplate template : James.getConfig().creatorTemplates)
                if(template.id.equals(id))
                    url = template.url;
            if(url == null) {
                response.append("\nThat template doesn't exist!");
                response.insert(0, "## Invalid Template\n");
            } else {
                response.append("\nHere it is:\n").append(url);
                response.insert(0, "## Your Template\n");
            }
            return buttonEvent.editReply().withContent(response.toString()).withComponents().then();
        }
    }
}
