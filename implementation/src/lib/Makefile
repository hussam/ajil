CC=gcc
CFLAGS=-ggdb3 -rdynamic -Wall -O3 #-pg #-pedantic #-O2
LDFLAGS=-lm  -ldl -nostartfiles -fpic -shared #-pg
SRC_DIR= lib
BIN_DIR= ../bin/lib
LIBRARY= ajil.so

$(LIBRARY): $(SRC_DIR)/ajillib.o
	$(CC) $(CFLAGS) $(LDFLAGS) -o $(BIN_DIR)/$@ $(SRC_DIR)/ajillib.o

ajillib.o: $(SRC_DIR)/ajillib.c
	$(CC) $(CFLAGS) -c $^ -o $@

clean:
	rm -vf *.o *~ $(BIN_DIR)/$(LIBRARY)

