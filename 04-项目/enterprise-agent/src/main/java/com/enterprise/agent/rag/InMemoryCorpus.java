package com.enterprise.agent.rag;

import dev.langchain4j.data.segment.TextSegment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 关键词检索用的内存语料库（Week 3 Day 5）。
 * 向量库负责语义检索，这里保留原文片段供关键词检索扫描。
 */
@Component
public class InMemoryCorpus {

    private final List<TextSegment> segments = new CopyOnWriteArrayList<>();

    public void addAll(List<TextSegment> newSegments) {
        segments.addAll(newSegments);
    }

    public List<TextSegment> getAll() {
        return List.copyOf(segments);
    }
}