package dev.rylex.nep.client;

import dev.rylex.nep.guide.NepConfigValues;
import guideme.compiler.PageCompiler;
import guideme.compiler.tags.FlowTagCompiler;
import guideme.document.flow.LytFlowParent;
import guideme.libs.mdast.mdx.model.MdxJsxElementFields;
import java.util.Set;

public final class NepConfigValueTag extends FlowTagCompiler {

    public static final String TAG = "nep:ConfigValue";

    @Override
    public Set<String> getTagNames() {
        return Set.of(TAG);
    }

    @Override
    protected void compile(PageCompiler compiler, LytFlowParent parent, MdxJsxElementFields el) {
        String name = el.getAttributeString("name", "");
        if (name.isEmpty()) {
            parent.appendError(compiler, TAG + " needs a name", el);
            return;
        }
        String value = NepConfigValues.format(name);
        if (value == null) {
            parent.appendError(compiler, "no nep setting is named '" + name + "'", el);
            return;
        }
        parent.appendText(value);
    }
}
