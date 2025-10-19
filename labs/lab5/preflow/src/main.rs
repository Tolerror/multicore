#[macro_use] extern crate text_io;

//use std::sync::{Mutex,Arc};
use std::collections::LinkedList;
use std::cmp;
// use std::thread;
use std::collections::VecDeque;
use std::sync::OnceLock;

struct Node {
	_i:	usize,			/* index of itself for debugging.	*/
	e:	i32,			/* excess preflow.			*/
	h:	usize,			/* height.				*/
}

struct Edge {
        u:      usize,  
        v:      usize,
        f:      i32,
        c:      i32,
}

impl Node {
	fn new(ii:usize) -> Node {
		Node { _i: ii, e: 0, h: 0 }
	}

}

impl Edge {
        fn new(uu:usize, vv:usize,cc:i32) -> Edge {
                Edge { u: uu, v: vv, f: 0, c: cc }      
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


fn other(u_index:usize, e:&Edge) -> usize{
    if u_index == e.u {
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

fn push(u_index:usize, v_index:usize, e:&mut Edge, node:&mut Vec<Node>, excess:&mut VecDeque<usize>){

    let d: i32;

    if u_index == e.u {
        d = cmp::min(node[u_index].e, e.c - e.f);
        e.f += d;
    }else{
        d = cmp::min(node[u_index].e, e.c + e.f);
        e.f -= d;
    }
    
    node[u_index].e -= d;
    node[v_index].e += d;

    if node[u_index].e > 0 {
        enter_excess(u_index, excess);
    }

    if node[v_index].e == d {
        enter_excess(v_index, excess);
    }
}


fn relabel(u:usize, node:&mut Vec<Node>) {
    node[u].h += 1;
   
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
	let debug = true;
    let f: i32; //final flow

    S.set(0).expect("you cannot set s twice");  //set them once, and use as global variables
    T.set(n-1).expect("you cannot set t twice");    //set them once, and use as global variables
    
	println!("n = {}", n);
	println!("m = {}", m);
    // println!("s = {}", get_s());
    // println!("t = {}", get_t());

	for i in 0..n {
		let u:Node = Node::new(i);
		node.push(u); 
		adj.push(LinkedList::new());
	}

	for i in 0..m {
		let u: usize = read!();
		let v: usize = read!();
		let c: i32 = read!();
		let e:Edge = Edge::new(u,v,c);
		adj[u].push_back(i);
		adj[v].push_back(i);
		edge.push(e); 
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

    node[get_s()].h = n;

	println!("initial pushes");
	let iter = adj[get_s()].iter();

	// but nothing is done here yet...
    
    //initial source pushes
    for &e in iter {     //returns the address of e (&usize)
        node[get_s()].e += edge[e].c;
        push(get_s(), other(get_s(), &edge[e]), &mut edge[e], &mut node, &mut excess);
    }


	while !excess.is_empty() {  //as long as there are excess nodes
		let _c = 0;
		let u = excess.pop_front().unwrap();
        let mut b: i32;
        let mut v: usize = 0; //may be unitialized, v node
        let mut e: usize = 0;  //may be unitiliazed, edge
        let mut found: bool = false;
        
        let iter_u_nodes = adj[u].iter(); //node u's adjacent node list LinkedList<usize>

        for &u_edge in iter_u_nodes {  //iterate through edges (&usize)
            e = u_edge;

            if u == edge[u_edge].u {
                v = edge[u_edge].v;
                b = 1;
            }else{
                v = edge[u_edge].u;
                b = -1;
            }

            if node[u].h > node[v].h && b*edge[u_edge].f < edge[u_edge].c {
                found = true;
                break;
            }else{
                found = false;
            }

        }
        
        if found {
            push(u, v, &mut edge[e], &mut node, &mut excess);
        }else{
            relabel(u, &mut node);
            enter_excess(u, &mut excess)
        }
	}

    f = node[get_t()].e;

	println!("f = {}", f);

}
