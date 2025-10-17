#include <stdio.h>
#include <omp.h>

int main(void){
    
    int i;

    #pragma omp parallel for private(i)
    for(i = 0 ; i < 3 ; i++){
        printf("i = %d\n", i);
    }
    
}
