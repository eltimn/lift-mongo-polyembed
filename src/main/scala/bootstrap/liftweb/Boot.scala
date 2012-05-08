package bootstrap.liftweb

import net.liftweb._
import common._
import http._
import util._
import util.Helpers._

import code.config._
import code.lib.Gravatar
import code.model.User

import net.liftmodules.mongoauth.MongoAuth

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot extends Loggable {
  def boot {
    logger.info("Run Mode: "+Props.mode.toString)

    // init mongodb
    MongoConfig.init()

    // init auth-mongo
    MongoAuth.authUserMeta.default.set(User)
    MongoAuth.loginTokenAfterUrl.default.set(Site.password.url)
    MongoAuth.siteName.default.set("lift-mongo-polyembed")
    MongoAuth.systemEmail.default.set("info@") // TODO: Set me
    MongoAuth.systemUsername.default.set("lift-mongo-polyembed Staff")

    // For S.loggedIn_? and TestCond.loggedIn/Out builtin snippet
    LiftRules.loggedInTest = Full(() => User.isLoggedIn)

    // checks for ExtSession cookie
    LiftRules.earlyInStateful.append(User.testForExtSession)

    // Gravatar
    Gravatar.defaultImage.default.set("wavatar")

    // config an email sender
    SmtpMailer.init

    // where to search snippet
    LiftRules.addToPackages("code")

    // set the default htmlProperties
    LiftRules.htmlProperties.default.set((r: Req) => new Html5Properties(r.userAgent))

    // Build SiteMap
    LiftRules.setSiteMap(Site.siteMap)

    // Error handler
    ErrorHandler.init

    // 404 handler
    LiftRules.uriNotFound.prepend(NamedPF("404handler") {
      case (req, failure) =>
        NotFoundAsTemplate(ParsePath(List("404"), "html", false, false))
    })

    // Show the spinny image when an Ajax call starts
    LiftRules.ajaxStart =
      Full(() => LiftRules.jsArtifacts.show("/img/spinner").cmd)

    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd =
      Full(() => LiftRules.jsArtifacts.hide("/img/spinner").cmd)

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))
  }
}
