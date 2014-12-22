import sbt.Keys._

// sbt-buildinfo
import sbtbuildinfo.Plugin._

// sbt-dependecy-graph
import net.virtualvoid.sbt.graph.Plugin._

// sbt-scalariform
import com.typesafe.sbt.SbtScalariform._
import scalariform.formatter.preferences._

// scoverage
import scoverage.ScoverageSbtPlugin._

// Resolvers
resolvers ++= Seq(
  "conjars" at "http://conjars.org/repo",
  "jboss nexus" at "https://repository.jboss.org/nexus/content/groups/public",
  "scalaz bintray" at "https://dl.bintray.com/scalaz/releases",
  "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
  "spray repo" at "http://repo.spray.io"
)

// Dependencies

val testDependencies = Seq (
  "com.typesafe.akka"          %% "akka-testkit"             % "2.3.8"  % "test",
  "org.scalacheck"             %% "scalacheck"               % "1.12.1" % "test",
  "org.specs2"                 %% "specs2"                   % "2.4.15" % "test"
)

val rootDependencies = Seq(
  "commons-daemon"             %  "commons-daemon"           % "1.0.15",
  "ch.qos.logback"             %  "logback-classic"          % "1.1.2",
  "ch.qos.logback"             %  "logback-core"             % "1.1.2",
  "com.amazonaws"              %  "aws-java-sdk-sqs"         % "1.9.13",
  "joda-time"                  %  "joda-time"                % "2.6",
  "com.github.nscala-time"     %% "nscala-time"              % "1.6.0",
  "com.typesafe"               %  "config"                   % "1.2.1",
  "com.typesafe.akka"          %% "akka-actor"               % "2.3.8",
  "com.typesafe.akka"          %% "akka-slf4j"               % "2.3.8",
  "com.typesafe.akka"          %% "akka-stream-experimental" % "1.0-M1",
  "io.argonaut"                %% "argonaut"                 % "6.1-M5",
  "io.dropwizard.metrics"      %  "metrics-graphite"         % "3.1.0",
  "net.ceedubs"                %% "ficus"                    % "1.1.1",
  "org.joda"                   %  "joda-convert"             % "1.7",
  "org.scalaz"                 %% "scalaz-core"              % "7.1.0"
)

val dependencies =
  rootDependencies ++
  testDependencies

// Settings
//
val forkedJvmOption = Seq(
  "-server",
  "-Dfile.encoding=UTF8",
  "-Duser.timezone=GMT",
  "-Xss1m",
  "-Xms2048m",
  "-Xmx2048m",
  "-XX:+CMSClassUnloadingEnabled",
  "-XX:ReservedCodeCacheSize=256m",
  "-XX:+DoEscapeAnalysis",
  "-XX:+UseConcMarkSweepGC",
  "-XX:+UseParNewGC",
  "-XX:+UseCodeCacheFlushing",
  "-XX:+UseCompressedOops"
)

val buildSettings = Seq(
  name := "rosetta",
  organization := "io.github.lvicentesanchez",
  scalaVersion := "2.11.4",
  scalaBinaryVersion := "2.11"
)

val compileSettings = Seq(
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-language:_",
    "-unchecked",
    //"-Xfatal-warnings",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-dead-code", // N.B. doesn't work well with the ??? hole
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Xfuture",
    "-Ywarn-unused-import"//, // 2.11 only
    //"-Yno-predef",
    //"-Yno-imports"
  )
)

val formatting =
  FormattingPreferences()
    .setPreference(AlignParameters, true)
    .setPreference(AlignSingleLineCaseStatements, false)
    .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 40)
    .setPreference(CompactControlReadability, false)
    .setPreference(CompactStringConcatenation, false)
    .setPreference(DoubleIndentClassDeclaration, true)
    .setPreference(FormatXml, true)
    .setPreference(IndentLocalDefs, false)
    .setPreference(IndentPackageBlocks, true)
    .setPreference(IndentSpaces, 2)
    .setPreference(IndentWithTabs, false)
    .setPreference(MultilineScaladocCommentsStartOnFirstLine, false)
    .setPreference(PlaceScaladocAsterisksBeneathSecondAsterisk, false)
    .setPreference(PreserveSpaceBeforeArguments, false)
    .setPreference(PreserveDanglingCloseParenthesis, true)
    .setPreference(RewriteArrowSymbols, false)
    .setPreference(SpaceBeforeColon, false)
    .setPreference(SpaceInsideBrackets, false)
    .setPreference(SpaceInsideParentheses, false)
    .setPreference(SpacesWithinPatternBinders, true)


val pluginsSettings =
  buildInfoSettings ++
  compileSettings ++
  buildSettings ++
  graphSettings ++
  scalariformSettings

val settings = Seq(
  libraryDependencies ++= dependencies,
  fork in run := true,
  fork in Test := true,
  fork in testOnly := true,
  connectInput in run := true,
  javaOptions in run ++= forkedJvmOption,
  javaOptions in Test ++= forkedJvmOption,
  mainClass in (Compile, run) := Option("io.github.lvicentesanchez.rosetta.RosettaRunner"),
  // assembly
  //
  jarName in assembly <<= (name, version) map ( (n, v) => s"$n-$v.jar" ),
  mainClass in assembly := Option("io.github.lvicentesanchez.rosetta.RosettaDaemon"),
  // build info
  //
  sourceGenerators in Compile <+= buildInfo,
  buildInfoKeys := Seq[BuildInfoKey](name, version),
  buildInfoPackage := "io.github.lvicentesanchez.rosetta.info",
  // formatting
  //
  ScalariformKeys.preferences := formatting,
  // scoverage
  //
  ScoverageKeys.coverageExcludedPackages := "<empty>;io\\.github\\.lvicentesanchez\\.daemon\\..*;io\\.github\\.lvicentesanchez\\.rosetta\\.Rosetta.*",
  // trick that makes provided dependencies work when running
  //
  run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in (Compile, run), runner in (Compile, run))
) ++ addArtifact(artifact in (Compile, assembly), assembly)

lazy val main =
  project
    .in(file("."))
    .configs(IntegrationTest)
    .settings(
      pluginsSettings ++ Defaults.itSettings ++ settings:_*
    )
