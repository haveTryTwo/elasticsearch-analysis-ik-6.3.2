/**
 * IK 中文分词  版本 5.0.1
 * IK Analyzer release 5.0.1
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

 * 
 */
package org.wltea.analyzer.lucene;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.core.IKSegmenter;
import org.wltea.analyzer.core.Lexeme;

import java.io.IOException;
import java.io.Reader;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

/**
 * IK分词器 Lucene Tokenizer适配器类
 * 兼容Lucene 4.0版本
 */
public final class IKTokenizer extends Tokenizer { // NOTE:htt, ik 分词器
	
	//IK分词器实现
	private IKSegmenter _IKImplement; // NOTE:htt, ik分词器主类
	
	//词元文本属性
	private final CharTermAttribute termAtt;
	//词元位移属性
	private final OffsetAttribute offsetAtt;
	//词元分类属性（该属性分类参考org.wltea.analyzer.core.Lexeme中的分类常量）
	private final TypeAttribute typeAtt;
	//记录最后一个词元的结束位置
	private int endPosition; // NOTE:htt, 记录最后一个词元在整个数据中的结束位置

   	private int skippedPositions; // TODO:htt, check

   	private PositionIncrementAttribute posIncrAtt;


    /**
	 * Lucene 4.0 Tokenizer适配器类构造函数
     */
	public IKTokenizer(Configuration configuration){
	    super();
	    offsetAtt = addAttribute(OffsetAttribute.class);
	    termAtt = addAttribute(CharTermAttribute.class);
	    typeAtt = addAttribute(TypeAttribute.class);
        posIncrAtt = addAttribute(PositionIncrementAttribute.class);

        _IKImplement = new IKSegmenter(input,configuration); // NOTE:htt, ik分词器主类
	}

	/* (non-Javadoc)
	 * @see org.apache.lucene.analysis.TokenStream#incrementToken()
	 */
	@Override
	public boolean incrementToken() throws IOException { // NOTE:htt, 增加token，从输入数据中获取下一个分词
		//清除所有的词元属性
		clearAttributes();
        skippedPositions = 0;

        Lexeme nextLexeme = _IKImplement.next(); // NOTE:htt, 获取下一个result词元，如果启动smart则会组合，如果为停用词则继续查找
		if(nextLexeme != null){
            posIncrAtt.setPositionIncrement(skippedPositions +1 );

			//将Lexeme转成Attributes
			//设置词元文本
			termAtt.append(nextLexeme.getLexemeText()); // NOTE:htt, 添加分词
			//设置词元长度
			termAtt.setLength(nextLexeme.getLength()); // NOTE:htt, 词元长度
			//设置词元位移
            offsetAtt.setOffset(correctOffset(nextLexeme.getBeginPosition()), correctOffset(nextLexeme.getEndPosition())); // NOTE:htt, 词元位移属性

            //记录分词的最后位置
			endPosition = nextLexeme.getEndPosition();
			//记录词元分类
			typeAtt.setType(nextLexeme.getLexemeTypeString());			
			//返会true告知还有下个词元
			return true;
		}
		//返会false告知词元输出完毕
		return false;
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.apache.lucene.analysis.Tokenizer#reset(java.io.Reader)
	 */
	@Override
	public void reset() throws IOException {
		super.reset();
		_IKImplement.reset(input);
        skippedPositions = 0;
	}	
	
	@Override
	public final void end() throws IOException {
        super.end();
	    // set final offset
		int finalOffset = correctOffset(this.endPosition);
		offsetAtt.setOffset(finalOffset, finalOffset);
        posIncrAtt.setPositionIncrement(posIncrAtt.getPositionIncrement() + skippedPositions);
	}
}
