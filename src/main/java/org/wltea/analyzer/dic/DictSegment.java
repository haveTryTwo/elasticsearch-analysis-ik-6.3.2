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
 * http://www.apache.org/licenses/LICENSE-2.0
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

package org.wltea.analyzer.dic;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 词典树分段，表示词典树的一个分枝
 */
class DictSegment implements Comparable<DictSegment> { // NOTE:htt, 词典树，用于词典查找

    //公用字典表，存储汉字
    private static final Map<Character, Character> charMap = new ConcurrentHashMap<Character, Character>(16, 0.95f);
    //数组大小上限
    private static final int ARRAY_LENGTH_LIMIT = 3;

    //Map存储结构
    private Map<Character, DictSegment> childrenMap; // NOTE:htt, 采用map结构保持字符
    //数组方式存储结构
    private DictSegment[] childrenArray; // NOTE:htt, 采用数组结构保持字符


    //当前节点上存储的字符
    private final Character nodeChar; // NOTE:htt, 记录当前存储的 字符， 如："石"
    //当前节点存储的Segment数目
    //storeSize <=ARRAY_LENGTH_LIMIT ，使用数组存储， storeSize >ARRAY_LENGTH_LIMIT ,则使用Map存储
    private int storeSize = 0;
    //当前DictSegment状态 ,默认 0 , 1表示从根节点到当前节点的路径表示一个词
    private int nodeState = 0;    // NOTE:htt, 0表示为词的中间，1表示词已经到达结尾;末尾字符，如果为1代表完整词并可以使用，为0则为完整的词但是被屏蔽


    DictSegment(Character nodeChar) {
        if (nodeChar == null) {
            throw new IllegalArgumentException("参数为空异常，字符不能为空");
        }
        this.nodeChar = nodeChar;
    }

    Character getNodeChar() {
        return nodeChar;
    }

    /*
     * 判断是否有下一个节点
     */
    boolean hasNextNode() { // NOTE:htt, 当前字符节点是否存储子字符串，即前缀树的后一部分
        return this.storeSize > 0;
    }

    /**
     * 匹配词段
     * @param charArray
     * @return Hit
     */
    Hit match(char[] charArray) {
        return this.match(charArray, 0, charArray.length, null);
    }

    /**
     * 匹配词段
     * @param charArray
     * @param begin
     * @param length
     * @return Hit
     */
    Hit match(char[] charArray, int begin, int length) {
        return this.match(charArray, begin, length, null);
    }

    /**
     * 匹配词段
     * @param charArray
     * @param begin
     * @param length
     * @param searchHit
     * @return Hit
     */
    Hit match(char[] charArray, int begin, int length,
            Hit searchHit) { // NOTE:htt, [begin, begin+lenght]查找该范围内的charArray是否有对应满足数据

        if (searchHit == null) {
            //如果hit为空，新建
            searchHit = new Hit();
            //设置hit的其实文本位置
            searchHit.setBegin(begin); // NOTE:htt, 记录首次进入的begin位置
        } else {
            //否则要将HIT状态重置
            searchHit.setUnmatch(); // NOTE:htt, 重置match状态
        }
        //设置hit的当前处理位置
        searchHit.setEnd(begin); // NOTE:htt, 记录当前查找的位置

        Character keyChar = Character.valueOf(charArray[begin]);
        DictSegment ds = null;

        //引用实例变量为本地变量，避免查询时遇到更新的同步问题
        DictSegment[] segmentArray = this.childrenArray;
        Map<Character, DictSegment> segmentMap = this.childrenMap;

        //STEP1 在节点中查找keyChar对应的DictSegment
        if (segmentArray != null) {
            //在数组中查找
            DictSegment keySegment = new DictSegment(keyChar);
            int position = Arrays.binarySearch(segmentArray, 0, this.storeSize, keySegment);
            if (position >= 0) {
                ds = segmentArray[position];
            }

        } else if (segmentMap != null) {
            //在map中查找
            ds = segmentMap.get(keyChar);
        }

        //STEP2 找到DictSegment，判断词的匹配状态，是否继续递归，还是返回结果
        if (ds != null) {
            if (length > 1) {
                //词未匹配完，继续往下搜索
                return ds.match(charArray, begin + 1, length - 1, searchHit); // NOTE:htt, 继续查找
            } else if (length == 1) {

                //搜索最后一个char
                if (ds.nodeState == 1) { // NOTE:htt, 如果为完整的词，并且启动则匹配
                    //添加HIT状态为完全匹配
                    searchHit.setMatch();
                }
                if (ds.hasNextNode()) { // NOTE:htt, 如果有子字符串，则对应为前缀
                    //添加HIT状态为前缀匹配
                    searchHit.setPrefix();
                    //记录当前位置的DictSegment
                    searchHit.setMatchedDictSegment(ds); // NOTE:htt, 记录当前找到的路径
                }
                return searchHit;
            }

        }
        //STEP3 没有找到DictSegment， 将HIT设置为不匹配
        return searchHit;
    }

    /**
     * 加载填充词典片段
     * @param charArray
     */
    void fillSegment(char[] charArray) { // NOTE:htt, 添加填充的词
        this.fillSegment(charArray, 0, charArray.length, 1);
    }

    /**
     * 屏蔽词典中的一个词
     * @param charArray
     */
    void disableSegment(char[] charArray) { // NOTE:htt, 添加屏蔽的词
        this.fillSegment(charArray, 0, charArray.length, 0);
    }

    /**
     * 加载填充词典片段
     * @param charArray
     * @param begin
     * @param length
     * @param enabled
     */
    private synchronized void fillSegment(char[] charArray, int begin, int length,
            int enabled) { // NOTE:htt, 将 中文字符纳入 加载到内存中
        //获取字典表中的汉字对象
        Character beginChar = Character.valueOf(charArray[begin]);
        Character keyChar = charMap.get(beginChar);
        //字典中没有该字，则将其添加入字典
        if (keyChar == null) {
            charMap.put(beginChar, beginChar);
            keyChar = beginChar;
        }

        //搜索当前节点的存储，查询对应keyChar的keyChar，如果没有则创建
        DictSegment ds = lookforSegment(keyChar, enabled); // NOTE:htt, 如果小于ARRAY_LENGTH_LIMIT采用数组存储，否则采用map存储
        if (ds != null) {
            //处理keyChar对应的segment
            if (length > 1) {
                //词元还没有完全加入词典树
                ds.fillSegment(charArray, begin + 1, length - 1, enabled); // NOTE:htt, 继续在当前字符下构建子树，用于将子串都纳入
            } else if (length == 1) {
                //已经是词元的最后一个char,设置当前节点状态为enabled，
                //enabled=1表明一个完整的词，enabled=0表示从词典中屏蔽当前词
                ds.nodeState = enabled; // NOTE:htt, 末尾字符，如果为1代表完整词并可以使用，为0则为完整的词但是被屏蔽
            }
        }

    }

    /**
     * 查找本节点下对应的keyChar的segment	 *
     * @param keyChar
     * @param create  =1如果没有找到，则创建新的segment ; =0如果没有找到，不创建，返回null
     * @return
     */
    private DictSegment lookforSegment(Character keyChar,
            int create) { // NOTE:htt, 如果小于ARRAY_LENGTH_LIMIT采用数组存储，否则采用map存储

        DictSegment ds = null;

        if (this.storeSize <= ARRAY_LENGTH_LIMIT) {
            //获取数组容器，如果数组未创建则创建数组
            DictSegment[] segmentArray = getChildrenArray();
            //搜寻数组
            DictSegment keySegment = new DictSegment(keyChar);
            int position = Arrays.binarySearch(segmentArray, 0, this.storeSize, keySegment);
            if (position >= 0) {
                ds = segmentArray[position];
            }

            //遍历数组后没有找到对应的segment
            if (ds == null && create == 1) {
                ds = keySegment;
                if (this.storeSize < ARRAY_LENGTH_LIMIT) {
                    //数组容量未满，使用数组存储
                    segmentArray[this.storeSize] = ds;
                    //segment数目+1
                    this.storeSize++;
                    Arrays.sort(segmentArray, 0, this.storeSize);

                } else {
                    //数组容量已满，切换Map存储
                    //获取Map容器，如果Map未创建,则创建Map
                    Map<Character, DictSegment> segmentMap = getChildrenMap();
                    //将数组中的segment迁移到Map中
                    migrate(segmentArray, segmentMap);
                    //存储新的segment
                    segmentMap.put(keyChar, ds);
                    //segment数目+1 ，  必须在释放数组前执行storeSize++ ， 确保极端情况下，不会取到空的数组
                    this.storeSize++;
                    //释放当前的数组引用
                    this.childrenArray = null; // NOTE:htt, 将数组置空，以便后续处理
                }
            }
        } else {
            //获取Map容器，如果Map未创建,则创建Map
            Map<Character, DictSegment> segmentMap = getChildrenMap();
            //搜索Map
            ds = segmentMap.get(keyChar);
            if (ds == null && create == 1) {
                //构造新的segment
                ds = new DictSegment(keyChar);
                segmentMap.put(keyChar, ds);
                //当前节点存储segment数目+1
                this.storeSize++;
            }
        }

        return ds;
    }


    /**
     * 获取数组容器
     * 线程同步方法
     */
    private DictSegment[] getChildrenArray() { // NOTE:htt, childrenArray按 3长度创建
        synchronized (this) {
            if (this.childrenArray == null) {
                this.childrenArray = new DictSegment[ARRAY_LENGTH_LIMIT];
            }
        }
        return this.childrenArray;
    }

    /**
     * 获取Map容器
     * 线程同步方法
     */
    private Map<Character, DictSegment> getChildrenMap() { // NOTE:htt, childrenMap按2倍的容量创建
        synchronized (this) {
            if (this.childrenMap == null) {
                this.childrenMap = new ConcurrentHashMap<Character, DictSegment>(ARRAY_LENGTH_LIMIT * 2, 0.8f);
            }
        }
        return this.childrenMap;
    }

    /**
     * 将数组中的segment迁移到Map中
     * @param segmentArray
     */
    private void migrate(DictSegment[] segmentArray,
            Map<Character, DictSegment> segmentMap) { // NOTE:htt, 将segmentArray数组内容迁移到 Map中，key为 nodeChar字符
        for (DictSegment segment : segmentArray) {
            if (segment != null) {
                segmentMap.put(segment.nodeChar, segment);
            }
        }
    }

    /**
     * 实现Comparable接口
     * @param o
     * @return int
     */
    public int compareTo(DictSegment o) {
        //对当前节点存储的char进行比较
        return this.nodeChar.compareTo(o.nodeChar); // NOTE:htt, 比较存储的字符是否相同
    }

}
