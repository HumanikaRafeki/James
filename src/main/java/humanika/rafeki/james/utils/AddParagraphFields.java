package humanika.rafeki.james.utils;

import java.util.stream.Stream;
import java.util.List;
import me.mcofficer.esparser.DataNode;
import discord4j.core.spec.EmbedCreateFields;

public class AddParagraphFields {
    private int fieldsSent = 0;
    private int paragraphsInField = 0;
    private StringBuilder builder = new StringBuilder(100);
    private int maxFields;
    private int sectionStart = 0;
    private boolean code = false;
    private final static String BEFORE_CODE = "```julia\n";
    private final static String AFTER_CODE = "\n```";
    private final static int CODE_LENGTH = BEFORE_CODE.length() + AFTER_CODE.length();
    private final static String ELIPSES = "...";

    public AddParagraphFields(int maxFields, boolean code) {
        this.maxFields = maxFields;
        this.code = code;
    }

    public void setMaxFields(int maxFields) {
        this.maxFields = maxFields;
    }

    public int getMaxFields() {
        return maxFields;
    }

    public /* synchronized */ boolean add(String title, List<EmbedCreateFields.Field> fields, List<DataNode> paragraphs, int maxCharsPerField) {
        Stream<String> strings = paragraphs.stream().map(node -> node.size() > 1 ? node.token(1) : node.token(0));
        return add(title, fields, strings, maxCharsPerField);
    }

    public /* synchronized */ boolean add(String title, List<EmbedCreateFields.Field> fields, Stream<String> paragraphs, int maxCharsPerField) {
        paragraphsInField = 0;
        builder.delete(0, builder.length());
        fieldsSent = fields.size();
        sectionStart = fieldsSent;
        if(code)
            builder.append(BEFORE_CODE);
        Iterable<String> itergraphs = () -> paragraphs.iterator();
        for(String paragraph : itergraphs) {
            int maxFieldSize = maxCharsPerField;
            if(code)
                maxFieldSize -= CODE_LENGTH;
            if(fieldsSent == 0)
                maxFieldSize -= title.length() + 1;
                
            if(builder.length() + 1 + paragraph.length() > maxFieldSize) {
                if(!addField(title, fields))
                    return false;
                if(paragraph.length() > maxFieldSize) {
                    if(code)
                        return false;
                    builder.append(paragraph.substring(0, maxFieldSize - ELIPSES.length())).append(ELIPSES);
                    if(!addField(title, fields))
                        return false;
                    continue;
                }
                if(code)
                    builder.append(BEFORE_CODE);
            }
            if(paragraphsInField > 0 && !code)
                builder.append('\n');
            builder.append(paragraph);
            paragraphsInField++;
        }
        if(builder.length() > 0 && fieldsSent < maxFields) {
            if(!addField(title, fields))
                return false;
            if(code)
                builder.append(BEFORE_CODE);
        }
        return true;
    }

    private boolean addField(String title, List<EmbedCreateFields.Field> fields) {
        if(code)
            builder.append(AFTER_CODE);
        if(fieldsSent == sectionStart)
            fields.add(EmbedCreateFields.Field.of(title, builder.toString(), false));
        else
            fields.add(EmbedCreateFields.Field.of("", builder.toString(), false));
        fieldsSent++;
        paragraphsInField = 0;
        builder.delete(0, builder.length());
        return fieldsSent < maxFields;
    }
}
