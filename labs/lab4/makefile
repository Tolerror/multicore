main:
	gcc -o preflow preflow.c pthread_barrier.c -g -pthread
	time sh check-solution.sh ./preflow
	@echo PASS all tests
