package org.jetbrains.jps.incremental.scala
package local

import java.io.File

import org.jetbrains.jps.incremental.scala.data.{CompilerData, CompilerJars, SbtData}
import sbt.internal.inc.{AnalysisStore, AnalyzingCompiler}

/**
 * @author Pavel Fatin
 */
trait CompilerFactory {
  def createCompiler(compilerData: CompilerData, client: Client, fileToStore: File => AnalysisStore): Compiler

  def getScalac(sbtData: SbtData, compilerJars: Option[CompilerJars], client: Client): Option[AnalyzingCompiler]
}
