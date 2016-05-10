#include "async.h"
#include <string.h>

#define PORT 4096
#define ADDRESS "127.0.0.5"
#define BUF_SIZE 100

void recv_message (int fd) {
	int len;
	void *buf;

	buf = malloc(BUF_SIZE);
	memset (buf, 0, BUF_SIZE);

	if ((len = recv (fd, buf, BUF_SIZE, 0)) < 0) {
		warn << "**error on receiving. Error #" << errno << "\n";
	}

	warn << (char *) buf << " (" << len << ")\n";
}

int main() {
	int fd;
	int opt = 1;

	async_init();

	if ((fd = inetsocket(SOCK_DGRAM, PORT)) < 0 ) {
		fatal << "Could not create socket. Error #" << errno << "\n";
	}
	setsockopt (fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));
	make_async(fd);

	fdcb (fd, selread, wrap(recv_message, fd));

	amain();
}
