import sbt._

object BintrayJetbrains {

  def jbBintrayResolver(name: String, repo: String, patterns: Patterns) =
    Resolver.url(name, url(s"http://dl.bintray.com/jetbrains/$repo"))(patterns)

  def jbSbtResolver(name: String, patterns: Patterns) =
    jbBintrayResolver(name, "sbt-plugins", patterns)

  object Resolvers {
    val mavenPatched  = "jb-maven-patched" at "http://dl.bintray.com/jetbrains/maven-patched/"
    val scalaTestFindersPatched  = jbBintrayResolver("scalatest-finders-patched", "scalatest", Resolver.ivyStylePatterns)
    val scalaPluginDeps  = jbBintrayResolver("scala-plugin-deps", "scala-plugin-deps", Resolver.ivyStylePatterns)
    val structureCore = jbSbtResolver("jb-structure-core", Resolver.ivyStylePatterns)
    val structureExtractor012   = jbSbtResolver("jb-structure-extractor-0.12", Patterns.structureExtractor012)
    val structureExtractor013   = jbSbtResolver("jb-structure-extractor-0.13", Patterns.structureExtractor013)
    val structureExtractor100M4 = {
      val defaultLocal = Resolver.defaultLocal
      FileRepository("jb-structure-extractor-1.0.0-M4", defaultLocal.configuration, Patterns.structureExtractor100M4)
    }
  }

  object Patterns {
    val structureExtractor012   = sbt.Patterns(false, "[organisation]/[module]/scala_2.9.2/sbt_0.12/[revision]/[type]s/[artifact](-[classifier]).[ext]")
    val structureExtractor013   = sbt.Patterns(false, "[organisation]/[module]/scala_2.10/sbt_0.13/[revision]/[type]s/[artifact](-[classifier]).[ext]")
    val structureExtractor100M4 = sbt.Patterns(false, s"$${ivy.home}/local/[organisation]/[module]/scala_2.11/sbt_1.0.0-M4/[revision]/[type]s/[artifact](-[classifier]).[ext]")
  }

  val allResolvers = Seq(Resolvers.mavenPatched, Resolvers.structureCore,
                         Resolvers.structureExtractor012, Resolvers.structureExtractor013, Resolvers.structureExtractor100M4,
                         Resolvers.scalaTestFindersPatched, Resolvers.scalaPluginDeps)
}
