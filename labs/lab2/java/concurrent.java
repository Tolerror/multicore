import java.util.Scanner;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.LinkedList;
import java.lang.Math;
import java.util.concurrent.locks.ReentrantLock;

import java.io.*;

class Graph {

	int	s;
	int	t;
	int	n;
	int	m;
	Node	excess;		// list of nodes with excess preflow
	Node	node[];
	Edge	edge[];

	Graph(Node node[], Edge edge[])
	{
		this.node	= node;
		this.n		= node.length;
		this.edge	= edge;
		this.m		= edge.length;
	}

	void enter_excess(Node u)
	{
		if (u != node[s] && u != node[t]) { //if u not sink or source, eligible
			u.next = excess;    //linked list add in front
			excess = u;
		}
	}

    Node other(Edge a, Node u){
        if (a.u == u)	
        return a.v;
        else
        return a.u;
    }

	void relabel(Node u){
        u.h++;
        // enter_excess(u);    //u is now active
	}

    //we need to lock each node, there is no going around it
    void lockNode(Node u){
       node[u.i].lock.lock(); 
    }

    void lockNode(Node u){
        node[u.i].lock.lock();
    }

	void push(Node u, Node v, Edge a){
        int d;  //min capacity to push

        lockNode(u);    //we need to make sure to lock all nodes in the same order, lock first u then v
        //check which direction we're facing
        if(u == a.u){   //if u is from
            d = Math.min(u.e, a.c - a.f);  //current excess in u or the remaining capacity on edge
            a.f += d;     //push forward flow on edge  
        }else{
            d = Math.min(u.e, a.c + a.f); //current excess in u or flow backwards (will result in negative flow, meaning backwards flow -c <= f <= c)
            a.f -= d;   //push backward flow on edge
        }

        u.e -= d;   //remove excess from u
        v.e += d;   //add excess to v

        
        //if u happen to have more excess, reinsert in active node list
        if(u.e > 0){
            enter_excess(u);
        }

        //if v's excess == d, we know that v excess was previously 0 (inactive). 
        //So we don't risk inserting it twice if it was already active.
        if(v.e == d){   
            enter_excess(v);
        }

        unlockNode(u);
        
	}

	int preflow(int s, int t){

		ListIterator<Edge>	iter;
		int			    b;
		Edge			a;
		Node			u;
		Node			v;
		
		this.s = s;
		this.t = t;
		node[s].h = n;  //set source height to number of nodes

		iter = node[s].adj.listIterator();
		while (iter.hasNext()) {    //for all of sources edges
			a = iter.next();

			node[s].e += a.c;   //sum up the total flow from source in source excess variable

			push(node[s], other(a, node[s]), a);    //push along all nodes of source
		}

		while (excess != null) {    //as long as active node list isnt empty
			u = excess; //set u to node fetched from list
			v = null;   //initially null, will set after
			a = null;   //initially null, will set after
			excess = u.next;    //next node

            boolean pushed = false; 
			iter = u.adj.listIterator();    
			while (iter.hasNext()) {    //iterate through all of u's adjacent edges
				a = iter.next();    //fetch one edge
                v = other(a, u);    //other end node of edge

                int residual;
                if(u == a.u){   //if going from u
                    residual = a.c - a.f;   //residual = remaining flow capacity of edge
                }else{          //if going from v
                    residual = a.c + a.f;   //residual = all capacity on edge + currently flowing 
                }

                //push requirement: if res > 0 AND height of pushing node > height of other node
                if(residual > 0 && u.h > v.h){  
                    push(u,v,a);    //push along u -> v on edge a
                    pushed = true;  
                    break;  //we managed to push along one of u's edges, break out of checking remaining edges
                }

			}

            if(!pushed){    //if we didnt manage to push, relabel u
                relabel(u);
            }

		// 	if (v != null)
		// 		push(u, v, a);
		// 	else
		// 		relabel(u);
		}

		return node[t].e;   //return max flow at sink
	}
}

class Node {
    int	h;  //height
    int	e;  //excess
    int	i;  //index
    Node	next;
    LinkedList<Edge>	adj;    //list of all neighbour edges
    final ReentrantLock lock = new ReentrantLock();

    Node(int i){
        this.i = i;
        adj = new LinkedList<Edge>();
    }
}

class Edge {
	Node	u;
	Node	v;
	int	f;
	int	c;

	Edge(Node u, Node v, int c)
	{
		this.u = u;
		this.v = v;
		this.c = c;

	}
}

class Preflow {
	public static void main(String args[])
	{
        //INPUT HANDLING %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
		double	begin = System.currentTimeMillis();
		Scanner s = new Scanner(System.in);
		int	n;  //number of nodes
		int	m;  //number of edges
		int	i;
		int	u;
		int	v;
		int	c;
		int	f;
		Graph	g;

		n = s.nextInt();
		m = s.nextInt();
		s.nextInt();
		s.nextInt();
		Node[] node = new Node[n];
		Edge[] edge = new Edge[m];

		for (i = 0; i < n; i += 1)
			node[i] = new Node(i);

		for (i = 0; i < m; i += 1) {
			u = s.nextInt();    //from node
			v = s.nextInt();    //to node
			c = s.nextInt();    //capacity 
			edge[i] = new Edge(node[u], node[v], c);
			node[u].adj.addLast(edge[i]);
			node[v].adj.addLast(edge[i]);
		}

		g = new Graph(node, edge);
        //INPUT HANDLING %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
       
        int NUM_THREADS = 8;    //TODO: to dynamically and efficiently set NUM_THREADS based on number of nodes

        //Thread work delegation
        int nodesPerThread = (n + NUM_THREADS - 1) / NUM_THREADS;

        // ReentrantLock[] nodeLocks = new ReentrantLock[n];
        // for(int i = 0 ; i < n ; i++) nodeLocks[i] = new ReentrantLock();

        Thread[] threads = new Thread[NUM_THREADS];
        for(int threadID = 0 ; threadID < NUM_THREADS ; threadID++){
            //For each Thread calculate a range of nodes to work on
            int start = threadID * nodesPerThread;
            int end = Math.min(start + nodesPerThread, n);

            threads[threadID] = new Thread(new Task(g, start, end));
            threads[threadID].start();
        }

        for(Thread t : threads) t.join();   //wait for all threads to finish

		// f = g.preflow(0, n-1);  //pass in source and sink indexes
		double	end = System.currentTimeMillis();
		System.out.println("t = " + (end - begin) / 1000.0 + " s");
		System.out.println("f = " + f);
	}
}


class Task implements Runnable{

    private final int start, end;
    private final Graph g;
    private final Queue<Node> localq = new ArrayDeque<>();

    Task(Graph g, int start, int end){
        this.g = g;
        this.start = start;
        this.end = end;

        for(int i = start ; i < end ; i++){
            if(g.node[i].e > 0 && i != g.s && i != g.t){
                localq.add(g.node[i]);
            }
        }

    }

    @Override
    public void run(){
        while(!localq.isEmpty()){
            Node u = localq.poll();

            boolean pushed = false;
            for(Edge a : u.adj){
                Node v = g.other(a, u);

                int residual = (u == a.u) ? a.c-a.f : a.c+a.f;

                if(residual > 0 && u.h > v.h){

                    //is node outside threads assigned chunk? Lock it
                    g.lockEdge(a);
                    try{
                        g.push(u,v,a);
                    }finally{   //ensure that we unlock before leaving
                        g.unlockEdge(a)
                        }
                    }

                        //if u still has excess, add it back into the local queue
                        if(u.e > 0 && u.i >= start && u.i < end){
                            localq.add(u);
                        }

                        //if v still has excess, add it back into local/external queue
                        if(v.e > 0){
                            //if v is within threads range
                            if(v.i >= start && v.i < end){
                                localq.add(v);
                            }else{
                                g.enqueueExternal(v);
                            }
                        }

                        pushed = true;
                        break;
                    }
                }

                if(!pushed){
                    g.relabel(u);
                    if(u.e > 0){
                        localq.add(u);
                    }
                }

            }
        }


    }

}
