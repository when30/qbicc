package cc.quarkus.qcc.compiler.native_image.llvm.generic;

import static cc.quarkus.qcc.machine.llvm.Types.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cc.quarkus.qcc.context.CompilationContext;
import cc.quarkus.qcc.graph.Action;
import cc.quarkus.qcc.graph.Add;
import cc.quarkus.qcc.graph.And;
import cc.quarkus.qcc.graph.BitCast;
import cc.quarkus.qcc.graph.BlockEntry;
import cc.quarkus.qcc.graph.Catch;
import cc.quarkus.qcc.graph.CmpEq;
import cc.quarkus.qcc.graph.CmpGe;
import cc.quarkus.qcc.graph.CmpGt;
import cc.quarkus.qcc.graph.CmpLe;
import cc.quarkus.qcc.graph.CmpLt;
import cc.quarkus.qcc.graph.CmpNe;
import cc.quarkus.qcc.graph.Convert;
import cc.quarkus.qcc.graph.Div;
import cc.quarkus.qcc.graph.Extend;
import cc.quarkus.qcc.graph.Goto;
import cc.quarkus.qcc.graph.If;
import cc.quarkus.qcc.graph.Mod;
import cc.quarkus.qcc.graph.Multiply;
import cc.quarkus.qcc.graph.Neg;
import cc.quarkus.qcc.graph.NodeVisitor;
import cc.quarkus.qcc.graph.Or;
import cc.quarkus.qcc.graph.PhiValue;
import cc.quarkus.qcc.graph.Return;
import cc.quarkus.qcc.graph.Select;
import cc.quarkus.qcc.graph.Shl;
import cc.quarkus.qcc.graph.Shr;
import cc.quarkus.qcc.graph.Sub;
import cc.quarkus.qcc.graph.Terminator;
import cc.quarkus.qcc.graph.Truncate;
import cc.quarkus.qcc.graph.ValueReturn;
import cc.quarkus.qcc.graph.Xor;
import cc.quarkus.qcc.graph.literal.IntegerLiteral;
import cc.quarkus.qcc.graph.schedule.Schedule;
import cc.quarkus.qcc.machine.llvm.BasicBlock;
import cc.quarkus.qcc.machine.llvm.FloatCondition;
import cc.quarkus.qcc.machine.llvm.FunctionDefinition;
import cc.quarkus.qcc.machine.llvm.IntCondition;
import cc.quarkus.qcc.machine.llvm.Value;
import cc.quarkus.qcc.machine.llvm.Values;
import cc.quarkus.qcc.machine.llvm.impl.LLVM;
import cc.quarkus.qcc.machine.llvm.op.Phi;
import cc.quarkus.qcc.type.BooleanType;
import cc.quarkus.qcc.type.FloatType;
import cc.quarkus.qcc.type.IntegerType;
import cc.quarkus.qcc.type.ReferenceType;
import cc.quarkus.qcc.type.SignedIntegerType;
import cc.quarkus.qcc.type.Type;
import cc.quarkus.qcc.type.VoidType;
import cc.quarkus.qcc.type.definition.MethodBody;
import cc.quarkus.qcc.type.definition.element.ExecutableElement;
import io.smallrye.common.constraint.Assert;

final class LLVMNodeVisitor implements NodeVisitor<Void, Value, Void, Void> {
    final CompilationContext ctxt;
    final Schedule schedule;
    final ExecutableElement element;
    final FunctionDefinition func;
    final cc.quarkus.qcc.graph.BasicBlock entryBlock;
    final Set<cc.quarkus.qcc.graph.BasicBlock> knownBlocks;
    final Map<cc.quarkus.qcc.graph.Value, Value> mappedValues = new HashMap<>();
    final Map<cc.quarkus.qcc.graph.BasicBlock, BasicBlock> mappedBlocks = new HashMap<>();
    final Map<Type, Value> types = new HashMap<>();

    LLVMNodeVisitor(final CompilationContext ctxt, final Schedule schedule, final ExecutableElement element, final FunctionDefinition func) {
        this.ctxt = ctxt;
        this.schedule = schedule;
        this.element = element;
        this.func = func;
        MethodBody methodBody = element.getMethodBody().getOrCreateMethodBody();
        entryBlock = methodBody.getEntryBlock();
        knownBlocks = entryBlock.calculateReachableBlocks();
    }

    // begin

    public void execute() {
        entryBlock.getTerminator().accept(this, null);
    }

    // actions

    public Void visit(final Void param, final BlockEntry node) {
        // no operation
        return null;
    }

    // terminators

    public Void visit(final Void param, final Goto node) {
        BasicBlock block = map(schedule.getBlockForNode(node));
        block.br(map(node.getResumeTarget()));
        return null;
    }

    public Void visit(final Void param, final If node) {
        BasicBlock block = map(schedule.getBlockForNode(node));
        block.br(map(node.getCondition()), map(node.getTrueBranch()), map(node.getFalseBranch()));
        return null;
    }

    public Void visit(final Void param, final Return node) {
        BasicBlock block = map(schedule.getBlockForNode(node));
        block.ret();
        return null;
    }

    public Void visit(final Void param, final ValueReturn node) {
        BasicBlock block = map(schedule.getBlockForNode(node));
        block.ret(map(node.getReturnValue().getType()), map(node.getReturnValue()));
        return null;
    }

    // values

    boolean isFloating(Type type) {
        return type instanceof FloatType;
    }

    boolean isSigned(Type type) {
        return type instanceof SignedIntegerType;
    }

    public Value visit(final Void param, final Add node) {
        Value inputType = map(node.getType());
        Value llvmLeft = map(node.getLeftInput());
        Value llvmRight = map(node.getRightInput());
        cc.quarkus.qcc.machine.llvm.BasicBlock target = map(schedule.getBlockForNode(node));
        return isFloating(node.getType()) ?
               target.fadd(inputType, llvmLeft, llvmRight).asLocal() :
               target.add(inputType, llvmLeft, llvmRight).asLocal();
    }

    public Value visit(final Void param, final And node) {
        Value llvmLeft = map(node.getLeftInput());
        Value llvmRight = map(node.getRightInput());
        return map(schedule.getBlockForNode(node)).and(map(node.getType()), llvmLeft, llvmRight).asLocal();
    }

    public Value visit(final Void param, final Or node) {
        Value llvmLeft = map(node.getLeftInput());
        Value llvmRight = map(node.getRightInput());
        return map(schedule.getBlockForNode(node)).or(map(node.getType()), llvmLeft, llvmRight).asLocal();
    }

    public Value visit(final Void param, final Xor node) {
        Value llvmLeft = map(node.getLeftInput());
        Value llvmRight = map(node.getRightInput());
        return map(schedule.getBlockForNode(node)).xor(map(node.getType()), llvmLeft, llvmRight).asLocal();
    }

    public Value visit(final Void param, final Multiply node) {
        Value inputType = map(node.getType());
        Value llvmLeft = map(node.getLeftInput());
        Value llvmRight = map(node.getRightInput());
        cc.quarkus.qcc.machine.llvm.BasicBlock target = map(schedule.getBlockForNode(node));
        return isFloating(node.getType()) ?
               target.fmul(inputType, llvmLeft, llvmRight).asLocal() :
               target.mul(inputType, llvmLeft, llvmRight).asLocal();
    }

    public Value visit(final Void param, final CmpEq node) {
        Value inputType = map(node.getType());
        Value llvmLeft = map(node.getLeftInput());
        Value llvmRight = map(node.getRightInput());
        cc.quarkus.qcc.machine.llvm.BasicBlock target = map(schedule.getBlockForNode(node));
        return isFloating(node.getType()) ?
               target.fcmp(FloatCondition.oeq, inputType, llvmLeft, llvmRight).asLocal() :
               target.icmp(IntCondition.eq, inputType, llvmLeft, llvmRight).asLocal();
    }

    public Value visit(final Void param, final CmpNe node) {
        Value inputType = map(node.getType());
        Value llvmLeft = map(node.getLeftInput());
        Value llvmRight = map(node.getRightInput());
        cc.quarkus.qcc.machine.llvm.BasicBlock target = map(schedule.getBlockForNode(node));
        return isFloating(node.getType()) ?
               target.fcmp(FloatCondition.one, inputType, llvmLeft, llvmRight).asLocal() :
               target.icmp(IntCondition.ne, inputType, llvmLeft, llvmRight).asLocal();
    }

    public Value visit(final Void param, final CmpLt node) {
        Value inputType = map(node.getType());
        Value llvmLeft = map(node.getLeftInput());
        Value llvmRight = map(node.getRightInput());
        cc.quarkus.qcc.machine.llvm.BasicBlock target = map(schedule.getBlockForNode(node));
        return isFloating(node.getType()) ?
               target.fcmp(FloatCondition.olt, inputType, llvmLeft, llvmRight).asLocal() :
                    isSigned(node.getType()) ?
                      target.icmp(IntCondition.slt, inputType, llvmLeft, llvmRight).asLocal() :
                      target.icmp(IntCondition.ult, inputType, llvmLeft, llvmRight).asLocal();
    }

    public Value visit(final Void param, final CmpLe node) {
        Value inputType = map(node.getType());
        Value llvmLeft = map(node.getLeftInput());
        Value llvmRight = map(node.getRightInput());
        cc.quarkus.qcc.machine.llvm.BasicBlock target = map(schedule.getBlockForNode(node));
        return isFloating(node.getType()) ?
               target.fcmp(FloatCondition.ole, inputType, llvmLeft, llvmRight).asLocal() :
                    isSigned(node.getType()) ?
                      target.icmp(IntCondition.sle, inputType, llvmLeft, llvmRight).asLocal() :
                      target.icmp(IntCondition.ule, inputType, llvmLeft, llvmRight).asLocal();
    }

    public Value visit(final Void param, final CmpGt node) {
        Value inputType = map(node.getType());
        Value llvmLeft = map(node.getLeftInput());
        Value llvmRight = map(node.getRightInput());
        cc.quarkus.qcc.machine.llvm.BasicBlock target = map(schedule.getBlockForNode(node));
        return isFloating(node.getType()) ?
               target.fcmp(FloatCondition.ogt, inputType, llvmLeft, llvmRight).asLocal() :
                    isSigned(node.getType()) ?
                      target.icmp(IntCondition.sgt, inputType, llvmLeft, llvmRight).asLocal() :
                      target.icmp(IntCondition.ugt, inputType, llvmLeft, llvmRight).asLocal();
    }

    public Value visit(final Void param, final CmpGe node) {
        Value inputType = map(node.getType());
        Value llvmLeft = map(node.getLeftInput());
        Value llvmRight = map(node.getRightInput());
        cc.quarkus.qcc.machine.llvm.BasicBlock target = map(schedule.getBlockForNode(node));
        return isFloating(node.getType()) ?
               target.fcmp(FloatCondition.oge, inputType, llvmLeft, llvmRight).asLocal() :
                    isSigned(node.getType()) ?
                      target.icmp(IntCondition.sge, inputType, llvmLeft, llvmRight).asLocal() :
                      target.icmp(IntCondition.uge, inputType, llvmLeft, llvmRight).asLocal();
    }

    public Value visit(final Void param, final Select node) {
        cc.quarkus.qcc.graph.Value trueValue = node.getTrueValue();
        Value inputType = map(trueValue.getType());
        cc.quarkus.qcc.graph.Value falseValue = node.getFalseValue();
        return map(schedule.getBlockForNode(node)).select(map(node.getCondition().getType()), map(node.getCondition()), inputType, map(trueValue), map(falseValue)).asLocal();
    }

    public Value visit(final Void param, final Catch node) {
        // todo: landingpad
        return null;
    }

    public Value visit(final Void param, final PhiValue node) {
        Phi phi = map(schedule.getBlockForNode(node)).phi(map(node.getType()));
        mappedValues.put(node, phi.asLocal());
        for (cc.quarkus.qcc.graph.BasicBlock knownBlock : knownBlocks) {
            cc.quarkus.qcc.graph.Value v = node.getValueForBlock(knownBlock);
            if (v != null) {
                // process dependencies
                v.accept(this, param);
                phi.item(map(v), map(knownBlock));
            }
        }
        return phi.asLocal();
    }

    public Value visit(final Void param, final Neg node) {
        Type javaInputType = node.getInput().getType();
        Value inputType = map(javaInputType);
        Value llvmInput = map(node.getInput());
        cc.quarkus.qcc.machine.llvm.BasicBlock target = map(schedule.getBlockForNode(node));
        return target.fneg(inputType, llvmInput).asLocal();
    }

    public Value visit(final Void param, final Shr node) {
        Value inputType = map(node.getType());
        Value llvmLeft = map(node.getLeftInput());
        Value llvmRight = map(node.getRightInput());
        cc.quarkus.qcc.machine.llvm.BasicBlock target = map(schedule.getBlockForNode(node));
        return isSigned(node.getType()) ?
               target.ashr(inputType, llvmLeft, llvmRight).asLocal() :
               target.lshr(inputType, llvmLeft, llvmRight).asLocal();
    }

    public Value visit(final Void param, final Shl node) {
        Value llvmLeft = map(node.getLeftInput());
        Value llvmRight = map(node.getRightInput());
        return map(schedule.getBlockForNode(node)).shl(map(node.getType()), llvmLeft, llvmRight).asLocal();
    }

    public Value visit(final Void param, final Sub node) {
        Value llvmLeft = map(node.getLeftInput());
        Value llvmRight = map(node.getRightInput());
        return map(schedule.getBlockForNode(node)).sub(map(node.getType()), llvmLeft, llvmRight).asLocal();
    }

    public Value visit(final Void param, final Div node) {
        Value inputType = map(node.getType());
        Value llvmLeft = map(node.getLeftInput());
        Value llvmRight = map(node.getRightInput());
        cc.quarkus.qcc.machine.llvm.BasicBlock target = map(schedule.getBlockForNode(node));
        return isFloating(node.getType()) ?
               target.fdiv(inputType, llvmLeft, llvmRight).asLocal() :
                    isSigned(node.getType()) ?
                      target.sdiv(inputType, llvmLeft, llvmRight).asLocal() :
                      target.udiv(inputType, llvmLeft, llvmRight).asLocal();
    }

    public Value visit(final Void param, final Mod node) {
        Value inputType = map(node.getType());
        Value llvmLeft = map(node.getLeftInput());
        Value llvmRight = map(node.getRightInput());
        cc.quarkus.qcc.machine.llvm.BasicBlock target = map(schedule.getBlockForNode(node));
        return isFloating(node.getType()) ?
               target.frem(inputType, llvmLeft, llvmRight).asLocal() :
                    isSigned(node.getType()) ?
                      target.srem(inputType, llvmLeft, llvmRight).asLocal() :
                      target.urem(inputType, llvmLeft, llvmRight).asLocal();
    }

    public Value visit(final Void param, final BitCast node) {
        Type javaInputType = node.getInput().getType();
        Type javaOutputType = node.getType();
        Value inputType = map(javaInputType);
        Value outputType = map(javaOutputType);
        Value llvmInput = map(node.getInput());
        cc.quarkus.qcc.machine.llvm.BasicBlock target = map(schedule.getBlockForNode(node));
        return target.bitcast(inputType, llvmInput, outputType).asLocal();
    }

    public Value visit(final Void param, final Convert node) {
        Type javaInputType = node.getInput().getType();
        Type javaOutputType = node.getType();
        Value inputType = map(javaInputType);
        Value outputType = map(javaOutputType);
        Value llvmInput = map(node.getInput());
        cc.quarkus.qcc.machine.llvm.BasicBlock target = map(schedule.getBlockForNode(node));
        return isFloating(javaInputType) ?
                    isSigned(javaOutputType) ?
                    target.fptosi(inputType, llvmInput, outputType).asLocal() :
                    target.fptoui(inputType, llvmInput, outputType).asLocal() :
                    isSigned(javaInputType) ?
                    target.sitofp(inputType, llvmInput, outputType).asLocal() :
                    target.uitofp(inputType, llvmInput, outputType).asLocal();
    }

    public Value visit(final Void param, final Extend node) {
        Type javaInputType = node.getInput().getType();
        Type javaOutputType = node.getType();
        Value inputType = map(javaInputType);
        Value outputType = map(javaOutputType);
        Value llvmInput = map(node.getInput());
        cc.quarkus.qcc.machine.llvm.BasicBlock target = map(schedule.getBlockForNode(node));
        return isFloating(javaInputType) ?
               target.fpext(inputType, llvmInput, outputType).asLocal() :
                    isSigned(javaInputType) ?
                    target.sext(inputType, llvmInput, outputType).asLocal() :
                    target.zext(inputType, llvmInput, outputType).asLocal();
    }

    public Value visit(final Void param, final Truncate node) {
        Type javaInputType = node.getInput().getType();
        Type javaOutputType = node.getType();
        Value inputType = map(javaInputType);
        Value outputType = map(javaOutputType);
        Value llvmInput = map(node.getInput());
        cc.quarkus.qcc.machine.llvm.BasicBlock target = map(schedule.getBlockForNode(node));
        return isFloating(javaInputType) ?
               target.ftrunc(inputType, llvmInput, outputType).asLocal() :
               target.trunc(inputType, llvmInput, outputType).asLocal();
    }

    // literals

    public Value visit(final Void param, final IntegerLiteral node) {
        return Values.intConstant(node.longValue());
    }

    // unknown node catch-all methods

    public Value visitUnknown(final Void param, final cc.quarkus.qcc.graph.Value node) {
        ctxt.error(element, node, "llvm: Unrecognized value %s", node.getClass());
        return LLVM.FALSE;
    }

    public Void visitUnknown(final Void param, final Action node) {
        ctxt.error(element, node, "llvm: Unrecognized action %s", node.getClass());
        return null;
    }

    public Void visitUnknown(final Void param, final Terminator node) {
        ctxt.error(element, node, "llvm: Unrecognized terminator %s", node.getClass());
        return null;
    }

    // mapping

    private BasicBlock map(cc.quarkus.qcc.graph.BasicBlock block) {
        BasicBlock mapped = mappedBlocks.get(block);
        if (mapped != null) {
            return mapped;
        }
        mapped = func.createBlock();
        mappedBlocks.put(block, mapped);
        block.getTerminator().accept(this, null);
        return mapped;
    }

    private Value map(Type type) {
        // todo: one global type registry?
        Value res = types.get(type);
        if (res != null) {
            return res;
        }
        if (type instanceof VoidType) {
            res = void_;
        } else if (type instanceof BooleanType) {
            // todo: sometimes it's one byte instead
            res = i1;
        } else if (type instanceof IntegerType) {
            // LLVM doesn't really care about signedness
            int bytes = (int) ((IntegerType) type).getSize();
            if (bytes == 1) {
                res = i8;
            } else if (bytes == 2) {
                res = i16;
            } else if (bytes == 4) {
                res = i32;
            } else if (bytes == 8) {
                res = i64;
            } else {
                throw Assert.unreachableCode();
            }
        } else if (type instanceof FloatType) {
            int bytes = (int) ((FloatType) type).getSize();
            if (bytes == 4) {
                res = float32;
            } else if (bytes == 8) {
                res = float64;
            } else {
                throw Assert.unreachableCode();
            }
        } else if (type instanceof ReferenceType) {
            // todo: lower class types to ref types at some earlier point
            res = ptrTo(i8);
        } else {
            throw new IllegalStateException();
        }
        types.put(type, res);
        return res;
    }



    private Value map(cc.quarkus.qcc.graph.Value value) {
        Value mapped = mappedValues.get(value);
        if (mapped != null) {
            return mapped;
        }
        mapped = value.accept(this, null);
        mappedValues.put(value, mapped);
        return mapped;
    }
}
