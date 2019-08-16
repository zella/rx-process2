package com.github.zella.rxprocess2;

import com.github.zella.rxprocess2.impl.javalang.JavaReactiveProcessBuilder;
import com.github.zella.rxprocess2.impl.nuprocess.NuReactiveProcessBuilder;
import com.zaxxer.nuprocess.NuProcess;
import com.zaxxer.nuprocess.NuProcessBuilder;

public final class RxProcess {


    private RxProcess() {
    }

    public static IReactiveProcessBuilder<Process> reactive(ProcessBuilder builder) {
        return new JavaReactiveProcessBuilder(builder);
    }

    public static IReactiveProcessBuilder<NuProcess> reactive(NuProcessBuilder builder) {
        return new NuReactiveProcessBuilder(builder);
    }

}
