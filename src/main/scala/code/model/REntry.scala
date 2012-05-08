package code
package model

import scala.xml.NodeSeq

import org.bson.types.ObjectId

import net.liftweb._
import common._
import http.{StringField => _, BooleanField => _, _}
import json._
import mongodb.{BsonDSL, JObjectParser}
import mongodb.record._
import mongodb.record.field._
import record.{Field, MandatoryTypedField}
import record.field._
import util.Helpers

import com.mongodb._

class REntry private() extends MongoRecord[REntry] with ObjectIdPk[REntry] {

  def meta = REntry
// entry context
  object title extends StringField(this,500)
  object description extends TextareaField(this,2000)
  object created extends DateTimeField(this)
  object updated extends DateTimeField(this)
  object IV extends DoubleField(this)
  object friendlyURL extends StringField(this,500)
  object contentType extends StringField(this,255)
  object content extends ContentField(this)
}

object REntry extends REntry with MongoMetaRecord[REntry] {
  import BsonDSL._

  override def collectionName = "REntries"

  ensureIndex(friendlyURL.name -> 1)
}

trait RContent

case class Article(
  text: String,
  author: String,
  ctype: String = "article"
) extends RContent

case class BlogPost(
  text: String,
  ctype: String = "blogpost"
) extends RContent

abstract class ContentField[OwnerType <: BsonRecord[OwnerType]](rec: OwnerType)
extends Field[RContent, OwnerType]
with MandatoryTypedField[RContent]
with MongoFieldFlavor[RContent]
{
  implicit val formats = owner.meta.formats

  def asJValue = valueBox.map(v => Extraction.decompose(v)) openOr (JNothing: JValue)

  def setFromJValue(jvalue: JValue): Box[RContent] = jvalue match {
    case JNothing|JNull => setBox(Empty)
    case json => setBox(
      Helpers.tryo((json \ "ctype").extract[String]) match {
        case Full("article") => Helpers.tryo[Article]{ json.extract[Article] }
        case Full("blogpost") => Helpers.tryo[BlogPost]{ json.extract[BlogPost] }
        case Full(x) => Failure("Unsupported ctype: "+x)
        case Empty => Empty
        case Failure(msg, _, _) => Failure(msg)
      }
    )
  }

  def asXHtml = <div></div>

  def defaultValue = null.asInstanceOf[RContent]

  def setFromAny(in: Any): Box[RContent] = in match {
    case dbo: DBObject => setFromDBObject(dbo)
    case null|None|Empty => setBox(defaultValueBox)
    case (failure: Failure) => setBox(failure)
    case _ => setBox(defaultValueBox)
  }

  override def setFromString(in: String): Box[RContent] = {
    Helpers.tryo{ JsonParser.parse(in) }.flatMap { jv =>
      setFromJValue(jv)
    }
  }

  def toForm: Box[NodeSeq] = Empty

  def owner = rec

  /*
  * Convert this field's value into a DBObject so it can be stored in Mongo.
  */
  def asDBObject: DBObject = {
    JObjectParser.parse(asJValue.asInstanceOf[JObject])
  }

  // set this field's value using a DBObject returned from Mongo.
  def setFromDBObject(dbo: DBObject): Box[RContent] = {
    val jvalue = JObjectParser.serialize(dbo)
    setFromJValue(jvalue)
  }
}
