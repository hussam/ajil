#include "async.h"
#include <stdlib.h>
#include <string.h>

#define PORT 4096
#define ADDRESS "127.0.0.5"

void send_message (int fd) {
	int len;
	char *message;
	struct sockaddr_in sin;

	len = (rand() % 5) + 3;
	message = (char *) malloc(len);
	memset (message, 'A', len);

	memset (&sin, 0, sizeof(struct sockaddr_in));
	sin.sin_family = AF_INET;
	sin.sin_addr.s_addr = inet_addr(ADDRESS);
	sin.sin_port = htons(PORT);

	sendto (fd, (void *) message, len, 0, (struct sockaddr *) &sin, sizeof(sin));

	// send a message every 0.5 seconds
	delaycb (0, 500000000, wrap(send_message, fd));
}

int main() {
	int fd;
	int opt = 1;

	async_init();

	if ((fd = socket(AF_INET, SOCK_DGRAM, 0)) < 0) {
		fatal << "Could not create socket. Error #" << errno << "\n";
	}
	setsockopt (fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
	make_async(fd);

	delaycb (1, 0, wrap(send_message, fd));

	amain();
}
