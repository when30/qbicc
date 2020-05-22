package cc.quarkus.qcc.machine.llvm.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cc.quarkus.qcc.machine.llvm.BasicBlock;
import cc.quarkus.qcc.machine.llvm.FunctionDefinition;
import cc.quarkus.qcc.machine.llvm.IntCondition;
import cc.quarkus.qcc.machine.llvm.Value;
import cc.quarkus.qcc.machine.llvm.op.Assignment;
import cc.quarkus.qcc.machine.llvm.op.AtomicRmwInstruction;
import cc.quarkus.qcc.machine.llvm.op.Binary;
import cc.quarkus.qcc.machine.llvm.op.Branch;
import cc.quarkus.qcc.machine.llvm.op.Call;
import cc.quarkus.qcc.machine.llvm.op.ExactBinary;
import cc.quarkus.qcc.machine.llvm.op.Fence;
import cc.quarkus.qcc.machine.llvm.op.Load;
import cc.quarkus.qcc.machine.llvm.op.NuwNswBinary;
import cc.quarkus.qcc.machine.llvm.op.OrderingConstraint;
import cc.quarkus.qcc.machine.llvm.op.Phi;
import cc.quarkus.qcc.machine.llvm.op.Return;
import cc.quarkus.qcc.machine.llvm.op.Select;
import cc.quarkus.qcc.machine.llvm.op.Store;
import io.smallrye.common.constraint.Assert;

/**
 *
 */
final class BasicBlockImpl extends AbstractEmittable implements BasicBlock {
    final BasicBlockImpl prev;
    final FunctionDefinitionImpl func;
    final List<AbstractEmittable> phis = new ArrayList<>();
    final List<AbstractEmittable> items = new ArrayList<>();
    String name;
    boolean terminated;

    BasicBlockImpl(final BasicBlockImpl prev, final FunctionDefinitionImpl func) {
        this.prev = prev;
        this.func = func;
    }

    public BasicBlock name(final String name) {
        this.name = Assert.checkNotNullParam("name", name);
        return this;
    }

    public FunctionDefinition functionDefinition() {
        return func;
    }

    private <I extends AbstractEmittable> I add(I item) {
        items.add(item);
        return item;
    }

    private <I extends AbstractEmittable> I addPhi(I item) {
        phis.add(item);
        return item;
    }

    private void checkTerminated() {
        if (terminated) {
            throw new IllegalStateException("Basic block already terminated");
        }
    }

    // not terminator, not starter

    public Phi phi(final Value type) {
        Assert.checkNotNullParam("type", type);
        return addPhi(new PhiImpl(this, (AbstractValue) type));
    }

    // terminators

    public Branch br(final BasicBlock dest) {
        Assert.checkNotNullParam("dest", dest);
        checkTerminated();
        Branch res = add(new UnconditionalBranchImpl((BasicBlockImpl) dest));
        terminated = true;
        return res;
    }

    public Branch br(final Value cond, final BasicBlock ifTrue, final BasicBlock ifFalse) {
        Assert.checkNotNullParam("cond", cond);
        Assert.checkNotNullParam("ifTrue", ifTrue);
        Assert.checkNotNullParam("ifFalse", ifFalse);
        checkTerminated();
        Branch res = add(new ConditionalBranchImpl((AbstractValue) cond, (BasicBlockImpl) ifTrue, (BasicBlockImpl) ifFalse));
        terminated = true;
        return res;
    }

    public Return ret() {
        checkTerminated();
        terminated = true;
        return add(VoidReturn.INSTANCE);
    }

    public Return ret(final Value type, final Value val) {
        checkTerminated();
        terminated = true;
        return add(new ValueReturn((AbstractValue) type, (AbstractValue) val));
    }

    public void unreachable() {
        checkTerminated();
        terminated = true;
        add(Unreachable.INSTANCE);
    }

    // starters

    public Assignment assign(final Value value) {
        Assert.checkNotNullParam("value", value);
        checkTerminated();
        return add(new AssignmentImpl(this, (AbstractValue) value));
    }

    public Select select(final Value condType, final Value cond, final Value valueType, final Value trueValue, final Value falseValue) {
        checkTerminated();
        return add(new SelectImpl(this, (AbstractValue) condType, (AbstractValue) cond, (AbstractValue) valueType, (AbstractValue) trueValue, (AbstractValue) falseValue));
    }

    public NuwNswBinary add(final Value type, final Value arg1, final Value arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        checkTerminated();
        return add(new AddImpl(this, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public NuwNswBinary sub(final Value type, final Value arg1, final Value arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        checkTerminated();
        return add(new SubImpl(this, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public NuwNswBinary mul(final Value type, final Value arg1, final Value arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        checkTerminated();
        return add(new MulImpl(this, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public NuwNswBinary shl(final Value type, final Value arg1, final Value arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        checkTerminated();
        return add(new ShlImpl(this, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public ExactBinary udiv(final Value type, final Value arg1, final Value arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        checkTerminated();
        return add(new UdivImpl(this, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public ExactBinary sdiv(final Value type, final Value arg1, final Value arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        checkTerminated();
        return add(new SdivImpl(this, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public ExactBinary lshr(final Value type, final Value arg1, final Value arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        checkTerminated();
        return add(new LshrImpl(this, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public ExactBinary ashr(final Value type, final Value arg1, final Value arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        checkTerminated();
        return add(new AshrImpl(this, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public Binary icmp(final IntCondition cond, final Value type, final Value arg1, final Value arg2) {
        Assert.checkNotNullParam("cond", cond);
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        checkTerminated();
        return add(new IcmpImpl(this, cond, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public Binary and(final Value type, final Value arg1, final Value arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        checkTerminated();
        return add(new AndImpl(this, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public Binary or(final Value type, final Value arg1, final Value arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        checkTerminated();
        return add(new OrImpl(this, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public Binary xor(final Value type, final Value arg1, final Value arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        checkTerminated();
        return add(new XorImpl(this, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public Binary urem(final Value type, final Value arg1, final Value arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        checkTerminated();
        return add(new URemImpl(this, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public Binary srem(final Value type, final Value arg1, final Value arg2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("arg1", arg1);
        Assert.checkNotNullParam("arg2", arg2);
        checkTerminated();
        return add(new SRemImpl(this, (AbstractValue) type, (AbstractValue) arg1, (AbstractValue) arg2));
    }

    public Call call(final Value type, final Value function) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("function", function);
        checkTerminated();
        return add(new CallImpl(this, (AbstractValue) type, (AbstractValue) function));
    }

    public Load load(final Value type, final Value pointeeType, final Value pointer) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("pointeeType", pointeeType);
        Assert.checkNotNullParam("pointer", pointer);
        checkTerminated();
        return add(new LoadImpl(this, (AbstractValue) type, (AbstractValue) pointeeType, (AbstractValue) pointer));
    }

    public Store store(final Value type, final Value value, final Value pointeeType, final Value pointer) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("value", value);
        Assert.checkNotNullParam("pointeeType", pointeeType);
        Assert.checkNotNullParam("pointer", pointer);
        checkTerminated();
        return add(new StoreImpl((AbstractValue) type, (AbstractValue) value, (AbstractValue) pointeeType, (AbstractValue) pointer));
    }

    public Fence fence(final OrderingConstraint ordering) {
        Assert.checkNotNullParam("ordering", ordering);
        checkTerminated();
        return add(new FenceImpl(ordering));
    }

    public AtomicRmwInstruction atomicrmw() {
        throw Assert.unsupported();
    }

    public BasicBlock createBlock() {
        return func.createBlock();
    }

    @SuppressWarnings("UnusedReturnValue")
    Appendable appendAsBlockTo(final Appendable target) throws IOException {
        final BasicBlockImpl prev = this.prev;
        if (prev != null) {
            prev.appendAsBlockTo(target);
        }
        if (phis.isEmpty() && items.isEmpty()) {
            // no block;
            return target;
        }
        if (! terminated) {
            throw new IllegalStateException("Basic block not terminated");
        }
        if (this != func.rootBlock) {
            if (name == null) {
                func.assignName(this);
            }
            target.append(name).append(':').append(System.lineSeparator());
        }
        for (List<AbstractEmittable> list : List.of(phis, items)) {
            for (AbstractEmittable item : list) {
                target.append("  ");
                item.appendTo(target);
                target.append(System.lineSeparator());
            }
        }
        return target;
    }

    public Appendable appendTo(final Appendable target) throws IOException {
        target.append('%');
        if (this == func.rootBlock) {
            target.append('0');
        } else {
            if (name == null) {
                func.assignName(this);
            }
            target.append(name);
        }
        return target;
    }
}
