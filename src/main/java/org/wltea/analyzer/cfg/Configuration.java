/**
 * 
 */
package org.wltea.analyzer.cfg;

import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.io.PathUtils;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.plugin.analysis.ik.AnalysisIkPlugin;
import org.wltea.analyzer.dic.Dictionary;

import java.io.File;
import java.nio.file.Path;

public class Configuration { // NOTE:htt, ik配置，包括是否启用smart机制

	private Environment environment;
	private Settings settings;

	//是否启用智能分词
	private  boolean useSmart; // NOTE:htt, 是否启用smart机制，默认是不启用

	//是否启用远程词典加载
	private boolean enableRemoteDict=false; // NOTE:htt, 默认启用远程词典选项为true

	//是否启用小写处理
	private boolean enableLowercase=true;

	//是否开启自动检查词库变更
	private boolean enableAutoCheckDict = true; // NOTE:htt, 默认为true

	@Inject
	public Configuration(Environment env,Settings settings) {
		this.environment = env;
		this.settings=settings;

		this.useSmart = settings.get("use_smart", "false").equals("true");
		this.enableLowercase = settings.get("enable_lowercase", "true").equals("true");
		this.enableRemoteDict = settings.get("enable_remote_dict", "true").equals("true");
		this.enableAutoCheckDict = settings.get("enable_autocheck_dict", "true").equals("true");

		Dictionary.initial(this);  // NOTE:htt, 初始化词库，包括本次词库，远程词库

	}

	public Path getConfigInPluginDir() { // NOTE:htt, 获取ik配置目录
		return PathUtils
				.get(new File(AnalysisIkPlugin.class.getProtectionDomain().getCodeSource().getLocation().getPath())
						.getParent(), "config")
				.toAbsolutePath();
	}

	public boolean isUseSmart() {
		return useSmart;
	}

	public Configuration setUseSmart(boolean useSmart) {
		this.useSmart = useSmart;
		return this;
	}

	public Environment getEnvironment() {
		return environment;
	}

	public Settings getSettings() {
		return settings;
	}

	public boolean isEnableRemoteDict() {
		return enableRemoteDict;
	}

	public boolean isEnableLowercase() {
		return enableLowercase;
	}

	public boolean isEnableAutoCheckDict() {
		return enableAutoCheckDict;
	}
}
