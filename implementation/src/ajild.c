#include "async.h"
#include "common.h"
//#include "params.h"

#define debug warn
#define MONITOR_ADDR "127.0.0.1"

#define MAX_SOCKETS 10
#define BUF_SIZE (2*1024*1204) + 4 + 2	// buffer size is 2MB + 4 bytes for group address prefix + 2 bytes for port
#define REPORT_BUF_SIZE 518				// group addr + # entries + 64 entries

// XXX: HACK
#define MAX_RATE 10
int epoch_sent = 0;
int quota_left = MAX_RATE;
int unreported_read = 0;
int unreported_sent = 0;
int tick = 0;

// Reporting
int is_reporting = 0;

struct out_msg {
	uint32_t *address;
	uint16_t *port;
	void *buf;
	unsigned int len;
	struct out_msg *next;
};

struct out_queue {
	struct out_msg *head, *tail;
};

int num_clients;
int writing_enabled[MAX_SOCKETS];
struct out_queue* out_queues[MAX_SOCKETS];
hashtable groups_stats;

// TODO: figure out how to receive information from monitor daemons. Should a
// TCP or UDP socket be used ?


// XXX: HACK
void report() {
	warn << tick << "\t" << unreported_read << "\t" << unreported_sent << "\t|\t" << epoch_sent << "\t" << quota_left << "\n";
	unreported_sent = 0;
	unreported_read = 0;
	tick++;
	delaycb (1, 0, wrap(report));
}

void write_out (int fd, int q_num) {
	struct sockaddr_in addr;
	struct out_msg *msg;

	// XXX: HACK
	if (out_queues[q_num]->head->len > quota_left) {
		fdcb (fd, selwrite, NULL);
		writing_enabled[q_num] = 0;
		return;
	}

	memset (&addr, 0, sizeof(struct sockaddr_in));
	msg = out_queues[q_num]->head;

	addr.sin_family = AF_INET;
	addr.sin_addr.s_addr = *(msg->address);
	addr.sin_port = *(msg->port);

	if (sendto (fd, msg->buf, msg->len, 0, (struct sockaddr *) &addr, sizeof(struct sockaddr_in)) < 0) {
		warn << "error on resending message. Error #" << errno << "\n";
	} else {
		//debug << "sent " << msg->len << "\n";
		epoch_sent += msg->len;			// XXX: HACK
		unreported_sent += msg->len;	// XXX: HACK
		quota_left -= msg->len;			// XXX: HACK
		out_queues[q_num]->head = msg->next;
		// unregister the write call back if nothing left to write
		if (out_queues[q_num]->head == NULL) {
			out_queues[q_num]->tail = NULL;
			fdcb (fd, selwrite, NULL);
			writing_enabled[q_num] = 0;
		}
		/*
		free(msg->address);
		free(msg->port);
		free(msg->buf);
		*/
		free(msg);
	}
}

void read_in (int fd, int q_num) {
	unsigned int len;
	void *buf;
	struct out_msg *new_msg;

	buf = malloc(BUF_SIZE);
	memset (buf, 0, BUF_SIZE);

	if ((len = recvfrom (fd, buf, BUF_SIZE, 0, NULL, NULL)) < 0) {
		warn << "error on receiving from client. Error #" << errno << "\n";
	} else if (len <= sizeof(uint32_t) + sizeof(uint16_t)) {
		warn << "received invalid message from client\n";
	} else {
		// adjust the length to reflect the actual message length
		len -= sizeof(uint32_t) + sizeof(uint16_t);	// remove address and port length

		//debug << "received " << len << "\n";
		unreported_read += len;	// XXX: HACK

		// Add new incoming message to the queue of outgoing messages
		new_msg = (struct out_msg *) malloc(sizeof(struct out_msg));
		memset(new_msg, 0, sizeof(struct out_msg));

		new_msg->address = (uint32_t *) buf;
		new_msg->port = (uint16_t *) (((char *) buf) + sizeof(uint32_t));
		new_msg->buf = ((char *) buf) + sizeof(uint32_t) + sizeof(uint16_t);
		new_msg->len = len;
		new_msg->next = NULL;		// redundant

		if (out_queues[q_num]->head == NULL) {
			out_queues[q_num]->head = new_msg;
		} else {
			out_queues[q_num]->tail->next = new_msg;
		}
		out_queues[q_num]->tail = new_msg;

		//debug << "Received: " << (char *) new_msg->buf << " :: On port " << DPORT + 1 + q_num << "\n";

		// Finally set up a call back to write outgoing messages
		if (!writing_enabled[q_num]) {
			fdcb (fd, selwrite, wrap(write_out, fd, q_num));
			writing_enabled[q_num] = 0;
		}
	}
}


// XXX: HACK
void check (int fd, int q_num) {
	if (out_queues[q_num]->head != NULL) {
		fdcb (fd, selwrite, wrap(write_out, fd, q_num));
	}
	quota_left = MAX_RATE;
	epoch_sent = 0;
	//debug << "----------------------------------------\n";
	delaycb (1, 0, wrap(check, fd, q_num));
}

void send_client_port (int ctrl_fd, int data_fd, uint16_t port) {
	// set up an out message queue for this client
	int q_num = port - DPORT - 1;
	out_queues[q_num] = (struct out_queue*) malloc(sizeof(struct out_queue));
	memset(out_queues[q_num], 0, sizeof(out_queue));
	writing_enabled[q_num] = 0;

	// unregister the call back on the write socket
	fdcb (ctrl_fd, selwrite, NULL);
	// register a reader call back for the data socket
	fdcb (data_fd, selread, wrap(read_in, data_fd, port - DPORT - 1));
	// send the port number for the client data socket
	send (ctrl_fd, &port, sizeof(uint16_t), 0);

	// XXX: HACK
	delaycb (1, 0, wrap(check, data_fd, q_num));
}

void accept_connection (int fd) {
	int ctrl_socket, data_socket, bufs;
	struct sockaddr_in sin;
	unsigned int sinlen;

	sinlen = sizeof(sin);
	ctrl_socket = accept(fd, (struct sockaddr *) &sin, &sinlen);
	if (ctrl_socket >= 0) {
		num_clients++;
		make_async(ctrl_socket);

		//  create a UDP socket to handle client messages
		data_socket = inetsocket(SOCK_DGRAM, DPORT + num_clients);
		if (data_socket < 0) {
			warn << "could not create UDP data socket for client\n";
		} else {
			warn << "accepted connection, bound to port " << DPORT + num_clients << "\n";
			bufs = BUF_SIZE;
			if ( setsockopt(data_socket, IPPROTO_IP, SO_RCVBUF, &bufs, sizeof(int)) < 0 ||
					setsockopt(data_socket, IPPROTO_IP, SO_SNDBUF, &bufs, sizeof(int)) < 0 ) {
				warn << "could not set buffer size for client UDP data socket\n";
				close(data_socket);
			} else {
				make_async(data_socket);
				// Now notify the client of the port number they should use
				fdcb (ctrl_socket, selwrite, wrap(send_client_port, ctrl_socket, data_socket, DPORT + num_clients));
			}
			// TODO: find a suitable position to turn on reporting
			if (!is_reporting) {
				delaycb (1, 0, wrap(report));
				is_reporting = 1;
			}
		}
	}
}

void read_in_stats(int fd) {
	unsigned int len;
	void *buf;
	uint16_t msg_len;

	buf = malloc(REPORT_BUF_SIZE);
	memset(buf, 0, REPORT_BUF_SIZE);

	if ((len = recv(fd, (void *) &msg_len, 2, 0)) < 0) {
		warn << "error receiving monitor report\n";
	} else if (len == 0) {
		warn << "monitor closed connection\n";
		fdcb (fd, selread, NULL);
		close(fd);
		return;
	} else if (len < 2) {
		warn << "received less than 2 bytes for monitor report message length\n";
		// TODO: continue here
		// XXX: also to think about ...how to detect which multicast group was a
		// received packed sent to ? so, should monitors be mounted on senders
		// rather than receivers ?
	}
}

void connect_to_monitor(int fd) {
	fdcb (fd, selread, wrap(read_in_stats, fd));
}

// Initialize the daemon socket
void init_server() {
	int server_socket;
	int opt = 1;

	// Create and bind the "server socket" to listen for incoming connections
	server_socket = inetsocket(SOCK_STREAM, DPORT);
	if (server_socket < 0) {
		fatal << "could not create server socket\n";
	}
	// Set reuse address on the socket
	setsockopt (server_socket, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
	// Make the socket asynchronous
	make_async(server_socket);
	// Listen for incoming connections
	if (listen(server_socket, 5) < 0) {
		fatal << "error while listening on server socket\n";
	}

	// Connect to monitors
	tcpconnect(MONITOR_ADDR, RPORT, wrap(connect_to_monitor));

	// Set up a call back to accept connections
	fdcb (server_socket, selread, wrap(accept_connection, server_socket));
}

int main() {
	num_clients = 0;
	groups_stats = hash_create();

	async_init();
	init_server();
	warn << "server set up. Listening for connections\n";
	amain();
}
