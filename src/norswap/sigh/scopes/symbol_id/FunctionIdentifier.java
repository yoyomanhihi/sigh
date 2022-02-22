package norswap.sigh.scopes.symbol_id;

import java.util.Arrays;

import norswap.sigh.types.Type;

public class FunctionIdentifier extends SymbolIdentifier {
    protected final Type[] paramTypes;

    public FunctionIdentifier(String name, Type[] paramTypes) {
        super(name);
        this.paramTypes = paramTypes;
    }

    @Override public String toString() {
        return name + "(" + Arrays.toString(paramTypes) + ")";
    }

    public Type[] paramTypes() {
        return paramTypes;
    }

    @Override public boolean equals(Object other) {
        return super.equals(other) && Arrays.equals(paramTypes, ((FunctionIdentifier) other).paramTypes);
    }

    @Override public int hashCode() {
        return super.hashCode() + Arrays.hashCode(paramTypes);
    }
}