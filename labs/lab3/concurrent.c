// LAB 2:

#include <assert.h>
#include <ctype.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h> // for using the boolean value.
#include <pthread.h> // import the lib for using thread in C
#include <unistd.h> // delay
// #include "queue.h"

#define PRINT 0 /* enable/disable prints. */
#define NBR_THREADS 15

#if PRINT
#define pr(...)                       \
	do                                \
	{                                 \
		fprintf(stderr, __VA_ARGS__); \
	} while (0)
#else
#define pr(...) /* no effect at all */
#endif

#define MIN(a, b) (((a) <= (b)) ? (a) : (b))

/* introduce names for some structs. a struct is like a class, except
 * it cannot be extended and has no member methods, and everything is
 * public.
 *
 * using typedef like this means we can avoid writing 'struct' in
 * every declaration. no new type is introduded and only a shorter name.
 *
 */

typedef struct graph_t graph_t;
typedef struct node_t node_t;
typedef struct edge_t edge_t;
typedef struct list_t list_t;






typedef struct{

    node_t* u;
    node_t* v;
    edge_t* e;
    bool should_relabel;
    int flow_to_push;

} operation_t;


typedef struct{

    operation_t* operations;
    int op_count;
    int capacity;

} thread_operations_t;


struct list_t
{
	edge_t *edge;
	list_t *next;
};

struct node_t
{
	int h;		  /* height.			*/
	int e;		  /* excess flow.			*/
	list_t *edge; /* adjacency list.		*/
	node_t *next; /* with excess preflow.		*/
	pthread_mutex_t node_lock;
    int id;
    int thread_id;
    bool queued;
};

struct edge_t
{
	node_t *u; /* one of the two nodes.	*/
	node_t *v; /* the other. 			*/
	int f;	   /* flow > 0 if from u to v.	*/
	int c;	   /* capacity.			*/
	// pthread_mutex_t edge_lock;
};

struct graph_t
{

	int n;			/* number of nodes.			*/
	int m;			/* number of edges			*/
	node_t *v;		/* pointer to array of n nodes.		*/
	edge_t *e;		/* pointer to array of m edges.		*/
    node_t *s;		/* pointer to the source.			*/
	node_t *t;		/* pointer to the sink.			*/
	node_t *excess; /* nodes with e > 0 except s,t.	*/
	int active_thread;
	pthread_barrier_t phase_barrier;
    thread_operations_t* thread_ops;
    int nbr_threads;
    bool* has_work;
    bool is_done;


    //stats
    // long phase_count;
    // long total_operations;
    // double sync_time;
    // pthread_mutex_t stats_lock;
};

typedef struct{
    void *g;    //graph
    int start;  //start index of node array
    int end;    //end index of node array
    int thread_id; //thread ID
    // int t;  //number of threads
}thread_args_t;




static char *progname;

#if PRINT

static int id(graph_t *g, node_t *v)  // this function can be used when we want to get index of object.
{
	/* return the node index for v.
	 *
	 * the rest is only for the curious.
	 *
	 * we convert a node pointer to its index by subtracting
	 * v and the array (which is a pointer) with all nodes.
	 *
	 * if p and q are pointers to elements of the same array,
	 * then p - q is the number of elements between p and q.
	 *
	 * we can of course also use q - p which is -(p - q)
	 *
	 * subtracting like this is only valid for pointers to the
	 * same array.
	 *
	 * what happens is a subtract instruction followed by a
	 * divide by the size of the array element.
	 *
	 */

	return v - g->v;
}
#endif

void error(const char *fmt, ...)
{
	/* print error message and exit.
	 *
	 * it can be used as printf with formatting commands such as:
	 *
	 *	error("height is negative %d", v->h);
	 *
	 * the rest is only for the really curious. the va_list
	 * represents a compiler-specific type to handle an unknown
	 * number of arguments for this error function so that they
	 * can be passed to the vsprintf function that prints the
	 * error message to buf which is then printed to stderr.
	 *
	 * the compiler needs to keep track of which parameters are
	 * passed in integer registers, floating point registers, and
	 * which are instead written to the stack.
	 *
	 * avoid ... in performance critical code since it makes
	 * life for optimizing compilers much more difficult. but in
	 * in error functions, they obviously are fine (unless we are
	 * sufficiently paranoid and don't want to risk an error
	 * condition escalate and crash a car or nuclear reactor
	 * instead of doing an even safer shutdown (corrupted memory
	 * can cause even more damage if we trust the stack is in good
	 * shape)).
	 *
	 */

	va_list ap;
	char buf[BUFSIZ];

	va_start(ap, fmt);
	vsprintf(buf, fmt, ap);

	if (progname != NULL)
		fprintf(stderr, "%s: ", progname);

	fprintf(stderr, "error: %s\n", buf);
	exit(1);
}

static int next_int()
{
	int x;
	int c;

	/* this is like Java's nextInt to get the next integer.
	 *
	 * we read the next integer one digit at a time which is
	 * simpler and faster than using the normal function
	 * fscanf that needs to do more work.
	 *
	 * we get the value of a digit character by subtracting '0'
	 * so the character '4' gives '4' - '0' == 4
	 *
	 * it works like this: say the next input is 124
	 * x is first 0, then 1, then 10 + 2, and then 120 + 4.
	 *
	 */

	x = 0;
	while (isdigit(c = getchar()))
		x = 10 * x + c - '0';

	return x;
}

static void *xmalloc(size_t s)
{
	void *p;

	/* allocate s bytes from the heap and check that there was
	 * memory for our request.
	 *
	 * memory from malloc contains garbage except at the beginning
	 * of the program execution when it contains zeroes for
	 * security reasons so that no program should read data written
	 * by a different program and user.
	 *
	 * size_t is an unsigned integer type (printed with %zu and
	 * not %d as for int).
	 *
	 */

	p = malloc(s);

	if (p == NULL)
		error("out of memory: malloc(%zu) failed", s);

	return p;
}

static void *xcalloc(size_t n, size_t s)
{
	void *p;

	p = xmalloc(n * s);

	/* memset sets everything (in this case) to 0. */
	memset(p, 0, n * s);

	/* for the curious: so memset is equivalent to a simple
	 * loop but a call to memset needs less memory, and also
	 * most computers have special instructions to zero cache
	 * blocks which usually are used by memset since it normally
	 * is written in assembler code. note that good compilers
	 * decide themselves whether to use memset or a for-loop
	 * so it often does not matter. for small amounts of memory
	 * such as a few bytes, good compilers will just use a
	 * sequence of store instructions and no call or loop at all.
	 *
	 */

	return p;
}




void init_thread_operations(thread_operations_t* ops, int capacity){
    ops->operations = xmalloc(capacity* sizeof(operation_t));
    ops->op_count = 0;
    ops->capacity = capacity;
}

void add_operation(thread_operations_t* ops, node_t* u, node_t* v, edge_t* e, int flow, bool relabel){
    if(ops->op_count >= ops->capacity){ //double operation array if capacity is exceeded
        ops->capacity *= 2;
        ops->operations = realloc(ops->operations, ops->capacity * sizeof(operation_t));
        if(!ops->operations) error("failed to reallocate operation array");
    }

    operation_t* op = &ops->operations[ops->op_count++];    //point to next operation in operation array
    op->u = u;
    op->v = v;
    op->e = e;
    op->flow_to_push = flow;
    op->should_relabel = relabel;
}


void prep_phase(graph_t* g, int thread_id, int start_node, int end_node){
    thread_operations_t* thread_ops = &g->thread_ops[thread_id];
    thread_ops->op_count = 0;   //prep phase reset from previous phase
    g->has_work[thread_id] = false;

    for(int i = start_node ; i < end_node ; i++){
        node_t* u = &g->v[i];

        if(u->e <= 0 || u == g->s || u == g->t) continue;   //skip this node

        list_t* edges = u->edge;
        bool found_push = 0;

        while(edges && !found_push){
            edge_t* e = edges->edge;
            node_t* v = (u == e->u) ? e->v : e->u;
            int direction = (u == e->u) ? 1 : -1;

            if(u->h > v->h && (direction * e->f) < e->c){
                int flow = MIN(u->e, e->c - direction*e->f);
                if(flow > 0){
                    add_operation(thread_ops, u, v, e, flow, false);
                    g->has_work[thread_id] = true;
                    found_push = true;
                }
            }

            edges = edges->next;

        }

        //if no push was found and the node has excess ->relabel
        if(!found_push && u->e > 0){
            add_operation(thread_ops, u, NULL, NULL, 0, true);
            g->has_work[thread_id] = true;
        }
    }
}


void action_phase(graph_t* g, int thread_id){

    //we delegate update responsibility to thread 0
    if(thread_id == 0){
        bool any_work = false;

        for(int t = 0 ; t < g->nbr_threads ; t++){
            if(g->has_work[t]) any_work = true;

            thread_operations_t* ops = &g->thread_ops[t];
            for(int i = 0 ; i < ops->op_count ; i++){
                operation_t* op = &ops->operations[i];

                if(op->should_relabel){
                    op->u->h += 1;
                }else{

                    node_t* u = op->u;
                    node_t* v = op->v;
                    edge_t* e = op->e;
                    int d = op->flow_to_push;

                    if(u == e->u){
                        e->f += d;
                    }else{
                        e->f -= d;
                    }
                    u->e -= d;
                    v->e += d;

                    assert(d >= 0);
                    assert(u->e >= 0);
                    assert(abs(e->f) <= e->c);
                }
            }
        }

        if(!any_work){
            g->is_done = true;
        }
        //update stats here

    }
}

void* barrier_worker(void* arg){
    thread_args_t* args = (thread_args_t*) arg;
    graph_t* g = args->g;
    int thread_id = args->thread_id;
    int start_node = args->start;
    int end_node = args->end;

    while(!g->is_done){

        prep_phase(g, thread_id, start_node, end_node);

        pthread_barrier_wait(&g->phase_barrier);

        action_phase(g, thread_id);

        pthread_barrier_wait(&g->phase_barrier);
    }
    return NULL;
}


int barrier_preflow(graph_t* g){
    node_t* s = g->s;
    s->h = g->n;


    list_t* p = s->edge;    //inital source push
    while(p != NULL){
        edge_t* e = p->edge;
        node_t* v = (s == e->u) ? e->v : e->u;

        s->e += e->c;
        if(s == e->u){
            e->f = e->c;
        }else{
            e->f = -e->c;
        }

        s->e -= e->c;
        v->e += e->c;

        p = p->next;

    }

    //barrier init
    int t = g->nbr_threads;
    pthread_barrier_init(&g->phase_barrier, NULL, t);

    g->thread_ops = xmalloc(t*sizeof(thread_operations_t));
    g->has_work = xmalloc(t*sizeof(bool));
    g->is_done = false;
    
    for(int i = 0 ; i < t ; i++){
        init_thread_operations(&g->thread_ops[i], 1000); //initial operation capacity per thread is 1000
        g->has_work[i] = false;
    }


    //thread init
    pthread_t threads[t];
    thread_args_t args[t];
    int nodes_per_thread = (g->n + t - 1) / t; 

    for(int i = 0 ; i < t ; i++){
        args[i].g = g;
        args[i].thread_id = i;
        args[i].start = i*nodes_per_thread;
        args[i].end = MIN(g->n, (i+1)*nodes_per_thread);

        if(pthread_create(&threads[i], NULL, barrier_worker, &args[i]) != 0){
            error("pthread_create failed");
        }
    }

    //stats viewing
    
    for(int i = 0 ; i < t ; i++){
        if(pthread_join(threads[i], NULL) != 0){
            error("pthread_join failed");
        }
    }

    //cleanup
    pthread_barrier_destroy(&g->phase_barrier);
    for(int i = 0 ; i < t ; i++){
        free(g->thread_ops[i].operations);
    }

    free(g->thread_ops);
    free(g->has_work);

    return g->t->e;     //final max flow
}




bool terminate_req(graph_t* g){
    return g->s->e + g->t->e != 0;
}


static void add_edge(node_t *u, edge_t *e)
{
	list_t *p;

	/* allocate memory for a list link and put it first
	 * in the adjacency list of u.
	 *
	 */

	p = xmalloc(sizeof(list_t)); // alocate memory for a new list_t element (p).
								 // the returned value is a pointer to newly allocated list_t struct.
	p->edge = e;				 // set the edge pointer of p to e.
	p->next = u->edge;			 // set the next pointer of p to the first edge of u.
	u->edge = p;				 //	make p is the new head of u's adjacency list.
}



static void connect(node_t *u, node_t *v, int c, edge_t *e)
{
	/* connect two nodes by putting a shared (same object)
	 * in their adjacency lists.
	 *
	 */
    e->f = 0;
	e->u = u; // set 1 endpoint of the edge to u.
	e->v = v; // Set 1 endpoint of the edge to v.
	e->c = c; // set the new capacity connecting 2 nodes.

	add_edge(u, e); // add the edge to u's adjacency list.
	add_edge(v, e); // add the edge to v's adjacency list.
}

static graph_t *new_graph(FILE *in, int n, int m, int t) // return an adress (pointer) to a graph_t object.
{
	graph_t *g; // pointer to a graph_t struct.
	node_t *u;	// pointer to a node_t struct.
	node_t *v;	// pointer to a node_v struct.
	int i;
	int a;
	int b;
	int c;

	g = xmalloc(sizeof(graph_t)); // alocate the memory for the graph structure

	g->n = n;
	g->m = m;
    g->nbr_threads = t;
	g->v = xmalloc(n*sizeof(node_t)); // pointer to an array of node_t struct
	g->e = xmalloc(m*sizeof(edge_t)); // pointer to an array of edge_t struct

    //node lock init
    for (int i = 0; i < n; i++){
        g->v[i].h = 0;
        g->v[i].e = 0;
        g->v[i].edge = NULL;
        g->v[i].next = NULL;
        g->v[i].id = i;
    }



	g->s = &g->v[0];						  // g->s: is a pointer that point to the address of first v element
	g->t = &g->v[n - 1];					  // &g->v[0] the address of the first node
	g->excess = NULL;						  // &g->v[n-1] gets the address of the last element in array v

	for (i = 0; i < m; i += 1)
	{ // loop through all edges
		a = next_int();
		b = next_int();
		c = next_int();
		u = &g->v[a];
		v = &g->v[b];
		connect(u, v, c, g->e + i); // g->e is a pointer to the first element
	} // g->e+i	is the pointer to the i-th edge
	  // the pointer references inside of edge struct will be added later.
	return g;
}


static node_t *other(node_t *u, edge_t *e)
{
	if (u == e->u)
		return e->v;
	else
		return e->u;
}



static void free_graph(graph_t *g) // this function releases all the memory allocated for the graph
{								   // (using malloc & calloc, etc)
	int i;						   // but never free it using free function, this applied for dynamically
	list_t *p;					   // allocated memory.
	list_t *q;

	for (i = 0; i < g->n; i += 1){ 
		p = g->v[i].edge;
		while (p != NULL)
		{
			q = p->next;
			free(p);
			p = q;
		}
	}

	free(g->v);
	free(g->e);
	free(g);
}

int main(int argc, char *argv[])
{
	FILE *in;	/* input file set to stdin	*/
	graph_t *g; /* undirected graph. 		*/
	int f;		/* output from preflow.		*/
	int n;		/* number of nodes.		*/
	int m;		/* number of edges.		*/

	progname = argv[0]; /* name is a string in argv[0]. */

	in = stdin; /* same as System.in in Java.	*/

	n = next_int();
	m = next_int();

	/* skip C and P from the 6railwayplanning lab in EDAF05 */
	next_int();
	next_int();

	g = new_graph(in, n, m, NBR_THREADS);

	fclose(in);

	f = barrier_preflow(g);

	printf("f = %d\n", f);

	free_graph(g);

	return 0;
}
