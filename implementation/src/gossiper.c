#include "gossiper.h"

void init_gossiper() {
	int gossip_socket;
	int opt = 1;

	is_gossiping = 0;

	// Create and bind the gossip socket to listen for incoming connections
	gossip_socket = inetsocket(SOCK_STREAM, DPORT);
	if (gossip_socket < 0) {
		fatal << "could not create gossip socket\n";
	}
	// Set reuse address on the socket
	setsockopt (gossip_socket, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
	// Make the socket asynchronous
	make_async(gossip_socket);
	// Listen for incoming connections
	if (listen(gossip_socket, 5) < 0) {
		fatal << "error while listening on gossip socket\n";
	}

	// Set up a call back to accept gossip connections
	fdcb (gossip_socket, selread, wrap(accept_connection, gossip_socket));
}

void accept_connection (int fd) {
	int client_socket, bufs;
	struct sockaddr_in sin;
	unsigned int sinlen;

	sinlen = sizeof(sin);
	client_socket = accept(fd, (struct sockaddr *) &sin, &sinlen);
	if (client_socket >= 0) {
		make_async(client_socket);

		// TODO: find a suitable position to turn on reporting
		if (!is_gossipng) {
			delaycb (1, 0, wrap(gossip));
			is_reporting = 1;
		}
	}
}

void gossip() {
	// TODO: do gossiping
}
