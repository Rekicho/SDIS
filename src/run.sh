# GLOBAL INFORMATION
ACCESS_POINT="PeerRMI"

# PEER INFORMATION
VERSION_APP="1.0"
PEER_ID="1"

# CLIENT INFORMATION
APP_NAME="TestApp"

# USER SPECIFCATION
FROM_PEER_ID=${1:-1}
TO_PEER_ID=${2:-$FROM_PEER_ID}


# SETUP
start rmiregistry
sleep 1

# RUN THE PEERS
for i in $(seq $FROM_PEER_ID $TO_PEER_ID)
do 
	start ./peer.sh $i
done

start ./client.sh BACKUP test1.pdf 3
# start ./client.sh RESTORE test1.pdf
# start ./client.sh DELETE test1.pdf
# start ./client.sh RECLAIM 0
# start ./client.sh STATE

$SHELL