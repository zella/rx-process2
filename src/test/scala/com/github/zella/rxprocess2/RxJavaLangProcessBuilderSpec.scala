package com.github.zella.rxprocess2


import java.io.File
import java.nio.charset.Charset
import java.nio.file.Files
import java.time.Instant
import java.util
import java.util.Collections
import java.util.concurrent.{Executors, TimeUnit}

import com.github.davidmoten.rx2.Strings
import com.github.zella.rxprocess2.errors.{ProcessException, ProcessTimeoutException}
import io.reactivex._
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import org.scalatest._

import scala.collection.JavaConverters._

class RxJavaLangProcessBuilderSpec extends FlatSpec with Matchers {

  private def init(cmd: Seq[String]): IReactiveProcessBuilder[Process] = {
    val pb = new ProcessBuilder(cmd: _*)
    RxProcess.reactive(pb)
  }

  "Process asWaitDone" should "be completed" in {

    val observer = new TestObserver[Exit]

    val src: Single[Exit] = init(Seq("echo", "hello world")).asWaitDone()

    src.subscribeOn(Schedulers.io).subscribe(observer)

    observer.await(5, TimeUnit.SECONDS)
    observer.assertNoErrors()
    observer.assertComplete()
    observer.assertResult(new Exit(0))
  }

  "Process asWaitDone with wrong process" should "be completed with non zero exit code and captured stderr" in {

    val observer = new TestObserver[Exit]

    val src: Single[Exit] = init(Seq("bash", "-c", "printf foo >>/dev/stderr && sleep 0.1 && exit 1"))
      .asWaitDone()

    src.subscribeOn(Schedulers.io).subscribe(observer)

    observer.await(5, TimeUnit.SECONDS)
    observer.assertNoErrors()
    observer.assertComplete()
    observer.values().get(0).statusCode shouldBe 1
    observer.values().get(0).err.get() shouldBe an[ProcessException]
    observer.values().get(0).err.get().getMessage shouldBe "foo"
  }

  "Process asStdoutBuffered" should "be completed with collected stdout" in {

    val observer = new TestObserver[String]

    val src: Single[Array[Byte]] = init(Seq("printf", "hello world")).asStdOutSingle()

    val decoded: Single[String] = src.map(b => new String(b))

    decoded.subscribeOn(Schedulers.io).subscribe(observer)

    observer.await(5, TimeUnit.SECONDS)
    observer.assertNoErrors()
    observer.assertComplete()
    observer.assertResult("hello world")
  }

  "Process asStdout with wrong process" should "be failed with exception and captured stderr" in {

    val observer = new TestObserver[String]

    val src: Observable[Array[Byte]] =
      init(Seq("bash", "-c", "printf foo && sleep 1 && printf bar >>/dev/stderr && sleep 0.1 && exit 1")).asStdOut()
    val decoded: Observable[String] = Strings.decode(src.toFlowable(BackpressureStrategy.BUFFER), Charset.defaultCharset()).toObservable

    decoded.subscribeOn(Schedulers.io).subscribe(observer)

    observer.await(5, TimeUnit.SECONDS)
    observer.assertError(classOf[ProcessException])
    observer.assertNotComplete()
    observer.assertValue("foo")
    observer.errors().get(0).getMessage shouldBe "bar"
  }


  "Process asStdout" should "be completed with stdout chunks" in {

    val observer = new TestObserver[String]

    val src: Observable[Array[Byte]] =
      init(Seq("bash", "-c", "printf hello && sleep 1 && printf world")).asStdOut()

    val decoded: Observable[String] = Strings.decode(src.toFlowable(BackpressureStrategy.BUFFER), Charset.defaultCharset()).toObservable

    decoded.subscribeOn(Schedulers.io).subscribe(observer)

    observer.await(5, TimeUnit.SECONDS)
    observer.assertNoErrors()
    observer.assertComplete()
    observer.assertResult("hello", "world")
  }

  //TODO test circular buffer error

  //STOP here
  "Process asStdInOut with input" should "be completed with input chunks" in {

    val started = new TestObserver[Process]
    val stdout = new TestObserver[String]
    val done = new TestObserver[Exit]

    val src: IReactiveProcess[Process] = init(Seq("cat")).biDirectional()

    src.started().subscribe(started)
    Strings.decode(src.stdOut().toFlowable(BackpressureStrategy.BUFFER), Charset.defaultCharset()).toObservable.subscribe(stdout)
    src.waitDone()
      .subscribeOn(Schedulers.io)
      .subscribe(done)

    started.await()
    started.values().get(0).isAlive shouldBe true

    src.stdIn().onNext("hello".getBytes)
    Thread.sleep(1000)
    src.stdIn().onNext("world".getBytes)
    src.stdIn().onComplete()

    done.await(5, TimeUnit.SECONDS)
    done.assertNoErrors()
    done.assertComplete()
    done.assertResult(new Exit(0))

    started.assertNoErrors()
    started.values().get(0).isAlive shouldBe false
    started.assertComplete()

    stdout.assertNoErrors()
    stdout.assertComplete()
    stdout.assertResult("hello", "world")
  }

  "Infinity Process asWaitDone " should "be disposed after timeout" in {

    val observer = new TestObserver[Exit]

    val src: Single[Exit] = init(Seq("sleep", "999")).asWaitDone(2, TimeUnit.SECONDS)

    src.subscribeOn(Schedulers.io).subscribe(observer)

    observer.await(5, TimeUnit.SECONDS)
    observer.assertNoErrors()
    observer.assertComplete()
    observer.values().get(0).statusCode shouldBe Integer.MIN_VALUE
    observer.values().get(0).err.get() shouldBe an[ProcessTimeoutException]
  }

  "Infinity Process asStdout" should "be disposed after timeout" in {

    val observer = new TestObserver[String]

    val src: Observable[Array[Byte]] = init(Seq("sleep", "999"))
      .asStdOut(2, TimeUnit.SECONDS)

    val decoded: Observable[String] = Strings.decode(src.toFlowable(BackpressureStrategy.BUFFER), Charset.defaultCharset()).toObservable

    decoded.subscribeOn(Schedulers.io).subscribe(observer)

    observer.await(5, TimeUnit.SECONDS)
    observer.assertError(classOf[ProcessTimeoutException])
    observer.assertNotComplete()
  }


  "Process asStdoutBuffered with long output" should "be completed with collected stdout" in {

    val testFile = new File(getClass.getClassLoader.getResource("long513339b.txt").getFile)

    val observer = new TestObserver[String]

    val src: Single[Array[Byte]] = init(Seq("cat", testFile.getAbsolutePath)).asStdOutSingle()

    val decoded: Single[String] = src.map(b => new String(b))

    decoded.subscribeOn(Schedulers.io).subscribe(observer)

    observer.await(5, TimeUnit.SECONDS)
    observer.assertNoErrors()
    observer.assertComplete()
    observer.assertResult(new String(Files.readAllBytes(testFile.toPath), "UTF-8"))
  }

  "Process asStdInOut with input" should "read early passed (before start) stdin" in {

    val started = new TestObserver[Process]
    val stdout = new TestObserver[String]
    val done = new TestObserver[Exit]

    val src = init(Seq("cat")).biDirectional()

    Strings.decode(src.stdOut().toFlowable(BackpressureStrategy.BUFFER), Charset.defaultCharset()).toObservable.subscribe(stdout)
    src.waitDone()
      .subscribeOn(Schedulers.io)
      .subscribe(done)

    src.stdIn().onNext("hello".getBytes)
    Thread.sleep(1000)
    src.stdIn().onNext("world".getBytes)

    src.started().subscribe(started)

    src.stdIn().onComplete()
    Thread.sleep(1000)

    stdout.assertNoErrors()
    stdout.assertComplete()
    stdout.assertResult("hello", "world")
  }

  "Process asStdoutBuffered" should "be executed serially in single thread scheduler" in {

    def between(v: Long, min: Long, max: Long) = v > min && v < max

    val sc = Schedulers.from(Executors.newSingleThreadExecutor())

    val src: Single[Array[Byte]] = init(Seq("bash", "-c", "sleep 1 && date +%s%3N")).asStdOutSingle().subscribeOn(sc)

    val decoded: Single[Instant] = src.map(b => Instant.ofEpochMilli(new String(b).trim.toLong))

    val test = Collections.synchronizedList(new util.ArrayList[Instant]()).asScala

    val whatToTest = decoded

    val now = Instant.now().toEpochMilli

    whatToTest.subscribe(s => test.append(s))
    whatToTest.subscribe(s => test.append(s))
    whatToTest.subscribe(s => test.append(s))
    whatToTest.subscribe(s => test.append(s))
    Thread.sleep(6000)

    between(Math.abs(test(0).toEpochMilli - now), 1000, 1300) shouldBe true
    between(Math.abs(test(1).toEpochMilli - now), 2000, 2300) shouldBe true
    between(Math.abs(test(2).toEpochMilli - now), 3000, 3300) shouldBe true
    between(Math.abs(test(3).toEpochMilli - now), 4000, 4300) shouldBe true
  }

  "Process asStdoutBuffered" should "be executed parallel in io thread scheduler" in {
    def between(v: Long, min: Long, max: Long) = v > min && v < max

    val src: Single[Array[Byte]] = init(Seq("bash", "-c", "sleep 1 && date +%s%3N")).asStdOutSingle()

    val decoded: Single[Instant] = src.map(b => Instant.ofEpochMilli(new String(b).trim.toLong))

    val test = Collections.synchronizedList(new util.ArrayList[Instant]()).asScala

    val whatToTest = decoded.subscribeOn(Schedulers.io())

    val now = Instant.now().toEpochMilli

    whatToTest.subscribe(s => test.append(s))
    whatToTest.subscribe(s => test.append(s))
    whatToTest.subscribe(s => test.append(s))
    whatToTest.subscribe(s => test.append(s))
    Thread.sleep(6000)

    between(Math.abs(test(0).toEpochMilli - now), 1000, 1300) shouldBe true
    between(Math.abs(test(1).toEpochMilli - now), 1000, 1300) shouldBe true
    between(Math.abs(test(2).toEpochMilli - now), 1000, 1300) shouldBe true
    between(Math.abs(test(3).toEpochMilli - now), 1000, 1300) shouldBe true
  }

  "Process asStdErrOut" should "be completed with stdout and stderr chunks" in {

    val observer = new TestObserver[ProcessChunk]

    val src: Observable[ProcessChunk] =
      init(Seq("bash", "-c", "printf foo && sleep 1 && printf bar >>/dev/stderr && sleep 1 && printf dar && sleep 0.1")).asStdErrOut()

    src.subscribeOn(Schedulers.io).subscribe(observer)

    observer.await(5, TimeUnit.SECONDS)
    observer.assertNoErrors()
    observer.assertComplete()
    observer.assertResult(
      new ProcessChunk("foo".getBytes, false),
      new ProcessChunk("bar".getBytes, true),
      new ProcessChunk("dar".getBytes, false)
    )
  }

//  "Process asStdout" should "be completed with last part of error" in {
//
//    System.setProperty("rxprocess2.stderrBuffer", "4")
//
//    val observer = new TestObserver[String]
//
//    val src: Observable[Array[Byte]] =
//      init(Seq("bash", "-c", "printf 12334567 >>/dev/stderr && printf 8 >>/dev/stderr && exit 1")).asStdOut()
//    val decoded: Observable[String] = Strings.decode(src.toFlowable(BackpressureStrategy.BUFFER), Charset.defaultCharset()).toObservable
//
//    decoded.subscribeOn(Schedulers.io).subscribe(observer)
//
//    observer.await(3, TimeUnit.SECONDS)
//    observer.assertError(classOf[ProcessException])
//    observer.errors().get(0).getMessage shouldBe "5678"
//  }

}