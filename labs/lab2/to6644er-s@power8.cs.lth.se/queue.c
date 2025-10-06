#include "queue.h"
#include <stdio.h>
#include <stdlib.h>






node_t* init_node(){

    node_t* node = (node_t*) malloc(sizeof(node_t)); 
    if(node == NULL){
        exit(1);
    }

    return node;
}


void init_queue(queue_t** q){

    *q = malloc(sizeof(queue_t));

    if(*q == NULL){
        exit(1);
    }

    node_t* n = init_node();
    n->next = NULL;
    (*q)->head = (*q)->tail = n;
    pthread_mutex_init(&(*q)->h_lock, NULL);
    pthread_mutex_init(&(*q)->t_lock, NULL);


}

void enqueue(queue_t* q, node_t* node){
    node_t* n = init_node();

    //data
    n->h = node->h;
    n->e = node->e;
    n->next = node->next;
    //data

    n->next = NULL;
    pthread_mutex_lock(&q->t_lock);
    q->tail->next = n;
    q->tail = n;
    pthread_mutex_unlock(&q->t_lock);
}

bool dequeue(queue_t* q, node_t* node){
    pthread_mutex_lock(&q->h_lock);
    node_t* old_head = q->head;
    node_t* new_head = old_head->next;
    
    if(new_head == NULL){
        pthread_mutex_unlock(&q->h_lock);
        return false;
    }

    node->h = new_head->h;
    node->e = new_head->e;
    node->next = new_head->next;
    q->head = new_head; 
    pthread_mutex_unlock(&q->h_lock);
    free(old_head);
    return true;
}

void free_queue(queue_t* q){
    node_t* curr = q->head;

    while(curr){
        node_t* tmp = curr;
        curr = curr->next;
        free(tmp);
    }

    pthread_mutex_destroy(&q->h_lock);
    pthread_mutex_destroy(&q->t_lock);
    free(q);
}

//
// void *work_function(void* q){
//
//         queue_t* queue = (queue_t*) q;
//         node_t t;
//         while(dequeue(q, &t)){
//         printf("Withrew node from queue: %d\n", t.h);
//     }
//
//
// }
//
//
//
//
// int main(){
//
//     queue_t* q;
//     init_queue(&q);
//     int t = 5;
//     int _nodes = 10;
//     pthread_t threads[t];
//
//
//     for(int i = 0 ; i < _nodes ; i++){
//         node_t* n = init_node();
//         n->h = i;
//         enqueue(q, n);
//         free(n);
//     }
//
//     //thread creation
//     for(int i = 0 ; i < t ; i++){
//         pthread_create(&threads[i], NULL, work_function, (void*) q);
//     }
//
//     //wait for all to finish
//     for(int i = 0 ; i < t ; i++){
//         pthread_join(threads[i], NULL);
//     }
//
//
//     free_queue(q);
//
//     return 0;
// }
