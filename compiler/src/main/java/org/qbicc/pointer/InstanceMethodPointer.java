package org.qbicc.pointer;

import org.qbicc.type.definition.element.MethodElement;

/**
 * An exact pointer to an instance method.
 */
public final class InstanceMethodPointer extends RootPointer {
    private final MethodElement instanceMethod;

    InstanceMethodPointer(MethodElement instanceMethod) {
        super(instanceMethod.getType().getPointer());
        if (instanceMethod.isStatic()) {
            throw new IllegalArgumentException("Method is static");
        }
        this.instanceMethod = instanceMethod;
    }

    public static InstanceMethodPointer of(final MethodElement methodElement) {
        return methodElement.getOrCreateInstanceMethodPointer(InstanceMethodPointer::new);
    }

    public MethodElement getInstanceMethod() {
        return instanceMethod;
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 19 + instanceMethod.hashCode();
    }

    @Override
    public boolean equals(final RootPointer other) {
        return other instanceof InstanceMethodPointer imp && equals(imp);
    }

    public boolean equals(final InstanceMethodPointer other) {
        return this == other || super.equals(other) && instanceMethod == other.instanceMethod;
    }

    @Override
    public StringBuilder toString(StringBuilder b) {
        return b.append('&').append(instanceMethod);
    }

    public <T, R> R accept(final Visitor<T, R> visitor, final T t) {
        return visitor.visit(t, this);
    }
}
