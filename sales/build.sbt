import Deps._
import com.typesafe.sbt.packager.docker.Cmd
import sbt.Keys._

lazy val sales = (project in file(".")).aggregate(`sales-contracts`, `sales-write-back`, `sales-write-front`, `sales-read-back`, `sales-read-front`)

lazy val `sales-contracts` = (project in file("contracts"))
  .settings(
    libraryDependencies += AkkaDDD.protocol
  )


lazy val `sales-write-back` = (project in file("write-back"))
  .settings(
    dockerExposedPorts := Seq(9101),
    javaOptions in Universal += "-DmainClass=ecommerce.sales.app.SalesBackendApp",
    libraryDependencies ++=
      Seq(AkkaDDD.core, AkkaDDD.test, AkkaDDD.eventStore, AkkaDDD.monitoring, AkkaDDD.scheduling)
  )
  .dependsOn(`sales-contracts`, lp("invoicing-contracts"), lp("shipping-contracts"), lp("commons"), lp("headquarters-event-tagging"))
  .enablePlugins(WriteBackPlugin)



lazy val `sales-write-front` = (project in file("write-front"))
  .settings(
      dockerExposedPorts := Seq(9100),
      javaOptions in Universal ++= Seq("-DmainClass=ecommerce.sales.app.SalesFrontApp"),
      libraryDependencies += AkkaDDD.writeFront,
      dockerCommands ++= Seq(
        Cmd("HEALTHCHECK", "--interval=10s --timeout=2s CMD curl -sf http://127.0.0.1:9100/ecommerce/sales/health")
      )
  )
  .dependsOn(`sales-contracts`, lp("commons"))
  .enablePlugins(HttpServerPlugin)



lazy val `sales-read-back` = (project in file("read-back"))
  .settings(
    javaOptions in Universal ++= Seq("-DmainClass=ecommerce.sales.app.SalesViewUpdateApp"),
    libraryDependencies ++= AkkaDDD.viewUpdateSql ++ Seq(AkkaDDD.eventStore)
  )
  .dependsOn(`sales-contracts`, lp("commons"))
  .enablePlugins(ApplicationPlugin)



lazy val `sales-read-front` = (project in file("read-front"))
  .settings(
    javaOptions in Universal ++= Seq("-DmainClass=ecommerce.sales.app.SalesReadFrontApp"),
    dockerExposedPorts := Seq(9110),
    dockerCommands ++= Seq(
      Cmd("HEALTHCHECK", "--interval=10s --timeout=2s CMD curl -sf http://127.0.0.1:9110/ecommerce/sales/health")
    )
  )
  .dependsOn(`sales-read-back` % "test->test;compile->compile", lp("commons"))
  .enablePlugins(HttpServerPlugin)
