package org.jetbrains.jps.incremental.scala
package local

import java.io.File

import org.jetbrains.jps.incremental.scala.data.CompilationData
import org.jetbrains.jps.incremental.scala.model.CompileOrder
import xsbti.compile.{CompileAnalysis, CompileOptions, CompileResult, DefinesClass, IncOptionsUtil, Inputs, PreviousResult, Setup}
import sbt.internal.inc.{Analysis, AnalysisStore, AnalyzingCompiler, CompileOutput, CompilerCache, Locate}
import sbt.internal.inc.javac.IncrementalCompilerJavaTools
import sbt.inc.IncrementalCompilerUtil
import sbt.util.InterfaceUtil

/**
 * @author Pavel Fatin
 */
class SbtCompiler(javac: IncrementalCompilerJavaTools, scalac: Option[AnalyzingCompiler], fileToStore: File => AnalysisStore) extends AbstractCompiler {

  private class Compilers(scalac: AnalyzingCompiler, javac: IncrementalCompilerJavaTools) extends xsbti.compile.Compilers

  def compile(compilationData: CompilationData, client: Client) {

    client.progress("Searching for changed files...")

    val order = compilationData.order match {
      case CompileOrder.Mixed => xsbti.compile.CompileOrder.Mixed
      case CompileOrder.JavaThenScala => xsbti.compile.CompileOrder.JavaThenScala
      case CompileOrder.ScalaThenJava => xsbti.compile.CompileOrder.ScalaThenJava
    }

    val compileOutput = CompileOutput(compilationData.output)

    val analysisStore = fileToStore(compilationData.cacheFile)
    val (previousAnalysis, previousSetup) = {
      analysisStore.get().map {
        case (a, s) => (a, Some(s))
      } getOrElse {
        (Analysis.Empty, None)
      }
    }

    val progress = getProgress(client)
    val reporter = getReporter(client)
    val logger = getLogger(client)

    val outputToAnalysisMap: xsbti.F1[File, xsbti.Maybe[CompileAnalysis]] = InterfaceUtil.f1(compilationData.outputToCacheMap.map { case (output, cache) =>
      val analysis = fileToStore(cache).get().map(_._1).getOrElse(Analysis.Empty)
      (output, xsbti.Maybe.just(analysis))
    })

    val incOptions = compilationData.sbtIncOptions match {
      case None => IncOptionsUtil.defaultIncOptions()
      case Some(opt) =>
        IncOptionsUtil.defaultIncOptions.withNameHashing(opt.nameHashing)
                          .withRecompileOnMacroDef(xsbti.Maybe.just(opt.recompileOnMacroDef))
                          .withTransitiveStep(opt.transitiveStep)
                          .withRecompileAllFraction(opt.recompileAllFraction)
    }

    val definesClass: xsbti.F1[File, DefinesClass] = InterfaceUtil.f1((f: File) => {
      val dc = Locate.definesClass(f)
      new DefinesClass {
        override def apply(s: String): Boolean = dc(s)
      }
    })

    val compilers = new Compilers(scalac.orNull, javac)
    val compileOptions = new CompileOptions(compilationData.classpath.toArray,
      compilationData.sources.toArray,
      compilationData.output,
      compilationData.scalaOptions.toArray,
      compilationData.javaOptions.toArray,
      100,
      InterfaceUtil.f1(identity[xsbti.Position]),
      order)
    val setup = new Setup(outputToAnalysisMap,
      definesClass,
      /*skip = */ false,
      compilationData.cacheFile,
      CompilerCache.fresh,
      incOptions,
      reporter,
      Array.empty)
    val previousResult = new PreviousResult(xsbti.Maybe.just(previousAnalysis),
      InterfaceUtil.o2m(previousSetup))
    val inputs = new Inputs(compilers,
      compileOptions,
      setup,
      previousResult)

    try {
      val incrementalCompiler = IncrementalCompilerUtil.defaultIncrementalCompiler
      val result = incrementalCompiler.compile(inputs, logger)

      analysisStore.set(result.analysis, result.setup)

    } catch {
      case _: xsbti.CompileFailed => // the error should be already handled via the `reporter`
    }
  }
}