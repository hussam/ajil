#include "hash.h"

#define HASH_SIZE 97		// Must be prime .. others to try: 53, 67, 73, 101, 139, 193
#define HASH(key) key % HASH_SIZE

// Create hashtable
hashtable hash_create()
{
	int i;
	hashtable htable = (hashtable) malloc(HASH_SIZE * sizeof(hash_elem *));

	for (i=0; i<HASH_SIZE; i++)
		htable[i] = NULL;

	return htable;
}

// Destroy hashtable
void hash_destroy(hashtable htable)
{
	int i;
	hash_elem *tmp;

	for (i=0; i<HASH_SIZE; i++) {
		if (htable[i] != NULL) {
			for (tmp = htable[i]; htable[i] != NULL; ) {
				free(tmp->value);			// free the memory of the stored value
				htable[i] = tmp->next;	// let the hashtable bucket point to the next element instead of the current one
				free(tmp);					// free the hash_elem container itself
			}
		}
	}
}

// Add key/value pair to the hashtable
void hash_add(hashtable htable, hashkey_t key, void *value, size_t value_len)
{
	hash_elem *new_elem;

	new_elem = (hash_elem *) malloc(sizeof(hash_elem));

	new_elem->key = key;
	new_elem->value = value;
	new_elem->value_len = value_len;
	new_elem->next = htable[HASH(key)];

	htable[HASH(key)] = new_elem;
}

// Remove first instance of the referenced key/value pair from hashtable
void hash_remove(hashtable htable, hashkey_t key)
{
	hash_elem *match, *first_match; *last_match;

	first_match = htable[HASH(key)];
	for (match = first_match; match != NULL; match = match->next) {
		if (match->key == key) {
			free(match->value);
			if (match == first_match) {	// special case if first match
				htable[HASH(key)] = match->next;
			} else {
				last_match->next = match->next;
			}
			free(match);
		} else {
			last_match = match;
		}
	}
}

// Get value associated with given key in the hashtable
// If value_len is not null, it will be set to the value of value_len of the
// found key (if found)
void *hash_get(hashtable htable, hashkey_t key, size_t *value_len)
{
	hash_elem *match;
	for (match = htable[HASH(key)]; match != NULL; match = match->next) {
		if (match->key == key) {
			if (value_len != NULL) {
				*value_len = match->value_len;
			}
			return match->value;
		}
	}
	return NULL:
}

// Get the first element in the hashtable
hash_elem *hash_first_elem(hashtable htable)
{
	int i;
	for (i=0; i<HASH_SIZE; i++) {
		if (hashlist[i] != NULL) {
			return hashlist[i];
		}
	}
	return NULL;
}

// Get the next element from the given key
hash_elem *hash_next_elem(hashtable htable, hash_elem *helem)
{
	int i;

	if (helem == NULL)
		return hash_first_elem(htable);

	if (helem->next != NULL) {
		return helem->next;
	} else {
		i = HASH(helem->key);
		for (++i; i < HASH_SIZE; i++) {
			if (hashlist[i] != NULL) {
				return hashlist[i];
			}
		}
		return NULL;
	}
}

// Count the number of elements in the hashtable
int hash_count(hashtable htable)
{
	int i, count;
	hash_elem *tmp;

	count = 0;
	for (i = 0 ; i < HASH_SIZE; i++) {
		for (tmp = htable[i]; tmp != NULL; tmp = tmp->next) {
			count++;
		}
	}
	return count;
}
