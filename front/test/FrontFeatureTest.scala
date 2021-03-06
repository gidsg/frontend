package test

import org.scalatest.FeatureSpec
import org.scalatest.GivenWhenThen
import org.scalatest.matchers.ShouldMatchers
import controllers.front.{ TrailblockAgent, FrontEdition, Front }
import model._
import org.joda.time.DateTime
import collection.JavaConversions._
import controllers.{ FrontController }
import play.api.mvc._
import model.Trailblock
import scala.Some
import model.TrailblockDescription
import views.support.{ Featured, Thumbnail, Headline }
import common.editions.{Us, Uk}
import common.Edition

class FrontFeatureTest extends FeatureSpec with GivenWhenThen with ShouldMatchers with Results {

  val TrailblockDescription = ItemTrailblockDescription

  feature("Network Front") {

    info("In order to explore the breaking news and navigate to site sections")
    info("As a Guardian reader")
    info("I want a single page that gives me a summary of the latest news")
    info("And has a summary and navigation to site sections")

    //Metrics
    info("Increase engagement (% of people continuing to a piece of content)")
    info("Page views should *not* decrease.")
    info("Increase repeat visits")

    //End to end integration tests


    scenario("Load the network front") {

      Given("I visit the network front")
      HtmlUnit("/uk") {
        browser =>
          import browser._

          Then("I should see the news trailblock")
          val news = $(".zone-news")
          news.find(".trail__headline") should have length (4)
      }
    }

    scenario("Section navigation") {
      Given("I visit the network front")
      HtmlUnit("/uk") {
        browser =>
          import browser._

          Then("I should see the link for section navigation")
          findFirst(".control--sections").href should endWith("/uk#footer-nav")
      }
    }

    scenario("Link to desktop version for UK edition") {
      Given("I visit the network front")
      HtmlUnit("/uk") {
        browser =>
          import browser._

          Then("I should see the link for the desktop site")
          findFirst(".main-site-link").href should endWith("/uk?view=desktop")
      }
    }

    scenario("Link to desktop version for US edition") {
      Given("I visit the network front")
      HtmlUnit.US("/us") {
        browser =>
          import browser._

          Then("I should see the link for the desktop site")
          findFirst(".main-site-link").href should endWith("/us?view=desktop")
      }
    }

    scenario("Copyright") {
      Given("I visit the network front")
      HtmlUnit("/uk") {
        browser =>
          import browser._

          Then("I should see the copyright")
          findFirst(".footer p").getText should startWith("© Guardian News and Media Limited")

      }
    }

    scenario("Link tracking") {
      Given("I visit the network front")
      HtmlUnit("/uk") {
        browser =>
          import browser._
          Then("All links should be tracked")
          $("a").filter(!_.hasAttribute("data-link-name")).foreach { link =>
            fail(s"Link with text '${link.getText}' and url '${link.getAttribute("href")}' is not tracked")
          }
      }
    }

    //lower level tests

    scenario("Display top stories") {
      Given("I visit the Network Front")

      Fake {
        //in real life these will always be editors picks only (content api does not do latest for front)
        val agent = TrailblockAgent(TrailblockDescription("", "Top Stories", 5)(Uk))

        agent.refresh()
        loadOrTimeout(agent)


        val trails = agent.trailblock.get.trails

        Then("I should see the Top Stories")
        //we cannot really guarantee a length here
        //but it is unlikely to ever be < 10
        trails.length should be > 10
      }
    }

    scenario("load latest trails if there are no editors picks for a block") {
      Given("I visit the Network Front")

      Fake {

        //in real life this tag will have no editors picks
        val agent = TrailblockAgent(TrailblockDescription("lifeandstyle/seasonal-food", "Seasonal food", 5)(Uk))

        agent.refresh()
        loadOrTimeout(agent)

        val trails = agent.trailblock.get.trails

        Then("I should see the latest trails for a block that has no editors picks")
        trails should have length (20) //if only latest you just get 20 latest, hence exact length
      }
    }

    scenario("load editors picks and latest") {
      Given("I visit the Network Front")

      Fake {

        //in real life this will always be a combination of editors picks + latest
        val agent = TrailblockAgent(TrailblockDescription("sport", "Sport", 5)(Uk))

        agent.refresh()
        loadOrTimeout(agent)

        val trails = agent.trailblock.get.trails

        Then("I should see a combination of editors picks and latest")
        trails.length should be > 20 //if it is a combo you get editors picks + 20 latest, hence > 20
      }
    }

    scenario("load different content for UK and US front") {
      Given("I visit the Network Front")

      Fake {

        //in real life these will always be editors picks only (content api does not do latest for front)
        val ukAgent = TrailblockAgent(TrailblockDescription("", "Top Stories", 5)(Uk))
        val usAgent = TrailblockAgent(TrailblockDescription("", "Top Stories", 5)(Us))

        ukAgent.refresh()
        usAgent.refresh()
        loadOrTimeout(ukAgent)
        loadOrTimeout(usAgent)

        val ukTrails = ukAgent.trailblock.get.trails
        val usTrails = usAgent.trailblock.get.trails

        Then("I should see UK Top Stories if I am in the UK edition")
        And("I should see US Top Stories if I am in the US edition")

        ukTrails should not equal (usTrails)
      }
    }

    scenario("de-duplicate visible trails") {

      Given("I am on the Network Front and I have not expanded any blocks")

      Fake {
        val duplicateStory = StubTrail("http://www.gu.com/1234")

        val description = TrailblockDescription("", "Name", 5)(Uk)

        val topStoriesBlock = new TrailblockAgent(description) {
          override lazy val trailblock = Some(Trailblock(description, duplicateStory :: createTrails("world", 9)))
        }

        val sportStoriesBlock = new TrailblockAgent(description) {
          override lazy val trailblock = Some(Trailblock(description, duplicateStory :: createTrails("sport", 9)))
        }

        val front = new FrontEdition(Uk, Nil) {
          override val manualAgents = Seq(topStoriesBlock, sportStoriesBlock)
        }

        Then("I should not see a link to the same piece of content twice")

        val topStories = front()(0)
        val sport = front()(1)

        topStories.trails.contains(duplicateStory) should be(true)

        sport.trails.contains(duplicateStory) should be(false)
        sport.trails should have length (9)
      }
    }

    scenario("do not de-duplicate from hidden trails") {

      Given("I am on the Network Front and I have not expanded any blocks")

      Fake {
        val duplicateStory = StubTrail("http://www.gu.com/1234")

        val description = TrailblockDescription("", "Name", 5)(Uk)
        //duplicate trail is hidden behind "more" button
        val topTrails = createTrails("news", 5) ::: duplicateStory :: createTrails("world", 4)

        val topStoriesBlock = new TrailblockAgent(description) {
          override lazy val trailblock = Some(Trailblock(description, topTrails))
        }

        val sportStoriesBlock = new TrailblockAgent(description) {
          override lazy val trailblock = Some(Trailblock(description, duplicateStory :: createTrails("sport", 9)))
        }

        val front = new FrontEdition(Uk, Nil) {
          override val manualAgents = Seq(topStoriesBlock, sportStoriesBlock)
        }

        Then("I should see a link that is a duplicate of a link that is hidden")

        val topStories = front()(0)
        val sport = front()(1)

        topStories.trails.contains(duplicateStory) should be(true)

        sport.trails.contains(duplicateStory) should be(true)
        sport.trails should have length (10)
      }
    }


    // this is so that the load balancer knows this server has a problem
    scenario("Return error if front is empty") {

      Given("I visit the network front")
      And("it is empty")

      Fake {
        val controller = new FrontController {
          override val front = new Front() {
            override def apply(path: String) = Nil
          }
        }

        Then("I should see an internal server error")
        controller.render("front")(TestRequest()).asInstanceOf[SimpleResult[AnyContent]].header.status should be(500)
      }
    }
  }

  private def loadOrTimeout(agent: TrailblockAgent) {
    val start = System.currentTimeMillis()
    while (!agent.trailblock.isDefined) {
      if (System.currentTimeMillis - start > 10000) throw new RuntimeException("Agent should have loaded by now")
    }
  }

  private def createTrails(section: String, numTrails: Int) = (1 to numTrails).toList map {
    i => StubTrail(s"http://gu.com/$section/$i")
  }
}

private case class StubTrail(url: String) extends Trail {
  override def webPublicationDate = new DateTime()

  override def linkText = ""

  override def headline = ""

  override def trailText = None

  override def section = ""

  override def sectionName = ""

  override def thumbnail = None

  override def images = Nil

  override def videoImages = Nil

  override def isLive = false
}
