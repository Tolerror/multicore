1. What is Amdahl’s law? Explain what it means and why it is important.
It explains how much faster a task can get if you optimize a part of the program.

S = 1 / (1-P) + P/N
where:
S = the overall speedup of the program
P = the proportion of the program that can be parallellized.
1 - P = the remaining proportion that cannot be parallellized. The serial part.
N = the number of parallell units.

example: If 70% of the program can be parallellized. P = 0.7.
S = 1 / (1-0.7) + 0.7/4 = 2.1 
This means that the program can get 2.1 times faster.

If we had an infinite amount of threads the speedup could only theoretically be:
S = 1/ (1-0.7) + 0.7/INF <=> S = 1 / 0.3 = 3.33

2. What can Helgrind and the Google thread sanitizer help you with in multicore programming?
Both Helgrind and Google thread sanitizer are tools used to detect data races when having a concurrently running program,
such as deadlocks and multiply accessed memory. Helgrind emulates a CPU, making it quite accurate but also very slow. Google sanitizer instead injects code in the binary when compiling
to detect data races during runtime.

3. What is an actor in Scala/Akka?
An actor is a concurrent object that has private data and communicates through the passing of messages with other actors. 
Each actor has a mailbox that handles messages one at a time. Messages received from another actor are guaranteed to be sequentially consistent,
however it does not mean that other actors cannot come in between with additional messages.

4. What happens to an actor when it receives a message? (is it interrupted, for instance?)
No it is not interrupted. The message is placed in the message queue of that actor and will
be executed in the order it was reached (FIFO).

5. Which guarantees about message arrival order does Akka give?
It is guaranteed that messages from an actor A are sequentially consistent but 
does not guarantee that other actors will not send messages in-between.
In short, Akka guarantees per sender message order but not a global message order.

6. How can you avoid data-races in Scala/Akka programs?
Data races are inherently avoided with the use of Akka since the Actors themselves are concurrent objects. 
Actors encapsulate their state and interact only through message passing. Since only one message is 
processed at a time for each actor, there is no data races with the internal data.
There are no shared mutable state between actors

7. What is the difference between self and this in Akka?
this - refers to the actual actor instance, the object itself.
self - refers to the ActorRef that Akka implements. This is the reference that is passed between actors.

this comes from the ordinary scala semantics and should not be passed between actors. 
No other actor should be able to access another actor's internal state.

8. When an Akka actor receives a message, how can it know the identity of the actor that sent the message so it can make a reply?
The message has an ActorRef that points to the sender of the message. With the sender() method, the implicit
ActorRef from a message is given.

9. If now Scala/Akka is much slower than Java and C, why would you consider using it for an Internet company that is intended to grow to millions of users?
It very easily scalable and implement, compared to C or Java. It has non-blocking communication which makes it easily scalable
and you dont have to think about locking or other potentially bug prone concurrency solutions. 
Easier maintenance is sometimes more desirable than raw performance that comes at the cost of complexity and poorer ease of scaling.

10. Explain the terms
Decomposition - Breaking a larger problem into smaller independent or partially independent problems that can be executed concurrently.
Assignment - Assigning which tasks to which processor or resource. For example assigning tasks equally to have good load balance.
Orchestration - Managing task coordination between processes, such as shared data, execution order and communication between processors.
Mapping - Mapping which hardware processor will handle which thread/task. The assignment of tasks or threads to actual physical hardware resources, CPU cores, GPU threads, etc.

11. Which two of the terms in the previous question are commonly together called partitioning?
Decomposition and assignment. Partitioning in the sense of decomposing a larger problem into smaller tasks and assigning those tasks to a resource. The task, processor pair is a partition of the larger problem.

12. What are the main goals in partitioning?
Dividing up the workload in a way that is most efficient with the least amount of overhead.
Points of importance when partionining:
Workload balance - Each partition should have roughly the same amount of work.
Independence - Each partition should be as self contained as possible. Minimize inter partition communication.
Completeness - The set of partitions should cover the whole problem. 
Efficiency - Partioning should minimize communication and coordiation overhead, hence increasing efficiency.

13. What is the difference between static and dynamic assignment of tasks to threads and when should each be used?
Static assignment is determined beforehand. Setting a fixed assignment structure before program execution without regard to fluctuating load balance down the line.
Pros: Low overhead since assignment is done once before program execution or at the start of program execution.
Cons: Does not guarantee good load balance.

Dynamic assignment is dynamically assigning the workload/tasks during program execution. Dynamically assigning such that load balance is preserved as well as possible.
Pros: Assures load balance to some degree hence preventing task starvation.
Cons: Higher overhead, if the overhead grows sufficiently large it might be more wise to stick to static assignment.

14. What is meant by systolic array? Why can it give very high performance and why is it difficult to achieve that performance for many programs?
A systolic array is a method of concurrently processing elements in a systematic pipeline fashion. Data is passed from neighbouring element to the next
in a synchronized pipelined manner. It is often used with two inputs and two outputs, data flowing systolically - like a "heartbeat". 
A good use case for systolic arrays is in matrix multiplication for graphics, where rows and columns are 
multiplied and added in a fixed pattern. This makes it highly parallellizable although most programs often require more "random" or irregular 
memory accesses that makes systolic arrays not very applicable for most use cases.

15. The Tera architecture was a parallel computer with no cache memories. What was the architectural feature that would give high performance anyway?
Instead each memory word has a full or empty bit. Writing to the word set the bit and reading it cleared the bit.
That way no processor has ownership over a word but instead all memory is shared uniformly.
This way the overhead that comes with caches such as cache coherence problems and cache misses.

16. What does it mean that a multicore is a cache coherent shared memory multiprocessor?
A multiprocessor has as the name implies multiple processors. For these processors to work in unison in any meaningful way
data needs to be shared amongst them. This is achieved by each processor having its own cache since reading and writing to and from memory is too slow. 
Each processor having its own cache comes with the problem of these caches not being synchronized with each other and the shared memory space. 
Hence the protocols for cache coherence were invented.
So as a whole, a multicore is therefore a cache coherent shared memory multiprocessor.

17. What is a directory of a memory block in a cache coherent shared memory multiprocessor?
A directory is a table for each memory block in memory that keeps track of all caches that has copies of data and their states.
So when a core wants to write to a memory block it consults with the directory of that memory block to send the appropiate messages for cache coherence
to the caches involved, which saves bandwidth since it does not need to broadcast to all caches.
Problem arises when the directory has too many entries when too many nodes has a copy of the memory block.
An alternative is to use a home location instead. Each memory block has a home location i.e. a home node where the directory lives.
It is the responsibility of the home node to maintain the directory and cache coherence. 
When a core writes to Shared memory in its cache, it goes to the memory address where
it finds the home location of the home node, which it then consults. The home node then checks the directory and sends out the appropiate 
cache coherence messages to the nodes involved.

18. What is the idea with a cache-only memory architecture? and why would that for some programs be more efficient than a normal cache coherent shared memory multiprocessor with a fixed home location of each memory block?
In COMA instead of having a static home location to each memory block the home location is instead dynamically "fleeting". 
Data diffuses to a suitable node that currently uses it and becomes the new home. This way the data is more likely to be local on subsequent accesses since it dynamically migrates
to spots in memory it is being frequently used. This improves performance for programs with dynamic or irregular memory access patterns, compared to static home location cache coherent shared memory multiprocessors.

19. What happens in Java if the main thread calls run on a thread?
The main thread will instead execute the run() method. Not to be confused with start() that actually creates a new thread which in turn calls run() for that newly created thread.

20. What is the semantics of a synchronized block in Java, and what is an object’s entry set and wait set in Java?
A synchronized block is in Java is a way to enforce mutual exclusion on some object. The current thread acquires the lock
for that object and only then is allowed to execute critical code within that block. Other threads are blocked on attempted access to
the synchronized block if the lock is already held. The lock is released after the thread exits the synchronized block.
The objects entry set is the queue of threads waiting to acquire the lock for the synchronized block.
The objects wait set is the queue of threads that have called wait() inside that objects synchronized block. The threads sleep in the queue until a notify() or notifyAll()
wakes them where they enter the entry set to try to acquire the lock again. When they reacquire the lock they continue from the point after the wait() in the synchronized block.

21. What is meant by a lock being reentrant?
Reentrant meaning the lock can be acquired by the same thread holding the lock. In other words allows for nested synchronized calls on the same object. Each time a thread holding the lock calls lock() the internal ReentrantLock counter
is incremented and unlock() decrements it. The lock is released when the counter == 0 on an unlock() call.
This is a solution so that threads do not deadlock themselves if they recursively call a method on itself directly or indirectly.

22. What is the semantics of volatile in Java (that is, what happens conceptually)?
Volatile in java enforces that the volatile variable update is seen by all threads. There is no caching involved and the variable is directly written to memory and read from memory.
The write to a volatile variable happens-before every subsequent read of that variable by any thread.

23. Is volatile in C the same as in Java? (yes/no answer is sufficient)
No, volatile in C is similar in that the compiler is limited to how it can reorder the volatile variable.
It means that the compiler is not allowed to do certain optimizations on the variable.
It does not guarantee the happens-before relation and visibility that Java guarantees.

24. What is the purpose of using a condition variable? Give an example.
Condition variables are used to allowd threads to wait for some condition to become true before doing something. 
Threads can sleep on the condition variable until another threads signals that the condition is satisfied, which avoids busy waiting.
Example:
Lock lock = new ReentrantLock();
Condition cond = lock.newCondition();
boolean ready = false;

//Thread 1
lock.lock()
try{
    while(!ready) 
        cond.await();

print("Condition met!");

} finally{ lock.unlock(); }

//Thread 2
lock.lock();
try{
    //do something
    ready = true;
    cond.signal();

} finally{ lock.unlock(); }

25. Is the operating system kernel always involved when a thread waits for a Pthreads mutex in Linux? Why or why not or when?
No, if the mutex is uncontended the thread can immediately acquire it without kernel interaction.
If the mutex is contended the thread may need to sleep, then the kernel have to put the thread to sleep and later wake it up.

26. What is Sequential Consistency? How is it defined? (not word for word but the essence)
A system is sequentially consistent if the result of an execution is the same as if the operations of all processors were executed in some sequential order,
and the operations of each processor appear in that sequence in the order specified by the program.
So all processors see the same global order as if all operations were executed one at a time sequentially.

27. Give one example of a compiler optimization that can break Sequential Consistency, and motivate why it can break it.
Reordering independent memory operations, meaning if the order for one thread is reordered in such a way that does not align with the global order, sequential consistency is broken.
Optimizations in the form of reorderings are allowed but not if it affects the program order of any other thread.
If Thread 1 were to reorder x and y assignments, SC would be violated since Thread 2 depends on the order.
Example:
//Thread 1
x = 1;
y = 2;

//Thread 2
if(x == 1){
    print("hello");
    if(y == 2){
    print("world");
    }
}

28. What is a write buffer?
It is a CPU hardware structure that temporarily holds memory writes so that the CPU can continue execution without having to wait for slow memory.
The write buffer holds the data while the CPU immediately continues executing subsequent instructions.
The buffer is eventually flushed and the data is written to main memory (in FIFO fashion).

29. Why is it, usually, a good idea to let reads ”bypass” writes, but why can that break Sequential Consistency?
With read bypass the write buffer, a FIFO queue is bypassed. By passing the write buffer, values that needs to be read for the program to continue executing
can be fetched immediately without having to wait for the queue. Although by bypassing the writes, the data in the writes won't be visible to other
threads that might depend on them, which in turn can break SC in case those writes does not confine to the sequential order of the other threads/global order.

30. Why can overlapping writes break Sequential Consistency?
If writes overlap different processors might observe the writes in different order orders depending on timing or buffering.
Meaning one processor could observe the value from one write while another processor observer the value from another. 
Which in turn breaks the global consistent view of memory, hence breaking SC.

31. Why can non-blocking reads break Sequential Consistency?
A non-blocking read is a read operation that does not wait for pending writes or synchronization.
It immediately returns whatever value it finds at that given time. The main purpose for this is to increase performance, by allowing speculative data
the CPU can fetch for data it needs later at an earlier time while waiting for memory for example. 
This can break sequential consistency since it would allow reads and writes to appear out of the global order in which all thread operations "adhere to".
If a read is done before a write which another thread depends on, that would break sequential consistency.

32. What it is a cache coherence protocol and which types of messages can it have and what would their purpose be?
Cache coherence - Is the concept of ensuring that all cores have a consistent view of data. This is done by having cache blocks be in one of the following states, example of the MESI protocol:
Modified - The only cache with a valid copy, which has been modified. (different from memory)
Exclusive - The only cache with a valid copy, which has not been modified. (same as in memory)
Shared - Multiple caches have valid copies, all clean. (none are modified) (same as in memory)
Invalid - Cache line is invalid. (fetch new)
Run through:
When reading a line:
- If line isnt in cache -> cache miss. Fetch from memory.
if no other core has that data, Exclusive tag.
if other cores have it, Shared tag.

When writing to a line:
- If line is Shared or Exclusive, cache must get exclusive ownership first before modifying.
    1. Sends invalidation messages to all other caches containing that line.
    2. Other caches mark their copies as Invalid.
    3. The writing core sets line to Modified.
    (if the line was already in Modified state it can write immediately, since it is already local)

When reading a line modified by another core:
- The modified data is forwarded to requesting core. Both caches tag data as Shared. If requesting core is the only one with a copy its instead Exclusive.

If a cache removes a line in Modified state. It first writes the line back to memory so memory becomes consistent again.

33. Why could it be useful to have both an exclusive state and a modified state of a cache block?
When a cache block is in Exclusive state it means it is the only owner of that memory and that the memory has not been changed. 
If we need to evict this cache block, we would not have to store in memory since it has not been modified and we save a store operation to memory which is slow.

34. Why can relaxed memory models improve performance?
It allows for reordering and speculative execution. By not having to wait for slow memory accesses and stalling the pipeline, we can proceed with subsequent operations such as read data that might be used later in the program.

35. What is the difference between Weak Ordering and Release Consistency?


Short transactions - At an L1 cache load the L2 cache is informed such that it can detect conflicts. 
When an L1 write is done, the modified cache block is removed from the L1 cache and moved to the L2 cache. 
Then on subsequent read, an L1 cache miss follows. This is expected.

Long transactions - Can use the L1 cache for speculative state. This means for the L2 cache to keep track on the speculative data in the L1 cache, 
the L1 cache must be invalidated in its entirety at the start of a new transaction. That way no data that the L2 cache is not aware of can exist.
An intial L1 cache miss is therefore expected.

