# GLOBAL INFORMATION
ACCESS_POINT="PeerRMI"

# PEER INFORMATION
VERSION_APP="1.0"
PEER_ID="1"

# CLIENT INFORMATION
APP_NAME="TestApp"

# SETUP
gnome-terminal -e "rmiregistry"
sleep 1

# RUN THE PEERS
for i in 1
do 
	gnome-terminal -- ./peer.sh $i
done

