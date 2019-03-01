package com.github.zella.rxprocess2;

import com.zaxxer.nuprocess.NuProcess;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Single;

import java.util.Arrays;

public class Example {
    public static void main(String[] args) {
        RxNuProcessBuilder builder = RxNuProcessBuilder.fromCommand(Arrays.asList("cat"))
                .withCwd(null)
                .withEnv(null);


        Single<Exit> done = builder.asWaitDone();

        Observable<byte[]> stdout = builder.asStdOut();

        PreparedStreams streams = builder.asStdInOut();

        Single<NuProcess> started = streams.started();
        Single<Exit> done2 = streams.waitDone();
        Observable<byte[]> stdout2 = streams.stdOut();
        Observer<byte[]> stdin = streams.stdIn();


    }
}
