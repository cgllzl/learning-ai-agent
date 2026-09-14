package com.enterprise.agent.evaluation.trajectory;

/**
 * 评估题的数据分区：DEVELOPMENT 像练习题，可反复用于调试；
 * HOLDOUT 像期末题，按评估约定只在方案定型后验收，避免对着答案优化。
 */
public enum EvaluationSplit {
    DEVELOPMENT,
    HOLDOUT
}
