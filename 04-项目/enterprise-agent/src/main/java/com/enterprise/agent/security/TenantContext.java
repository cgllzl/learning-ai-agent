package com.enterprise.agent.security;

import java.util.function.Supplier;

/**
 * 租户上下文：把当前请求的 SecuritySubject 放到 ThreadLocal 里。
 * 这样下游的 Tool 不需要层层传 tenantId，直接从这里取即可。
 */
public final class TenantContext {

    private static final ThreadLocal<SecuritySubject> HOLDER = new ThreadLocal<>();

    private TenantContext() {
    }

    public static <T> T run(SecuritySubject subject, Supplier<T> action) {
        HOLDER.set(subject);
        try {
            return action.get();
        } finally {
            HOLDER.remove();
        }
    }

    public static SecuritySubject current() {
        SecuritySubject subject = HOLDER.get();
        if (subject == null) {
            throw new IllegalStateException("当前线程没有租户上下文，请先通过 TenantContext.run() 设置");
        }
        return subject;
    }

    public static String requiredTenantId() {
        return current().tenantId();
    }
}
