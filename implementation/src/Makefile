DAEMON= ajild
MONITOR= ajil-monitor
LIBRARY= ajil.so

all: $(DAEMON) $(MONITOR) $(LIBRARY)

CFLAGS = -Wall -g -O2 -I/usr/local/include/sfslite -L/usr/local/lib/sfslite
LFLAGS = -lm -lasync -lresolv
CC = g++ $(CFLAGS)
BIN_DIR= ../bin

$(DAEMON): ajild.c
	$(CC) -o $(BIN_DIR)/$@ $^ $(LFLAGS)

$(MONITOR): monitor.c
	$(CC) -o $(BIN_DIR)/$@ $^ $(LFLAGS)

$(LIBRARY):
	make -f lib/Makefile

clean:
	rm -vf *.o *~ lib/*.o $(BIN_DIR)/$(DAEMON) $(BIN_DIR)/$(MONITOR); make -f lib/Makefile clean
