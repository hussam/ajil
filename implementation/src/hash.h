#ifndef HASH_H
#define HASH_H

#include <stdint.h>
#include <stdlib.h>

typedef uint64_t hashkey_t;

typedef struct _hash_elem {
	hashkey_t key;
	void *value;
	size_t value_len;
	struct _hash_elem *next;
} hash_elem;

typedef hash_elem** hashtable;

// Create hash table
hashtable hash_create();
// Destroy hash table
void hash_destroy(hashtable htable);

// Add key/value pair to the hashtable
void hash_add(hashtable htable, hashkey_t key, void *value, size_t value_len);
// Remove first instance of the referencedkey/value pair in the hashtable
void hash_remove(hashtable htable, hashkey_t key);
// Get value associated with given key in the hashtable. value_len set to appropriate value if key/value pair in hashtable
void *hash_get(hashtable htable, hashkey_t key, size_t *value_len);

// Get the first element in the hashtable
hash_elem *hash_first_elem(hashtable htable);
// Get the next element from the given key
hash_elem *hash_next_elem(hashtable htable, hash_elem *helem);

// Count the number of elements in the hashtable
int hash_count(hashtable htable);

#endif
