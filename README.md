**Rx java2 wrpapper for NuProcess** - Low-overhead, non-blocking I/O, external Process implementation for Java.

### Usage:

TODO publish to maven

**Build process**:

	RxNuProcessBuilder builder = RxNuProcessBuilder.fromCommand(Arrays.asList("cat"))
		.withCwd(...)
		.withEnv(...);
		

**Here multiple variants of process execution**:

Returns exit code with optional exception with captured stderr:

	Single<Exit> done = builder.asWaitDone();

Returns stdout stream, stream - cold, so subscription start the process:	

	Observable<byte[]> stdout = builder.asStdOut();
	
Returns set of "callback "streams, all streams except waitDone - hot. Subscription to `waitDone`	strart the process. You can push to stdin. More on javadoc:

	PreparedStreams streams = builder.asStdInOut();

	Single<NuProcess> started = streams.started();
	Single<Exit> done = streams.waitDone();
	Observable<byte[]> stdout = streams.stdOut();
	Observer<byte[]> stdin = streams.stdIn();
