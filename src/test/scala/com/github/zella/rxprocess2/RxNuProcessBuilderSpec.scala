package com.github.zella.rxprocess2


import java.io.File
import java.nio.charset.Charset
import java.nio.file.{Files, Paths}
import java.util
import java.util.concurrent.TimeUnit

import com.github.davidmoten.rx2.Strings
import com.github.zella.rxprocess2.errors.{ProcessException, ProcessTimeoutException}
import com.zaxxer.nuprocess.NuProcess
import io.reactivex._
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import org.scalatest._

class RxNuProcessBuilderSpec extends FlatSpec with Matchers {

  "Process asWaitDone" should "be completed" in {

    val observer = new TestObserver[Exit]

    val src: Single[Exit] = RxNuProcessBuilder.fromCommand(util.Arrays.asList("echo", "hello world"))
      .asWaitDone()

    src.subscribeOn(Schedulers.io).subscribe(observer)

    observer.await(5, TimeUnit.SECONDS)
    observer.assertNoErrors()
    observer.assertComplete()
    observer.assertResult(new Exit(0))
  }

  "Process asWaitDone with wrong process" should "be completed with non zero exit code and captured stderr" in {

    val observer = new TestObserver[Exit]

    val src: Single[Exit] = RxNuProcessBuilder.fromCommand(util.Arrays.asList("bash", "-c", "printf foo >>/dev/stderr && exit 1"))
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

    val src: Single[Array[Byte]] = RxNuProcessBuilder.fromCommand(util.Arrays.asList("printf", "hello world"))
      .asStdOutSingle()

    val decoded: Single[String] = src.map(b => new String(b))

    decoded.subscribeOn(Schedulers.io).subscribe(observer)

    observer.await(5, TimeUnit.SECONDS)
    observer.assertNoErrors()
    observer.assertComplete()
    observer.assertResult("hello world")
  }

  "Process asStdout with wrong process" should "be failed with exception and captured stderr" in {

    val observer = new TestObserver[String]

    val src: Observable[Array[Byte]] = RxNuProcessBuilder.fromCommand(util.Arrays.asList("bash", "-c", "printf foo && sleep 1 && printf bar >>/dev/stderr && exit 1"))
      .asStdOut()

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

    val src: Observable[Array[Byte]] = RxNuProcessBuilder.fromCommand(util.Arrays.asList("bash", "-c", "printf hello && sleep 1 && printf world"))
      .asStdOut()

    val decoded: Observable[String] = Strings.decode(src.toFlowable(BackpressureStrategy.BUFFER), Charset.defaultCharset()).toObservable

    decoded.subscribeOn(Schedulers.io).subscribe(observer)

    observer.await(5, TimeUnit.SECONDS)
    observer.assertNoErrors()
    observer.assertComplete()
    observer.assertResult("hello", "world")
  }

  "Process asStdInOut with input" should "be completed with input chunks" in {

    val started = new TestObserver[NuProcess]
    val stdout = new TestObserver[String]
    val done = new TestObserver[Exit]

    val src = RxNuProcessBuilder.fromCommand(util.Arrays.asList("cat"))
      .asStdInOut()

    src.started().subscribe(started)
    Strings.decode(src.stdOut().toFlowable(BackpressureStrategy.BUFFER), Charset.defaultCharset()).toObservable.subscribe(stdout)
    src.waitDone()
      .subscribeOn(Schedulers.io)
      .subscribe(done)

    started.await()
    started.values().get(0).isRunning shouldBe true

    src.stdIn().onNext("hello".getBytes)
    Thread.sleep(1000)
    src.stdIn().onNext("world".getBytes)
    src.stdIn().onComplete()

    done.await(5, TimeUnit.SECONDS)
    done.assertNoErrors()
    done.assertComplete()
    done.assertResult(new Exit(0))

    started.assertNoErrors()
    started.values().get(0).isRunning shouldBe false
    started.assertComplete()

    stdout.assertNoErrors()
    stdout.assertComplete()
    stdout.assertResult("hello", "world")
  }

  "Infinity Process asWaitDone " should "be disposed after timeout" in {

    val observer = new TestObserver[Exit]

    val src: Single[Exit] = RxNuProcessBuilder.fromCommand(util.Arrays.asList("sleep", "999"))
      .asWaitDone(2, TimeUnit.SECONDS)

    src.subscribeOn(Schedulers.io).subscribe(observer)

    observer.await(5, TimeUnit.SECONDS)
    observer.assertNoErrors()
    observer.assertComplete()
    observer.values().get(0).statusCode shouldBe Integer.MIN_VALUE
    observer.values().get(0).err.get() shouldBe an[ProcessTimeoutException]
  }

  "Infinity Process asStdout" should "be disposed after timeout" in {

    val observer = new TestObserver[String]

    val src: Observable[Array[Byte]] = RxNuProcessBuilder.fromCommand(util.Arrays.asList("sleep", "999"))
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

    val src: Single[Array[Byte]] = RxNuProcessBuilder.fromCommand(util.Arrays.asList("cat", testFile.getAbsolutePath))
      .asStdOutSingle()

    val decoded: Single[String] = src.map(b => new String(b))

    decoded.subscribeOn(Schedulers.io).subscribe(observer)

    observer.await(5, TimeUnit.SECONDS)
    observer.assertNoErrors()
    observer.assertComplete()
    observer.assertResult(new String(Files.readAllBytes(testFile.toPath), "UTF-8"))
  }

  "Process asStdInOut with input" should "read early passed (before start) stdin" in {

    val started = new TestObserver[NuProcess]
    val stdout = new TestObserver[String]
    val done = new TestObserver[Exit]

    val src = RxNuProcessBuilder.fromCommand(util.Arrays.asList("cat"))
      .asStdInOut()

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


}