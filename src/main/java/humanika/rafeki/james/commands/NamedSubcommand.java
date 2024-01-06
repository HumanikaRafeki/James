package humanika.rafeki.james.commands;

public class NamedSubcommand extends PrimitiveCommand {
    private String name = null;
    private String fullName = null;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    NamedSubcommand withName(String name, String fullName) {
        try {
            NamedSubcommand result = (NamedSubcommand)clone();
            result.name = name;
            result.fullName = fullName;
            return result;
        } catch(CloneNotSupportedException cnse) {
            // Should never happen.
            return null;
        }
    }
}
