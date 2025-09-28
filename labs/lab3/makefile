main:
	gcc -std=gnu18 -o preflow preflow.c pthread_barrier.c -g -O3 -pthread
	time sh check-solution.sh ./preflow
	@echo PASS all tests

para:
	gcc -std=gnu18 -o concurrent concurrent.c pthread_barrier.c -g -O3 -pthread
	time sh check-solution.sh ./concurrent
	@echo PASS all tests
