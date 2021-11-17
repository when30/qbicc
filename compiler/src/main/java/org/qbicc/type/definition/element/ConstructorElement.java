package org.qbicc.type.definition.element;

/**
 *
 */
public final class ConstructorElement extends InvokableElement {
    public static final ConstructorElement[] NO_CONSTRUCTORS = new ConstructorElement[0];

    ConstructorElement(BuilderImpl builder) {
        super(builder);
    }

    public <T, R> R accept(final ElementVisitor<T, R> visitor, final T param) {
        return visitor.visit(param, this);
    }

    public String toString() {
        final String packageName = getEnclosingType().getDescriptor().getPackageName();
        if (packageName.isEmpty()) {
            return getEnclosingType().getDescriptor().getClassName()+getDescriptor();
        }
        return packageName+"."+getEnclosingType().getDescriptor().getClassName()+getDescriptor();
    }

    public static Builder builder() {
        return new BuilderImpl();
    }

    public interface Builder extends InvokableElement.Builder {
        ConstructorElement build();
    }

    static final class BuilderImpl extends InvokableElement.BuilderImpl implements Builder {
        BuilderImpl() {}

        public ConstructorElement build() {
            return new ConstructorElement(this);
        }
    }
}
