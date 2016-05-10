#include <stdlib.h>
#include "async.h"
#include "common.h"
#include "ajil_hash.h"

#define debug warn

#define MAX_CLIENTS 10

int num_clients;
int clients[MAX_CLIENTS];
hashtable groups_table;

void report () {
	int i;
	int buf_len;
	void *buf, *tmp;
	uint32_t group_addr;
	uint16_t num_groups, group_to_report, num_senders;
	hashtable senders_table;
	hash_elem *groups_itr, *senders_itr;

	num_groups = hash_count(groups_table);
	if (num_groups > 0) {
		group_to_report = rand() % num_groups;

		groups_itr = NULL;
		for (i=0; i < group_to_report; i++) {
			groups_itr = hash_get_next_group(groups_itr);
		}

		group_addr    = (uint32_t) groups_itr->key;
		senders_table = (hashtable) groups_itr->value;
		senders_itr   = hash_get_next_sender (senders_table, NULL);
		num_senders   = hash_count(senders_table);

		// report message will be
		// | Msg len | Group addr | payload entries
		buf_len = 2 + 4;
		// Payload entry has | Sender addr + port | Sender Traffic
		buf_len += num_senders * (4 + 2 + 2);

		buf = malloc(buf_len);
		memset(buf, 0, buf_len);

		tmp = buf;
		// copy msg length
		tmp = htons(buf_len);
		tmp += sizeof(uint16_t);
		// copy target group address
		tmp = htonl(group_addr);
		tmp += sizeof(uint32_t);
		// copy payload entries
		do {
			// sender address
			tmp = htonl( get_sender_addr(senders_itr) );
			tmp += sizeof(uint32_t);
			// sender port
			tmp = htons( get_sender_port(senders_itr) );
			tmp += sizeof(uint16_t);
			// sender traffic total
			tmp = htons( get_sender_total(senders_itr) );
			tmp += sizeof(uint16_t);
		} while ((senders_itr = hash_get_next_sender(senders_table, senders_itr)) != NULL);
		// done writing the fixed part report message

		// reset iterator
		senders_itr = hash_get_next_sender (senders_table, NULL);

		// send report message to all senders in the group
		for (i=0; i<num_clients; i++) {
			send(clients[i], buf, buflen, 0);
		}

		// Garbage cleanup
		free(buf);
		hash_free(senders_table);
		hash_remove_group(groups_table, group_addr);
	}
	delaycb (REPORT_PERIOD, 0, wrap(report));
}


void read_in (int fd) {
	unsigned int len;
	MSG_STAT *buf;

	hashtable group_stat;
	sender_key *skey;
	uint16_t *sender_total;

	buf = (MSG_STAT *) malloc(sizeof(MSG_STAT));
	memset (buf, 0, sizeof(MSG_STAT));

	if ((len = recvfrom (fd, (void *) buf, sizeof(MSG_STAT), 0, NULL, NULL)) < 0) {
		warn << "error on receiving from client. Error #" << errno << "\n";
	} else if (len < sizeof(MSG_STAT)) {
		warn << "received invalid stat message from client\n";
	} else {
		// update the per group/sender statistics to account for the new message

		// get the group statistics hash table for this group,
		// create a new one if none exists
		group_stat = hash_get_group (groups_table, buf->group_addr);
		if (group_stat == NULL) {
			group_stat = hash_create();
			hash_add_group (groups_table, buf->group_addr, group_stat);
		}

		// update the sender's stats in the group
		skey = (sender_key *) malloc(sizeof(sender_key));
		skey->addr = buf->sender_addr;
		skey->port = buf->sender_port;
		sender_total = hash_get_sender_stat (group_stat, skey);
		if (sender_total = NULL) {
			*sender_total = (uint16_t *) malloc(sizeof(uint16_t));
			*sender_total = buf->msg_len;
			hash_add_sender_stat (group_stat, skey, sender_total);
		} else {
			*sender_total = (*sender_total) + buf->msg_len;
		}
	}
}

void accept_connection (int fd) {
	int client_socket;
	struct sockaddr_in sin;
	unsigned int sinlen;

	sinlen = sizeof(sin);
	client_socket = accept(fd, (struct sockaddr *) &sin, &sinlen);
	if (client_socket >= 0) {
		clients[num_clients] = client_socket;
		// stop accepting connections if at MAX_CLIENTS
		num_clients++;
		if (num_clients == MAX_CLIENTS) {
			fdcb (fd, selread, NULL);
		}
	}

}

// Initialize the monitor and report sockets
void init_monitor() {
	int monitor_socket;		// listens for incoming sending stats
	int server_socket;		// ajil daemons connect to this socket to receive sending stats
	int opt = 1;

	// Create and bind the "monitor socket" to listen for incoming statistics
	// Create and bind the "server socket" that listens for incoming connections
	if ((monitor_socket = inetsocket(SOCK_DGRAM, MPORT))< 0) {
		fatal << "could not create monitor socket\n";
	}
	if ((server_socket = inetsocket(SOCK_STREAM, RPORT)) < 0) {
		fatal << "could not create server socket\n";
	}
	// Set reuse address on the sockets
	setsockopt (monitor_socket, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
	setsockopt (server_socket, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
	// Make the sockets asynchronous
	make_async(monitor_socket);
	make_async(server_socket);

	// Listen for incoming connections (report subscriptions)
	if (listen(server_socket, 5) < 0) {
		fatal << "error while listening on server socket\n";
	}
	// Set up a call back to accept connections
	fdcb (server_socket, selread, wrap(accept_connection, server_socket));

	// Set up a call back to read incoming reports
	fdcb (monitor_socket, selread, wrap(read_in, monitor_socket));

	// Start periodic call to report info if available
	delaycb (REPORT_PERIOD, 0, wrap(report));
}

int main() {
	num_clients = 0;
	groups_table = hash_create();

	async_init();
	init_monitor();
	warn << "monitor set up. Listening for connections\n";
	amain();
}
