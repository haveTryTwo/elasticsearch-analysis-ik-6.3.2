package org.elasticsearch.plugin.analysis.ik;

import org.apache.lucene.analysis.Analyzer;
import org.elasticsearch.index.analysis.AnalyzerProvider;
import org.elasticsearch.index.analysis.IkAnalyzerProvider;
import org.elasticsearch.index.analysis.IkTokenizerFactory;
import org.elasticsearch.index.analysis.TokenizerFactory;
import org.elasticsearch.indices.analysis.AnalysisModule;
import org.elasticsearch.plugins.AnalysisPlugin;
import org.elasticsearch.plugins.Plugin;

import java.util.HashMap;
import java.util.Map;


public class AnalysisIkPlugin extends Plugin implements AnalysisPlugin { // NOTE:htt, 分析插件

	public static String PLUGIN_NAME = "analysis-ik";

    @Override
    public Map<String, AnalysisModule.AnalysisProvider<TokenizerFactory>> getTokenizers() { // NOTE:htt, 只有在涉及到分词时才会加载；如果没有移动索引则不会加载；
        Map<String, AnalysisModule.AnalysisProvider<TokenizerFactory>> extra = new HashMap<>();


        extra.put("ik_smart", IkTokenizerFactory::getIkSmartTokenizerFactory); // NOTE:htt, ik分词器，采用精简方式，创建工厂
        extra.put("ik_max_word", IkTokenizerFactory::getIkTokenizerFactory); // NOTE:htt, ik分词器，非精简方式，创建工厂

        return extra;
    }

    @Override
    public Map<String, AnalysisModule.AnalysisProvider<AnalyzerProvider<? extends Analyzer>>> getAnalyzers() { // NOTE:htt, 只有在涉及到分析器时才会加载；如果没有移动索引则不会加载；
        Map<String, AnalysisModule.AnalysisProvider<AnalyzerProvider<? extends Analyzer>>> extra = new HashMap<>();

        extra.put("ik_smart", IkAnalyzerProvider::getIkSmartAnalyzerProvider); // NOTE:htt, 采用精简方式分词
        extra.put("ik_max_word", IkAnalyzerProvider::getIkAnalyzerProvider); // NOTE:htt, 采用最多词方式进行分词

        return extra;
    }

}
