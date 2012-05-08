package code
package config

import net.liftweb._
import common._
import json._
import mongodb._
import util.Props

import com.mongodb.{DBAddress, Mongo}

object MongoConfig extends Loggable {
  implicit val formats = DefaultFormats

  case class CloudFoundryMongo(name: String, label: String, plan: String, credentials: CloudFoundryMongoCredentials)
  case class CloudFoundryMongoCredentials(hostname: String, port: String, username: String, password: String, name: String, db: String)

  /*
   * This checks to see if it's running in a CloudFoundry environment and
   * gets the MongoDB info from there if it is.
   */
  def init() {
    Option(System.getenv("VCAP_SERVICES")) match {
      case Some(s) =>
        try {
          // cloud foundry environment
          parse(s) \ "mongodb-1.8" match {
            case JArray(ary) => ary foreach { mngoJson =>
              val credentials = mngoJson.extract[CloudFoundryMongo].credentials

              logger.debug("MongoDB hostname: %s".format(credentials.hostname))
              logger.debug("MongoDB port: %s".format(credentials.port))
              logger.debug("MongoDB db: %s".format(credentials.db))
              logger.debug("MongoDB username: %s".format(credentials.username))
              logger.debug("MongoDB password: %s".format(credentials.password))

              MongoDB.defineDbAuth(
                DefaultMongoIdentifier,
                new Mongo(credentials.hostname, credentials.port.toInt),
                credentials.db,
                credentials.username,
                credentials.password
              )
              logger.info("MongoDB inited on CloudFoundry: %s".format(credentials.name))
            }
            case x => logger.warn("Json parse error: %s".format(x))
          }
        }
        catch {
          case e => logger.error("Error initing Mongo: %s".format(e.getMessage))
        }
      case _ => {
        /*
         * First checks for existence of mongo.default.url. If not found, then
         * checks for mongo.default.host, port, and name. Uses defaults if those
         * are not found.
         */
        val defaultDbAddress = Props.get("mongo.default.url")
          .map(url => new DBAddress(url))
          .openOr(new DBAddress(
            Props.get("mongo.default.host", "localhost"),
            Props.getInt("mongo.default.port", 27017),
            Props.get("mongo.default.name", "lift-mongo-polyembed")
          ))

        /*
         * If mongo.default.user, and pwd are defined, configure Mongo using authentication.
         */
        (Props.get("mongo.default.user"), Props.get("mongo.default.pwd")) match {
          case (Full(user), Full(pwd)) =>
            MongoDB.defineDbAuth(
              DefaultMongoIdentifier,
              new Mongo(defaultDbAddress),
              defaultDbAddress.getDBName,
              user,
              pwd
            )
            logger.info("MongoDB inited using authentication: %s".format(defaultDbAddress.toString))
          case _ =>
            MongoDB.defineDb(
              DefaultMongoIdentifier,
              new Mongo(defaultDbAddress),
              defaultDbAddress.getDBName
            )
            logger.info("MongoDB inited: %s".format(defaultDbAddress.toString))
        }
      }
    }
  }
}
