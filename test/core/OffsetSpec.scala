package core

import cats.implicits._
import play.api.libs.json.{JsSuccess, Json}
import tests.TestSpec

class OffsetSpec extends TestSpec {

  "Offset#default" must {
    "return value 0" in {
      Offset.default mustBe Offset(0)
    }
  }

  "Offset#reads" must {
    "parse valid Offset properly (integer)" in {
      Json.parse("0").validate[Offset] mustBe JsSuccess(Offset(0))
    }
    "deny invalid Offset (integer)" in {
      Json.parse("-1").validate[Offset].isError
    }
  }

  "Offset#queryBindable" must {
    val offset = Offset(9876545)

    "bind the Offset identifier" in {
      Offset
        .queryBindable
        .bind("offset", Map("offset" -> Seq("9876545"))) mustBe
        Some(Right(offset))
    }
    "return error if not bindable by parsing error" in {
      Offset.queryBindable.bind("offset", Map("offset" -> Seq("foo"))) mustBe
        Some(Left("offset.parse.expected.int"))
    }
    "unbind the Offset identifier." in {
      Offset.queryBindable.unbind("offset", offset) mustBe "offset=9876545"
    }
  }

  "Offset#show" must {
    "render a Offset properly" in {
      Offset(1337).show mustBe "1337"
    }
  }

  "Offset#parser" must {
    "use the defined implicit" in {
      Offset.parse.run("1000001") mustBe Right(Offset(1000001))
    }
    "unparsable Offset" in {
      Offset.parse.run("bloerk") mustBe Left("parse.expected.int")
    }
  }

  "BookingNumber#validate" must {
    "validated Offset" in {
      Offset.validate.run(Offset(1000000)) mustBe Right(Offset(1000000))
    }
    "invalidated Offset" in {
      Offset.validate.run(Offset(-1)) mustBe Left("validate.range.min")
    }
  }

}
