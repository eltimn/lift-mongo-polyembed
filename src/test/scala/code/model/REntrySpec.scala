package code
package model

import net.liftweb.common.Full

class REntrySpec extends MongoBaseSpec {
  "REntry" should {
    "save Article properly" in {
      val rec = REntry.createRecord
      val art = Article("blah blah", "Me")
      rec.content(art)
      rec.save

      val rec2 = REntry.find(rec.id.is)
      rec2 map { r2 =>
        r2.content.is should be (rec.content.is)
      }
    }
    "save BlogPost properly" in {
      val rec = REntry.createRecord
      val bp = BlogPost("blah blah")
      rec.content(bp)
      rec.save

      val rec2 = REntry.find(rec.id.is)
      rec2 map { r2 =>
        r2.content.is should be (rec.content.is)
      }
    }
  }
}
