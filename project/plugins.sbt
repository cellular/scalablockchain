// Custom Plugin Resolvers
credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")
libraryDependencies += "org.vafer" % "jdeb" % "1.8" artifacts Artifact("jdeb", "jar", "jar")

// Build plugins
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.8.2")
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.9.0")

// Testing plugins
addSbtPlugin("com.sksamuel.scapegoat" %% "sbt-scapegoat" % "1.1.0")
addSbtPlugin("org.scalastyle" % "scalastyle-sbt-plugin" % "1.0.0")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.6.1")
