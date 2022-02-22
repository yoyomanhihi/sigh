package norswap.sigh.scopes.symbol_id;

public class SymbolIdentifier {
    protected final String name;

    public SymbolIdentifier(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof SymbolIdentifier)) return false;

        SymbolIdentifier otherSymbol = (SymbolIdentifier) other;

        return name.equals(otherSymbol.name);
    }

    @Override public int hashCode() {
        return name.hashCode();
    }
}
