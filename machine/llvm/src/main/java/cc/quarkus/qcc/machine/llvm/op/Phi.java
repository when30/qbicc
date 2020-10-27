package cc.quarkus.qcc.machine.llvm.op;

import cc.quarkus.qcc.machine.llvm.LLBasicBlock;
import cc.quarkus.qcc.machine.llvm.LLValue;

/**
 *
 */
public interface Phi extends YieldingInstruction {
    Phi meta(String name, LLValue data);

    Phi comment(String comment);

    Phi item(LLValue data, LLBasicBlock incoming);
}
