package humanika.rafeki.james.commands;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.OptionalInt;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import javax.imageio.ImageIO;

import reactor.core.publisher.Mono;
import discord4j.core.object.entity.Attachment;
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.spec.MessageCreateFields;

import humanika.rafeki.james.James;
import humanika.rafeki.james.utils.ImageSwizzler;

public class SwizzleImageCommand extends SlashCommand {
    private static final String[] VAR_ARRAY = {"image1", "image2", "image3", "image4"};
    private static final List<String> VAR_LIST = Arrays.asList(VAR_ARRAY);

    public SwizzleImageCommand() {}

    @Override
    public String getName() {
        return "swizzleimage";
    }

    @Override
    public Mono<Void> handle(ChatInputInteractionEvent event) {
        if(!event.getInteraction().getGuildId().isPresent())
            return handleDirectMessage(event);
        StringBuffer errors = new StringBuffer();
        List<Attachment> imageAttachments;

        Optional<Long> swizzle = getLong(event, "swizzle");
        String arg = swizzle.isPresent() ? swizzle.get().toString() : null;
        ArrayList<MessageCreateFields.File> result = new ArrayList<>();

        for(String var : VAR_LIST) {
            Optional<Attachment> maybeData = getAttachment(event, var);
            if(!maybeData.isPresent())
                continue;
            Attachment data = maybeData.get();
            try {
                processOneFile(data, errors, result, arg);
            } catch(IOException ioe) {
                errors.append(data.getFilename() + ": unable to read file");
            }
        }

        if(result.size() < 1) {
            if(errors.length() < 1)
                // Should never happen, but just in case of logic errors:
                errors.append("Please attach one or more images.");
            return event.reply(errors.toString()).withEphemeral(isEphemeral(event));
        } else if(errors.length() > 0)
            return event.reply(errors.toString()).withFiles(result).withEphemeral(isEphemeral(event));
        else
            return event.reply().withFiles(result).withEphemeral(isEphemeral(event));
    }

    private void processOneFile(Attachment data, StringBuffer errors, ArrayList<MessageCreateFields.File> result, String swizzle) throws IOException {
        int maxSwizzleImageSize = James.getConfig().maxSwizzleImageSize;
        String filename = data.getFilename();
        OptionalInt width = data.getWidth();
        OptionalInt height = data.getHeight();
        if(!width.isPresent() || !height.isPresent() || width.getAsInt() < 1 || height.getAsInt() < 1) {
            errors.append(filename + ": is not an image\n");
            return;
        }
        int w = width.getAsInt();
        int h = height.getAsInt();
        if(w > maxSwizzleImageSize || h > maxSwizzleImageSize) {
            errors.append(filename + " is larger than " + maxSwizzleImageSize + "px.");
            return;
        }
        String urlString = data.getUrl();
        URL url = new URL(urlString);
        BufferedImage image = ImageIO.read(url);
        InputStream swizzled = new ImageSwizzler().swizzle(image, swizzle);
        String outputName = filename.replace("\\.[^.]+$","") + ".png";
        if(data.isSpoiler())
            result.add(MessageCreateFields.FileSpoiler.of(outputName, swizzled));
        else
            result.add(MessageCreateFields.File.of(outputName, swizzled));
    }
}
