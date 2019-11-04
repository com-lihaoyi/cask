Cask ships with a lightweight Actor library, making it very easy for you to
define asynchronous pipelines. Cask uses these actors to model [websocket server
and client connections](http://www.lihaoyi.com/cask/#websockets), but you can
also use them for your own purposes, even outside a web application via the
standalone `cask-actor` artifact:


```scala
// Mill
ivy"com.lihaoyi::cask-actor:0.3.3"

// SBT
"com.lihaoyi" %% "cask-actor" % "0.3.3"
```

Cask Actors are much more lightweight solution than a full-fledged framework
like Akka: Cask Actors do not support any sort of distribution or clustering,
and run entirely within a single process. Cask Actors are garbage collectible,
and you do not need to manually terminate them or manage their lifecycle.


## Cask Actors


At their core, Actors are simply objects who receive messages via a `send`
method, and asynchronously process those messages one after the other:

```scala
trait Actor[T]{
  def send(t: T): Unit

  def sendAsync(f: scala.concurrent.Future[T]): Unit
}
```

This processing happens in the background, and can take place without blocking.
After a messsage is sent, the thread or actor that called `.send()` can
immediately go on to do other things, even if the message hasn't been processed
yet. Messages sent to an actor that is already busy will be queued up until the
actor is free.

Cask provides three primary classes you can inherit from to define actors:

```scala
abstract class SimpleActor[T]()(implicit ac: Context) extends Actor[T]{
  def run(msg: T): Unit
}

abstract class BatchActor[T]()(implicit ac: Context) extends Actor[T]{
  def runBatch(msgs: Seq[T]): Unit
}

abstract class StateMachineActor[T]()(implicit ac: Context) extends Actor[T]() {
  class State(val run: T => State)
  protected[this] def initialState: State
}
```

`SimpleActor` works by providing a `run` function that will be run on each
message. `BatchActor` allows you to provide a `runBatch` function that works on
groups of messages at a time: this is useful when message processing can be
batched together for better efficiency, e.g. making batched database queries
instead of many individual. `StateMachineActor` allows you to define actors via
a set of distinct states, each of which has a separate `run` callback that
transitions the actor to a different state.

Note that any exception that is thrown while an Actor is processing a message
(or batch of messages, in the case of `BatchActor`) is simply reported to the
`cask.actor.Context`'s `reportFailure` function: the default just prints to the
console using `.printStackTrace()`, but you can hook in to pass the exceptions
elsewhere e.g. if you have a remote error aggregating service. The actor
continues processing messages after the failure in the state that it was left
in.

Cask Actors are meant to manage mutable state internal to the Actor. Note that
it is up to you to mark the state `private` to avoid accidental external access.
Each actor may run on a different thread, and the same actor may run on
different threads at different times, so you should ensure you do not mutate
shared mutable state otherwise you risk race conditions.

## Writing Actors
### Example: Asynchronous Logging using an Actor

Here is a small demonstration of using a `cask.actor.SimpleActor` to perform
asynchronous logging to disk:

```scala
import cask.actor.{SimpleActor, Context}
class Logger(log: os.Path, old: os.Path, rotateSize: Int)
            (implicit ac: Context) extends SimpleActor[String]{
  def run(s: String) = {
    val newLogSize = logSize + s.length + 1
    if (newLogSize <= rotateSize) logSize = newLogSize
    else {
      logSize = s.length
      os.move(log, old, replaceExisting = true)
    }
    os.write.append(log, s + "\n", createFolders = true)
  }
  private var logSize = 0
}

implicit val ac = new Context.Test()

val logPath = os.pwd / "out" / "scratch" / "log.txt"
val oldPath  = os.pwd / "out" / "scratch" / "log-old.txt"

val logger = new Logger(logPath, oldPath, rotateSize = 50)
```

In the above example, we are defining a single `Logger` actor class, which we
are instantiating once as `val logger`. We can now send as many messages as we
want via `logger.send`: while the processing of a message make take some time
(here are are both writing to disk, as well as providing
[log-rotation](https://en.wikipedia.org/wiki/Log_rotation) to avoid the logfile
growing in size forever) the fact that it's in a separate actor means the
processing happens in the background without slowing down the main logic of your
program. Cask Actors process messages one at a time, so by putting the file
write-and-rotate logic inside an Actor we can be sure to avoid race conditions
that may arise due to multiple threads mangling the same file at once.

Here's the result of sending messages to the actor:

```scala
logger.send("I am cow")
logger.send("hear me moo")
logger.send("I weight twice as much as you")
logger.send("And I look good on the barbecue")
logger.send("Yoghurt curds cream cheese and butter")
logger.send("Comes from liquids from my udder")
logger.send("I am cow, I am cow")
logger.send("Hear me moo, moooo")

// Logger hasn't finished yet, running in the background
ac.waitForInactivity()
// Now logger has finished

os.read.lines(oldPath) ==> Seq("Comes from liquids from my udder")
os.read.lines(logPath) ==> Seq("I am cow, I am cow", "Hear me moo, moooo")
```

Using Actors is ideal for scenarios where the dataflow is one way: e.g. when
logging, you only write logs, and never need to wait for the results of
processing them.

All cask actors require a `cask.actor.Context`, which is an extended
`scala.concurrent.ExecutionContext`. Here we are using `Context.Test`, which
also provides the handy `waitForInactivity()` method which blocks until all
asynchronous actor processing has completed.

Note that `logger.send` is thread-safe: multiple threads can be sending logging
messages to the `logger` at once, and the `.send` method will make sure the
messages are properly queued up and executed one at a time.

### Strawman: Synchronized Logging

To illustrate further the use case of actors, let us consider the earlier
example but using a `synchronized` method instead of a `cask.actor.SimpleActor`
to perform the logging:

```scala
val rotateSize = 50
val logPath = os.pwd / "out" / "scratch" / "log.txt"
val oldPath = os.pwd / "out" / "scratch" / "log-old.txt"

var logSize = 0

def logLine(s: String): Unit = synchronized{
  val newLogSize = logSize + s.length + 1
  if (newLogSize <= rotateSize) logSize = newLogSize
  else {
    logSize = 0
    os.move(logPath, oldPath, replaceExisting = true)
  }

  os.write.append(logPath, s + "\n", createFolders = true)
}

logLine("I am cow")
logLine("hear me moo")
logLine("I weight twice as much as you")
logLine("And I look good on the barbecue")
logLine("Yoghurt curds cream cheese and butter")
logLine("Comes from liquids from my udder")
logLine("I am cow, I am cow")
logLine("Hear me moo, moooo")

os.read(oldPath).trim() ==> "Yoghurt curds cream cheese and butter\nComes from liquids from my udder"
os.read(logPath).trim() ==> "I am cow, I am cow\nHear me moo, moooo"
```

This is similar to the earlier Actor example, but with two main caveats:

- Your program execution stops when calling `logLine`, until the call to
  `logLine` completes. Thus the calls to `logLine` can end up slowing down your
  program, even though your program really doesn't need the result of `logLine`
  in order to make progress

- Since `logLine` ends up managing some global mutable state (writing to and
  rotating log files) we need to make it `synchronized`. That means that if
  multiple threads in your program are calling `logLine`, it is possible that
  some threads will be blocked waiting for other threads to complete their
  `logLine` calls.

Using Cask Actors to perform logging avoids both these issues: calls to
`logger.send` happen in the background without slowing down your main program,
and multiple threads can call `logger.send` without being blocked by each other.

### Parallelism using Actor Pipelines

Another advantage of Actors is that you can get pipelined parallelism when
processing data. In the following example, we define two actor classes `Writer`
and `Logger`, and two actors `val writer` and `val logger`. `Writer` handles the
same writing-strings-to-disk-and-rotating-log-files logic we saw earlier, while
`Logger` adds another step of encoding the data (here just using Base64) before
it gets written to disk:

```scala
class Writer(log: os.Path, old: os.Path, rotateSize: Int)
            (implicit ac: Context) extends SimpleActor[String]{
  def run(s: String) = {
    val newLogSize = logSize + s.length + 1
    if (newLogSize <= rotateSize) logSize = newLogSize
    else {
      logSize = s.length
      os.move(log, old, replaceExisting = true)
    }
    os.write.append(log, s + "\n", createFolders = true)
  }
  private var logSize = 0
}

class Logger(dest: Actor[String])(implicit ac: Context) extends SimpleActor[String]{
  def run(s: String) = dest.send(java.util.Base64.getEncoder.encodeToString(s.getBytes))
}

implicit val ac = new Context.Test()

val logPath = os.pwd / "out" / "scratch" / "log.txt"
val oldPath  = os.pwd / "out" / "scratch" / "log-old.txt"

val writer = new Writer(logPath, oldPath, rotateSize = 50)
val logger = new Logger(writer)
```

Although we have added another Base64 encoding step to the logging process, this
new step lives in a separate actor from the original write-to-disk step, and
both of these can run in parallel as well as in parallel with the main logic. By
constructing our data processing flows using Actors, we can take advantage of
pipeline parallelism to distribute the processing over multiple threads and CPU
cores, so adding steps to the pipeline neither slows it down nor does it slow
down the execution of the main program.

We can send messages to this actor and verify that it writes lines to the log
file base64 encoded:

```scala
logger.send("I am cow")
logger.send("hear me moo")
logger.send("I weight twice as much as you")
logger.send("And I look good on the barbecue")
logger.send("Yoghurt curds cream cheese and butter")
logger.send("Comes from liquids from my udder")
logger.send("I am cow, I am cow")
logger.send("Hear me moo, moooo")

ac.waitForInactivity()

os.read(oldPath) ==> "Q29tZXMgZnJvbSBsaXF1aWRzIGZyb20gbXkgdWRkZXI=\n"
os.read(logPath) ==> "SSBhbSBjb3csIEkgYW0gY293\nSGVhciBtZSBtb28sIG1vb29v\n"

def decodeFile(p: os.Path) = {
  os.read.lines(p).map(s => new String(java.util.Base64.getDecoder.decode(s)))
}

decodeFile(oldPath) ==> Seq("Comes from liquids from my udder")
decodeFile(logPath) ==> Seq("I am cow, I am cow", "Hear me moo, moooo")
```

You can imagine adding additional stages to this actor pipeline, to perform
other sorts of processing, and have those additional stages running in parallel
as well.

### Batch Logging using BatchActor

Sometimes it is more efficient for an Actor to handle all incoming messages at
once. You may be working with a HTTP API that lets you send one batch request
rather than a hundred small ones, or with a database that lets you send one
batch query to settle all incoming messages. In these situations, you can use a
`BatchActor`.

This example again shows a logging pipeline, but instead of the two stages being
"encoding" and "writing to disk", our two stages are "handling log rotating" and
"batch writing":

```scala
sealed trait Msg
case class Text(value: String) extends Msg
case class Rotate() extends Msg
class Writer(log: os.Path, old: os.Path)
            (implicit ac: Context) extends BatchActor[Msg]{
  def runBatch(msgs: Seq[Msg]): Unit = {
    msgs.lastIndexOf(Rotate()) match{
      case -1 => os.write.append(log, groupMsgs(msgs), createFolders = true)
      case rotateIndex =>
        val prevRotateIndex = msgs.lastIndexOf(Rotate(), rotateIndex - 1)
        if (prevRotateIndex != -1) os.remove.all(log)
        os.write.append(log, groupMsgs(msgs.slice(prevRotateIndex, rotateIndex)), createFolders = true)
        os.move(log, old, replaceExisting = true)
        os.write.over(log, groupMsgs(msgs.drop(rotateIndex)), createFolders = true)
    }
  }
  def groupMsgs(msgs: Seq[Msg]) = msgs.collect{case Text(value) => value}.mkString("\n") + "\n"
}

class Logger(dest: Actor[Msg], rotateSize: Int)
            (implicit ac: Context) extends SimpleActor[String]{
  def run(s: String) = {
    val newLogSize = logSize + s.length + 1
    if (newLogSize <= rotateSize) logSize = newLogSize
    else {
      logSize = s.length
      dest.send(Rotate())
    }
    dest.send(Text(s))
  }
  private var logSize = 0
}

implicit val ac = new Context.Test()

val logPath = os.pwd / "out" / "scratch" / "log.txt"
val oldPath  = os.pwd / "out" / "scratch" / "log-old.txt"

val writer = new Writer(logPath, oldPath)
val logger = new Logger(writer, rotateSize = 50)
```

Here the `Logger` actor takes incoming log lines and decides when it needs to
trigger a log rotation, while sending both the log lines and rotation commands
as `Text` and `Rotate` commands to the `Writer` batch actor which handles
batches of these messages via its `runBatch` method. `Writer` filters through
the list of incoming messages to decide what it needs to do: either there are
zero `Rotate` commands and it simply appends all incoming `Text`s to the log
file, or there are one-or-more `Rotate` commands it needs to do a log rotation,
writing the batched messages once to the log file pre- and post-rotation.

We can send messages to the logger and verify that it behaves the same as the
`SimpleActor` example earlier:

```scala
logger.send("I am cow")
logger.send("hear me moo")
logger.send("I weight twice as much as you")
logger.send("And I look good on the barbecue")
logger.send("Yoghurt curds cream cheese and butter")
logger.send("Comes from liquids from my udder")
logger.send("I am cow, I am cow")
logger.send("Hear me moo, moooo")

ac.waitForInactivity()

os.read.lines(oldPath) ==> Seq("Comes from liquids from my udder")
os.read.lines(logPath) ==> Seq("I am cow, I am cow", "Hear me moo, moooo")
```

Using a `BatchActor` here helps reduce the number of writes to the filesystem:
no matter how many messages get queued up, our batch actor only makes two
writes. Furthermore, if there are more than two `Rotate` commands in the same
batch, earlier `Text` log lines can be discarded without being written at all!
Together this can greatly improve the performance of working with external APIs.

Note that when extending `BatchActor`, it is up to the implementer to ensure
that the `BatchActor`s `runBatch` method has the same visible effect as if they
had run a single `run` method on each message individually. Violating that
assumption may lead to weird bugs, where the actor behaves differently depending
on how the messages are batched (which is nondeterministic, and may depend on
thread scheduling and other performance related details).

### Debounced Logging using State Machines

The last common API we will look at is using `StateMachineActor`. We will define
an actor that debounces writes to disk, ensuring they do not happen any more
frequently than once every 50 milliseconds. This is a common pattern when
working with an external API that you do not want to overload with large numbers
of API calls.

```scala
sealed trait Msg
case class Flush() extends Msg
case class Text(value: String) extends Msg

class Logger(log: os.Path, debounceTime: java.time.Duration)
            (implicit ac: Context) extends StateMachineActor[Msg]{
  def initialState = Idle()
  case class Idle() extends State({
    case Text(value) =>
      ac.scheduleMsg(this, Flush(), debounceTime)
      Buffering(Vector(value))
  })
  case class Buffering(buffer: Vector[String]) extends State({
    case Text(value) => Buffering(buffer :+ value)
    case Flush() =>
      os.write.append(log, buffer.mkString(" ") + "\n", createFolders = true)
      Idle()
  })
}

implicit val ac = new Context.Test()

val logPath = os.pwd / "out" / "scratch" / "log.txt"

val logger = new Logger(logPath, java.time.Duration.ofMillis(50))
```

In this example, we use `StateMachineActor` to define a `Logger` actor with two
states `Idle` and `Buffering`.

This actor starts out with its `initalState = Idle()`. When it receives a `Text`
message, it schedules a `Flush` message to be sent 50 milliseconds in the
future, and transitions into the `Buffering` state. While in `Buffering`, any
additional `Text` messages are simply accumulated onto the buffer, until the
`Flush` is received again and all the buffered messages are flushed to disk.
Each group of messages is written as a single line, separated by newlines (just
so we can see the effect of the batching in the output). The output is as
follows:

```scala
logger.send(Text("I am cow"))
logger.send(Text("hear me moo"))
Thread.sleep(100)
logger.send(Text("I weight twice as much as you"))
logger.send(Text("And I look good on the barbecue"))
Thread.sleep(100)
logger.send(Text("Yoghurt curds cream cheese and butter"))
logger.send(Text("Comes from liquids from my udder"))
logger.send(Text("I am cow, I am cow"))
logger.send(Text("Hear me moo, moooo"))

ac.waitForInactivity()

os.read.lines(logPath) ==> Seq(
  "I am cow hear me moo",
  "I weight twice as much as you And I look good on the barbecue",
  "Yoghurt curds cream cheese and butter Comes from liquids from my udder I am cow, I am cow Hear me moo, moooo",
)
```

You can see that when sending the text messages to the `logger` in three groups
separated by 100 millisecond waits, the final log file ends up having three
lines of logs each of which contains multiple messages buffered together.

In general, `StateMachineActor` is very useful in cases where there are multiple
distinct states which an Actor can be in, as it forces you explicitly define the
states, the members of each state, as well as the state transitions that occur
when each state receives each message. When the number of distinct states grows,
`StateMachineActor` can be significantly easier to use than `SimpleActor`.

While it is good practice to make your `State`s immutable, `StateMachineActor`
does not enforce it. Similarly, it is generally good practice to avoid defining
"auxiliary" mutable state `var`s in the body of a `StateMachineActor`. The
library does not enforce that either, but doing so somewhat defeats the purpose
of using a `StateMachineActor` to model your actor state in the first place, in
which case you might as well use `SimpleActor`.

Note that while multiple threads can send messages to `Logger` at once, and the
`Flush()` message can also be sent at an arbitrary time in the future thanks to
the `ac.scheduleMsg` call, the actor will only ever process one message at a
time. This means you can be sure that it will transition through the two states
`Idle` and `Buffering` in a straightforward manner, without worry about multiple
threads executing at once and messing up the simple state machine.

## Debugging Actors

### Debug Logging State Machines

When using `StateMachineActor`, all your actor's internal state should be in the
single `state` variable. You can thus easily override `def run` to print the
state before and after each message is received:

```scala
override def run(msg: Msg): Unit = {
  println(s"$state + $msg -> ")
  super.run(msg)
  println(state)
}
```

If your `StateMachineActor` is misbehaving, this should hopefully make it easier
to trace what it is doing in response to each message, so you can figure out
exactly why it is misbehaving:

```scala
logger.send(Text("I am cow"))
// Idle() + Text(I am cow) -> 
// Buffering(Vector(I am cow))
logger.send(Text("hear me moo"))
// Buffering(Vector(I am cow)) + Text(hear me moo) -> 
// Buffering(Vector(I am cow, hear me moo))
Thread.sleep(100)
// Buffering(Vector(I am cow, hear me moo)) + Debounced() -> 
// Idle()
logger.send(Text("I weight twice as much as you"))
// Idle() + Text(I weight twice as much as you) -> 
// Buffering(Vector(I weight twice as much as you))
logger.send(Text("And I look good on the barbecue"))
// Buffering(Vector(I weight twice as much as you)) + Text(And I look good on the barbecue) -> 
// Buffering(Vector(I weight twice as much as you, And I look good on the barbecue))
Thread.sleep(100)
// Buffering(Vector(I weight twice as much as you, And I look good on the barbecue)) + Debounced() -> 
// Idle()
logger.send(Text("Yoghurt curds cream cheese and butter"))
// Idle() + Text(Yoghurt curds cream cheese and butter) -> 
// Buffering(Vector(Yoghurt curds cream cheese and butter))
logger.send(Text("Comes from liquids from my udder"))
// Buffering(Vector(Yoghurt curds cream cheese and butter)) +
// Text(Comes from liquids from my udder) -> Buffering(Vector(Yoghurt curds cream cheese and butter, Comes from liquids from my udder))
logger.send(Text("I am cow, I am cow"))
// Buffering(Vector(Yoghurt curds cream cheese and butter, Comes from liquids from my udder)) + Text(I am cow, I am cow) -> 
// Buffering(Vector(Yoghurt curds cream cheese and butter, Comes from liquids from my udder, I am cow, I am cow))
logger.send(Text("Hear me moo, moooo"))
// Buffering(Vector(Yoghurt curds cream cheese and butter, Comes from liquids from my udder, I am cow, I am cow)) + Text(Hear me moo, moooo) -> 
// Buffering(Vector(Yoghurt curds cream cheese and butter, Comes from liquids from my udder, I am cow, I am cow, Hear me moo, moooo))

ac.waitForInactivity()
// Buffering(Vector(Yoghurt curds cream cheese and butter, Comes from liquids from my udder, I am cow, I am cow, Hear me moo, moooo)) + Debounced() ->
// Idle()
```

Logging every message received and processed by one or more Actors may get very
verbose in a large system with lots going on; you can use a conditional
`if(...)` in your `override def run` to specify exactly which state transitions
on which actors you care about (e.g. only actors handling a certain user ID) to
cut down on the noise:


```scala
override def run(msg: Msg): Unit = {
  if (???) println(s"$state + $msg -> ")
  super.run(msg)
  if (???) println(state)
}
```

Note that if you have multiple actors sending messages to each other, by default
they run on a thread pool and so the `println` messages above may become
interleaved and hard to read. To resolve that, you can try
[Running Actors Single Threaded](#running-actors-single-threaded).

### Debugging using Context Logging

Apart from logging individual Actors, you can also insert logging into the
`cask.actor.Context` to log certain state transitions or actions. For example,
you can log every time a message is run on an actor by overriding the
`reportRun` callback:

```scala
implicit val ac = new Context.Test(){
  override def reportRun(a: Actor[_], msg: Any, token: Context.Token): Unit = {
    println(s"$a <- $msg")
    super.reportRun(a, msg, token)
  }
}
```

Running this on the
[two-actor pipeline example](#parallelism-using-actor-pipelines) from earlier,
it helps us visualize exactly what our actors are going:

```text
cask.actor.JvmActorsTest$Logger$5@4a903c98 <- I am cow
cask.actor.JvmActorsTest$Logger$5@4a903c98 <- hear me moo
cask.actor.JvmActorsTest$Logger$5@4a903c98 <- I weight twice as much as you
cask.actor.JvmActorsTest$Writer$2@3bb87fa0 <- SSBhbSBjb3c=
cask.actor.JvmActorsTest$Logger$5@4a903c98 <- And I look good on the barbecue
cask.actor.JvmActorsTest$Logger$5@4a903c98 <- Yoghurt curds cream cheese and butter
cask.actor.JvmActorsTest$Logger$5@4a903c98 <- Comes from liquids from my udder
cask.actor.JvmActorsTest$Logger$5@4a903c98 <- I am cow, I am cow
cask.actor.JvmActorsTest$Logger$5@4a903c98 <- Hear me moo, moooo
cask.actor.JvmActorsTest$Writer$2@3bb87fa0 <- aGVhciBtZSBtb28=
cask.actor.JvmActorsTest$Writer$2@3bb87fa0 <- SSB3ZWlnaHQgdHdpY2UgYXMgbXVjaCBhcyB5b3U=
cask.actor.JvmActorsTest$Writer$2@3bb87fa0 <- QW5kIEkgbG9vayBnb29kIG9uIHRoZSBiYXJiZWN1ZQ==
cask.actor.JvmActorsTest$Writer$2@3bb87fa0 <- WW9naHVydCBjdXJkcyBjcmVhbSBjaGVlc2UgYW5kIGJ1dHRlcg==
cask.actor.JvmActorsTest$Writer$2@3bb87fa0 <- Q29tZXMgZnJvbSBsaXF1aWRzIGZyb20gbXkgdWRkZXI=
cask.actor.JvmActorsTest$Writer$2@3bb87fa0 <- SSBhbSBjb3csIEkgYW0gY293
cask.actor.JvmActorsTest$Writer$2@3bb87fa0 <- SGVhciBtZSBtb28sIG1vb29v
```

### Running Actors Single Threaded

We can also replace the default `scala.concurrent.ExecutionContext.global`
executor with a single-threaded executor, if we want our Actor pipeline to
behave 100% deterministically:

```scala
implicit val ac = new Context.Test(
  scala.concurrent.ExecutionContext.fromExecutor(
    java.util.concurrent.Executors.newSingleThreadExecutor()
  )
){
  override def reportRun(a: Actor[_], msg: Any, token: Context.Token): Unit = {
    println(s"$a <- $msg")
    super.reportRun(a, msg, token)
  }
}
```

Any asynchronous Actor pipeline should be able to run no a
`newSingleThreadExecutor`. While it would be slower than running on the default
thread pool, it should make execution of your actors much more deterministic -
only one actor will be running at a time - and make it easier to track down
logical bugs without multithreaded parallelism getting in the way.
