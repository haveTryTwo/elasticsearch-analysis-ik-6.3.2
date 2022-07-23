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
 * 
 */
package org.wltea.analyzer.core;

import java.util.Stack;
import java.util.TreeSet;

/**
 * IK分词歧义裁决器
 */
class IKArbitrator { // NOTE:Htt, IK分词歧义处理器

	IKArbitrator(){
		
	}
	
	/**
	 * 分词歧义处理
//	 * @param orgLexemes
	 * @param useSmart
	 */
	void process(AnalyzeContext context , boolean useSmart){ // NOTE:htt, 处理词元，如果有冲突但未启用smart则不处理；否则组合词元选择最优方案
		QuickSortSet orgLexemes = context.getOrgLexemes();
		Lexeme orgLexeme = orgLexemes.pollFirst();
		
		LexemePath crossPath = new LexemePath();
		while(orgLexeme != null){
			if(!crossPath.addCrossLexeme(orgLexeme)){ // NOTE:htt, 如果词元没有重叠，则根据未启用smart则添加，否则词元组合最优选择并添加
				//找到与crossPath不相交的下一个crossPath	
				if(crossPath.size() == 1 || !useSmart){
					//crossPath没有歧义 或者 不做歧义处理
					//直接输出当前crossPath
					context.addLexemePath(crossPath);
				}else{ // NOTE:htt, 如果启用smart，则进行冲突识别，选择最合适组合词元
					//对当前的crossPath进行歧义处理
					QuickSortSet.Cell headCell = crossPath.getHead();
					LexemePath judgeResult = this.judge(headCell, crossPath.getPathLength()); // NOTE:htt, 歧义识别，并获取最优方案
					//输出歧义处理结果judgeResult
					context.addLexemePath(judgeResult);
				}
				
				//把orgLexeme加入新的crossPath中
				crossPath = new LexemePath();
				crossPath.addCrossLexeme(orgLexeme);
			}
			orgLexeme = orgLexemes.pollFirst(); // NOTE:htt, 继续下一个词元
		}
		
		
		//处理最后的path
		if(crossPath.size() == 1 || !useSmart){
			//crossPath没有歧义 或者 不做歧义处理
			//直接输出当前crossPath
			context.addLexemePath(crossPath);
		}else{
			//对当前的crossPath进行歧义处理
			QuickSortSet.Cell headCell = crossPath.getHead();
			LexemePath judgeResult = this.judge(headCell, crossPath.getPathLength());
			//输出歧义处理结果judgeResult
			context.addLexemePath(judgeResult);
		}
	}
	
	/**
	 * 歧义识别
	 * @param lexemeCell 歧义路径链表头
	 * @param fullTextLength 歧义路径文本长度
	 * @return
	 */
	private LexemePath judge(QuickSortSet.Cell lexemeCell , int fullTextLength){ // NOTE:htt, 歧义识别，并获取最优方案
		//候选路径集合
		TreeSet<LexemePath> pathOptions = new TreeSet<LexemePath>();
		//候选结果路径
		LexemePath option = new LexemePath();
		
		//对crossPath进行一次遍历,同时返回本次遍历中有冲突的Lexeme栈
		Stack<QuickSortSet.Cell> lexemeStack = this.forwardPath(lexemeCell , option); // NOTE:htt, 添加不冲突词元到option，并获取有冲突词元
		
		//当前词元链并非最理想的，加入候选路径集合
		pathOptions.add(option.copy());
		
		//存在歧义词，处理
		QuickSortSet.Cell c = null;
		while(!lexemeStack.isEmpty()){
			c = lexemeStack.pop();
			//回滚词元链
			this.backPath(c.getLexeme() , option); // NOTE:htt, 回滚option词元，直到可以接受冲突词元l
			//从歧义词位置开始，递归，生成可选方案
			this.forwardPath(c , option); // NOTE:htt, 从冲突词元开始继续添加词元，生成备选方案
			pathOptions.add(option.copy());
		}
		
		//返回集合中的最优方案
		return pathOptions.first(); // NOTE:htt, 从方案中选择最优
	}
	
	/**
	 * 向前遍历，添加词元，构造一个无歧义词元组合
//	 * @param LexemePath path
	 * @return
	 */
	private Stack<QuickSortSet.Cell> forwardPath(QuickSortSet.Cell lexemeCell , LexemePath option){ // NOTE:htt, 添加不冲突词元到option，如果有冲突则返回
		//发生冲突的Lexeme栈
		Stack<QuickSortSet.Cell> conflictStack = new Stack<QuickSortSet.Cell>();
		QuickSortSet.Cell c = lexemeCell;
		//迭代遍历Lexeme链表
		while(c != null && c.getLexeme() != null){
			if(!option.addNotCrossLexeme(c.getLexeme())){ // NOTE:htt, 如果添加不相交词元失败，则添加到conflict中
				//词元交叉，添加失败则加入lexemeStack栈
				conflictStack.push(c);
			}
			c = c.getNext();
		}
		return conflictStack;
	}
	
	/**
	 * 回滚词元链，直到它能够接受指定的词元
//	 * @param lexeme
	 * @param l
	 */
	private void backPath(Lexeme l, LexemePath option){ // NOTE:htt, 回滚option词元，直到可以接受冲突词元l
		while(option.checkCross(l)){
			option.removeTail();
		}
		
	}
	
}
