package org.qbicc.graph.literal;

import org.qbicc.graph.ValueVisitor;
import org.qbicc.pointer.Pointer;
import org.qbicc.type.PointerType;
import org.qbicc.type.ValueType;

/**
 * A literal referring to some program object.
 */
public final class PointerLiteral extends Literal {
    private final Pointer pointer;

    PointerLiteral(Pointer pointer) {
        this.pointer = pointer;
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return b.append(pointer);
    }

    @Override
    public PointerType getType() {
        return pointer.getType();
    }

    public ValueType getValueType() {
        return getType().getPointeeType();
    }

    public Pointer getPointer() {
        return pointer;
    }

    @Override
    public <T, R> R accept(ValueVisitor<T, R> visitor, T param) {
        return visitor.visit(param, this);
    }

    @Override
    public boolean isZero() {
        return false;
    }

    @Override
    public boolean equals(Literal other) {
        return other instanceof PointerLiteral pol && equals(pol);
    }

    public boolean equals(PointerLiteral other) {
        return this == other || other != null && pointer.equals(other.pointer);
    }

    @Override
    public int hashCode() {
        return pointer.hashCode();
    }
}
