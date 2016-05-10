#ifndef COMMON_H
#define COMMON_H

#define MPORT 5000	// Monitor Port
#define RPORT 5001	// Monitor Reporter Port
#define LPORT 5002	// Listener Port
#define DPORT 5003	// Daemon Port

#define REPORT_PERIOD 2		// how often do monitors send reports

typedef struct _msg_stat {
	uint16_t sender_port;
	uint32_t sender_addr;
	uint32_t group_addr;
	uint64_t msg_len;
} MSG_STAT;

#endif
