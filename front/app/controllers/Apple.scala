package controllers

import common.AkkaSupport
import akka.util.Duration
import java.util.concurrent.TimeUnit
import io.Source
import conf.ContentApi
import akka.actor.Cancellable
import model.{ Trail, Article, Content }

object Apple extends AkkaSupport {

  private val stockAgent = play_akka.agent[Option[String]](None)

  private val blogAgent = play_akka.agent[Option[Article]](None)

  private val trailAgent = play_akka.agent[Seq[Trail]](Nil)

  private var tasks: Seq[Cancellable] = Nil

  def start() {
    tasks = tasks ++ Seq(play_akka.scheduler.every(Duration(5, TimeUnit.MINUTES)) {
      stockAgent.sendOff { old =>

        val str = Source.fromURL("http://www.google.com/finance/info?q=AAPL", "UTF-8").getLines().mkString
        val price = str.split(":")(4).split(",")(0).replace("\"", "").trim
        val change = str.split(":")(11).split(",")(0).replace("\"", "").trim
        Some(price + " (" + change + ")")
      }
    })

    tasks = tasks ++ Seq(play_akka.scheduler.every(Duration(30, TimeUnit.SECONDS)) {
      blogAgent.sendOff { old =>

        val content = new Article(ContentApi
          .search
          .edition("UK")
          .pageSize(1)
          .showFields("body,liveBloggingNow")
          .tag("tone/minutebyminute,technology/apple")
          .results(0))

        if (content.isLive) {
          Some(content)
        } else {
          None
        }
      }
    })

    tasks = tasks ++ Seq(play_akka.scheduler.every(Duration(60, TimeUnit.SECONDS)) {
      trailAgent.sendOff { old =>

        val content = ContentApi
          .search
          .edition("UK")
          .pageSize(2)
          .showFields("body,liveBloggingNow")
          .tag("-tone/minutebyminute,technology/apple,type/gallery|type/article|type/video")
          .results

        content map { new Content(_) }
      }
    })
  }

  def stop() {
    tasks.foreach(_.cancel())
    blogAgent.close()
    stockAgent.close()
    trailAgent.close()
  }

  def stockPrice = stockAgent()

  def blog = blogAgent()

  def blogPara = blog.flatMap(_.body.split("""<!-- Block (\d)+ -->""").lastOption)

  def trails = trailAgent()
}
