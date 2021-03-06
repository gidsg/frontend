package conf

import common.BaseGuardianConfiguration
import com.gu.management.{ PropertiesPage, ManifestPage }
import com.gu.management.{ Manifest => ManifestFile }
import com.gu.management.play.{ Management => GuManagement }
import com.gu.management.logback.LogbackLevelPage
import implicits.Strings

object RouterConfiguration extends BaseGuardianConfiguration("frontend-router")

object ManifestData extends Strings {
  lazy val build = ManifestFile.asKeyValuePairs.getOrElse("Build", "DEV").dequote.trim
}

object Management extends GuManagement {
  val applicationName = "frontend-router"

  lazy val pages = List(
    new ManifestPage,
    new UrlPagesHealthcheckManagementPage(
      "/uk",
      "/sport/2012/sep/23/world-road-race-championship-gilbert-cavendish",
      "/football"
    ) { override val base = "http://localhost" },
    new PropertiesPage(RouterConfiguration.toString),
    new LogbackLevelPage(applicationName)
  )
}
