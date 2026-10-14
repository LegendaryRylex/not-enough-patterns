package dev.rylex.nep.compat.create;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import java.util.List;

record MatrixTemplate(List<GenericStack> stacks) {
    MatrixTemplate {
        stacks = List.copyOf(stacks);
    }

    List<AEKey> keys() {
        return stacks.stream().map(GenericStack::what).toList();
    }

    boolean isEmpty() {
        return stacks.isEmpty();
    }
}
