#[macro_use] extern crate text_io;

use std::sync::{Mutex,Arc,Barrier};
use std::collections::LinkedList;
use std::cmp;
use std::thread;
use std::collections::VecDeque;
use std::sync::OnceLock;


struct Node {
	i:	usize,			/* index of itself for debugging.	*/
	e:	i32,			/* excess preflow.			*/
	h:	usize,			/* height.				*/
}

struct Edge {
        u:      usize,  
        v:      usize,
        f:      i32,
        c:      i32,
}

struct Operation {
    u: usize,
    v: Option<usize>,
    e: Option<Arc<Mutex<Edge>>>,
    should_relabel: bool,
    flow_to_push: i32,
}

struct ThreadOperations {
    operations: Vec<Operation>,
    

}

impl Node {
	fn new(ii:usize) -> Node {
		Node { i: ii, e: 0, h: 0 }
	}

}

impl Edge {
        fn new(uu:usize, vv:usize,cc:i32) -> Edge {
                Edge { u: uu, v: vv, f: 0, c: cc }      
        }
}

impl Operation {
    fn new(u:usize, v:Option<usize>, e:Option<Arc<Mutex<Edge>>>, should_relabel:bool, flow_to_push:i32) -> Operation {
        Operation {u, v, e, should_relabel, flow_to_push}
    }
}

impl ThreadOperations {
    fn new() -> ThreadOperations {
        ThreadOperations{ operations: Vec::new(), }
    }

    fn create_vec(size: usize) -> Vec<Mutex<ThreadOperations>> {
        let mut vec = Vec::new();
        for _ in 0..size {
            vec.push(Mutex::new(ThreadOperations::new()));
        }
        vec //return vec
    }
}


//Globals:
static S: OnceLock<usize> = OnceLock::new();
static T: OnceLock<usize> = OnceLock::new();

fn get_s() -> usize {
    *S.get().expect("S not yet initialized")
}

fn get_t() -> usize {
    *T.get().expect("T not yet initialized")
}

fn other(u:usize, e:&Edge, _nodes: &Vec<Arc<Mutex<Node>>>) -> usize {
    if u == e.u {
        return e.v;
    }else{
        return e.u;
    }
}

fn enter_excess(u_index:usize, excess:&mut VecDeque<usize>){
    if u_index != get_s() && u_index != get_t() {
        excess.push_back(u_index);
    }
}

fn push(u_index:usize, v_index:usize, e_index:usize, node: &Vec<Arc<Mutex<Node>>>, edge: &Vec<Arc<Mutex<Edge>>>, excess:&mut VecDeque<usize>){
    let mut u = node[u_index].lock().unwrap();
    let mut v = node[v_index].lock().unwrap();
    let mut e = edge[e_index].lock().unwrap();

    let d: i32;

    if u.i == e.u {
        d = cmp::min(u.e, e.c - e.f);
        e.f += d;
    }else{
        d = cmp::min(u.e, e.c + e.f);
        e.f -= d;
    }
    
    u.e -= d;
    v.e += d;

    if u.e > 0 {
        enter_excess(u.i, excess);
    }

    if v.e == d {
        enter_excess(v.i, excess);
    }
}




fn prep_phase(
    node: &Arc<Vec<Arc<Mutex<Node>>>>,
    edge: &Arc<Vec<Arc<Mutex<Edge>>>>,
    adj: &Arc<Vec<LinkedList<usize>>>,
    _excess: &Arc<Mutex<VecDeque<usize>>>,
    thread_ops: &Arc<Vec<Mutex<ThreadOperations>>>,
    thread_id: usize,
    start_node: usize,
    end_node: usize
){
    
    //iterate through delegated range of nodes
    for n_idx in start_node..end_node {
        // let u_arc = node[n_idx].clone();
        // let u = u_arc.lock().unwrap();
        let u_e;
        let u_h;
        let u_i;

        {
            let u = node[n_idx].lock().unwrap();
            if u.e <= 0 || u.i == get_s() || u.i == get_t() {
                continue;
            }
            u_e = u.e;
            u_h = u.h;
            u_i = u.i;
        }

        let mut found_push:bool = false;
        let adj_iter = adj[u_i].iter();

        for &e_idx_lock in adj_iter { //iterate through all edges till we find viable push
          if found_push {
                return;
            } 

            let v_idx:usize; 
            let direction:i32;
            // let e_idx_arc = &edge[e_idx_lock];
            // let e_idx = e_idx_arc.lock().unwrap();
            let e_c;
            let e_f;

            {
                let e_idx = edge[e_idx_lock].lock().unwrap();
                if u_i == e_idx.u {
                    v_idx = e_idx.v;
                    direction = 1;
                }else{
                    v_idx = e_idx.u; 
                    direction = -1;
                } 
                e_c = e_idx.c;
                e_f = e_idx.f;
            }

            let v_h;
            {
                let v = node[v_idx].lock().unwrap();
                v_h = v.h;
            }

            // let v_arc = node[v_idx].clone();
            // let v = v_arc.lock().unwrap();
            if u_h > v_h && (direction * e_f) < e_c {
                let flow = cmp::min(u_e, e_c - direction*e_f);
                if flow > 0 {
                    let mut thread_op_arr = thread_ops[thread_id].lock().unwrap(); 
                    let op = Operation::new(u_i, Some(v_idx), Some(edge[e_idx_lock].clone()), false, flow);
                    thread_op_arr.operations.push(op);
                    found_push = true;
                }
            }
        }

        if !found_push && u_e > 0 {
            let op = Operation::new(u_i, None, None, true, 0);
            let mut thread_op_arr = thread_ops[thread_id].lock().unwrap();
            thread_op_arr.operations.push(op);
        }
    }
}

fn action_phase(
    node: &Arc<Vec<Arc<Mutex<Node>>>>,
    _edge: &Arc<Vec<Arc<Mutex<Edge>>>>,
    _adj: &Arc<Vec<LinkedList<usize>>>,
    _excess: &Arc<Mutex<VecDeque<usize>>>,
    thread_ops: &Arc<Vec<Mutex<ThreadOperations>>>,
    is_done: &Arc<Mutex<bool>>,
    nbr_threads: usize,
    thread_id: usize
){
    
    if thread_id == 0 {
       let mut any_work:bool = false; 

        for i in 0..nbr_threads {
            let thread_op_arr_arc = &thread_ops[i];
            let mut thread_op_arr = thread_op_arr_arc.lock().unwrap();
            if !thread_op_arr.operations.is_empty(){
                any_work = true; 
            }

            while let Some(op) = thread_op_arr.operations.pop() {

                if op.should_relabel {
                    let u_arc = &node[op.u];
                    let mut u = u_arc.lock().unwrap();
                    u.h += 1;
                }else{

                    let u_arc = &node[op.u];
                    let mut u = u_arc.lock().unwrap();

                    if let Some(v_idx) = op.v {
                        let v_arc = &node[v_idx];
                        let mut v = v_arc.lock().unwrap();

                        if let Some(edge_arc) = &op.e {
                            let edge_lock = edge_arc.clone();
                            let mut edge = edge_lock.lock().unwrap();
                            let flow = op.flow_to_push;

                            if u.i == edge.u {
                                edge.f += flow;
                            }else{
                                edge.f -= flow;
                            }
                            u.e -= flow;
                            v.e += flow;
                        }
                    }
                }
            }
        }

        if !any_work {
            //just do access and mutate the is_done bool value
            let is_done_clone = is_done.clone();
            let mut is_done_lock = is_done_clone.lock().unwrap();
            *is_done_lock = true;
        }
    }
}


fn main() {

	let n: usize = read!();		/* n nodes.						*/
	let m: usize = read!();		/* m edges.						*/
	let _c: usize = read!();	/* underscore avoids warning about an unused variable.	*/
	let _p: usize = read!();	/* c and p are in the input from 6railwayplanning.	*/

	let mut node = vec![];
	let mut edge = vec![];
	let mut adj: Vec<LinkedList<usize>> = Vec::with_capacity(n);
	let mut excess: VecDeque<usize> = VecDeque::new();

	let debug = false;
    let f: i32; //final flow
    let nbr_threads = 15;

    S.set(0).expect("you cannot set s twice");  //set them once, and use as global variables
    T.set(n-1).expect("you cannot set t twice");    //set them once, and use as global variables
    

	println!("n = {}", n);
	println!("m = {}", m);

	for i in 0..n {
		let u:Node = Node::new(i);
		node.push(Arc::new(Mutex::new(u))); 
		adj.push(LinkedList::new());
	}

    {   //block to set node[s].h = n    
        let source_clone = Arc::clone(&node[get_s()]);
        let mut source_lock = source_clone.lock().unwrap();
        source_lock.h = n;
    }

	for i in 0..m {
		let u: usize = read!();
		let v: usize = read!();
		let c: i32 = read!();
		let e:Edge = Edge::new(u,v,c);
		adj[u].push_back(i);
		adj[v].push_back(i);
		edge.push(Arc::new(Mutex::new(e))); 
	}

	if debug {
		for i in 0..n {
			print!("adj[{}] = ", i);
			let iter = adj[i].iter();

			for e in iter {
				print!("e = {}, ", e);
			}
			println!("");
		}
	}

	println!("initial pushes");

	let iter = adj[get_s()].iter(); //adjacency list with edges to source node

	   //initial source pushes
	   for &e_index in iter {                    

	       let e_arc = edge[e_index].clone();
	       let e_lock = e_arc.lock().unwrap();

	       let u_arc = node[get_s()].clone();
	       let mut u_lock = u_arc.lock().unwrap();

	       u_lock.e += e_lock.c;

	       let v_index = other(u_lock.i, &*e_lock, &node);

	       drop(u_lock);
	       drop(e_lock);

	       push(get_s(), v_index, e_index, &node, &edge, &mut excess);
	   }

    //threads init
    let node = Arc::new(node);
    let edge = Arc::new(edge);
    let adj = Arc::new(adj);
    let excess = Arc::new(Mutex::new(excess));  //this needs an additional mutex since mutliple threads might write to it
    let barrier = Arc::new(Barrier::new(nbr_threads));
    let is_done = Arc::new(Mutex::new(false));

    let thread_ops = Arc::new(ThreadOperations::create_vec(nbr_threads));

    let nodes_per_thread = (n + nbr_threads - 1) / nbr_threads;
    let mut threads = vec![]; //array with threads

    for i in 0..nbr_threads {
        let start_node = i * nodes_per_thread; 
        let end_node = cmp::min(n, (i+1) * nodes_per_thread);

        //clone arcs for thread access
        let node_clone = Arc::clone(&node);
        let edge_clone = Arc::clone(&edge);
        let adj_clone = Arc::clone(&adj);
        let excess_clone = Arc::clone(&excess);
        let is_done_clone = Arc::clone(&is_done);
        let thread_ops_clone = Arc::clone(&thread_ops);
        let barrier_clone = Arc::clone(&barrier);


        let ts = thread::spawn(move || {

            while !*is_done_clone.lock().unwrap() {
                
                prep_phase(
                    &node_clone,
                    &edge_clone,
                    &adj_clone,
                    &excess_clone,
                    &thread_ops_clone,
                    i,
                    start_node,
                    end_node
                );

                barrier_clone.wait();

                if i == 0 {
                    action_phase(
                        &node_clone,
                        &edge_clone,
                        &adj_clone,
                        &excess_clone,
                        &thread_ops_clone,
                        &is_done_clone,
                        nbr_threads,
                        i
                    );
                }

                barrier_clone.wait();

            }

        });

        threads.push(ts);
    }


    for t in threads {  //all threads done at this point
        t.join().unwrap();
    }

    let node_t_lock = node[get_t()].lock().unwrap();

    f = node_t_lock.e;

	println!("f = {}", f);

}
