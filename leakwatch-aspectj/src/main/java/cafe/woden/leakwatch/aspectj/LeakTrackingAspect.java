package cafe.woden.leakwatch.aspectj;

import cafe.woden.leakwatch.annotations.LeakTracked;
import cafe.woden.leakwatch.annotations.RetentionSuspect;
import cafe.woden.leakwatch.core.LeakWatchRuntime;
import cafe.woden.leakwatch.core.LeakWatchWeavingState;
import cafe.woden.leakwatch.core.TrackingAnnotationSupport;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
/**
 * AspectJ advice that turns LeakWatch annotations into runtime tracking calls.
 */

@Aspect
public class LeakTrackingAspect {
    static {
        LeakWatchWeavingState.markAspectClassLoaded();
    }


    @AfterReturning("initialization((@cafe.woden.leakwatch.annotations.LeakTracked *).new(..)) && this(instance)")
    public void afterLeakTrackedInitialization(Object instance) {
        LeakWatchWeavingState.markAdviceObserved("lifecycle-initialization");
        trackLifecycleIfNeeded(instance);
    }

    @AfterReturning("initialization((@cafe.woden.leakwatch.annotations.RetentionSuspect *).new(..)) && this(instance)")
    public void afterRetentionTrackedInitialization(Object instance) {
        LeakWatchWeavingState.markAdviceObserved("retention-initialization");
        RetentionSuspect retentionSuspect = instance.getClass().getAnnotation(RetentionSuspect.class);
        if (retentionSuspect != null) {
            LeakWatchRuntime.registry().trackRetention(instance, instance.getClass(), retentionSuspect);
        }
    }

    @After("execution(@cafe.woden.leakwatch.annotations.CleanupMethod * *(..)) && this(instance)")
    public void afterAnnotatedCleanup(Object instance, JoinPoint joinPoint) {
        LeakWatchWeavingState.markAdviceObserved("annotated-cleanup");
        LeakWatchRuntime.registry().markCleaned(instance, joinPoint.getSignature().getName());
    }

    @After("execution(* *.close()) && this(instance)")
    public void afterConventionClose(Object instance) {
        LeakWatchWeavingState.markAdviceObserved("convention-close");
        autoMarkConventionalCleanup(instance, "close");
    }

    @After("execution(* *.dispose()) && this(instance)")
    public void afterConventionDispose(Object instance) {
        LeakWatchWeavingState.markAdviceObserved("convention-dispose");
        autoMarkConventionalCleanup(instance, "dispose");
    }

    @After("execution(* *.shutdown()) && this(instance)")
    public void afterConventionShutdown(Object instance) {
        LeakWatchWeavingState.markAdviceObserved("convention-shutdown");
        autoMarkConventionalCleanup(instance, "shutdown");
    }

    @After("execution(* *.disconnect()) && this(instance)")
    public void afterConventionDisconnect(Object instance) {
        LeakWatchWeavingState.markAdviceObserved("convention-disconnect");
        autoMarkConventionalCleanup(instance, "disconnect");
    }

    @After("execution(* *.unsubscribe()) && this(instance)")
    public void afterConventionUnsubscribe(Object instance) {
        LeakWatchWeavingState.markAdviceObserved("convention-unsubscribe");
        autoMarkConventionalCleanup(instance, "unsubscribe");
    }

    private void trackLifecycleIfNeeded(Object instance) {
        LeakTracked leakTracked = TrackingAnnotationSupport.resolveLeakTracked(instance.getClass());
        if (leakTracked != null) {
            LeakWatchRuntime.registry().trackLifecycle(instance, instance.getClass(), leakTracked);
        }
    }

    private void autoMarkConventionalCleanup(Object instance, String methodName) {
        LeakTracked leakTracked = TrackingAnnotationSupport.resolveLeakTracked(instance.getClass());
        if (leakTracked == null) {
            return;
        }
        if (!leakTracked.conventionCleanupDetection()) {
            return;
        }
        if (!LeakWatchRuntime.registry().isConventionCleanupMethod(instance.getClass(), methodName)) {
            return;
        }

        LeakWatchRuntime.registry().markCleaned(instance, methodName);
    }
}
