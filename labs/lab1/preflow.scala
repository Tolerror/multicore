import scala.util._
import java.util.Scanner
import java.io._
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.{Await,ExecutionContext,Future,Promise}
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.io._

case class Flow(f: Int)
case class Debug(debug: Boolean)
case class Control(control:ActorRef)
case class Source(n: Int)

case class Push(from: ActorRef, edge: Edge, amount: Int)  //Push message to send
case class Active(n:ActorRef)
case class Inactive(n:ActorRef)
case class Pending(n:Int)
case class HeightRequest(node: ActorRef)
case class HeightResponse(node: ActorRef, h: Int)

case object Print
case object Start
case object Excess
case object Maxflow
case object Sink
case object Hello
case object Work
case object HeightRequest

class Edge(var u: ActorRef, var v: ActorRef, var c: Int) {
	var	f = 0
}

class Node(val index: Int) extends Actor {
	var	e = 0;				/* excess preflow. 						*/
	var	h = 0;				/* height. 							*/
	var	control:ActorRef = null		/* controller to report to when e is zero. 			*/
	var	source:Boolean	= false		/* true if we are the source.					*/
	var	sink:Boolean	= false		/* true if we are the sink.					*/
	var	edge: List[Edge] = Nil		/* adjacency list with edge objects shared with other nodes.	*/
	var	debug = false			/* to enable printing.						*/
    
	
	def min(a:Int, b:Int) : Int = { if (a < b) a else b }

	def id: String = "@" + index;

	def other(a:Edge, u:ActorRef) : ActorRef = { if (u == a.u) a.v else a.u }

	def status: Unit = { if (debug) println(id + " e = " + e + ", h = " + h) }

	def enter(func: String): Unit = { if (debug) { println(id + " enters " + func); status } }
	def exit(func: String): Unit = { if (debug) { println(id + " exits " + func); status } }

	def relabel : Unit = {

		enter("relabel")

		h += 1

		exit("relabel")
        control ! Active(self)
	}


    def tryPush(): Unit = {
      edges.foreach{ edge => 
       val otherNode = other(edge,self) 
      }
    }

	def receive = {

	case Debug(debug: Boolean)	=> this.debug = debug

	case Print => status

	case Excess => { sender ! Flow(e) /* send our current excess preflow to actor that asked for it. */ }

	case edge:Edge => { this.edge = edge :: this.edge /* put this edge first in the adjacency-list. */ }

	case Control(control:ActorRef)	=> this.control = control

	case Sink	=> { sink = true }

	case Source(n:Int)	=> { h = n; source = true; control ! Active(self) }

    case Push(from, e, delta) => {
      this.e += delta   //increase excess
      control ! Pending(-1)

      if(!sink && e > 0){
        control ! Active(self)
      }
    } 

    case Work => {
      if (source && e == 0){
        //initial pushes
        edges.foreach{ edge => 
          if(self == edge.u){
            val delta = edge.c
            edge.f += delta
            e -= delta
            edge.v ! Push(self, edge, delta)
            control ! Pending(1)
          }
        }
      }

      if(e > 0 && !sink){
        //push logic
        var pushed = false

        //push to all neighbour nodes
        edges.foreach { edge => 
          val otherNode = other(edge,self)
          val residual = if(self == edge.u) edge.c - edge.f else edge.f
          val delta = min(e, residual) //min between current nodes excess and residual

          if(delta > 0){
            otherNode ! HeightRequest //if we have flow to push check other nodes height
            pushed = true
          }
        }

        if(!pushed && e > 0){
          //couldnt push to neighbours -> relabel
          relabel()
          // control ! Active(self)  //might have to uncomment this
        }

      }
    }

    case HeightRequest => sender ! HeightResponse(self, h) //send node height to requesting sender

    case HeightResponse(node, otherHeight) => {
      //find edge to responding node
      edges.find(edge => other(edge, self) == node).foreach{ edge => 
        val residual = if(self == edge.u) edge.c - edge.f else edge.f
        val delta = min(e, residual)

        if(delta > 0 && this.h > otherHeight){
          //Requirements met, push
          if(self == edge.u) edge.f += delta else edge.f -= delta
          e -= delta
        node ! Push(self, edge, delta)
        control ! Pending(1)
        }
      }
      
      //Check activity after pushes
      if(e > 0){ 
        control ! Active(self) 
      }else{
        control ! Inactive(self)
      }

    }

    case _		=> {
      println("" + index + " received an unknown message" + _) 
    }

    assert(false)
    }

    // def push(e: Edge): Unit = {
    //   val v = other(e, self) //find other endpoint
    //   val residual = 
    //     if(self == e.u) e.c - e.f //if starting node
    //     else e.f  //latter node
    //
    //     val delta = min(this.e, residual) //min to push
    //
    //     if(delta > 0 && this.h > getHeight(v) + 1){
    //       if(self == e.u) e.f += delta else e.f -= delta
    //
    //       this.e -= delta //in any case reduce edge flow
    //       updateActivity()
    //       control ! Pending(1)
    //       v ! Push(self, e, delta)
    //     }
    // }

}


class Preflow extends Actor
{
	var	s	= 0;			/* index of source node.					*/
	var	t	= 0;			/* index of sink node.					*/
	var	n	= 0;			/* number of vertices in the graph.				*/
	var	edge:Array[Edge]	= null	/* edges in the graph.						*/
	var	node:Array[ActorRef]	= null	/* vertices in the graph.					*/
	var	ret:ActorRef 		= null	/* Actor to send result to.					*/
    var activeNodes = Set.empty[ActorRef]
    var pendingMessages = 0

	def receive = {

	case node: Array[ActorRef]	=> {
		this.node = node
		n = node.size
		s = 0
		t = n-1
		for (u <- node)
			u ! Control(self)

        //initialize sink and source
        node(0) ! Source(node.length)
        node(node.length - 1) ! Sink
	}

	case edge:Array[Edge] => this.edge = edge

	case Flow(f:Int) => {
		ret ! f			/* somebody (hopefully the sink) told us its current excess preflow. */
	}

	case Maxflow => {
		ret = sender
        checkDone()
        // if(activeNodes.isEmpty){
        //   node(t) ! Excess	/* ask sink for its excess preflow (which certainly still is zero). */
        // }
	}

    case Active(n) => { 
      if(!activeNodes.contains(n)){
        activeNodes += n 
        n ! Work
      }
    }

    case Inactive(n) => { 
      activeNodes -= n 
      checkDone()
    }


    case Pending(d) => {
      pendingMessages += d
      checkDone()
    }

    // case HeightRequest(node) => {
    //   val height = heightCache
    // }

	}

    def checkDone(): Unit = {
      if(ret != null && activeNodes.isEmpty && pendingMessages == 0){ //done only when no active nodes remain and no messages are pending
        node.last ! Excess
      }

    } 

}

object main extends App {
	implicit val t = Timeout(4 seconds);

	val	begin = System.currentTimeMillis()
	val system = ActorSystem("Main")
	val control = system.actorOf(Props[Preflow], name = "control")

	var	n = 0;
	var	m = 0;
	var	edge: Array[Edge] = null
	var	node: Array[ActorRef] = null

	val	s = new Scanner(System.in);

	n = s.nextInt
	m = s.nextInt

	/* next ignore c and p from 6railwayplanning */
	s.nextInt
	s.nextInt

	node = new Array[ActorRef](n)

	for (i <- 0 to n-1)
		node(i) = system.actorOf(Props(new Node(i)), name = "v" + i)

	edge = new Array[Edge](m)

	for (i <- 0 to m-1) {

		val u = s.nextInt
		val v = s.nextInt
		val c = s.nextInt

		edge(i) = new Edge(node(u), node(v), c)

		node(u) ! edge(i)
		node(v) ! edge(i)
	}

	control ! node
	control ! edge

	val flow = control ? Maxflow
	val f = Await.result(flow, t.duration)

	println("f = " + f)

	system.stop(control);
	system.terminate()

	val	end = System.currentTimeMillis()

	println("t = " + (end - begin) / 1000.0 + " s")
}
