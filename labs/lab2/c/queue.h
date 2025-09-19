#ifndef QUEUE_H
#define QUEUE_H

#include <pthread.h>
#include <stdbool.h>

typedef struct node_t node_t;
typedef struct queue_t queue_t;


struct queue_t{
    
    node_t* head;
    node_t* tail;
    pthread_mutex_t h_lock;
    pthread_mutex_t t_lock;

};

struct node_t{
    
    int h;
    int e;
    node_t* next;
    
};

void init_queue(queue_t** q);

void enqueue(queue_t* q, node_t* node);

bool dequeue(queue_t* q, node_t* node);

void free_queue(queue_t* q);







#endif //QUEUE_H
