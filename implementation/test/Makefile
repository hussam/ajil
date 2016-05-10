all: send recv

CFLAGS = -Wall -g -O2 -I/usr/local/include/sfslite -L/usr/local/lib/sfslite
LFLAGS = -lm -lasync -lresolv
CC = g++ $(CFLAGS)

send: send.c
	$(CC) -o $@ $^ $(LFLAGS)

recv: recv.c
	$(CC) -o $@ $^ $(LFLAGS)

clean:
	rm -fr *.o *~ send recv
