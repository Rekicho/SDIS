# GLOBAL INFORMATION
ACCESS_POINT="PeerRMI"

# CLIENT INFORMATION
APP_NAME="TestApp"

# USER SPECIFCATION
VERSION_APP=${1:-"1.0"}
FROM_PEER_ID=${2:-1}
TO_PEER_ID=${3:-$FROM_PEER_ID}


# RUN THE PEERS
for i in $(seq $FROM_PEER_ID $TO_PEER_ID)
do 
	gnome-terminal -- ./peer.sh $VERSION_APP $i
done