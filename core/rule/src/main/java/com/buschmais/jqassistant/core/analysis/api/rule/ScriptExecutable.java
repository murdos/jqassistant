package com.buschmais.jqassistant.core.analysis.api.rule;

import com.buschmais.jqassistant.core.rule.impl.SourceExecutable;
import com.buschmais.jqassistant.core.shared.annotation.ToBeRemovedInVersion;

/**
 * Represents an executable script.
 */
@Deprecated
@ToBeRemovedInVersion(major = 1, minor = 5)
public class ScriptExecutable extends SourceExecutable<String> {

    public ScriptExecutable(String language, String source) {
        super(language, source);
    }
}
