package com.github.zella.rxprocess2;

import com.github.zella.rxprocess2.impl.javalang.JavaReactiveProcessBuilder;
import com.github.zella.rxprocess2.impl.nuprocess.NuNonBlockingReactiveProcessBuilder;
import com.github.zella.rxprocess2.impl.nuprocess.NuReactiveProcessBuilder;
import com.zaxxer.nuprocess.NuProcess;
import com.zaxxer.nuprocess.NuProcessBuilder;

public final class RxProcess {


    private RxProcess() {
    }

    /**
     * Create blocking reactive api for process builder.
     * <p>
     * Note, that it blocks thread until process ends. If you want run few processes at constant parallelism level, you prefer blocking implementations.
     *
     * @param builder
     * @return Reactive representation of process builder
     */
    public static IReactiveProcessBuilder<Process> reactive(ProcessBuilder builder) {
        return new JavaReactiveProcessBuilder(builder);
    }

    /**
     * Create blocking reactive api for process builder
     * <p>
     * Note, that it blocks thread until process ends. If you want run few processes at constant parallelism level, you prefer blocking implementations.
     *
     * @param builder
     * @return Reactive representation of process builder
     */
    public static IReactiveProcessBuilder<NuProcess> reactive(NuProcessBuilder builder) {
        return new NuReactiveProcessBuilder(builder);
    }

    /**
     * Create reactive non blocking api for process builder
     *
     * @param builder
     * @return Non blocking reactive representation of process builder
     */
    public static IReactiveProcessBuilder<NuProcess> reactiveNonBlocking(NuProcessBuilder builder) {
        return new NuNonBlockingReactiveProcessBuilder(builder);
    }


}
