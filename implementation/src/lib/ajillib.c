#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <string.h>
#include <unistd.h>
#include <netdb.h>
#include <netinet/in.h>
#include <sys/socket.h>
#include <sys/select.h>
#include <sys/types.h>
#include <sys/time.h>
#include <arpa/inet.h>
#include <assert.h>
#include <errno.h>
#include <time.h>
#include <sys/mman.h>
#define __USE_GNU
#include <dlfcn.h>
#define _GNU_SOURCE
#include <sched.h>
#include <signal.h>

#include "common.h"

#define DADDRESS "127.0.0.1"
#define addr_len sizeof(uint32_t)
#define port_len sizeof(uint16_t)
#define addr_port_len sizeof(uint32_t) + sizeof(uint16_t)

int ctrl_socket;
uint16_t data_port;


/* Handle initialization */
void __attribute__ ((constructor)) _init(void);
void __attribute__ ((destructor)) _fini(void);


/* Original function pointers. Will be filled in by init()  */
ssize_t (*orig_sendto)(int s, const void *buf, size_t len, int flags,
		const struct sockaddr *to, socklen_t tolen);
int (*orig_setsockopt)(int s, int level, int optname,
		const void *optval, socklen_t optlen);
ssize_t (*orig_recvfrom)(int s, void *buf, size_t len, int flags,
		struct sockaddr *from, socklen_t *fromlen);
int (*orig_bind)(int sockfd, const struct sockaddr *my_addr, socklen_t addrlen);

/*** FUNCTIONS ***/


/* Take a copy of the original functions */
void *preload(char *name)
{
	void *ptr = dlsym (RTLD_NEXT, name);
	if (dlerror() != NULL)
	{
		printf ("Loading %s: %s\n", name, dlerror());
		exit (-1);
	}
	return ptr;
}

/* Initialize module */
void __attribute__ ((constructor)) _init(void)
{
	int opt = 1;
	struct sockaddr_in sin;

	orig_sendto = preload("sendto");
	orig_recvfrom = preload("recvfrom");
	orig_setsockopt = preload("setsockopt");
	orig_bind = preload("bind");

	ctrl_socket = socket (AF_INET, SOCK_STREAM, IPPROTO_TCP);
	if (ctrl_socket < 0) {
		printf ("Ajil lib failed to create control socket\n");
		exit (-1);
	}
	orig_setsockopt (ctrl_socket, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));

	memset (&sin, 0, sizeof(struct sockaddr_in));
	sin.sin_family = AF_INET;
	sin.sin_addr.s_addr = inet_addr(DADDRESS);
	sin.sin_port = htons(DPORT);

	if (connect (ctrl_socket, (struct sockaddr *) &sin, sizeof(struct sockaddr_in)) < 0) {
		printf ("Could not connect to Ajil daemon\n");
		exit (-1);
	} else {
		printf ("Connected to Ajil daemon\n");
		usleep (500);
		if (recv (ctrl_socket, (void *) &data_port, sizeof(uint16_t), 0) < 0) {
			printf ("Error receiving port number from daemon\n");
			exit (-1);
		}
	}
}

/* Clean-up */
void __attribute__ ((destructor)) _fini(void)
{
	close(ctrl_socket);
}

/* Modified sendto() */
ssize_t sendto(int s, const void *buf, size_t len, int flags, const struct sockaddr *to, socklen_t tolen)
{
	void *new_buf;
	struct sockaddr_in sin, *orig_to;

	orig_to = (struct sockaddr_in *) to;

	// Create new buffer prefixed with address and port to send to the daemon
	new_buf = malloc(len + addr_port_len);
	memcpy(new_buf, &((orig_to->sin_addr).s_addr), addr_len);
	memcpy(new_buf + addr_len, &((orig_to->sin_port)), port_len);
	memcpy(new_buf + addr_port_len, buf, len);

	// Set up new destination modified packet
	memset (&sin, 0, sizeof(struct sockaddr_in));
	sin.sin_family = orig_to->sin_family;
	sin.sin_port = htons(data_port);
	sin.sin_addr.s_addr = inet_addr(DADDRESS);

	return orig_sendto(s, new_buf, len + addr_port_len, flags, (struct sockaddr *) &sin, sizeof(sin));
}

/* Overload setsockopt */
int setsockopt(int s, int level, int optname, const void *optval, socklen_t optlen)
{
	printf("Called setsockopt\n");
	return orig_setsockopt(s, level, optname, optval, optlen);
}


/* Overload recvfrom */
ssize_t recvfrom(int s, void *buf, size_t len, int flags, struct sockaddr *from, socklen_t *fromlen)
{
	printf("Called recvfrom\n");
	return orig_recvfrom(s, buf, len, flags, from, fromlen);
}


/* Overload bind */
int bind(int sockfd, const struct sockaddr *my_addr, socklen_t addrlen) {
	printf("Called bind\n");
	return orig_bind(sockfd, my_addr, addrlen);
}
