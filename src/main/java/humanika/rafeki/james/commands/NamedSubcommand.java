package humanika.rafeki.james.commands;

public class NamedSubcommand extends PrimitiveSlashSubcommand {
    private String name = null;

    @Override
    public String getName() {
        return name;
    }

    NamedSubcommand withName(String name) {
        try {
            NamedSubcommand result = (NamedSubcommand)clone();
            result.name = name;
            return result;
        } catch(CloneNotSupportedException cnse) {
            // Should never happen.
            return null;
        }
    }
}
