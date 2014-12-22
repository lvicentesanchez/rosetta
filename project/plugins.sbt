// Comment to get more information during initialization
//
logLevel := Level.Warn

// Resolvers
//

resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
      
resolvers += "Softprops Maven" at "http://dl.bintray.com/content/softprops/maven"

// Assembly
//
addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.12.0")

// Build Info
//
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.3.2")

// Dependency graph
//
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.7.4")

// Scalariform
//
addSbtPlugin("com.typesafe.sbt" % "sbt-scalariform" % "1.3.0")

// Scoverage
//
addSbtPlugin("org.scoverage" %% "sbt-scoverage" % "1.0.1")

// Update plugin
//
addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.1.7")
