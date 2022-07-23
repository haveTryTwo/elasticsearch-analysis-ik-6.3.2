/**
 * IK 中文分词  版本 5.0
 * IK Analyzer release 5.0
 * 
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * 源代码由林良益(linliangyi2005@gmail.com)提供
 * 版权声明 2012，乌龙茶工作室
 * provided by Linliangyi and copyright 2012 by Oolong studio
 */
package org.wltea.analyzer.core;

import org.wltea.analyzer.cfg.Configuration;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * IK分词器主类
 *
 */
public final class IKSegmenter { // NOTE:htt, ik分词器主类
	
	//字符窜reader
	private Reader input; // NOTE:htt, 待读取的input
	//分词器上下文
	private AnalyzeContext context;  // NOTE:htt, 分词上下文内容
	//分词处理器列表
	private List<ISegmenter> segmenters; // NOTE:htt, 分词器处理列表
	//分词歧义裁决器
	private IKArbitrator arbitrator; // NOTE:Htt, IK分词歧义处理器
    private  Configuration configuration; // NOTE:htt, ik配置，包括是否启用smart机制
	

	/**
	 * IK分词器构造函数
	 * @param input
     */
	public IKSegmenter(Reader input ,Configuration configuration){
		this.input = input; // NOTE:htt, 数据读入input
        this.configuration = configuration;
        this.init(); // NOTEhtt, ik分词器初始化
	}

	
	/**
	 * 初始化
	 */
	private void init(){ // NOTEhtt, ik分词器初始化
		//初始化分词上下文
		this.context = new AnalyzeContext(configuration);
		//加载子分词器
		this.segmenters = this.loadSegmenters(); // NOTE:htt, 加载分词器
		//加载歧义裁决器
		this.arbitrator = new IKArbitrator(); // NOTE:Htt, IK分词歧义处理器
	}
	
	/**
	 * 初始化词典，加载子分词器实现
	 * @return List<ISegmenter>
	 */
	private List<ISegmenter> loadSegmenters(){ // NOTE:htt, 加载分词器
		List<ISegmenter> segmenters = new ArrayList<ISegmenter>(4);
		//处理字母的子分词器
		segmenters.add(new LetterSegmenter());  // NOTE:htt, 英文字母和数字分词器
		//处理中文数量词的子分词器
		segmenters.add(new CN_QuantifierSegmenter()); // NOTE:htt, 分析中文量词
		//处理中文词的子分词器
		segmenters.add(new CJKSegmenter()); // NOTE:htt, 中日韩分词
		return segmenters;
	}
	
	/**
	 * 分词，获取下一个词元
	 * @return Lexeme 词元对象
	 * @throws java.io.IOException
	 */
	public synchronized Lexeme next()throws IOException{ // NOTE:htt, 获取下一个result词元，如果启动smart则会组合，如果为停用词则继续查找
		Lexeme l = null;
		while((l = context.getNextLexeme()) == null ){ // NOTE:htt, 获取下一个result词元，如果启动smart则会组合，如果为停用词则继续查找
			/*
			 * 从reader中读取数据，填充buffer
			 * 如果reader是分次读入buffer的，那么buffer要  进行移位处理
			 * 移位处理上次读入的但未处理的数据
			 */
			int available = context.fillBuffer(this.input); // NOTE:htt, 添加读取的内容
			if(available <= 0){
				//reader已经读完
				context.reset();
				return null;
				
			}else{
				//初始化指针
				context.initCursor(); // NOTE:htt, 处理第一个字符
				do{
        			//遍历子分词器
        			for(ISegmenter segmenter : segmenters){
        				segmenter.analyze(context); // NOTE:htt, 英文字母、中文量词、中日韩分词进行分析
        			}
        			//字符缓冲区接近读完，需要读入新的字符
        			if(context.needRefillBuffer()){ // NOTE:htt, 要取新数据则break
        				break;
        			}
   				//向前移动指针
				}while(context.moveCursor()); // NOTE:htt, 移动游标，直到需要读取新数据
				//重置子分词器，为下轮循环进行初始化
				for(ISegmenter segmenter : segmenters){
					segmenter.reset();
				}
			}
			//对分词进行歧义处理
			this.arbitrator.process(context, configuration.isUseSmart()); // NOTE:htt, 处理词元，如果有冲突但未启用smart则不处理；否则组合词元选择最优方案
			//将分词结果输出到结果集，并处理未切分的单个CJK字符
			context.outputToResult(); // NOTE:htt, 输出 [index, cursor]之间的分词
			//记录本次分词的缓冲区位移
			context.markBufferOffset(); // NOTE:htt, 累积当前的segmentbuff相对于reader起始的累积位置
		}
		return l;
	}

	/**
     * 重置分词器到初始状态
     * @param input
     */
	public synchronized void reset(Reader input) { // NOTE:htt, 重置
		this.input = input;
		context.reset();
		for(ISegmenter segmenter : segmenters){
			segmenter.reset();
		}
	}
}
