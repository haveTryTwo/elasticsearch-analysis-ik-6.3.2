package org.elasticsearch.index.analysis;

import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.lucene.IKAnalyzer;

public class IkAnalyzerProvider extends AbstractIndexAnalyzerProvider<IKAnalyzer> { // NOTE:htt, ik分析器 provider
    private final IKAnalyzer analyzer; // NOTE:htt, ik分析器

    public IkAnalyzerProvider(IndexSettings indexSettings, Environment env, String name, Settings settings,boolean useSmart) {
        super(indexSettings, name, settings);

        Configuration configuration=new Configuration(env,settings).setUseSmart(useSmart); // NOTE:htt, ik配置，包括是否启用smart机制

        analyzer=new IKAnalyzer(configuration); // NOTE:htt, ik分析器
    }

    public static IkAnalyzerProvider getIkSmartAnalyzerProvider(IndexSettings indexSettings, Environment env, String name, Settings settings) {
        return new IkAnalyzerProvider(indexSettings,env,name,settings,true); // NOTE:htt, 采用精简方式分词
    }

    public static IkAnalyzerProvider getIkAnalyzerProvider(IndexSettings indexSettings, Environment env, String name, Settings settings) {
        return new IkAnalyzerProvider(indexSettings,env,name,settings,false); // NOTE:htt, 采用最大词方式分词
    }

    @Override public IKAnalyzer get() {
        return this.analyzer;
    }
}
