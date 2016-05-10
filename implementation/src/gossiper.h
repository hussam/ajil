#ifndef __GOSSIPER_H
#define __GOSSIPER_H

#include "async.h"
#include "common.h"

int is_gossiping;

void init_gossiper();
void accept_connection(int fd);
void gossip();

typedef struct __gossip_state {
} gossip_state;

#endif
