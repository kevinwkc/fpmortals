// Copyright: 2017 - 2018 Sam Halliday
// License: http://www.gnu.org/licenses/gpl-3.0.en.html

package fommil
package algebra

import prelude._, Z._

trait Drone[F[_]] {
  def getBacklog: F[Int]
  def getAgents: F[Int]
}

final case class MachineNode(id: String)

trait Machines[F[_]] {
  def getTime: F[Instant]
  def getManaged: F[NonEmptyList[MachineNode]]
  def getAlive: F[Map[MachineNode, Instant]]
  def start(node: MachineNode): F[Unit]
  def stop(node: MachineNode): F[Unit]
}

// everything below this line is boilerplate that should be generated by a
// plugin. Watch out for scalaz-boilerplate
object Drone {

  def liftM[F[_]: Monad, G[_[_], _]: MonadTrans](f: Drone[F]): Drone[G[F, ?]] =
    new Drone[G[F, ?]] {
      def getBacklog = f.getBacklog.liftM[G]
      def getAgents  = f.getAgents.liftM[G]
    }

  def liftIO[F[_]: MonadIO](io: Drone[IO]): Drone[F] = new Drone[F] {
    def getBacklog = io.getBacklog.liftIO[F]
    def getAgents  = io.getAgents.liftIO[F]
  }

  sealed abstract class Ast[A]
  final case class GetBacklog() extends Ast[Int]
  final case class GetAgents()  extends Ast[Int]

  def liftF[F[_]](implicit I: Ast :<: F): Drone[Free[F, ?]] =
    new Drone[Free[F, ?]] {
      def getBacklog: Free[F, Int] = Free.liftF(I(GetBacklog()))
      def getAgents: Free[F, Int]  = Free.liftF(I(GetAgents()))
    }

  def interpreter[F[_]](f: Drone[F]): Ast ~> F = λ[Ast ~> F] {
    case GetBacklog() => f.getBacklog
    case GetAgents()  => f.getAgents
  }

}

object Machines {
  def liftM[F[_]: Monad, G[_[_], _]: MonadTrans](
    f: Machines[F]
  ): Machines[G[F, ?]] =
    new Machines[G[F, ?]] {
      def getTime                  = f.getTime.liftM[G]
      def getManaged               = f.getManaged.liftM[G]
      def getAlive                 = f.getAlive.liftM[G]
      def start(node: MachineNode) = f.start(node).liftM[G]
      def stop(node: MachineNode)  = f.stop(node).liftM[G]
    }

  def liftIO[F[_]: MonadIO](io: Machines[IO]): Machines[F] = new Machines[F] {
    def getTime                  = io.getTime.liftIO[F]
    def getManaged               = io.getManaged.liftIO[F]
    def getAlive                 = io.getAlive.liftIO[F]
    def start(node: MachineNode) = io.start(node).liftIO[F]
    def stop(node: MachineNode)  = io.stop(node).liftIO[F]
  }

  sealed abstract class Ast[A]
  final case class GetTime()                extends Ast[Instant]
  final case class GetManaged()             extends Ast[NonEmptyList[MachineNode]]
  final case class GetAlive()               extends Ast[Map[MachineNode, Instant]]
  final case class Start(node: MachineNode) extends Ast[Unit]
  final case class Stop(node: MachineNode)  extends Ast[Unit]

  def liftF[F[_]](implicit I: Ast :<: F): Machines[Free[F, ?]] =
    new Machines[Free[F, ?]] {
      def getTime                  = Free.liftF(I(GetTime()))
      def getManaged               = Free.liftF(I(GetManaged()))
      def getAlive                 = Free.liftF(I(GetAlive()))
      def start(node: MachineNode) = Free.liftF(I(Start(node)))
      def stop(node: MachineNode)  = Free.liftF(I(Stop(node)))
    }

  def interpreter[F[_]](f: Machines[F]): Ast ~> F = λ[Ast ~> F] {
    case GetTime()    => f.getTime
    case GetManaged() => f.getManaged
    case GetAlive()   => f.getAlive
    case Start(node)  => f.start(node)
    case Stop(node)   => f.stop(node)
  }

}
