import sbt._

class SantaClausProject(info: ProjectInfo) extends DefaultProject(info) with MavenizerPlugin{

	val scala_stm = "org.scala-tools" %% "scala-stm" % "0.3"

}