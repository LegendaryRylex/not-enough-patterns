package dev.rylex.nep.pattern.encoding;

import appeng.api.stacks.GenericStack;
import java.util.List;

public record EncodedIngredients(List<List<GenericStack>> inputs, List<GenericStack> outputs) {}
