package humanika.rafeki.james.utils;

import java.util.List;
import me.mcofficer.esparser.DataNode;
import discord4j.core.spec.EmbedCreateFields;

public class AddParagraphFields {
    private int fieldsSent = 0;
    private int paragraphsInField = 0;
    private StringBuilder builder = new StringBuilder(100);
    private int maxFields;
    private int sectionStart = 0;

    public AddParagraphFields(int maxFields) {
        this.maxFields = maxFields;
    }

    public void setMaxFields(int maxFields) {
        this.maxFields = maxFields;
    }

    public int getMaxFields() {
        return maxFields;
    }

    public /* synchronized */ void add(String title, List<EmbedCreateFields.Field> fields, List<DataNode> paragraphs, int maxCharsPerField) {
        paragraphsInField = 0;
        builder.delete(0, builder.length());
        fieldsSent = fields.size();
        sectionStart = fieldsSent;

        for(DataNode node : paragraphs) {
            if(node.size() < 1)
                continue;
                
            String paragraph = node.size() > 1 ? node.token(1) : node.token(0);
            int maxFieldSize = maxCharsPerField;
            if(fieldsSent == 0)
                maxFieldSize -= title.length() + 1;
                
            if(builder.length() + 1 + paragraph.length() > maxFieldSize) {
                if(!addField(title, fields))
                    break;
                if(paragraph.length() > maxFieldSize) {
                    builder.append(paragraph.substring(0, maxFieldSize - 3)).append("...");
                    if(!addField(title, fields))
                        break;
                    continue;
                }
            }
            if(paragraphsInField > 0)
                builder.append('\n');
            builder.append(paragraph);
            paragraphsInField++;
            if(builder.length() > 0 && fieldsSent < maxFields)
                addField(title, fields);
        }
    }

    private boolean addField(String title, List<EmbedCreateFields.Field> fields) {
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
