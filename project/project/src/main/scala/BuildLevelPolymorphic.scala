package psp

import sbt._, Keys._

/** Code which we want access to in both the metabuild and the build.
 *  And if it comes up, the project itself.
 */
package object meta {
  def pluginIDs = Seq(
    "me.lessis"               % "bintray-sbt"                %  "0.1.2",
    "com.typesafe"            % "sbt-mima-plugin"            %  "0.1.6",
    "com.gilt"                % "sbt-dependency-graph-sugar" %  "0.7.4",
    "org.scoverage"          %% "sbt-scoverage"              % "0.99.7.1"
    // "com.sksamuel.scoverage" %% "sbt-coveralls"              %   "0.0.5"
  )
  implicit class ModuleIDOps(val m: ModuleID) extends AnyVal {
    def exceptScala: ModuleID = m excludeAll ExclusionRule("org.scala-lang")
    def sbtPlugin: ModuleID   = Defaults.sbtPluginExtra(m, "0.13", "2.10").exceptScala
  }
}
