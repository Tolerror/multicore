Test commands used: 
perf record -e cycles -c 100000 ./<filename> < ../../data/huge/001.in

perf report filtering only source code functions:
perf report --no-children --dsos=<filename>

to check function traffic with perf:
perf annotate -s <function-name>

# Queue solution with 15 threads.
Samples: 3K of event 'cycles:Pu', Event count (approx.): 3799256827
Overhead  Command     Symbol
  14,37%  concurrent  [.] main
  10,18%  concurrent  [.] work
   0,35%  concurrent  [.] pthread_mutex_unlock@plt
   0,31%  concurrent  [.] pthread_mutex_lock@plt
   0,28%  concurrent  [.] free@plt
   0,13%  concurrent  [.] getc@plt
   0,07%  concurrent  [.] push
   0,06%  concurrent  [.] malloc@plt


# Global lock solution with 15 threads.
  38,94%  concurrent  libc.so.6             [.] pthread_mutex_lock@@GLIBC_2.2.5
  19,96%  concurrent  libc.so.6             [.] __GI___pthread_mutex_unlock_usercnt
  17,70%  concurrent  concurrent            [.] main
   8,48%  concurrent  concurrent            [.] worker_function
   4,58%  concurrent  libc.so.6             [.] cfree@GLIBC_2.2.5
   3,13%  concurrent  libc.so.6             [.] _int_free_chunk
   1,77%  concurrent  libc.so.6             [.] _IO_getc
   1,29%  concurrent  libc.so.6             [.] pthread_mutex_unlock@@GLIBC_2.2.5
   1,23%  concurrent  libc.so.6             [.] _int_malloc
   0,96%  concurrent  concurrent            [.] free@plt



# Sequential preflow
Samples: 2K of event 'cycles:Pu', Event count (approx.): 2663835798
Overhead  Command  Symbol
  63,15%  preflow  [.] preflow
  28,50%  preflow  [.] main
   0,15%  preflow  [.] free@plt
   0,11%  preflow  [.] getc@plt
