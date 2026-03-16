package cafe.woden.leakwatch.core;

import cafe.woden.leakwatch.annotations.FallbackCleanupAction;

record FallbackCleanupPlan(
    FallbackCleanupAction<Object> action,
    Object detachedState,
    String actionClassName
) {
    void execute() throws Exception {
        action.cleanup(detachedState);
    }
}
