package org.qbicc.plugin.llvm;

import org.qbicc.context.CompilationContext;
import org.qbicc.type.definition.DefinedTypeDefinition;

import java.nio.file.Path;
import java.util.function.Consumer;

public class LLVMDefaultModuleCompileStage implements Consumer<CompilationContext> {
    private final boolean isPie;
    private final boolean compileOutput;

    public LLVMDefaultModuleCompileStage(boolean isPie, boolean compileOutput) {
        this.isPie = isPie;
        this.compileOutput = compileOutput;
    }

    @Override
    public void accept(CompilationContext context) {
        LLVMModuleGenerator generator = new LLVMModuleGenerator(context, isPie ? 2 : 0, isPie ? 2 : 0);
        DefinedTypeDefinition defaultTypeDefinition = context.getDefaultTypeDefinition();
        Path modulePath = generator.processProgramModule(context.getOrAddProgramModule(defaultTypeDefinition));
        if (compileOutput) {
            LLVMCompiler compiler = new LLVMCompiler(context, isPie);
            compiler.compileModule(context, defaultTypeDefinition.load(), modulePath);
        }
    }
}
