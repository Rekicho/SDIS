Used Code:

Peer.java -> function hexString -> Taken from https://stackoverflow.com/a/9855338
MCThread.java -> function deleteFolder -> Taken from https://stackoverflow.com/a/7768086

Compiling:

    cd src
	javac *.java

Executing:

    cd src

    Peers:

        java Peer <version> <peerID> <RemoteObjectName> <MC_ADDRESS> <MC_PORT> <MDB_ADDRESS> <MDB_PORT> <MDR_ADDRESS> <MDR_PORT>

    TestApp:

        java TestApp <RemoteObjectName> <sub_protocol> <opnd_1> <opnd_2>

Using Scripts:
    chmod +x *.sh

    ./compile.sh
            Compiles all the files in the file directory

    ./peers.sh <version> <initialID> <finalID>
            Opens a terminal and runs the Peer app for each id from initialId to finalId, both inclusive

    ./peer.sh <version> <peerId>
            Runs the peer with id 'peerId' and version 'version' in the current terminal

    ./test.sh <peerId> <sub_protocol> <operand1> <operand2>
            Runs the TestApp using as the initiater-peer the peer with id 'peerId'.
            It runs the command following the id: '<sub_protocol> <operand1> <operand2>'.

    ./reset.sh
            Deletes the persistent memory of the peers which allow a clean run