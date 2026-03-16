package cafe.woden.leakwatch.jol;

import cafe.woden.leakwatch.core.ShallowSizeEstimator;
import org.openjdk.jol.vm.VirtualMachine;

final class JolShallowSizeEstimator implements ShallowSizeEstimator {
    private final VirtualMachine virtualMachine;

    JolShallowSizeEstimator(VirtualMachine virtualMachine) {
        this.virtualMachine = virtualMachine;
    }

    @Override
    public long estimateShallowSize(Object instance) {
        return virtualMachine.sizeOf(instance);
    }

    @Override
    public String description() {
        return "org.openjdk.jol.vm.VM.current().sizeOf via leakwatch-jol";
    }
}
