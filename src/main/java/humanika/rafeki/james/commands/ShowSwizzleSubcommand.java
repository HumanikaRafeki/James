package humanika.rafeki.james.commands;

import discord4j.core.object.entity.Attachment;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.core.spec.MessageCreateFields;
import humanika.rafeki.james.James;
import humanika.rafeki.james.data.Government;
import humanika.rafeki.james.utils.ImageSwizzler;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import javax.imageio.ImageIO;
import reactor.core.publisher.Mono;

public class ShowSwizzleSubcommand extends PrimitiveCommand {
    private static final String[] vectorStrings = {
        "{GL_RED, GL_GREEN, GL_BLUE, GL_ALPHA}, // 0 red + yellow markings (republic)",
        "{GL_RED, GL_BLUE, GL_GREEN, GL_ALPHA}, // 1 red + magenta markings",
        "{GL_GREEN, GL_RED, GL_BLUE, GL_ALPHA}, // 2 green + yellow (free worlds)",
        "{GL_BLUE, GL_RED, GL_GREEN, GL_ALPHA}, // 3 green + cyan",
        "{GL_GREEN, GL_BLUE, GL_RED, GL_ALPHA}, // 4 blue + magenta (syndicate)",
        "{GL_BLUE, GL_GREEN, GL_RED, GL_ALPHA}, // 5 blue + cyan (merchant)",
        "{GL_GREEN, GL_BLUE, GL_BLUE, GL_ALPHA}, // 6 red and black (pirate)",
        "{GL_RED, GL_BLUE, GL_BLUE, GL_ALPHA}, // 7 pure red",
        "{GL_RED, GL_GREEN, GL_GREEN, GL_ALPHA}, // 8 faded red",
        "{GL_BLUE, GL_BLUE, GL_BLUE, GL_ALPHA}, // 9 pure black",
        "{GL_GREEN, GL_GREEN, GL_GREEN, GL_ALPHA}, // 10 faded black",
        "{GL_RED, GL_RED, GL_RED, GL_ALPHA}, // 11 pure white",
        "{GL_BLUE, GL_BLUE, GL_GREEN, GL_ALPHA}, // 12 darkened blue",
        "{GL_BLUE, GL_BLUE, GL_RED, GL_ALPHA}, // 13 pure blue",
        "{GL_GREEN, GL_GREEN, GL_RED, GL_ALPHA}, // 14 faded blue",
        "{GL_BLUE, GL_GREEN, GL_GREEN, GL_ALPHA}, // 15 darkened cyan",
        "{GL_BLUE, GL_RED, GL_RED, GL_ALPHA}, // 16 pure cyan",
        "{GL_GREEN, GL_RED, GL_RED, GL_ALPHA}, // 17 faded cyan",
        "{GL_BLUE, GL_GREEN, GL_BLUE, GL_ALPHA}, // 18 darkened green",
        "{GL_BLUE, GL_RED, GL_BLUE, GL_ALPHA}, // 19 pure green",
        "{GL_GREEN, GL_RED, GL_GREEN, GL_ALPHA}, // 20 faded green",
        "{GL_GREEN, GL_GREEN, GL_BLUE, GL_ALPHA}, // 21 darkened yellow",
        "{GL_RED, GL_RED, GL_BLUE, GL_ALPHA}, // 22 pure yellow",
        "{GL_RED, GL_RED, GL_GREEN, GL_ALPHA}, // 23 faded yellow",
        "{GL_GREEN, GL_BLUE, GL_GREEN, GL_ALPHA}, // 24 darkened magenta",
        "{GL_RED, GL_BLUE, GL_RED, GL_ALPHA}, // 25 pure magenta",
        "{GL_RED, GL_GREEN, GL_RED, GL_ALPHA}, // 26 faded magenta",
        "{GL_BLUE, GL_ZERO, GL_ZERO, GL_ALPHA}, // 27 red only (cloaked)",
        "{GL_ZERO, GL_ZERO, GL_ZERO, GL_ALPHA} // 28 black only (outline)"
    };

    @Override
    public String getName() {
        return "swizzle";
    }

    @Override
    public String getFullName() {
        return "show swizzle";
    }

    @Override
    public Mono<Void> handleChatCommand() {
        Optional<Long> swizzleArg = data.getLong("swizzle");

        int swizzle = swizzleArg.isPresent() ? (int)swizzleArg.get().longValue() : 0;
        if(swizzle < 0 || swizzle >= vectorStrings.length)
            return getChatEvent().reply("Swizzle not found!");
        String vectorInfo = vectorStrings[swizzle];


        EmbedCreateSpec embed = EmbedCreateSpec.create()
            .withTitle("Swizzle " + swizzle + " Sample")
            .withThumbnail(James.getConfig().swizzledThumbnailPath + swizzle + ".png");

        StringBuilder description = new StringBuilder();
        description.append("**Swizzle Vector:**\n```").append(vectorStrings[swizzle]).append("```");

        Optional<List<Government>> governments = James.getState().governmentsWithSwizzle(swizzle);
        if(governments.isPresent()) {
            description.append("\n\n**Governments using this swizzle:**");
            for(Government gov : governments.get())
                description.append("\n- ").append(gov.name);
        } else
            description.append("\n\n**No governments use this swizzle.**\n");

        embed = embed.withDescription(description.toString());
        return getChatEvent().reply().withEmbeds(embed).withEphemeral(data.isEphemeral());
    }
}
