package com.buschmais.jqassistant.core.analysis.api;

import com.buschmais.jqassistant.core.model.api.rule.RuleSet;
import com.buschmais.jqassistant.core.report.api.ReportWriterException;

/**
 * Defines the interface for the constraint analyzer.
 */
public interface Analyzer {

    /**
     * Executes the given rules set.
     *
     * @param ruleSet The rules set.
     * @throws ReportWriterException If the report cannot be written.
     */
    void execute(RuleSet ruleSet) throws AnalyzerException;

}
