package com.bnpp.regliss.utils.thread;

public class ThreadScopeContextHolder {
    private ThreadScopeContextHolder() { throw new UnsupportedOperationException("Only static methods class"); }

    // TODO: [ILLEGIBLE - IntelliJ shows this field initializer folded as "initialValue() -> { return new ThreadScopeAttributes(); }"; actual unfolded source (likely an anonymous ThreadLocal<ThreadScopeAttributes> subclass overriding initialValue()) not captured]
    private static final ThreadLocal<ThreadScopeAttributes> threadScopeAttributesHolder;

    /**
     * @throws IllegalStateException
     */
    public static ThreadScopeAttributes currentThreadScopeAttributes() {
        ThreadScopeAttributes accessor = threadScopeAttributesHolder.get();

        if (accessor == null) {
            throw new IllegalStateException("No thread scoped attributes.");
        }

        return accessor;
    }

    public static void clearThread() {
        if (threadScopeAttributesHolder.get() != null) {
            threadScopeAttributesHolder.get().clear();
            threadScopeAttributesHolder.remove();
        }
    }
}
