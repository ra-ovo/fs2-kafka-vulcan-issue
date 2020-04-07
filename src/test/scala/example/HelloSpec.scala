package example

import java.time.Instant
import java.{util => ju}
import scala.concurrent.duration._

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import fs2.kafka.vulcan.{
  AvroSettings,
  SchemaRegistryClientSettings,
  Auth,
  avroSerializer,
  avroDeserializer
}
import example.Model.Person

class HelloSpec extends AnyFlatSpec with Matchers {
  import cats.effect._
  import cats.implicits._
  import fs2.kafka._

  implicit val cs = IO.contextShift(scala.concurrent.ExecutionContext.global)
  implicit val t = IO.timer(scala.concurrent.ExecutionContext.global)


  "fs2-kafka" should "roundtrip" in {
    def producer(k: String, v: Person) = fs2.Stream
      .emit(ProducerRecords.one(ProducerRecord("test-topic-1", k, v)))
      .covary[IO]
      .through {
        produce(Config.producerSettings)
      }

    def consumer(v: Person) = consumerStream[IO]
      .using(Config.consumerSettings)
      .evalTap(_.subscribeTo("test-topic-1"))
      .flatMap { _.stream.mapAsync(10) { committable =>
        committable.offset.commit >> IO(committable.record.value shouldBe v)
      }}
      .interruptAfter(1.seconds)


    val key = "1"
    val value = Person("C3p", "0'" + ju.UUID.randomUUID().toString)
    val logic = producer(key, value) >> consumer(value)
    
    logic.compile.drain.unsafeRunSync
  }

  object Config {
    private val avroSettings =
      AvroSettings {
        SchemaRegistryClientSettings[IO]("http://127.0.0.1:8081")
        //.withAuth(Auth.Basic("", ""))
      }
    private val bootStrapServers = "127.0.0.1:9092"
    private val consumerGroup = s"group-1"

   val consumerSettings = ConsumerSettings(
      keyDeserializer = Deserializer[IO, String],
      valueDeserializer = avroDeserializer[Person](Person.codec).using(avroSettings)
    ).withAutoOffsetReset(AutoOffsetReset.Latest)
     .withBootstrapServers(bootStrapServers)
     .withGroupId(consumerGroup)
     .withEnableAutoCommit(false)

    val producerSettings = ProducerSettings(
      keySerializer = Serializer[IO, String],
      valueSerializer = avroSerializer[Person](Person.codec).using(avroSettings)
    ).withBootstrapServers(bootStrapServers)
  }
}

object Model {
  import cats.implicits._
  import vulcan.Codec
  
  final case class Person(firstName: String, lastName: String)

  object Person {
    val codec = Codec.record[Person](
      name = "Person",
      namespace = "com.example",
      doc = Some("Person with a full name and optional age")
    ) { field =>
      field("fullName", p => s"${p.firstName} ${p.lastName}") *>
        (
          field("firstName", _.firstName),
          field("lastName", _.lastName),
          ).mapN(Person.apply)
    }
  }
}
