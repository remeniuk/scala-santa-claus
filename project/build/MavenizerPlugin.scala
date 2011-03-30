import sbt._
import FileUtilities._
import java.io.File
import scala.xml.{Node, Elem, NodeSeq}
import scala.xml.transform._

trait MavenizerPlugin extends BasicManagedProject {

  override def pomExtra =
    <build>
      <plugins>
        <plugin>
          <groupId>org.scala-tools</groupId>
          <artifactId>maven-scala-plugin</artifactId>
          <executions>
            <execution>
              <goals>
                <goal>compile</goal>
                <goal>testCompile</goal>
              </goals>
            </execution>
          </executions>
          <version>2.9.1</version>
        </plugin>
      </plugins>
    </build>
	
  lazy val multimoduleProject =
    <artifactId>{name}</artifactId> ++
  <packaging>pom</packaging> ++
  <modules>{
      subProjects.keys.map(module => <module>{module}</module>)
    }</modules>
	
  object PackagingTypeChanger extends RuleTransformer(new RewriteRule() {
      override def transform(node: Node): Seq[Node] = node match {
        case elem @ Elem(_, "packaging", _, _, _)  => multimoduleProject
        case other => other
      }
    })
	
  override def pomPostProcess(pom: Node): Node =
    if(subProjects.size > 0) PackagingTypeChanger(pom)
  else pom
							
  lazy val mavenize = task {
    log.info("Mavenizing project " + name)
    val pomPath = info.projectPath / "pom.xml"
    touch(pomPath, log)
    (outputPath ** "*.pom").get.foreach(copyFile(_, pomPath, log))
    None
  } dependsOn(makePom)
	
}
