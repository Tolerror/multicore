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
in a synchronized pipelined manner. A good use case for systolic arrays is in matrix multiplication for graphics, where rows and columns are 
multiplied and added in a fixed pattern. This makes it highly parallellizable although most programs often require more "random" or irregular 
memory accesses that makes systolic arrays not very applicable for most use cases.

15. The Tera architecture was a parallel computer with no cache memories. What was the architectural feature that would give high performance anyway?


Short transactions - At an L1 cache load the L2 cache is informed such that it can detect conflicts. 
When an L1 write is done, the modified cache block is removed from the L1 cache and moved to the L2 cache. 
Then on subsequent read, an L1 cache miss follows. This is expected.

Long transactions - Can use the L1 cache for speculative state. This means for the L2 cache to keep track on the speculative data in the L1 cache, 
the L1 cache must be invalidated in its entirety at the start of a new transaction. That way no data that the L2 cache is not aware of can exist.
An intial L1 cache miss is therefore expected.

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

TODO: Next Lecture - 9
