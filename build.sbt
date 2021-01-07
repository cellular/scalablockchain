name := "TesseractBlockchain"

lazy val root = (project in file("."))
  .enablePlugins(
    BuildInfoPlugin,
    JDebPackaging,
    PlayScala,
    PlayNettyServer,
    SystemdPlugin
  )
  .disablePlugins(
    PlayAkkaHttpServer
  )
  .settings(
    buildInfoKeys := BuildInfoKey.ofN(name, version, scalaVersion, sbtVersion),
    buildInfoPackage := organization.value
  )

PlayKeys.devSettings += "play.server.provider" -> "play.core.server.NettyServerProvider"

scalacOptions in Compile ++= Seq("-deprecation", "-explaintypes", "-feature", "-unchecked")
scalaVersion := "2.13.2"
fork in Test := true // @see https://github.com/sbt/sbt/issues/3022
testOptions in Test += Tests.Argument(TestFrameworks.ScalaTest, "-oSD")

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
libraryDependencies ++= Seq(
  guice,
  ws,
  "org.typelevel" %% "cats-effect" % "2.3.1",
  "dev.zio" %% "zio-interop-cats" % "2.2.0.1",
  "dev.zio" %% "zio" % "1.0.3",
  "org.bouncycastle" % "bcpkix-jdk15to18" % "1.66",
  "org.mockito" %% "mockito-scala-scalatest" % "1.14.8" % "test",
  "org.mockito" % "mockito-inline" % "3.3.3" % "test",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % "test",
  "org.scalatestplus" %% "scalacheck-1-14" % "3.2.0.0" % "test",
  "org.scalacheck" %% "scalacheck" % "1.14.3" % "test",
  "com.danielasfregola" %% "random-data-generator" % "2.8" % "test",
  ("io.netty" % "netty-transport-native-epoll" % "4.1.50.Final").classifier("linux-x86_64")
)

// swagger-play last working version works with jackson 2.10.2 and below.
dependencyOverrides += "com.fasterxml.jackson.module"  %% "jackson-module-scala" % "2.10.2"

maintainer in Linux := "ronakkany"
packageSummary in Linux := "TesseractBlockchain"
packageDescription := "TesseractBlockchain"
debianPackageDependencies in Debian := Seq("default-jre | java8-core.util.runtime | java8-core.util.runtime-headless")
daemonUser in Linux := normalizedName.value
daemonGroup in Linux := (daemonUser in Linux).value

publishArtifact in(Compile, packageDoc) := false
sources in(Compile, doc) := Seq.empty

scapegoatConsoleOutput := true
scapegoatIgnoredFiles := Seq(".*routes.main..*Routes.scala")
scapegoatVersion in ThisBuild := "1.4.4"
scapegoatDisabledInspections := Seq("VariableShadowing")

//coverageEnabled in Test := true
coverageFailOnMinimum := true
coverageHighlighting := true
coverageMinimum := 100
coverageExcludedPackages := """<empty>;.*.Module;.*controllers\..*Reverse.*;router.Routes.*"""

scalastyleFailOnError := true
