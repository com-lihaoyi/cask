Cask ships with a lightweight Actor library, making it very easy for you to
define asynchronous pipelines. Cask uses these actors to model [websocket server
and client connections](http://www.lihaoyi.com/cask/#websockets), but you can
also use them for your own purposes, even outside a web application via the
standalone `cask-actor` artifact:


```scala
// Mill
ivy"com.lihaoyi::cask-actor:0.2.9"

// SBT
"com.lihaoyi" %% "cask-actor" % "0.2.9"
```

Cask Actors are much more lightweight solution than a full-fledged framework
like Akka: Cask Actors do not support any sort of distribution or clustering,
and run entirely within a single process. Cask Actors are garbage collectible,
and you do not need to manually terminate them or manage their lifecycle.

## A Logger Actor

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

## Strawman: Synchronized Logging

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

## Actor Pipelines

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

Although we have added another Base64 encoding step to the logging process, this
new step lives in a separate actor from the original write-to-disk step, and
both of these can run in parallel as well as in parallel with the main logic. By
constructing our data processing flows using Actors, we can take advantage of
pipeline parallelism to distribute the processing over multiple threads and CPU
cores, so adding steps to the pipeline neither slows it down nor does it slow
down the execution of the main program.

You can imagine adding additional stages to this actor pipeline, to perform
other sorts of processing, and have those additional stages running in parallel
as well.