main:
	gcc -o preflow preflow.c pthread_barrier.c -g -pthread -O3
	time sh check-solution.sh ./preflow
	@echo PASS all tests

para:
	gcc -o concurrent concurrent.c pthread_barrier.c -g -pthread -O3
	time sh check-solution.sh ./concurrent
	@echo PASS all tests
