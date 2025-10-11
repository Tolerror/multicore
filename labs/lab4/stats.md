(without -O3)
Sequential preflow with int:
3.00user 0.04system 0:03.04elapsed 100%CPU (0avgtext+0avgdata 87648maxresident)k

Sequential preflow with atomic_int:
3.03user 0.05system 0:03.08elapsed 100%CPU (0avgtext+0avgdata 88012maxresident)k


preflow perf record:
  58,59%  preflow  preflow               [.] preflow
  21,41%  preflow  preflow               [.] free_graph
  10,79%  preflow  preflow               [.] next_int
   3,25%  preflow  libc.so.6             [.] getchar
   1,68%  preflow  libc.so.6             [.] _int_free_chunk
   0,77%  preflow  libc.so.6             [.] __ctype_b_loc
   0,71%  preflow  preflow               [.] add_edge
   0,69%  preflow  libc.so.6             [.] _int_malloc
   0,56%  preflow  libc.so.6             [.] cfree@GLIBC_2.2.5
   0,23%  preflow  preflow               [.] new_graph
   0,21%  preflow  preflow               [.] xmalloc
   0,17%  preflow  libc.so.6             [.] malloc
   0,17%  preflow  preflow               [.] connect
   0,16%  preflow  preflow               [.] push
   0,10%  preflow  preflow               [.] leave_excess
   0,07%  preflow  [unknown]             [k] 0xffffffff9a201298
   0,06%  preflow  preflow               [.] __ctype_b_loc@plt
   0,06%  preflow  preflow               [.] enter_excess
   0,04%  preflow  libc.so.6             [.] __memset_avx512_unaligned_erms
   0,04%  preflow  [unknown]             [k] 0xffffffff9a201280
   0,03%  preflow  [unknown]             [k] 0xffffffff9b7cef26
   0,03%  preflow  [unknown]             [k] 0xffffffff9a2015f0
   0,03%  preflow  [unknown]             [k] 0xffffffff9b7cae85
   0,02%  preflow  libc.so.6             [.] alloc_perturb
   0,02%  preflow  preflow               [.] relabel
   0,02%  preflow  [unknown]             [k] 0xffffffff9a201290
   0,02%  preflow  [unknown]             [k] 0xffffffff9a201602
   0,01%  preflow  [unknown]             [k] 0xffffffff9a200080
   0,01%  preflow  libc.so.6             [.] _IO_file_underflow@@GLIBC_2.2.5
   0,01%  preflow  [unknown]             [k] 0xffffffff9a200084
   0,01%  preflow  [unknown]             [k] 0xffffffff9b7cef2c
   0,00%  preflow  ld-linux-x86-64.so.2  [.] do_lookup_x
   0,00%  preflow  libc.so.6             [.] __uflow
   0,00%  preflow  libc.so.6             [.] _int_free_merge_chunk


atomic perf record:
  57,84%  atomic   atomic            [.] preflow
  21,52%  atomic   atomic            [.] free_graph
   9,38%  atomic   atomic            [.] next_int
   4,23%  atomic   libc.so.6         [.] getchar
   1,72%  atomic   libc.so.6         [.] _int_free_chunk
   1,47%  atomic   libc.so.6         [.] _int_malloc
   0,66%  atomic   libc.so.6         [.] __ctype_b_loc
   0,49%  atomic   libc.so.6         [.] cfree@GLIBC_2.2.5
   0,45%  atomic   libc.so.6         [.] malloc
   0,33%  atomic   atomic            [.] new_graph
   0,31%  atomic   atomic            [.] relabel
   0,28%  atomic   atomic            [.] xmalloc
   0,24%  atomic   atomic            [.] enter_excess
   0,23%  atomic   atomic            [.] add_edge
   0,21%  atomic   atomic            [.] push
   0,14%  atomic   atomic            [.] connect
   0,11%  atomic   [unknown]         [k] 0xffffffff9a201298
   0,08%  atomic   atomic            [.] leave_excess
   0,08%  atomic   atomic            [.] __ctype_b_loc@plt
   0,04%  atomic   libc.so.6         [.] __memset_avx512_unaligned_erms
   0,04%  atomic   [unknown]         [k] 0xffffffff9a201280
   0,03%  atomic   [unknown]         [k] 0xffffffff9a2015f0
   0,02%  atomic   [unknown]         [k] 0xffffffff9a201290
   0,02%  atomic   [unknown]         [k] 0xffffffff9b7cae85
   0,01%  atomic   [unknown]         [k] 0xffffffff9a30410b
   0,01%  atomic   libc.so.6         [.] __uflow
   0,01%  atomic   libc.so.6         [.] alloc_perturb
   0,01%  atomic   [unknown]         [k] 0xffffffff9a201602
   0,01%  atomic   [unknown]         [k] 0xffffffff9b7cef26
   0,01%  atomic   [unknown]         [k] 0xffffffff9a200080
   0,01%  atomic   [unknown]         [k] 0xffffffff9a200084
   0,01%  atomic   [unknown]         [k] 0xffffffff9b7ce564
   0,01%  atomic   [unknown]         [k] 0xffffffff9b7cef2c
   0,01%  atomic   [unknown]         [k] 0xffffffff9b7cefb8



objdump of relabel() with volatile variable enclosure in preflow.c:
int:
  400ae3:       25 34 12 00 00          and    $0x1234,%eax
  400ae8:       89 05 a2 25 00 00       mov    %eax,0x25a2(%rip)        # 403090 <test>
  400aee:       48 8b 45 f0             mov    -0x10(%rbp),%rax
  400af2:       8b 00                   mov    (%rax),%eax
  400af4:       8d 50 01                lea    0x1(%rax),%edx
  400af7:       48 8b 45 f0             mov    -0x10(%rbp),%rax
  400afb:       89 10                   mov    %edx,(%rax)
  400afd:       8b 05 8d 25 00 00       mov    0x258d(%rip),%eax        # 403090 <test>
  400b03:       25 78 56 00 00          and    $0x5678,%eax

atomic_int:
  400ae3:       25 34 12 00 00          and    $0x1234,%eax
  400ae8:       89 05 a2 25 00 00       mov    %eax,0x25a2(%rip)        # 403090 <test>
  400aee:       48 8b 45 f0             mov    -0x10(%rbp),%rax
  400af2:       8b 00                   mov    (%rax),%eax
  400af4:       8d 50 01                lea    0x1(%rax),%edx
  400af7:       48 8b 45 f0             mov    -0x10(%rbp),%rax
  400afb:       89 10                   mov    %edx,(%rax)
  400afd:       8b 05 8d 25 00 00       mov    0x258d(%rip),%eax        # 403090 <test>
  400b03:       25 78 56 00 00          and    $0x5678,%eax


in free_graph():
int:
  400b7a:       25 34 12 00 00          and    $0x1234,%eax
  400b7f:       89 05 0b 25 00 00       mov    %eax,0x250b(%rip)        # 403090 <test>
  400b85:       8b 05 05 25 00 00       mov    0x2505(%rip),%eax        # 403090 <test>
  400b8b:       45 89 1a                mov    %r11d,(%r10)
  400b8e:       25 78 56 00 00          and    $0x5678,%eax


atomic_int:
  400b26:       25 34 12 00 00          and    $0x1234,%eax
  400b2b:       89 05 5f 25 00 00       mov    %eax,0x255f(%rip)        # 403090 <test>
  400b31:       f0 83 06 01             lock addl $0x1,(%rsi)
  400b35:       8b 05 55 25 00 00       mov    0x2555(%rip),%eax        # 403090 <test>
  400b3b:       25 78 56 00 00          and    $0x5678,%eax

Conclusion:
Negligible overhead, although using atomic_int limits the compiler in optimizing by reordering instructions regarding the atomic_ints.
It also introduces locks under the hood even if there is no concurrency.



#Different memory model execution times:

Strictly sequentially consistent:
11.76user 1.79system 0:02.16elapsed 626%CPU (0avgtext+0avgdata 88776maxresident)k

Relaxed:
11.16user 1.41system 0:02.02elapsed 620%CPU (0avgtext+0avgdata 88140maxresident)k

