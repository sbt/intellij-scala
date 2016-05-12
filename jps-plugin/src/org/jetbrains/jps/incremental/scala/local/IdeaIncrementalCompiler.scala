package org.jetbrains.jps.incremental.scala
package local

import java.io.File

import org.jetbrains.jps.incremental.scala.data.CompilationData
import sbt.internal.inc.{AnalyzingCompiler, CompilerArguments, CompileOutput, CompilerCache}
import xsbti.api.{ClassLike, DependencyContext}
import xsbti.compile.DependencyChanges
import xsbti.{Position, Severity}

/**
 * Nikolay.Tropin
 * 11/18/13
 */
class IdeaIncrementalCompiler(scalac: AnalyzingCompiler) extends AbstractCompiler {
  def compile(compilationData: CompilationData, client: Client): Unit = {
    val progress = getProgress(client)
    val reporter = getReporter(client)
    val logger = getLogger(client)
    val clientCallback = new ClientCallback(client)

    val out =
      if (compilationData.outputGroups.size <= 1) CompileOutput(compilationData.output)
      else CompileOutput(compilationData.outputGroups: _*)
    val cArgs = new CompilerArguments(scalac.scalaInstance, scalac.cp)
    val options = "IntellijIdea.simpleAnalysis" +: cArgs(Nil, compilationData.classpath, None, compilationData.scalaOptions)

    try scalac.compile(compilationData.sources, emptyChanges, options, out, clientCallback, reporter, CompilerCache.fresh, logger, Option(progress))
    catch {
      case _: xsbti.CompileFailed => // the error should be already handled via the `reporter`
    }
  }

}

private class ClientCallback(client: Client) extends ClientCallbackBase {


  override def generatedNonLocalClass(source: File, classFile: File, binaryClassName: String, srcClassName: String): Unit = {
    client.generated(source, classFile, binaryClassName)
  }

  // override def endSource(source: File) {
  //   client.processed(source)
  // }

  override def nameHashing() = true
}

abstract class ClientCallbackBase extends xsbti.AnalysisCallback {

  def api(x$1: File,x$2: ClassLike): Unit = {}
  def binaryDependency(x$1: File,x$2: String,x$3: String,x$4: File,x$5: DependencyContext): Unit = {}
  def classDependency(x$1: String,x$2: String,x$3: DependencyContext): Unit = {}
  def generatedLocalClass(x$1: File,x$2: File): Unit = {}
  def generatedNonLocalClass(x$1: File,x$2: File,x$3: String,x$4: String): Unit = {}
  def problem(x$1: String,x$2: Position,x$3: String,x$4: Severity,x$5: Boolean): Unit = {}
  def startSource(x$1: File): Unit = {}
  def usedName(x$1: String,x$2: String): Unit = {}

}

private object emptyChanges extends DependencyChanges {
  val modifiedBinaries = new Array[File](0)
  val modifiedClasses = new Array[String](0)
  def isEmpty = true
}