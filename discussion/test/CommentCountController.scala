package test

import org.scalatest.FlatSpec
import org.scalatest.matchers.ShouldMatchers
import play.api.test.Helpers._
import play.api.test.FakeRequest

class CommentCountControllerTest extends FlatSpec with ShouldMatchers {

  val callbackName = "foo"

  "Discussion" should "return 200" in Fake {
    val result = controllers.CommentCountController.render("p/3gd58")(TestRequest())
    status(result) should be(200)
  }

  it should "return JSONP when callback is supplied" in Fake {
    val fakeRequest = FakeRequest(GET, "/discussion/comment-counts.json?shortUrls=/p/3gd58&callback=" + callbackName).withHeaders("host" -> "localhost:9000")
    val result = controllers.CommentCountController.render("/p/3gd58")(fakeRequest)

    status(result) should be(200)
    contentType(result).get should be("application/javascript")
    contentAsString(result) should startWith(callbackName + "({\"counts\"") // the callback
  }

}
