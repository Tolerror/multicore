import java.util.Scanner;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.LinkedList;
import java.lang.Math;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;
import java.util.ArrayDeque;

import java.io.*;

class Graph {

    int s;
    int t;
    int n;
    int m;
    Node excess; // list of nodes with excess preflow
    int numThreads;
    ConcurrentLinkedQueue<Node>[] externalQueue;
    Node node[];
    Edge edge[];

    Graph(Node node[], Edge edge[], int numThreads) {
        this.node = node;
        this.n = node.length;
        this.edge = edge;
        this.m = edge.length;
        this.numThreads = numThreads;

        ConcurrentLinkedQueue<Node>[] tmp = new ConcurrentLinkedQueue[numThreads];
        for (int i = 0; i < numThreads; i++) {
            tmp[i] = new ConcurrentLinkedQueue<>();
        }
        this.externalQueue = tmp;
    }

    // returns which thread owns node
    int ownerThread(int nodeIndex) {
        int nodesPerThread = (n + numThreads - 1) / numThreads;
        return Math.min(nodeIndex / nodesPerThread, numThreads - 1);
    }

    void enqueueExternal(Node u) {
        if ((u.i == s) || (u.i == t)) {
            return;
        }
        int owner = ownerThread(u.i);
        externalQueue[owner].offer(u); // the same as add-function
    }

    Node dequeueExternal(int threadID) {
        return externalQueue[threadID].poll();
    }

    boolean hasExternalWork(int threadID) {
        return !externalQueue[threadID].isEmpty();
    }

    void enter_excess(Node u) {
        if (u != node[s] && u != node[t]) { // if u not sink or source, eligible
            u.next = excess; // linked list add in front
            excess = u;
        }
    }

    Node other(Edge a, Node u) {
        if (a.u == u)
            return a.v;
        else
            return a.u;
    }

    void relabel(Node u) {
        int minHeight = Integer.MAX_VALUE;
        for (Edge a : u.adj) {
            Node v = other(a, u);
            int residual = (u == a.u) ? a.c - a.f : a.c + a.f;
            if (residual > 0) {
                minHeight = Math.min(minHeight, v.h);
            }
        }
        u.h = minHeight + 1;

        // enter_excess(u); //u is now active
    }

    // we need to lock each node, there is no going around it
    void lockNode(Node u) {
        node[u.i].lock.lock(); // u.i gets the index -> node[u.i] get the current node -> get lock
    } // -> execute lock-method.

    void unlockNode(Node u) {
        node[u.i].lock.unlock();
    }

    void push(Node u, Node v, Edge a) {
        int d; // min capacity to push

        // check which direction we're facing
        if (u == a.u) { // if u is from
            d = Math.min(u.e, a.c - a.f); // current excess in u or the remaining capacity on edge
            a.f += d; // push forward flow on edge
        } else {
            d = Math.min(u.e, a.c + a.f); // current excess in u or flow backwards (will result in negative flow,
                                          // meaning backwards flow -c <= f <= c)
            a.f -= d; // push backward flow on edge
        }

        System.out.println("push from " + u.i +" " + v.i );
        u.e -= d; // remove excess from u
        v.e += d; // add excess to v

        // if u happen to have more excess, reinsert in active node list
        // if(u.e > 0){
        // enter_excess(u);
        // }

        // if v's excess == d, we know that v excess was previously 0 (inactive).
        // So we don't risk inserting it twice if it was already active.
        // if(v.e == d){
        // enter_excess(v);
        // }

        // unlockNode(v);
        // unlockNode(u);

    }

    int preflow(int s, int t, int numThreads) {

        ListIterator<Edge> iter;
        int b;
        Edge a;
        Node u;
        Node v;

        this.s = s;
        this.t = t;
        node[s].h = n; // set source height to number of nodes

        iter = node[s].adj.listIterator();
        // initial pushes
        while (iter.hasNext()) { // for all of sources edges
            a = iter.next();

            node[s].e += a.c; // sum up the total flow from source in source excess variable

            push(node[s], other(a, node[s]), a); // push along all nodes of source
        }

        int nodesPerThread = (n + numThreads - 1) / numThreads;

        Thread[] threads = new Thread[numThreads];
        for (int threadID = 0; threadID < numThreads; threadID++) {
            // For each Thread calculate a range of nodes to work on
            int start = threadID * nodesPerThread;
            int end = Math.min(start + nodesPerThread, n); // TODO: check the logic here.

            threads[threadID] = new Thread(new Task(this, start, end, threadID));
            threads[threadID].start();
        }

        for (Thread th : threads) {
            try {
                th.join(); // wait for all threads to finish
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return node[this.t].e; // return max flow at sink
    }
}

class Node {
    int h; // height
    int e; // excess
    int i; // index
    Node next;
    LinkedList<Edge> adj; // list of all neighbour edges
    final ReentrantLock lock = new ReentrantLock();

    Node(int i) {
        this.i = i;
        adj = new LinkedList<Edge>();
    }
}

class Edge {
    Node u;
    Node v;
    int f;
    int c;

    Edge(Node u, Node v, int c) {
        this.u = u;
        this.v = v;
        this.c = c;

    }
}

class Preflow {
    public static void main(String args[]) {
        // INPUT HANDLING %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
        double begin = System.currentTimeMillis();
        Scanner s = new Scanner(System.in);
        int n; // number of nodes
        int m; // number of edges
        int i; // node list index
        int u; // u node index
        int v; // v node index
        int c; // capacity
        int f; // flow
        Graph g;

        n = s.nextInt();
        m = s.nextInt();
        s.nextInt();
        s.nextInt();
        Node[] node = new Node[n];
        Edge[] edge = new Edge[m];

        for (i = 0; i < n; i += 1)
            node[i] = new Node(i);

        for (i = 0; i < m; i += 1) {
            u = s.nextInt(); // from node
            v = s.nextInt(); // to node
            c = s.nextInt(); // capacity
            edge[i] = new Edge(node[u], node[v], c);
            node[u].adj.addLast(edge[i]);
            node[v].adj.addLast(edge[i]);
        }

        int numThreads = 2; // TODO: Dynamically set thread count based on node count

        g = new Graph(node, edge, numThreads);
        // INPUT HANDLING %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

        f = g.preflow(0, n - 1, numThreads); // pass in source and sink indexes
        double end = System.currentTimeMillis();
        System.out.println("t = " + (end - begin) / 1000.0 + " s");
        System.out.println("f = " + f);
    }
}

class Task implements Runnable {

    private final int start, end, threadID;
    private final Graph g;
    private final Queue<Node> localq = new ArrayDeque<>();

    Task(Graph g, int start, int end, int threadID) {
        this.g = g;
        this.start = start;
        this.end = end;
        this.threadID = threadID;

        // add all active nodes to node local queue
        for (int i = start; i < end; i++) {
            if (g.node[i].e > 0 && i != g.s && i != g.t) {
                localq.add(g.node[i]);
            }
        }
    }

    private void lockNodes(Node a, Node b) {
        if (a.i < b.i) {
            a.lock.lock();
            b.lock.lock();
        } else {
            b.lock.lock();
            a.lock.lock();
        }
    }

    private void unlockNodes(Node a, Node b) {
        if (a.i < b.i) {
            a.lock.unlock();
            b.lock.unlock();
        } else {
            b.lock.unlock();
            a.lock.unlock();
        }
    }

    @Override
    public void run() {

        while (!localq.isEmpty() || g.hasExternalWork(threadID)) { // terminates if this is not true
            Node u = localq.poll();
            if (u == null) {
                u = g.dequeueExternal(threadID);
                if (u == null)
                    continue;
            }

            boolean pushed = false;

            // iterate neighbour edges of u
            for (Edge a : u.adj) {
                Node v = g.other(a, u);

                lockNodes(u, v);
                try {

                    int residual = (u == a.u) ? a.c - a.f : a.c + a.f;

                    if (residual > 0 && u.h > v.h) {
                        g.push(u, v, a);

                        // handle queues
                        if (u.e > 0 && u.i >= start && u.i < end) {
                            localq.add(u);
                        }
                        
                        // if v still has excess, add it back into local/external queue
                        if (v.e > 0) {
                            // if v is within threads range
                            if (v.i >= start && v.i < end) {
                                localq.add(v);
                            } else {
                                g.enqueueExternal(v);
                            }
                        }

                        pushed = true;
                        // break;
                    }
                } finally {
                    unlockNodes(v, u);
                }
            }

            if (!pushed) {
                u.lock.lock();
                try {
                    if (u.e > 0) {
                        if (u.i >= start && u.i < end) {
                            g.relabel(u);
                            localq.add(u);
                        }
                    }
                } finally {
                    u.lock.unlock();
                }

            }

        }
    }

}
