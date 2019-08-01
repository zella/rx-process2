**Rx java2 wrpapper for NuProcess** - Low-overhead, non-blocking I/O, external Process implementation for Java. 

### Usage:
Add dependency:

	<dependency>
	    <groupId>com.github.zella</groupId>
	    <artifactId>rx-process2</artifactId>
	    <version>0.1.0-RC5</version>
	</dependency>


**Build process**:

	RxNuProcessBuilder builder = RxNuProcessBuilder.fromCommand(Arrays.asList("cat"))
		.withCwd(...)
		.withEnv(...);
		

**Here multiple variants of process execution**:

Returns exit code with optional exception with captured stderr:

	Single<Exit> done = builder.asWaitDone();

Returns stdout stream, stream - cold, so subscription start the process:	

	Observable<byte[]> stdout = builder.asStdOut();
	
Returns single stdout:

	Single<byte[]> stdout = builder.asStdOutSingle();
	
Returns set of "callback "streams, all streams except waitDone - hot. Subscription to `waitDone`	strart the process. You can push to stdin. More on javadoc or see tests:

	PreparedStreams streams = builder.asStdInOut();

	Single<NuProcess> started = streams.started();
	Single<Exit> done = streams.waitDone();
	Observable<byte[]> stdout = streams.stdOut();
	Subscriber<byte[]> stdin = streams.stdIn();
