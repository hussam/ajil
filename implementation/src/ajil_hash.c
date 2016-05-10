#ifndef AJIL_HASH_H
#define AJIL_HASH_H

#include "hash.h"

#define KEY(k) ((uint64_t) k)
#define SKEY(k) ((uint64_t(k->port) << 32) | uint64_t(k->addr))

typedef struct _sender_key {
	uint32_t addr;
	uint16_t port;
} sender_key;

/* retreive sender information from hash table entry */
inline uint32_t get_sender_addr (HASH_ELEM *hashentry) {
	return (uint32_t) (hashentry->key & 0x00000000ffffffff);
}
inline uint16_t get_sender_port (HASH_ELEM *hashentry) {
	return (uint16_t) ((hashentry->key & 0x0000ffff00000000) >> 32);
}
inline uint16_t get_sender_total (HASH_ELEM *hashentry) {
	return &((uint16_t *) (hashentry->value));
}


#endif
