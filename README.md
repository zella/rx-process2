**Rx-java2 wrapper for subprocess execution** - Support java.lang.Process and com.zaxxer.nuprocess.NuProcess. 

### Usage:
Add dependency:

	<dependency>
	    <groupId>com.github.zella</groupId>
	    <artifactId>rx-process2</artifactId>
	    <version>0.2.0-BETA1</version>
	</dependency>


**Build process**:

    ProcessBuilder jBuilder = ...; //or NuProcessBuilder, NuProcess 

    IReactiveProcessBuilder<Process> builder = RxProcess.reactive(jBuilder);

**Here multiple variants of process execution**:

    Observable<ProcessChunk> stdOutErr = builder.asStdErrOut();
    
    Observable<byte[]> stdout = builder.asStdOut();

    Single<byte[]> stdoutSingle = builder.asStdOutSingle();

    Single<Exit> waitExit = builder.asWaitDone();



**Bidirectional communication**

    IReactiveProcess<Process> bi = builder.biDirectional();
    
    Single<Process> started = bi.started();
    Subscriber<byte[]> stdin = bi.stdIn();
    Observable<byte[]> stdoutBi = bi.stdOut();
    Observable<ProcessChunk> stdoutErrBi = bi.stdOutErr();
    //Cold subscription, start the process
    Single<Exit> waitExitBi = bi.waitDone();
    //write to stdin
    stdin.onNext("hello".getBytes());

